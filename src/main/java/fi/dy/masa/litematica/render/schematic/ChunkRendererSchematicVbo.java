package fi.dy.masa.litematica.render.schematic;

import com.google.common.collect.Sets;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.render.RenderUtils;
import fi.dy.masa.litematica.render.schematic.BufferBuilderCache;
import fi.dy.masa.litematica.render.schematic.ChunkCacheSchematic;
import fi.dy.masa.litematica.render.schematic.ChunkRenderDataSchematic;
import fi.dy.masa.litematica.render.schematic.ChunkRenderTaskSchematic;
import fi.dy.masa.litematica.render.schematic.WorldRendererSchematic;
import fi.dy.masa.litematica.util.OverlayType;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.util.Color4f;
import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.SubChunkPos;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.BlockView;
import net.minecraft.world.chunk.WorldChunk;

public class ChunkRendererSchematicVbo {
    public static int schematicRenderChunksUpdated;
    protected volatile WorldSchematic world;
    protected final WorldRendererSchematic worldRenderer;
    protected final ReentrantLock chunkRenderLock;
    protected final ReentrantLock chunkRenderDataLock;
    protected final Set<BlockEntity> setBlockEntities = new HashSet<BlockEntity>();
    protected final BlockPos.Mutable position;
    protected final BlockPos.Mutable chunkRelativePos;
    protected final Map<RenderLayer, VertexBuffer> vertexBufferBlocks;
    protected final VertexBuffer[] vertexBufferOverlay;
    protected final List<IntBoundingBox> boxes = new ArrayList<IntBoundingBox>();
    protected final EnumSet<OverlayRenderType> existingOverlays = EnumSet.noneOf(OverlayRenderType.class);
    private Box boundingBox;
    protected Color4f overlayColor;
    protected boolean hasOverlay = false;
    protected ChunkCacheSchematic schematicWorldView;
    protected ChunkCacheSchematic clientWorldView;
    protected ChunkRenderTaskSchematic compileTask;
    protected ChunkRenderDataSchematic chunkRenderData;
    private boolean needsUpdate;
    private boolean needsImmediateUpdate;

    public ChunkRendererSchematicVbo(WorldSchematic world, WorldRendererSchematic worldRenderer) {
        this.world = world;
        this.worldRenderer = worldRenderer;
        this.chunkRenderData = ChunkRenderDataSchematic.EMPTY;
        this.chunkRenderLock = new ReentrantLock();
        this.chunkRenderDataLock = new ReentrantLock();
        this.vertexBufferBlocks = new HashMap<RenderLayer, VertexBuffer>();
        this.vertexBufferOverlay = new VertexBuffer[OverlayRenderType.values().length];
        this.position = new BlockPos.Mutable();
        this.chunkRelativePos = new BlockPos.Mutable();
        for (RenderLayer layer : RenderLayer.getBlockLayers()) {
            this.vertexBufferBlocks.put(layer, new VertexBuffer(VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL));
        }
        for (int i = 0; i < OverlayRenderType.values().length; ++i) {
            this.vertexBufferOverlay[i] = new VertexBuffer(VertexFormats.POSITION_COLOR);
        }
    }

    public boolean hasOverlay() {
        return this.hasOverlay;
    }

    public EnumSet<OverlayRenderType> getOverlayTypes() {
        return this.existingOverlays;
    }

    public VertexBuffer getBlocksVertexBufferByLayer(RenderLayer layer) {
        return this.vertexBufferBlocks.get((Object)layer);
    }

    public VertexBuffer getOverlayVertexBuffer(OverlayRenderType type) {
        return this.vertexBufferOverlay[type.ordinal()];
    }

    public ChunkRenderDataSchematic getChunkRenderData() {
        return this.chunkRenderData;
    }

    public void setChunkRenderData(ChunkRenderDataSchematic data) {
        this.chunkRenderDataLock.lock();
        try {
            this.chunkRenderData = data;
        }
        finally {
            this.chunkRenderDataLock.unlock();
        }
    }

    public BlockPos getOrigin() {
        return this.position;
    }

