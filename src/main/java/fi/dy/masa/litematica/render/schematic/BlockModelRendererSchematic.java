package fi.dy.masa.litematica.render.schematic;

import java.util.BitSet;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockRenderView;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.mixin.IMixinBlockModelRenderer;
import fi.dy.masa.litematica.render.schematic.BlockModelRendererSchematic.VertexTranslations;

public class BlockModelRendererSchematic
{
    private final BlockModelRenderer vanillaRenderer;
    private final Random random = new Random();

    public BlockModelRendererSchematic(BlockColors blockColorsIn)
    {
       
     this.vanillaRenderer = MinecraftClient.getInstance().getBlockRenderManager().getModelRenderer();
    }

    public boolean renderModel(BlockRenderView worldIn, BakedModel modelIn, BlockState stateIn, BlockPos posIn, MatrixStack matrices, VertexConsumer vertexConsumer, long rand)
    {
        boolean ao = MinecraftClient.isAmbientOcclusionEnabled() && stateIn.getLuminance() == 0 && modelIn.useAmbientOcclusion();
        Vec3d offset = stateIn.getOffsetPos(worldIn, posIn);
        matrices.translate(offset.x, offset.y, offset.z);
        int overlay = OverlayTexture.DEFAULT_UV;
        try
        {
            if (ao)
            {
                return this.renderModelSmooth(worldIn, modelIn, stateIn, posIn, matrices, vertexConsumer, this.random, rand, overlay);
            }
            else
            {
                return this.renderModelFlat(worldIn, modelIn, stateIn, posIn, matrices, vertexConsumer, this.random, rand, overlay);
            }
        }
        catch (Throwable throwable)
        {
            CrashReport crashreport = CrashReport.create(throwable, "Tesselating block model");
            CrashReportSection crashreportcategory = crashreport.addElement("Block model being tesselated");
            CrashReportSection.addBlockInfo(crashreportcategory, posIn, stateIn);
            crashreportcategory.add("Using AO", Boolean.valueOf(ao));
            throw new CrashException(crashreport);
        }
    }

    public boolean renderModelSmooth(BlockRenderView worldIn, BakedModel modelIn, BlockState stateIn, BlockPos posIn, MatrixStack matrices, VertexConsumer vertexConsumer, Random random, long seedIn, int overlay) {
    
        boolean renderedSomething = false;
        float[] quadBounds = new float[Direction.values().length * 2];
        BitSet bitset = new BitSet(3);
        AmbientOcclusionCalculator aoFace = new AmbientOcclusionCalculator();
       
        for (Direction side : Direction.values())
        {
            random.setSeed(seedIn);
            List<BakedQuad> quads = modelIn.getQuads(stateIn, side, random);

            if (quads.isEmpty() == false)
            {
                if (this.shouldRenderModelSide(worldIn, stateIn, posIn, side))
                {
                    this.renderQuadsSmooth(worldIn, stateIn, posIn, matrices, vertexConsumer, quads, quadBounds, bitset, aoFace, overlay);
                    renderedSomething = true;
                }
            }
        }

        random.setSeed(seedIn);
        List<BakedQuad> quads = modelIn.getQuads(stateIn, (Direction) null, random);

        if (quads.isEmpty() == false)
        {
            this.renderQuadsSmooth(worldIn, stateIn, posIn, matrices, vertexConsumer, quads, quadBounds, bitset, aoFace, overlay);
            renderedSomething = true;
        }

        return renderedSomething;
    }

    public boolean renderModelFlat(BlockRenderView worldIn, BakedModel modelIn, BlockState stateIn, BlockPos posIn, MatrixStack matrices, VertexConsumer vertexConsumer, Random random, long seedIn, int overlay) {
        boolean renderedSomething = false;
        BitSet bitset = new BitSet(3);

        for (Direction side : Direction.values())
        {
            random.setSeed(seedIn);
            List quads = modelIn.getQuads(stateIn, side, random);
            if (quads.isEmpty() || !this.shouldRenderModelSide(worldIn, stateIn, posIn, side)) continue;
            int light = WorldRenderer.getLightmapCoordinates(worldIn, stateIn, posIn.offset(side));
            this.renderQuadsFlat(worldIn, stateIn, posIn, light, overlay, false, matrices, vertexConsumer, quads, bitset);
            renderedSomething = true;
        }

        random.setSeed(seedIn);
        List quads = modelIn.getQuads(stateIn, null, random);

        if (quads.isEmpty() == false)
        {
            this.renderQuadsFlat(worldIn, stateIn, posIn, -1, overlay, true, matrices, vertexConsumer, quads, bitset);
            renderedSomething = true;
        }

        return renderedSomething;
    }

