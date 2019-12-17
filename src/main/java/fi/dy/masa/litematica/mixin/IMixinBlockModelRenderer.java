package fi.dy.masa.litematica.mixin;

import java.util.BitSet;
import javax.annotation.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value={BlockModelRenderer.class})
public interface IMixinBlockModelRenderer {
    @Invoker(value="getQuadDimensions")
    public void invokeGetQuadDimensions(BlockRenderView var1, BlockState var2, BlockPos var3, int[] var4, Direction var5, @Nullable float[] var6, BitSet var7);

    @Invoker(value="renderQuad")
    public void invokeRenderQuad(BlockRenderView var1, BlockState var2, BlockPos var3, VertexConsumer var4, MatrixStack.Entry var5, BakedQuad var6, float var7, float var8, float var9, float var10, int var11, int var12, int var13, int var14, int var15);
}