    public Box getBoundingBox() {
        if (this.boundingBox == null) {
            int x = this.position.getX();
            int y = this.position.getY();
            int z = this.position.getZ();
            this.boundingBox = new Box(x, y, z, (x + 16), (y + 16), (z + 16));
        }
        return this.boundingBox;
    }

    public void setPosition(int x, int y, int z) {
        if (x != this.position.getX() || y != this.position.getY() || z != this.position.getZ()) {
            this.clear();
            this.position.set(x, y, z);
            this.boundingBox = new Box(x, y, z, (x + 16), (y + 16), (z + 16));
        }
    }

    protected double getDistanceSq() {
        Entity entity = EntityUtils.getCameraEntity();
        double x = this.position.getX() + 8.0 - entity.getX();
        double y = this.position.getY() + 8.0 - entity.getY();
        double z = this.position.getZ() + 8.0 - entity.getZ();
        return x * x + y * y + z * z;
    }

    public void deleteGlResources() {
        this.clear();
        this.world = null;
        this.vertexBufferBlocks.values().forEach(buf -> buf.close());
        for (int i = 0; i < this.vertexBufferOverlay.length; ++i) {
            if (this.vertexBufferOverlay[i] == null) continue;
            this.vertexBufferOverlay[i].close();
        }
    }