    private boolean shouldRenderModelSide(BlockRenderView worldIn, BlockState stateIn, BlockPos posIn, Direction side)
    {
        return DataManager.getRenderLayerRange().isPositionAtRenderEdgeOnSide(posIn, side) ||
               (Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.getBooleanValue() && Configs.Visuals.RENDER_TRANSLUCENT_INNER_SIDES.getBooleanValue()) ||
               Block.shouldDrawSide(stateIn, worldIn, posIn, side);
    }

    private void renderQuadsSmooth(BlockRenderView world, BlockState state, BlockPos pos, MatrixStack matrices, VertexConsumer vertexConsumer, List<BakedQuad> list, float[] box, BitSet flags, AmbientOcclusionCalculator ambientOcclusionCalculator, int overlay) {
   
        int size = list.size();
        for (int i = 0; i < size; ++i) {
            BakedQuad bakedQuad = list.get(i);
            ((IMixinBlockModelRenderer) this.vanillaRenderer).invokeGetQuadDimensions(world, state, pos,
                    bakedQuad.getVertexData(), bakedQuad.getFace(), box, flags);
            ambientOcclusionCalculator.apply(world, state, pos, bakedQuad.getFace(), box, flags);
            ((IMixinBlockModelRenderer) this.vanillaRenderer).invokeRenderQuad(world, state, pos, vertexConsumer,
                    matrices.peek(), bakedQuad, ambientOcclusionCalculator.brightness[0],
                    ambientOcclusionCalculator.brightness[1], ambientOcclusionCalculator.brightness[2],
                    ambientOcclusionCalculator.brightness[3], ambientOcclusionCalculator.light[0],
                    ambientOcclusionCalculator.light[1], ambientOcclusionCalculator.light[2],
                    ambientOcclusionCalculator.light[3], overlay);
        }
    }

    private void renderQuadsFlat(BlockRenderView world, BlockState state, BlockPos pos, int light, int overlay, boolean useWorldLight, MatrixStack matrices, VertexConsumer vertexConsumer, List<BakedQuad> list, BitSet flags) {
        int size = list.size();
        for (int i = 0; i < size; ++i) {
            BakedQuad bakedQuad = list.get(i);
            if (useWorldLight) {
                ((IMixinBlockModelRenderer)this.vanillaRenderer).invokeGetQuadDimensions(world, state, pos, bakedQuad.getVertexData(), bakedQuad.getFace(), null, flags);
                BlockPos blockPos = flags.get(0) ? pos.offset(bakedQuad.getFace()) : pos;
                light = WorldRenderer.getLightmapCoordinates((BlockRenderView)world, (BlockState)state, (BlockPos)blockPos);
            }
            ((IMixinBlockModelRenderer)this.vanillaRenderer).invokeRenderQuad(world, state, pos, vertexConsumer, matrices.peek(), bakedQuad, 1.0f, 1.0f, 1.0f, 1.0f, light, light, light, light, overlay);
        }
    }
 
  
    class AmbientOcclusionCalculator {
        private final float[] brightness = new float[4];
        private final int[] light = new int[4];

