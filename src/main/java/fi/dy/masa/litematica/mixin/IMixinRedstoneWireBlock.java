package fi.dy.masa.litematica.mixin;

import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.enums.WireConnection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({RedstoneWireBlock.class})
public interface IMixinRedstoneWireBlock {
  @Invoker("getRenderConnectionType")
  WireConnection invokeGetSide(BlockView paramBlockView, BlockPos paramBlockPos, Direction paramDirection);
}
