package fi.dy.masa.litematica.render.schematic;

import com.google.common.collect.Lists;
import com.google.common.primitives.Doubles;
import fi.dy.masa.litematica.render.schematic.BufferBuilderCache;
import fi.dy.masa.litematica.render.schematic.ChunkRenderDataSchematic;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import net.minecraft.util.math.Vec3d;

public class ChunkRenderTaskSchematic
implements Comparable<ChunkRenderTaskSchematic> {
    private final ChunkRendererSchematicVbo chunkRenderer;
    private final Type type;
    private final List<Runnable> listFinishRunnables = Lists.newArrayList();
    private final ReentrantLock lock = new ReentrantLock();
    private final Supplier<Vec3d> cameraPosSupplier;
    private final double distanceSq;
    private BufferBuilderCache bufferBuilderCache;
    private ChunkRenderDataSchematic chunkRenderData;
    private Status status = Status.PENDING;
    private boolean finished;

    public ChunkRenderTaskSchematic(ChunkRendererSchematicVbo renderChunkIn, Type typeIn, Supplier<Vec3d> cameraPosSupplier, double distanceSqIn) {
        this.chunkRenderer = renderChunkIn;
        this.type = typeIn;
        this.cameraPosSupplier = cameraPosSupplier;
        this.distanceSq = distanceSqIn;
    }

    public Supplier<Vec3d> getCameraPosSupplier() {
        return this.cameraPosSupplier;
    }

    public Status getStatus() {
        return this.status;
    }

    public ChunkRendererSchematicVbo getRenderChunk() {
        return this.chunkRenderer;
    }

    public ChunkRenderDataSchematic getChunkRenderData() {
        return this.chunkRenderData;
    }

    public void setChunkRenderData(ChunkRenderDataSchematic chunkRenderData) {
        this.chunkRenderData = chunkRenderData;
    }

    public BufferBuilderCache getBufferCache() {
        return this.bufferBuilderCache;
    }

    public void setRegionRenderCacheBuilder(BufferBuilderCache cache) {
        this.bufferBuilderCache = cache;
    }

    public void setStatus(Status statusIn) {
        this.lock.lock();
        try {
            this.status = statusIn;
        }
        finally {
            this.lock.unlock();
        }
    }

    public void finish() {
        this.lock.lock();
        try {
            if (this.type == Type.REBUILD_CHUNK && this.status != Status.DONE) {
                this.chunkRenderer.setNeedsUpdate(false);
            }
            this.finished = true;
            this.status = Status.DONE;
            for (Runnable runnable : this.listFinishRunnables) {
                runnable.run();
            }
        }
        finally {
            this.lock.unlock();
        }
    }

    public void addFinishRunnable(Runnable runnable) {
        this.lock.lock();
        try {
            this.listFinishRunnables.add(runnable);
            if (this.finished) {
                runnable.run();
            }
        }
        finally {
            this.lock.unlock();
        }
    }

    public ReentrantLock getLock() {
        return this.lock;
    }

    public Type getType() {
        return this.type;
    }

    public boolean isFinished() {
        return this.finished;
    }

    @Override
    public int compareTo(ChunkRenderTaskSchematic other) {
        return Doubles.compare((double)this.distanceSq, (double)other.distanceSq);
    }

    public double getDistanceSq() {
        return this.distanceSq;
    }

    public static enum Type {
        REBUILD_CHUNK,
        RESORT_TRANSPARENCY;

    }

    public static enum Status {
        PENDING,
        COMPILING,
        UPLOADING,
        DONE;

    }

}