        public void apply(BlockRenderView worldIn, BlockState state, BlockPos centerPos, Direction direction,
                float[] faceShape, BitSet shapeState) {
            BlockModelRendererSchematic.EnumNeighborInfo neighborInfo = BlockModelRendererSchematic.EnumNeighborInfo
                    .getNeighbourInfo(direction);
            BlockModelRendererSchematic.VertexTranslations vertexTranslations = BlockModelRendererSchematic.VertexTranslations
                    .getVertexTranslations(direction);

            float f8 = 1.0F, f7 = f8, f6 = f7, f5 = f6, f4 = f5, f3 = f4, f2 = f3, f1 = f2, f = f1;

            int l1 = 15728880, k1 = l1, j1 = k1, i3 = j1, i1 = i3, l = i1, k = l, j = k, i = j;

            if (shapeState.get(1) && neighborInfo.doNonCubicWeight) {

                float f29 = (f3 + f + f5 + f8) * 0.25F;
                float f30 = (f2 + f + f4 + f8) * 0.25F;
                float f31 = (f2 + f1 + f6 + f8) * 0.25F;
                float f32 = (f3 + f1 + f7 + f8) * 0.25F;
                float f13 = faceShape[(neighborInfo.vert0Weights[0]).shape]
                        * faceShape[(neighborInfo.vert0Weights[1]).shape];
                float f14 = faceShape[(neighborInfo.vert0Weights[2]).shape]
                        * faceShape[(neighborInfo.vert0Weights[3]).shape];
                float f15 = faceShape[(neighborInfo.vert0Weights[4]).shape]
                        * faceShape[(neighborInfo.vert0Weights[5]).shape];
                float f16 = faceShape[(neighborInfo.vert0Weights[6]).shape]
                        * faceShape[(neighborInfo.vert0Weights[7]).shape];
                float f17 = faceShape[(neighborInfo.vert1Weights[0]).shape]
                        * faceShape[(neighborInfo.vert1Weights[1]).shape];
                float f18 = faceShape[(neighborInfo.vert1Weights[2]).shape]
                        * faceShape[(neighborInfo.vert1Weights[3]).shape];
                float f19 = faceShape[(neighborInfo.vert1Weights[4]).shape]
                        * faceShape[(neighborInfo.vert1Weights[5]).shape];
                float f20 = faceShape[(neighborInfo.vert1Weights[6]).shape]
                        * faceShape[(neighborInfo.vert1Weights[7]).shape];
                float f21 = faceShape[(neighborInfo.vert2Weights[0]).shape]
                        * faceShape[(neighborInfo.vert2Weights[1]).shape];
                float f22 = faceShape[(neighborInfo.vert2Weights[2]).shape]
                        * faceShape[(neighborInfo.vert2Weights[3]).shape];
                float f23 = faceShape[(neighborInfo.vert2Weights[4]).shape]
                        * faceShape[(neighborInfo.vert2Weights[5]).shape];
                float f24 = faceShape[(neighborInfo.vert2Weights[6]).shape]
                        * faceShape[(neighborInfo.vert2Weights[7]).shape];
                float f25 = faceShape[(neighborInfo.vert3Weights[0]).shape]
                        * faceShape[(neighborInfo.vert3Weights[1]).shape];
                float f26 = faceShape[(neighborInfo.vert3Weights[2]).shape]
                        * faceShape[(neighborInfo.vert3Weights[3]).shape];
                float f27 = faceShape[(neighborInfo.vert3Weights[4]).shape]
                        * faceShape[(neighborInfo.vert3Weights[5]).shape];
                float f28 = faceShape[(neighborInfo.vert3Weights[6]).shape]
                        * faceShape[(neighborInfo.vert3Weights[7]).shape];
                this.brightness[vertexTranslations.vert0] = f29 * f13 + f30 * f14 + f31 * f15 + f32 * f16;
                this.brightness[vertexTranslations.vert1] = f29 * f17 + f30 * f18 + f31 * f19 + f32 * f20;
                this.brightness[vertexTranslations.vert2] = f29 * f21 + f30 * f22 + f31 * f23 + f32 * f24;
                this.brightness[vertexTranslations.vert3] = f29 * f25 + f30 * f26 + f31 * f27 + f32 * f28;
                int i2 = getAoBrightness(l, i, j1, i3);
                int j2 = getAoBrightness(k, i, i1, i3);
                int k2 = getAoBrightness(k, j, k1, i3);
                int l2 = getAoBrightness(l, j, l1, i3);
                this.light[vertexTranslations.vert0] = getVertexBrightness(i2, j2, k2, l2, f13, f14, f15, f16);
                this.light[vertexTranslations.vert1] = getVertexBrightness(i2, j2, k2, l2, f17, f18, f19, f20);
                this.light[vertexTranslations.vert2] = getVertexBrightness(i2, j2, k2, l2, f21, f22, f23, f24);
                this.light[vertexTranslations.vert3] = getVertexBrightness(i2, j2, k2, l2, f25, f26, f27, f28);
            } else {

                float f9 = (f3 + f + f5 + f8) * 0.25F;
                float f10 = (f2 + f + f4 + f8) * 0.25F;
                float f11 = (f2 + f1 + f6 + f8) * 0.25F;
                float f12 = (f3 + f1 + f7 + f8) * 0.25F;
                this.light[vertexTranslations.vert0] = getAoBrightness(l, i, j1, i3);
                this.light[vertexTranslations.vert1] = getAoBrightness(k, i, i1, i3);
                this.light[vertexTranslations.vert2] = getAoBrightness(k, j, k1, i3);
                this.light[vertexTranslations.vert3] = getAoBrightness(l, j, l1, i3);
                this.brightness[vertexTranslations.vert0] = f9;
                this.brightness[vertexTranslations.vert1] = f10;
                this.brightness[vertexTranslations.vert2] = f11;
                this.brightness[vertexTranslations.vert3] = f12;
            }
        }