    public void resortTransparency(ChunkRenderTaskSchematic task) {
        OverlayRenderType type;
        RenderLayer layerTranslucent = RenderLayer.getTranslucent();
        ChunkRenderDataSchematic data = task.getChunkRenderData();
        BufferBuilderCache buffers = task.getBufferCache();
        BufferBuilder.State bufferState = data.getBlockBufferState(layerTranslucent);
        Vec3d cameraPos = task.getCameraPosSupplier().get();
        float x = (float)cameraPos.x - (float)this.position.getX();
        float y = (float)cameraPos.y - (float)this.position.getY();
        float z = (float)cameraPos.z - (float)this.position.getZ();
        if (bufferState != null && !data.isBlockLayerEmpty(layerTranslucent)) {
            BufferBuilder buffer = buffers.getBlockBufferByLayer(layerTranslucent);
            this.preRenderBlocks(buffer);
            buffer.restoreState(bufferState);
            this.postRenderBlocks(layerTranslucent, x, y, z, buffer, data);
        }
        if (Configs.Visuals.ENABLE_SCHEMATIC_OVERLAY.getBooleanValue() && (bufferState = data.getOverlayBufferState(type = OverlayRenderType.QUAD)) != null && !data.isOverlayTypeEmpty(type)) {
            BufferBuilder buffer = buffers.getOverlayBuffer(type);
            this.preRenderOverlay(buffer, type.getGlMode());
            buffer.restoreState(bufferState);
            this.postRenderOverlay(type, x, y, z, buffer, data);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void rebuildChunk(ChunkRenderTaskSchematic task) {
        ChunkRenderDataSchematic data = new ChunkRenderDataSchematic();
        task.getLock().lock();
        try {
            if (task.getStatus() != ChunkRenderTaskSchematic.Status.COMPILING) {
                return;
            }
            task.setChunkRenderData(data);
        }
        finally {
            task.getLock().unlock();
        }
        HashSet<BlockEntity> tileEntities = new HashSet<BlockEntity>();
        BlockPos.Mutable posChunk = this.position;
        LayerRange range = DataManager.getRenderLayerRange();
        this.existingOverlays.clear();
        this.hasOverlay = false;
        List<IntBoundingBox> list = this.boxes;
        synchronized (list) {
            if (!(this.boxes.isEmpty() || this.schematicWorldView.isEmpty() && this.clientWorldView.isEmpty() || !range.intersects(new SubChunkPos(posChunk.getX() >> 4, posChunk.getY() >> 4, posChunk.getZ() >> 4)))) {
                ++schematicRenderChunksUpdated;
                Vec3d cameraPos = task.getCameraPosSupplier().get();
                float x = (float)cameraPos.x - (float)this.position.getX();
                float y = (float)cameraPos.y - (float)this.position.getY();
                float z = (float)cameraPos.z - (float)this.position.getZ();
                HashSet<RenderLayer> usedLayers = new HashSet<RenderLayer>();
                BufferBuilderCache buffers = task.getBufferCache();
                MatrixStack matrices = new MatrixStack();
                for (IntBoundingBox box : this.boxes) {
                    if ((box = range.getClampedRenderBoundingBox(box)) == null) continue;
                    BlockPos posFrom = new BlockPos(box.minX, box.minY, box.minZ);
                    BlockPos posTo = new BlockPos(box.maxX, box.maxY, box.maxZ);
                    for (BlockPos posMutable : BlockPos.Mutable.iterate((BlockPos)posFrom, (BlockPos)posTo)) {
                        matrices.push();
                        matrices.translate((posMutable.getX() & 0xF), (posMutable.getY() & 0xF), (posMutable.getZ() & 0xF));
                        this.renderBlocksAndOverlay(posMutable, data, tileEntities, usedLayers, matrices, buffers);
                        matrices.pop();
                    }
                }
                for (RenderLayer layerTmp : RenderLayer.getBlockLayers()) {
                    if (usedLayers.contains((Object)layerTmp)) {
                        data.setBlockLayerUsed(layerTmp);
                    }
                    if (!data.isBlockLayerStarted(layerTmp)) continue;
                    this.postRenderBlocks(layerTmp, x, y, z, buffers.getBlockBufferByLayer(layerTmp), data);
                }
                if (this.hasOverlay) {
                    for (OverlayRenderType type : this.existingOverlays) {
                        if (!data.isOverlayTypeStarted(type)) continue;
                        data.setOverlayTypeUsed(type);
                        this.postRenderOverlay(type, x, y, z, buffers.getOverlayBuffer(type), data);
                    }
                }
            }
        }
        this.chunkRenderLock.lock();
        try {
            HashSet set = Sets.newHashSet(tileEntities);
            HashSet set1 = Sets.newHashSet(this.setBlockEntities);
            set.removeAll(this.setBlockEntities);
            set1.removeAll(tileEntities);
            this.setBlockEntities.clear();
            this.setBlockEntities.addAll(tileEntities);
            this.worldRenderer.updateBlockEntities(set1, set);
        }
        finally {
            this.chunkRenderLock.unlock();
        }
        data.setTimeBuilt(this.world.getTime());
    }

    protected void renderBlocksAndOverlay(BlockPos pos, ChunkRenderDataSchematic data, Set<BlockEntity> tileEntities, Set<RenderLayer> usedLayers, MatrixStack matrices, BufferBuilderCache buffers) {
        BlockState stateSchematic = this.schematicWorldView.getBlockState(pos);
        BlockState stateClient = this.clientWorldView.getBlockState(pos);
        Block blockSchematic = stateSchematic.getBlock();
        boolean clientHasAir = stateClient.isAir();
        boolean schematicHasAir = stateSchematic.isAir();
        boolean missing = false;
        if (clientHasAir && schematicHasAir) {
            return;
        }
        this.overlayColor = null;
        if (clientHasAir || stateSchematic != stateClient && Configs.Visuals.RENDER_COLLIDING_SCHEMATIC_BLOCKS.getBooleanValue()) {
            RenderLayer layer;
            BufferBuilder bufferSchematic;
            if (blockSchematic.hasBlockEntity()) {
                this.addBlockEntity(pos, data, tileEntities);
            }
            boolean translucent = Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.getBooleanValue();
            FluidState fluidState = stateSchematic.getFluidState();
            if (!fluidState.isEmpty()) {
                layer = RenderLayers.getFluidLayer((FluidState)fluidState);
                bufferSchematic = buffers.getBlockBufferByLayer(layer);
                if (!data.isBlockLayerStarted(layer)) {
                    data.setBlockLayerStarted(layer);
                    this.preRenderBlocks(bufferSchematic);
                }
                if (this.worldRenderer.renderFluid(this.schematicWorldView, fluidState, pos, bufferSchematic)) {
                    usedLayers.add(layer);
                }
            }
            if (stateSchematic.getRenderType() != BlockRenderType.INVISIBLE) {
                layer = translucent ? RenderLayer.getTranslucent() : RenderLayers.getBlockLayer((BlockState)stateSchematic);
                bufferSchematic = buffers.getBlockBufferByLayer(layer);
                if (!data.isBlockLayerStarted(layer)) {
                    data.setBlockLayerStarted(layer);
                    this.preRenderBlocks(bufferSchematic);
                }
                if (this.worldRenderer.renderBlock(this.schematicWorldView, stateSchematic, pos, matrices, bufferSchematic)) {
                    usedLayers.add(layer);
                }
                if (clientHasAir) {
                    missing = true;
                }
            }
        }
        if (Configs.Visuals.ENABLE_SCHEMATIC_OVERLAY.getBooleanValue()) {
            OverlayType type = this.getOverlayType(stateSchematic, stateClient);
            this.overlayColor = this.getOverlayColor(type);
            if (this.overlayColor != null) {
                this.renderOverlay(type, pos, stateSchematic, missing, data, buffers);
            }
        }
    }

    protected void renderOverlay(OverlayType type, BlockPos pos, BlockState stateSchematic, boolean missing, ChunkRenderDataSchematic data, BufferBuilderCache buffers) {
        BakedModel bakedModel;
        BlockPos.Mutable relPos = this.getChunkRelativePosition(pos);
        if (Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_SIDES.getBooleanValue()) {
            BufferBuilder bufferOverlayQuads = buffers.getOverlayBuffer(OverlayRenderType.QUAD);
            if (!data.isOverlayTypeStarted(OverlayRenderType.QUAD)) {
                data.setOverlayTypeStarted(OverlayRenderType.QUAD);
                this.preRenderOverlay(bufferOverlayQuads, OverlayRenderType.QUAD);
            }
            if (Configs.Visuals.OVERLAY_REDUCED_INNER_SIDES.getBooleanValue()) {
                BlockPos.Mutable posMutable = new BlockPos.Mutable();
                for (int i = 0; i < 6; ++i) {
                    Direction side = PositionUtils.FACING_ALL[i];
                    posMutable.set(pos.getX() + side.getOffsetX(), pos.getY() + side.getOffsetY(), pos.getZ() + side.getOffsetZ());
                    BlockState adjStateSchematic = this.schematicWorldView.getBlockState((BlockPos)posMutable);
                    BlockState adjStateClient = this.clientWorldView.getBlockState((BlockPos)posMutable);
                    OverlayType typeAdj = this.getOverlayType(adjStateSchematic, adjStateClient);
                    if (missing && Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_SIDES.getBooleanValue()) {
                        BakedModel bakedModel2 = this.worldRenderer.getModelForState(stateSchematic);
                        if (type.getRenderPriority() <= typeAdj.getRenderPriority() && Block.isFaceFullSquare((VoxelShape)stateSchematic.getCollisionShape((BlockView)this.schematicWorldView, pos), (Direction)side)) continue;
                        RenderUtils.drawBlockModelQuadOverlayBatched(bakedModel2, stateSchematic, (BlockPos)relPos, side, this.overlayColor, 0.0, bufferOverlayQuads);
                        continue;
                    }
                    if (type.getRenderPriority() <= typeAdj.getRenderPriority()) continue;
                    RenderUtils.drawBlockBoxSideBatchedQuads((BlockPos)relPos, side, this.overlayColor, 0.0, bufferOverlayQuads);
                }
            } else if (missing && Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_SIDES.getBooleanValue()) {
                bakedModel = this.worldRenderer.getModelForState(stateSchematic);
                RenderUtils.drawBlockModelQuadOverlayBatched(bakedModel, stateSchematic, (BlockPos)relPos, this.overlayColor, 0.0, bufferOverlayQuads);
            } else {
                fi.dy.masa.malilib.render.RenderUtils.drawBlockBoundingBoxSidesBatchedQuads((BlockPos)relPos, (Color4f)this.overlayColor, 0.0, (BufferBuilder)bufferOverlayQuads);
            }
        }
        if (Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_OUTLINES.getBooleanValue()) {
            BufferBuilder bufferOverlayOutlines = buffers.getOverlayBuffer(OverlayRenderType.OUTLINE);
            if (!data.isOverlayTypeStarted(OverlayRenderType.OUTLINE)) {
                data.setOverlayTypeStarted(OverlayRenderType.OUTLINE);
                this.preRenderOverlay(bufferOverlayOutlines, OverlayRenderType.OUTLINE);
            }
            this.overlayColor = new Color4f(this.overlayColor.r, this.overlayColor.g, this.overlayColor.b, 1.0f);
            if (Configs.Visuals.OVERLAY_REDUCED_INNER_SIDES.getBooleanValue()) {
                OverlayType[][][] adjTypes = new OverlayType[3][3][3];
                BlockPos.Mutable posMutable = new BlockPos.Mutable();
                for (int y = 0; y <= 2; ++y) {
                    for (int z = 0; z <= 2; ++z) {
                        for (int x = 0; x <= 2; ++x) {
                            if (x != 1 || y != 1 || z != 1) {
                                posMutable.set(pos.getX() + x - 1, pos.getY() + y - 1, pos.getZ() + z - 1);
                                BlockState adjStateSchematic = this.schematicWorldView.getBlockState((BlockPos)posMutable);
                                BlockState adjStateClient = this.clientWorldView.getBlockState((BlockPos)posMutable);
                                adjTypes[x][y][z] = this.getOverlayType(adjStateSchematic, adjStateClient);
                                continue;
                            }
                            adjTypes[x][y][z] = type;
                        }
                    }
                }
                if (missing && Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_OUTLINE.getBooleanValue()) {
                    BakedModel bakedModel3 = this.worldRenderer.getModelForState(stateSchematic);
                    if (stateSchematic.isOpaque()) {
                        this.renderOverlayReducedEdges(pos, adjTypes, type, bufferOverlayOutlines);
                    } else {
                        RenderUtils.drawBlockModelOutlinesBatched(bakedModel3, stateSchematic, (BlockPos)relPos, this.overlayColor, 0.0, bufferOverlayOutlines);
                    }
                } else {
                    this.renderOverlayReducedEdges(pos, adjTypes, type, bufferOverlayOutlines);
                }
            } else if (missing && Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_OUTLINE.getBooleanValue()) {
                bakedModel = this.worldRenderer.getModelForState(stateSchematic);
                RenderUtils.drawBlockModelOutlinesBatched(bakedModel, stateSchematic, (BlockPos)relPos, this.overlayColor, 0.0, bufferOverlayOutlines);
            } else {
                fi.dy.masa.malilib.render.RenderUtils.drawBlockBoundingBoxOutlinesBatchedLines((BlockPos)relPos, (Color4f)this.overlayColor, 0.0, (BufferBuilder)bufferOverlayOutlines);
            }
        }
    }

    protected BlockPos.Mutable getChunkRelativePosition(BlockPos pos) {
        return this.chunkRelativePos.set(pos.getX() & 0xF, pos.getY() & 0xF, pos.getZ() & 0xF);
    }

    protected void renderOverlayReducedEdges(BlockPos pos, OverlayType[][][] adjTypes, OverlayType typeSelf, BufferBuilder bufferOverlayOutlines) {
        OverlayType[] neighborTypes = new OverlayType[4];
        Vec3i[] neighborPositions = new Vec3i[4];
        int lines = 0;
        for (Direction.Axis axis : PositionUtils.AXES_ALL) {
            for (int corner = 0; corner < 4; ++corner) {
                Vec3i[] offsets = PositionUtils.getEdgeNeighborOffsets(axis, corner);
                int index = -1;
                boolean hasCurrent = false;
                for (int i = 0; i < 4; ++i) {
                    Vec3i offset = offsets[i];
                    OverlayType type = adjTypes[offset.getX() + 1][offset.getY() + 1][offset.getZ() + 1];
                    if (type == OverlayType.NONE || index != -1 && type.getRenderPriority() < neighborTypes[index - 1].getRenderPriority()) continue;
                    if (index < 0 || type.getRenderPriority() > neighborTypes[index - 1].getRenderPriority()) {
                        index = 0;
                    }
                    neighborPositions[index] = new Vec3i(pos.getX() + offset.getX(), pos.getY() + offset.getY(), pos.getZ() + offset.getZ());
                    neighborTypes[index] = type;
                    hasCurrent |= i == 0;
                    ++index;
                }
                if (index <= 0 || !hasCurrent) continue;
                Vec3i posTmp = new Vec3i(pos.getX(), pos.getY(), pos.getZ());
                int ind = -1;
                for (int i = 0; i < index; ++i) {
                    Vec3i tmp = neighborPositions[i];
                    if (tmp.getX() > posTmp.getX() || tmp.getY() > posTmp.getY() || tmp.getZ() > posTmp.getZ()) continue;
                    posTmp = tmp;
                    ind = i;
                }
                if (posTmp.getX() != pos.getX() || posTmp.getY() != pos.getY() || posTmp.getZ() != pos.getZ()) continue;
                RenderUtils.drawBlockBoxEdgeBatchedLines((BlockPos)this.getChunkRelativePosition(pos), axis, corner, this.overlayColor, bufferOverlayOutlines);
                ++lines;
            }
        }
    }

    protected OverlayType getOverlayType(BlockState stateSchematic, BlockState stateClient) {
        if (stateSchematic == stateClient) {
            return OverlayType.NONE;
        }
        boolean clientHasAir = stateClient.isAir();
        boolean schematicHasAir = stateSchematic.isAir();
        if (schematicHasAir) {
            return clientHasAir ? OverlayType.NONE : OverlayType.EXTRA;
        }
        if (clientHasAir) {
            return OverlayType.MISSING;
        }
        if (stateSchematic.getBlock() != stateClient.getBlock()) {
            return OverlayType.WRONG_BLOCK;
        }
        return OverlayType.WRONG_STATE;
    }

    @Nullable
    protected Color4f getOverlayColor(OverlayType overlayType) {
        Color4f overlayColor = null;
        switch (overlayType) {
            case MISSING: {
                if (!Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_MISSING.getBooleanValue()) break;
                overlayColor = Configs.Colors.SCHEMATIC_OVERLAY_COLOR_MISSING.getColor();
                break;
            }
            case EXTRA: {
                if (!Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_EXTRA.getBooleanValue()) break;
                overlayColor = Configs.Colors.SCHEMATIC_OVERLAY_COLOR_EXTRA.getColor();
                break;
            }
            case WRONG_BLOCK: {
                if (!Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_WRONG_BLOCK.getBooleanValue()) break;
                overlayColor = Configs.Colors.SCHEMATIC_OVERLAY_COLOR_WRONG_BLOCK.getColor();
                break;
            }
            case WRONG_STATE: {
                if (!Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_WRONG_STATE.getBooleanValue()) break;
                overlayColor = Configs.Colors.SCHEMATIC_OVERLAY_COLOR_WRONG_STATE.getColor();
                break;
            }
        }
        return overlayColor;
    }

    private void addBlockEntity(BlockPos pos, ChunkRenderDataSchematic chunkRenderData, Set<BlockEntity> blockEntities) {
        BlockEntityRenderer tesr;
        BlockEntity te = this.schematicWorldView.getBlockEntity(pos, WorldChunk.CreationType.CHECK);
        if (te != null && (tesr = BlockEntityRenderDispatcher.INSTANCE.get(te)) != null) {
            chunkRenderData.addBlockEntity(te);
            if (tesr.rendersOutsideBoundingBox(te)) {
                blockEntities.add(te);
            }
        }
    }

    private void preRenderBlocks(BufferBuilder buffer) {
        buffer.begin(7, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);
    }

    private void postRenderBlocks(RenderLayer layer, float x, float y, float z, BufferBuilder buffer, ChunkRenderDataSchematic chunkRenderData) {
        if (layer == RenderLayer.getTranslucent() && !chunkRenderData.isBlockLayerEmpty(layer)) {
            buffer.sortQuads(x, y, z);
            chunkRenderData.setBlockBufferState(layer, buffer.popState());
        }
        buffer.end();
    }

    private void preRenderOverlay(BufferBuilder buffer, OverlayRenderType type) {
        this.existingOverlays.add(type);
        this.hasOverlay = true;
        buffer.begin(type.getGlMode(), VertexFormats.POSITION_COLOR);
    }

    private void preRenderOverlay(BufferBuilder buffer, int glMode) {
        buffer.begin(glMode, VertexFormats.POSITION_COLOR);
    }

    private void postRenderOverlay(OverlayRenderType type, float x, float y, float z, BufferBuilder buffer, ChunkRenderDataSchematic chunkRenderData) {
        if (type == OverlayRenderType.QUAD && !chunkRenderData.isOverlayTypeEmpty(type)) {
            buffer.sortQuads(x, y, z);
            chunkRenderData.setOverlayBufferState(type, buffer.popState());
        }
        buffer.end();
    }

    public ChunkRenderTaskSchematic makeCompileTaskChunkSchematic(Supplier<Vec3d> cameraPosSupplier) {
        this.chunkRenderLock.lock();
        ChunkRenderTaskSchematic generator = null;
        try {
            this.finishCompileTask();
            this.rebuildWorldView();
            generator = this.compileTask = new ChunkRenderTaskSchematic(this, ChunkRenderTaskSchematic.Type.REBUILD_CHUNK, cameraPosSupplier, this.getDistanceSq());
        }
        finally {
            this.chunkRenderLock.unlock();
        }
        return generator;
    }

    @Nullable
    public ChunkRenderTaskSchematic makeCompileTaskTransparencySchematic(Supplier<Vec3d> cameraPosSupplier) {
        this.chunkRenderLock.lock();
        try {
            if (this.compileTask == null || this.compileTask.getStatus() != ChunkRenderTaskSchematic.Status.PENDING) {
                if (this.compileTask != null && this.compileTask.getStatus() != ChunkRenderTaskSchematic.Status.DONE) {
                    this.compileTask.finish();
                }
                this.compileTask = new ChunkRenderTaskSchematic(this, ChunkRenderTaskSchematic.Type.RESORT_TRANSPARENCY, cameraPosSupplier, this.getDistanceSq());
                this.compileTask.setChunkRenderData(this.chunkRenderData);
                ChunkRenderTaskSchematic chunkRenderTaskSchematic = this.compileTask;
                return chunkRenderTaskSchematic;
            }
        }
        finally {
            this.chunkRenderLock.unlock();
        }
        return null;
    }

    protected void finishCompileTask() {
        this.chunkRenderLock.lock();
        try {
            if (this.compileTask != null && this.compileTask.getStatus() != ChunkRenderTaskSchematic.Status.DONE) {
                this.compileTask.finish();
                this.compileTask = null;
            }
        }
        finally {
            this.chunkRenderLock.unlock();
        }
    }

    public ReentrantLock getLockCompileTask() {
        return this.chunkRenderLock;
    }

    public void clear() {
        this.finishCompileTask();
        this.chunkRenderData = ChunkRenderDataSchematic.EMPTY;
        this.needsUpdate = true;
    }

    public void setNeedsUpdate(boolean immediate) {
        if (this.needsUpdate) {
            immediate |= this.needsImmediateUpdate;
        }
        this.needsUpdate = true;
        this.needsImmediateUpdate = immediate;
    }

    public void clearNeedsUpdate() {
        this.needsUpdate = false;
        this.needsImmediateUpdate = false;
    }

    public boolean needsUpdate() {
        return this.needsUpdate;
    }

    public boolean needsImmediateUpdate() {
        return this.needsUpdate && this.needsImmediateUpdate;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void rebuildWorldView() {
        List<IntBoundingBox> list = this.boxes;
        synchronized (list) {
            ClientWorld worldClient = MinecraftClient.getInstance().world;
            this.schematicWorldView = new ChunkCacheSchematic(this.world, worldClient, (BlockPos)this.position, 2);
            this.clientWorldView = new ChunkCacheSchematic(worldClient, worldClient, (BlockPos)this.position, 2);
            BlockPos.Mutable pos = this.position;
            SubChunkPos subChunk = new SubChunkPos(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
            this.boxes.clear();
            this.boxes.addAll(DataManager.getSchematicPlacementManager().getTouchedBoxesInSubChunk(subChunk));
        }
    }

    public static enum OverlayRenderType {
        OUTLINE(1),
        QUAD(7);

        private final int glMode;

        private OverlayRenderType(int glMode) {
            this.glMode = glMode;
        }

        public int getGlMode() {
            return this.glMode;
        }
    }

}