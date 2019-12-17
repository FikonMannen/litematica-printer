package fi.dy.masa.litematica.render.schematic;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.primitives.Doubles;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.render.schematic.BufferBuilderCache;
import fi.dy.masa.litematica.render.schematic.ChunkRenderDataSchematic;
import fi.dy.masa.litematica.render.schematic.ChunkRenderTaskSchematic;
import fi.dy.masa.litematica.render.schematic.ChunkRenderWorkerLitematica;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.apache.logging.log4j.Logger;

public class ChunkRenderDispatcherLitematica {
    private static final Logger LOGGER = Litematica.logger;
    private static final ThreadFactory THREAD_FACTORY = new ThreadFactoryBuilder().setNameFormat("Litematica Chunk Batcher %d").setDaemon(true).build();
    private final List<Thread> listWorkerThreads = Lists.newArrayList();
    private final List<ChunkRenderWorkerLitematica> listThreadedWorkers = new ArrayList<ChunkRenderWorkerLitematica>();
    private final PriorityBlockingQueue<ChunkRenderTaskSchematic> queueChunkUpdates = Queues.newPriorityBlockingQueue();
    private final BlockingQueue<BufferBuilderCache> queueFreeRenderBuilders;
    private final Queue<PendingUpload> queueChunkUploads = Queues.newPriorityQueue();
    private final ChunkRenderWorkerLitematica renderWorker;
    private final int countRenderBuilders;
    private Vec3d cameraPos;

    public ChunkRenderDispatcherLitematica() {
        int i;
        int threadLimitMemory = Math.max(1, (int)((double)Runtime.getRuntime().maxMemory() * 0.3) / 10485760);
        int threadLimitCPU = Math.max(1, MathHelper.clamp((int)Runtime.getRuntime().availableProcessors(), (int)1, (int)(threadLimitMemory / 5)));
        this.countRenderBuilders = MathHelper.clamp((int)(threadLimitCPU * 10), (int)1, (int)threadLimitMemory);
        this.cameraPos = Vec3d.ZERO;
        if (threadLimitCPU > 1) {
            for (i = 0; i < threadLimitCPU; ++i) {
                ChunkRenderWorkerLitematica worker = new ChunkRenderWorkerLitematica(this);
                Thread thread = THREAD_FACTORY.newThread(worker);
                thread.start();
                this.listThreadedWorkers.add(worker);
                this.listWorkerThreads.add(thread);
            }
        }
        this.queueFreeRenderBuilders = Queues.newArrayBlockingQueue((int)this.countRenderBuilders);
        for (i = 0; i < this.countRenderBuilders; ++i) {
            this.queueFreeRenderBuilders.add(new BufferBuilderCache());
        }
        this.renderWorker = new ChunkRenderWorkerLitematica(this, new BufferBuilderCache());
    }

    public void setCameraPosition(Vec3d cameraPos) {
        this.cameraPos = cameraPos;
    }

    public Vec3d getCameraPos() {
        return this.cameraPos;
    }

