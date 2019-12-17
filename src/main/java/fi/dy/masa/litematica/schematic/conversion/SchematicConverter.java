package fi.dy.masa.litematica.schematic.conversion;

import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.util.InfoUtils;
import java.util.Arrays;
import java.util.IdentityHashMap;
import javax.annotation.Nullable;
import net.minecraft.block.BannerBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChorusPlantBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.FireBlock;
import net.minecraft.block.FlowerPotBlock;
import net.minecraft.block.GrassBlock;
import net.minecraft.block.MyceliumBlock;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.RepeaterBlock;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.SnowyBlock;
import net.minecraft.block.StainedGlassPaneBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.StemBlock;
import net.minecraft.block.TallFlowerBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.TripwireBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.block.WallBannerBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.block.WallSkullBlock;
import net.minecraft.nbt.CompoundTag;


public class SchematicConverter
{
  private final IdentityHashMap<Class<? extends Block>, SchematicConversionFixers.IStateFixer> fixersPerBlock = new IdentityHashMap<>();
  private IdentityHashMap<BlockState, SchematicConversionFixers.IStateFixer> postProcessingStateFixers = new IdentityHashMap<>();


  
  private SchematicConverter() { addPostUpdateBlocks(); }



  
  public static SchematicConverter create() { return new SchematicConverter(); }


  
  public boolean getConvertedStatesForBlock(int schematicBlockId, String blockName, BlockState[] paletteOut) {
    int shiftedOldVanillaId = SchematicConversionMaps.getOldNameToShiftedBlockId(blockName);
    int successCount = 0;

    
    if (shiftedOldVanillaId >= 0) {
      
      for (int meta = 0; meta < 16; meta++)
      {
        
        BlockState state = SchematicConversionMaps.get_1_13_2_StateForIdMeta(shiftedOldVanillaId & 0xFFF0 | meta);
        
        if (state != null)
        {
          paletteOut[schematicBlockId << 4 | meta] = state;
          successCount++;
        }
      
      }
    
    } else {
      
      InfoUtils.showGuiOrInGameMessage(Message.MessageType.ERROR, "Failed to convert block with old name '" + blockName + "'", new Object[0]);
    } 
    
    return (successCount > 0);
  }

  
  public boolean getVanillaBlockPalette(BlockState[] paletteOut) {
    for (int idMeta = 0; idMeta < paletteOut.length; idMeta++) {
      
      BlockState state = SchematicConversionMaps.get_1_13_2_StateForIdMeta(idMeta);
      
      if (state != null)
      {
        paletteOut[idMeta] = state;
      }
    } 
    
    return true;
  }

  
  public BlockState[] getBlockStatePaletteForBlockPalette(String[] blockPalette) {
    BlockState[] palette = new BlockState[blockPalette.length * 16];
    Arrays.fill((Object[])palette, Blocks.AIR.getDefaultState());
    
    for (int schematicBlockId = 0; schematicBlockId < blockPalette.length; schematicBlockId++) {
      
      String blockName = blockPalette[schematicBlockId];
      getConvertedStatesForBlock(schematicBlockId, blockName, palette);
    } 
    
    return palette;
  }






  
  public boolean createPostProcessStateFilter(BlockState[] palette) {
    boolean needsPostProcess = false;
    this.postProcessingStateFixers.clear();



    
    for (int i = 0; i < palette.length; i++) {
      
      BlockState state = palette[i];
      
      if (needsPostProcess(state)) {
        
        this.postProcessingStateFixers.put(state, getFixerFor(state));
        needsPostProcess = true;
      } 
    } 
    
    return needsPostProcess;
  }


  
  public IdentityHashMap<BlockState, SchematicConversionFixers.IStateFixer> getPostProcessStateFilter() { return this.postProcessingStateFixers; }



  
  private boolean needsPostProcess(BlockState state) { return (!state.isAir() && this.fixersPerBlock.containsKey(state.getBlock().getClass())); }



  
  @Nullable
  private SchematicConversionFixers.IStateFixer getFixerFor(BlockState state) { return this.fixersPerBlock.get(state.getBlock().getClass()); }















  
  public CompoundTag fixTileEntityNBT(CompoundTag tag, BlockState state) { return tag; }



  
  private void addPostUpdateBlocks() {
    this.fixersPerBlock.put(ChorusPlantBlock.class, SchematicConversionFixers.FIXER_CHRORUS_PLANT);
    this.fixersPerBlock.put(DoorBlock.class, SchematicConversionFixers.FIXER_DOOR);
    this.fixersPerBlock.put(FenceBlock.class, SchematicConversionFixers.FIXER_FENCE);
    this.fixersPerBlock.put(FenceGateBlock.class, SchematicConversionFixers.FIXER_FENCE_GATE);
    this.fixersPerBlock.put(FireBlock.class, SchematicConversionFixers.FIXER_FIRE);
    this.fixersPerBlock.put(GrassBlock.class, SchematicConversionFixers.FIXER_DIRT_SNOWY);
    this.fixersPerBlock.put(MyceliumBlock.class, SchematicConversionFixers.FIXER_DIRT_SNOWY);
    this.fixersPerBlock.put(PaneBlock.class, SchematicConversionFixers.FIXER_PANE);
    this.fixersPerBlock.put(RepeaterBlock.class, SchematicConversionFixers.FIXER_REDSTONE_REPEATER);
    this.fixersPerBlock.put(RedstoneWireBlock.class, SchematicConversionFixers.FIXER_REDSTONE_WIRE);
    this.fixersPerBlock.put(SnowyBlock.class, SchematicConversionFixers.FIXER_DIRT_SNOWY);
    this.fixersPerBlock.put(StemBlock.class, SchematicConversionFixers.FIXER_STEM);
    this.fixersPerBlock.put(StainedGlassPaneBlock.class, SchematicConversionFixers.FIXER_PANE);
    this.fixersPerBlock.put(StairsBlock.class, SchematicConversionFixers.FIXER_STAIRS);
    this.fixersPerBlock.put(TallFlowerBlock.class, SchematicConversionFixers.FIXER_DOUBLE_PLANT);
    this.fixersPerBlock.put(TallPlantBlock.class, SchematicConversionFixers.FIXER_DOUBLE_PLANT);
    this.fixersPerBlock.put(TripwireBlock.class, SchematicConversionFixers.FIXER_TRIPWIRE);
    this.fixersPerBlock.put(VineBlock.class, SchematicConversionFixers.FIXER_VINE);
    this.fixersPerBlock.put(WallBlock.class, SchematicConversionFixers.FIXER_WALL);

    
    this.fixersPerBlock.put(BannerBlock.class, SchematicConversionFixers.FIXER_BANNER);
    this.fixersPerBlock.put(WallBannerBlock.class, SchematicConversionFixers.FIXER_BANNER_WALL);
    this.fixersPerBlock.put(BedBlock.class, SchematicConversionFixers.FIXER_BED);
    this.fixersPerBlock.put(FlowerPotBlock.class, SchematicConversionFixers.FIXER_FLOWER_POT);
    this.fixersPerBlock.put(NoteBlock.class, SchematicConversionFixers.FIXER_NOTE_BLOCK);
    this.fixersPerBlock.put(SkullBlock.class, SchematicConversionFixers.FIXER_SKULL);
    this.fixersPerBlock.put(WallSkullBlock.class, SchematicConversionFixers.FIXER_SKULL_WALL);
  }
}
