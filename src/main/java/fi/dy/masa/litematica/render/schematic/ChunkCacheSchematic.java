package fi.dy.masa.litematica.render.schematic;

import fi.dy.masa.litematica.world.FakeLightingProvider;
import javax.annotation.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.level.ColorResolver;

public class ChunkCacheSchematic
implements BlockRenderView {
    private static final BlockState AIR = Blocks.AIR.getDefaultState();
    protected final ClientWorld world;
    protected final ClientWorld worldClient;
    protected final FakeLightingProvider lightingProvider = new FakeLightingProvider();
    protected int chunkStartX;
    protected int chunkStartZ;
    protected WorldChunk[][] chunkArray;
    protected boolean empty;

    public ChunkCacheSchematic(ClientWorld worldIn, ClientWorld clientWorld, BlockPos pos, int expand) {
        int cz;
        int cx;
        this.world = worldIn;
        this.worldClient = clientWorld;
        this.chunkStartX = pos.getX() - expand >> 4;
        this.chunkStartZ = pos.getZ() - expand >> 4;
        int chunkEndX = pos.getX() + expand + 15 >> 4;
        int chunkEndZ = pos.getZ() + expand + 15 >> 4;
        this.chunkArray = new WorldChunk[chunkEndX - this.chunkStartX + 1][chunkEndZ - this.chunkStartZ + 1];
        this.empty = true;
        for (cx = this.chunkStartX; cx <= chunkEndX; ++cx) {
            for (cz = this.chunkStartZ; cz <= chunkEndZ; ++cz) {
                this.chunkArray[cx - this.chunkStartX][cz - this.chunkStartZ] = worldIn.getChunk(cx, cz);
            }
        }
        block2: for (cx = pos.getX() >> 4; cx <= pos.getX() + 15 >> 4; ++cx) {
            for (cz = pos.getZ() >> 4; cz <= pos.getZ() + 15 >> 4; ++cz) {
                WorldChunk chunk = this.chunkArray[cx - this.chunkStartX][cz - this.chunkStartZ];
                if (chunk == null || chunk.method_12228(pos.getY(), pos.getY() + 15)) continue;
                this.empty = false;
                continue block2;
            }
        }
    }

    public boolean isEmpty() {
        return this.empty;
    }

    public BlockState getBlockState(BlockPos pos) {
        if (pos.getY() >= 0 && pos.getY() < 256) {
            WorldChunk chunk;
            int cx = (pos.getX() >> 4) - this.chunkStartX;
            int cz = (pos.getZ() >> 4) - this.chunkStartZ;
            if (cx >= 0 && cx < this.chunkArray.length && cz >= 0 && cz < this.chunkArray[cx].length && (chunk = this.chunkArray[cx][cz]) != null) {
                return chunk.getBlockState(pos);
            }
        }
        return AIR;
    }

    @Nullable
    public BlockEntity getBlockEntity(BlockPos pos) {
        return this.getBlockEntity(pos, WorldChunk.CreationType.CHECK);
    }

    @Nullable
    public BlockEntity getBlockEntity(BlockPos pos, WorldChunk.CreationType type) {
        int i = (pos.getX() >> 4) - this.chunkStartX;
        int j = (pos.getZ() >> 4) - this.chunkStartZ;
        return this.chunkArray[i][j].getBlockEntity(pos, type);
    }

    public int getLightLevel(LightType var1, BlockPos var2) {
        return 15;
    }

    public FluidState getFluidState(BlockPos pos) {
        return this.getBlockState(pos).getFluidState();
    }

    public LightingProvider getLightingProvider() {
        return this.lightingProvider;
    }

    public int getColor(BlockPos pos, ColorResolver colorResolver) {
        return colorResolver.getColor(this.worldClient.getBiome(pos), (double)pos.getX(), (double)pos.getZ());
    }
}