        private int getAoBrightness(int br1, int br2, int br3, int br4) {
            if (br1 == 0) {
                br1 = br4;
            }

            if (br2 == 0) {
                br2 = br4;
            }

            if (br3 == 0) {
                br3 = br4;
            }

            return br1 + br2 + br3 + br4 >> 2 & 0xFF00FF;
        }

        private int getVertexBrightness(int p_178203_1_, int p_178203_2_, int p_178203_3_, int p_178203_4_,
                float p_178203_5_, float p_178203_6_, float p_178203_7_, float p_178203_8_) {
            int i = (int) ((p_178203_1_ >> 16 & 0xFF) * p_178203_5_ + (p_178203_2_ >> 16 & 0xFF) * p_178203_6_
                    + (p_178203_3_ >> 16 & 0xFF) * p_178203_7_ + (p_178203_4_ >> 16 & 0xFF) * p_178203_8_) & 0xFF;
            int j = (int) ((p_178203_1_ & 0xFF) * p_178203_5_ + (p_178203_2_ & 0xFF) * p_178203_6_
                    + (p_178203_3_ & 0xFF) * p_178203_7_ + (p_178203_4_ & 0xFF) * p_178203_8_) & 0xFF;
            return i << 16 | j;
        }
    }
    public static enum EnumNeighborInfo
    {
        DOWN(new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH}, 0.5F, true, new Orientation[]{Orientation.FLIP_WEST, Orientation.SOUTH, Orientation.FLIP_WEST, Orientation.FLIP_SOUTH, Orientation.WEST, Orientation.FLIP_SOUTH, Orientation.WEST, Orientation.SOUTH}, new Orientation[]{Orientation.FLIP_WEST, Orientation.NORTH, Orientation.FLIP_WEST, Orientation.FLIP_NORTH, Orientation.WEST, Orientation.FLIP_NORTH, Orientation.WEST, Orientation.NORTH}, new Orientation[]{Orientation.FLIP_EAST, Orientation.NORTH, Orientation.FLIP_EAST, Orientation.FLIP_NORTH, Orientation.EAST, Orientation.FLIP_NORTH, Orientation.EAST, Orientation.NORTH}, new Orientation[]{Orientation.FLIP_EAST, Orientation.SOUTH, Orientation.FLIP_EAST, Orientation.FLIP_SOUTH, Orientation.EAST, Orientation.FLIP_SOUTH, Orientation.EAST, Orientation.SOUTH}),
        UP(new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH}, 1.0F, true, new Orientation[]{Orientation.EAST, Orientation.SOUTH, Orientation.EAST, Orientation.FLIP_SOUTH, Orientation.FLIP_EAST, Orientation.FLIP_SOUTH, Orientation.FLIP_EAST, Orientation.SOUTH}, new Orientation[]{Orientation.EAST, Orientation.NORTH, Orientation.EAST, Orientation.FLIP_NORTH, Orientation.FLIP_EAST, Orientation.FLIP_NORTH, Orientation.FLIP_EAST, Orientation.NORTH}, new Orientation[]{Orientation.WEST, Orientation.NORTH, Orientation.WEST, Orientation.FLIP_NORTH, Orientation.FLIP_WEST, Orientation.FLIP_NORTH, Orientation.FLIP_WEST, Orientation.NORTH}, new Orientation[]{Orientation.WEST, Orientation.SOUTH, Orientation.WEST, Orientation.FLIP_SOUTH, Orientation.FLIP_WEST, Orientation.FLIP_SOUTH, Orientation.FLIP_WEST, Orientation.SOUTH}),
        NORTH(new Direction[]{Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST}, 0.8F, true, new Orientation[]{Orientation.UP, Orientation.FLIP_WEST, Orientation.UP, Orientation.WEST, Orientation.FLIP_UP, Orientation.WEST, Orientation.FLIP_UP, Orientation.FLIP_WEST}, new Orientation[]{Orientation.UP, Orientation.FLIP_EAST, Orientation.UP, Orientation.EAST, Orientation.FLIP_UP, Orientation.EAST, Orientation.FLIP_UP, Orientation.FLIP_EAST}, new Orientation[]{Orientation.DOWN, Orientation.FLIP_EAST, Orientation.DOWN, Orientation.EAST, Orientation.FLIP_DOWN, Orientation.EAST, Orientation.FLIP_DOWN, Orientation.FLIP_EAST}, new Orientation[]{Orientation.DOWN, Orientation.FLIP_WEST, Orientation.DOWN, Orientation.WEST, Orientation.FLIP_DOWN, Orientation.WEST, Orientation.FLIP_DOWN, Orientation.FLIP_WEST}),
        SOUTH(new Direction[]{Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP}, 0.8F, true, new Orientation[]{Orientation.UP, Orientation.FLIP_WEST, Orientation.FLIP_UP, Orientation.FLIP_WEST, Orientation.FLIP_UP, Orientation.WEST, Orientation.UP, Orientation.WEST}, new Orientation[]{Orientation.DOWN, Orientation.FLIP_WEST, Orientation.FLIP_DOWN, Orientation.FLIP_WEST, Orientation.FLIP_DOWN, Orientation.WEST, Orientation.DOWN, Orientation.WEST}, new Orientation[]{Orientation.DOWN, Orientation.FLIP_EAST, Orientation.FLIP_DOWN, Orientation.FLIP_EAST, Orientation.FLIP_DOWN, Orientation.EAST, Orientation.DOWN, Orientation.EAST}, new Orientation[]{Orientation.UP, Orientation.FLIP_EAST, Orientation.FLIP_UP, Orientation.FLIP_EAST, Orientation.FLIP_UP, Orientation.EAST, Orientation.UP, Orientation.EAST}),
        WEST(new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH}, 0.6F, true, new Orientation[]{Orientation.UP, Orientation.SOUTH, Orientation.UP, Orientation.FLIP_SOUTH, Orientation.FLIP_UP, Orientation.FLIP_SOUTH, Orientation.FLIP_UP, Orientation.SOUTH}, new Orientation[]{Orientation.UP, Orientation.NORTH, Orientation.UP, Orientation.FLIP_NORTH, Orientation.FLIP_UP, Orientation.FLIP_NORTH, Orientation.FLIP_UP, Orientation.NORTH}, new Orientation[]{Orientation.DOWN, Orientation.NORTH, Orientation.DOWN, Orientation.FLIP_NORTH, Orientation.FLIP_DOWN, Orientation.FLIP_NORTH, Orientation.FLIP_DOWN, Orientation.NORTH}, new Orientation[]{Orientation.DOWN, Orientation.SOUTH, Orientation.DOWN, Orientation.FLIP_SOUTH, Orientation.FLIP_DOWN, Orientation.FLIP_SOUTH, Orientation.FLIP_DOWN, Orientation.SOUTH}),
        EAST(new Direction[]{Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH}, 0.6F, true, new Orientation[]{Orientation.FLIP_DOWN, Orientation.SOUTH, Orientation.FLIP_DOWN, Orientation.FLIP_SOUTH, Orientation.DOWN, Orientation.FLIP_SOUTH, Orientation.DOWN, Orientation.SOUTH}, new Orientation[]{Orientation.FLIP_DOWN, Orientation.NORTH, Orientation.FLIP_DOWN, Orientation.FLIP_NORTH, Orientation.DOWN, Orientation.FLIP_NORTH, Orientation.DOWN, Orientation.NORTH}, new Orientation[]{Orientation.FLIP_UP, Orientation.NORTH, Orientation.FLIP_UP, Orientation.FLIP_NORTH, Orientation.UP, Orientation.FLIP_NORTH, Orientation.UP, Orientation.NORTH}, new Orientation[]{Orientation.FLIP_UP, Orientation.SOUTH, Orientation.FLIP_UP, Orientation.FLIP_SOUTH, Orientation.UP, Orientation.FLIP_SOUTH, Orientation.UP, Orientation.SOUTH});

        //private final Direction[] corners;
        //private final float shadeWeight;
        private final boolean doNonCubicWeight;
        private final Orientation[] vert0Weights;
        private final Orientation[] vert1Weights;
        private final Orientation[] vert2Weights;
        private final Orientation[] vert3Weights;
        private static final EnumNeighborInfo[] VALUES = new EnumNeighborInfo[6];

        private EnumNeighborInfo(Direction[] p_i46236_3_, float p_i46236_4_, boolean p_i46236_5_, Orientation[] p_i46236_6_, Orientation[] p_i46236_7_, Orientation[] p_i46236_8_, Orientation[] p_i46236_9_)
        {
            //this.corners = p_i46236_3_;
            //this.shadeWeight = p_i46236_4_;
            this.doNonCubicWeight = p_i46236_5_;
            this.vert0Weights = p_i46236_6_;
            this.vert1Weights = p_i46236_7_;
            this.vert2Weights = p_i46236_8_;
            this.vert3Weights = p_i46236_9_;
        }

        public static EnumNeighborInfo getNeighbourInfo(Direction p_178273_0_)
        {
            return VALUES[p_178273_0_.getId()];
        }

        static
        {
            VALUES[Direction.DOWN.getId()] = DOWN;
            VALUES[Direction.UP.getId()] = UP;
            VALUES[Direction.NORTH.getId()] = NORTH;
            VALUES[Direction.SOUTH.getId()] = SOUTH;
            VALUES[Direction.WEST.getId()] = WEST;
            VALUES[Direction.EAST.getId()] = EAST;
        }
    }

    public static enum Orientation
    {
        DOWN(Direction.DOWN, false),
        UP(Direction.UP, false),
        NORTH(Direction.NORTH, false),
        SOUTH(Direction.SOUTH, false),
        WEST(Direction.WEST, false),
        EAST(Direction.EAST, false),
        FLIP_DOWN(Direction.DOWN, true),
        FLIP_UP(Direction.UP, true),
        FLIP_NORTH(Direction.NORTH, true),
        FLIP_SOUTH(Direction.SOUTH, true),
        FLIP_WEST(Direction.WEST, true),
        FLIP_EAST(Direction.EAST, true);

        private final int shape;

        private Orientation(Direction p_i46233_3_, boolean p_i46233_4_)
        {
            this.shape = p_i46233_3_.getId() + (p_i46233_4_ ? Direction.values().length : 0);
        }
    }

    static enum VertexTranslations
    {
        DOWN(0, 1, 2, 3),
        UP(2, 3, 0, 1),
        NORTH(3, 0, 1, 2),
        SOUTH(0, 1, 2, 3),
        WEST(3, 0, 1, 2),
        EAST(1, 2, 3, 0);

        private final int vert0;
        private final int vert1;
        private final int vert2;
        private final int vert3;
        private static final VertexTranslations[] VALUES = new VertexTranslations[6];

        private VertexTranslations(int p_i46234_3_, int p_i46234_4_, int p_i46234_5_, int p_i46234_6_)
        {
            this.vert0 = p_i46234_3_;
            this.vert1 = p_i46234_4_;
            this.vert2 = p_i46234_5_;
            this.vert3 = p_i46234_6_;
        }

        public static VertexTranslations getVertexTranslations(Direction p_178184_0_)
        {
            return VALUES[p_178184_0_.getId()];
        }

        static
        {
            VALUES[Direction.DOWN.getId()] = DOWN;
            VALUES[Direction.UP.getId()] = UP;
            VALUES[Direction.NORTH.getId()] = NORTH;
            VALUES[Direction.SOUTH.getId()] = SOUTH;
            VALUES[Direction.WEST.getId()] = WEST;
            VALUES[Direction.EAST.getId()] = EAST;
        }
    }
}