    public String getDebugInfo() {
        return this.listWorkerThreads.isEmpty() ? String.format("pC: %03d, single-threaded", this.queueChunkUpdates.size()) : String.format("pC: %03d, pU: %1d, aB: %1d", this.queueChunkUpdates.size(), this.queueChunkUploads.size(), this.queueFreeRenderBuilders.size());
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean runChunkUploads(long finishTimeNano) {
        boolean processedTask;
        boolean ranTasks = false;
        do {
            Object generator;
            processedTask = false;
            if (this.listWorkerThreads.isEmpty() && (generator = this.queueChunkUpdates.poll()) != null) {
                try {
                    this.renderWorker.processTask((ChunkRenderTaskSchematic)generator);
                    processedTask = true;
                }
                catch (InterruptedException var8) {
                    LOGGER.warn("Skipped task due to interrupt");
                }
            }
            generator = this.queueChunkUploads;
            synchronized (generator) {
                if (!this.queueChunkUploads.isEmpty()) {
                    this.queueChunkUploads.poll().uploadTask.run();
                    processedTask = true;
                    ranTasks = true;
                }
            }
        } while (finishTimeNano != 0L && processedTask && finishTimeNano >= System.nanoTime());
        return ranTasks;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean updateChunkLater(ChunkRendererSchematicVbo renderChunk) {
        boolean flag1;
        renderChunk.getLockCompileTask().lock();
        try {
            final ChunkRenderTaskSchematic generator = renderChunk.makeCompileTaskChunkSchematic(this::getCameraPos);
            generator.addFinishRunnable(new Runnable(){

                @Override
                public void run() {
                    ChunkRenderDispatcherLitematica.this.queueChunkUpdates.remove(generator);
                }
            });
            boolean flag = this.queueChunkUpdates.offer(generator);
            if (!flag) {
                generator.finish();
            }
            flag1 = flag;
        }
        finally {
            renderChunk.getLockCompileTask().unlock();
        }
        return flag1;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean updateChunkNow(ChunkRendererSchematicVbo chunkRenderer) {
        boolean flag;
        chunkRenderer.getLockCompileTask().lock();
        try {
            ChunkRenderTaskSchematic generator = chunkRenderer.makeCompileTaskChunkSchematic(this::getCameraPos);
            try {
                this.renderWorker.processTask(generator);
            }
            catch (InterruptedException interruptedException) {
                // empty catch block
            }
            flag = true;
        }
        finally {
            chunkRenderer.getLockCompileTask().unlock();
        }
        return flag;
    }

    public void stopChunkUpdates() {
        this.clearChunkUpdates();
        ArrayList<BufferBuilderCache> list = new ArrayList<BufferBuilderCache>();
        while (list.size() != this.countRenderBuilders) {
            this.runChunkUploads(Long.MAX_VALUE);
            try {
                list.add(this.allocateRenderBuilder());
            }
            catch (InterruptedException interruptedException) {}
        }
        this.queueFreeRenderBuilders.addAll(list);
    }

    public void freeRenderBuilder(BufferBuilderCache builderCache) {
        this.queueFreeRenderBuilders.add(builderCache);
    }

    public BufferBuilderCache allocateRenderBuilder() throws InterruptedException {
        return this.queueFreeRenderBuilders.take();
    }

    public ChunkRenderTaskSchematic getNextChunkUpdate() throws InterruptedException {
        return this.queueChunkUpdates.take();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean updateTransparencyLater(ChunkRendererSchematicVbo renderChunk) {
        boolean flag;
        renderChunk.getLockCompileTask().lock();
        try {
            final ChunkRenderTaskSchematic generator = renderChunk.makeCompileTaskTransparencySchematic(this::getCameraPos);
            if (generator == null) {
                boolean flag2;
                boolean bl = flag2 = true;
                return bl;
            }
            generator.addFinishRunnable(new Runnable(){

                @Override
                public void run() {
                    ChunkRenderDispatcherLitematica.this.queueChunkUpdates.remove(generator);
                }
            });
            flag = this.queueChunkUpdates.offer(generator);
        }
        finally {
            renderChunk.getLockCompileTask().unlock();
        }
        return flag;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public ListenableFuture<Object> uploadChunkBlocks(final RenderLayer layer, final BufferBuilder buffer, final ChunkRendererSchematicVbo renderChunk, final ChunkRenderDataSchematic chunkRenderData, final double distanceSq) {
        if (MinecraftClient.getInstance().isOnThread()) {
            this.uploadVertexBuffer(buffer, renderChunk.getBlocksVertexBufferByLayer(layer));
            return Futures.immediateFuture(null);
        }
        ListenableFutureTask futureTask = ListenableFutureTask.create((Runnable)new Runnable(){

            @Override
            public void run() {
                ChunkRenderDispatcherLitematica.this.uploadChunkBlocks(layer, buffer, renderChunk, chunkRenderData, distanceSq);
            }
        }, null);
        Queue<PendingUpload> queue = this.queueChunkUploads;
        synchronized (queue) {
            this.queueChunkUploads.add(new PendingUpload((ListenableFutureTask<Object>)futureTask, distanceSq));
            return futureTask;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public ListenableFuture<Object> uploadChunkOverlay(final ChunkRendererSchematicVbo.OverlayRenderType type, final BufferBuilder buffer, final ChunkRendererSchematicVbo renderChunk, final ChunkRenderDataSchematic compiledChunk, final double distanceSq) {
        if (MinecraftClient.getInstance().isOnThread()) {
            this.uploadVertexBuffer(buffer, renderChunk.getOverlayVertexBuffer(type));
            return Futures.immediateFuture(null);
        }
        ListenableFutureTask futureTask = ListenableFutureTask.create((Runnable)new Runnable(){

            @Override
            public void run() {
                ChunkRenderDispatcherLitematica.this.uploadChunkOverlay(type, buffer, renderChunk, compiledChunk, distanceSq);
            }
        }, null);
        Queue<PendingUpload> queue = this.queueChunkUploads;
        synchronized (queue) {
            this.queueChunkUploads.add(new PendingUpload((ListenableFutureTask<Object>)futureTask, distanceSq));
            return futureTask;
        }
    }

    private void uploadVertexBuffer(BufferBuilder buffer, VertexBuffer vertexBuffer) {
        vertexBuffer.submitUpload(buffer);
    }

    public void clearChunkUpdates() {
        while (!this.queueChunkUpdates.isEmpty()) {
            ChunkRenderTaskSchematic generator = this.queueChunkUpdates.poll();
            if (generator == null) continue;
            generator.finish();
        }
    }

    public boolean hasChunkUpdates() {
        return this.queueChunkUpdates.isEmpty() && this.queueChunkUploads.isEmpty();
    }

    public void stopWorkerThreads() {
        this.clearChunkUpdates();
        for (ChunkRenderWorkerLitematica worker : this.listThreadedWorkers) {
            worker.notifyToStop();
        }
        for (Thread thread : this.listWorkerThreads) {
            try {
                thread.interrupt();
                thread.join();
            }
            catch (InterruptedException interruptedexception) {
                LOGGER.warn("Interrupted whilst waiting for worker to die", (Throwable)interruptedexception);
            }
        }
        this.queueFreeRenderBuilders.clear();
    }

    public boolean hasNoFreeRenderBuilders() {
        return this.queueFreeRenderBuilders.isEmpty();
    }

    public static class PendingUpload
    implements Comparable<PendingUpload> {
        private final ListenableFutureTask<Object> uploadTask;
        private final double distanceSq;

        public PendingUpload(ListenableFutureTask<Object> uploadTaskIn, double distanceSqIn) {
            this.uploadTask = uploadTaskIn;
            this.distanceSq = distanceSqIn;
        }

        @Override
        public int compareTo(PendingUpload other) {
            return Doubles.compare((double)this.distanceSq, (double)other.distanceSq);
        }
    }

}
