package fi.dy.masa.litematica.schematic.conversion;

import fi.dy.masa.litematica.mixin.IMixinFenceGateBlock;
import fi.dy.masa.litematica.mixin.IMixinRedstoneWireBlock;
import fi.dy.masa.litematica.mixin.IMixinStairsBlock;
import fi.dy.masa.litematica.mixin.IMixinVineBlock;
import fi.dy.masa.litematica.util.PositionUtils;
import net.minecraft.block.AbstractBannerBlock;
import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.block.AbstractSkullBlock;
import net.minecraft.block.AttachedStemBlock;
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
import net.minecraft.block.GourdBlock;
import net.minecraft.block.HorizontalConnectedBlock;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.RepeaterBlock;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.SnowyBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.StemBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.TripwireBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.block.WallBannerBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.block.WallSkullBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.Instrument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Property;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;

public class SchematicConversionFixers
{
    private static final BooleanProperty[] FENCE_WALL_PROP_MAP = new BooleanProperty[] { null, null,
            HorizontalConnectedBlock.NORTH, HorizontalConnectedBlock.SOUTH,
            HorizontalConnectedBlock.WEST, HorizontalConnectedBlock.EAST };

  public static final IStateFixer FIXER_BANNER = (reader, state, pos) -> {
      CompoundTag tag = reader.getBlockEntityData(pos);
      
      if (tag != null) {
        
        DyeColor colorOrig = ((AbstractBannerBlock)state.getBlock()).getColor();
        DyeColor colorFromData = DyeColor.byId(15 - tag.getInt("Base"));
        
        if (colorOrig != colorFromData) {
          
          Integer rotation = (Integer)state.get((Property)BannerBlock.ROTATION);
          
          switch (colorFromData) {
            case WHITE:
              state = Blocks.WHITE_BANNER.getDefaultState(); break;
            case ORANGE: state = Blocks.ORANGE_BANNER.getDefaultState(); break;
            case MAGENTA: state = Blocks.MAGENTA_BANNER.getDefaultState(); break;
            case LIGHT_BLUE: state = Blocks.LIGHT_BLUE_BANNER.getDefaultState(); break;
            case YELLOW: state = Blocks.YELLOW_BANNER.getDefaultState(); break;
            case LIME: state = Blocks.LIME_BANNER.getDefaultState(); break;
            case PINK: state = Blocks.PINK_BANNER.getDefaultState(); break;
            case GRAY: state = Blocks.GRAY_BANNER.getDefaultState(); break;
            case LIGHT_GRAY: state = Blocks.LIGHT_GRAY_BANNER.getDefaultState(); break;
            case CYAN: state = Blocks.CYAN_BANNER.getDefaultState(); break;
            case PURPLE: state = Blocks.PURPLE_BANNER.getDefaultState(); break;
            case BLUE: state = Blocks.BLUE_BANNER.getDefaultState(); break;
            case BROWN: state = Blocks.BROWN_BANNER.getDefaultState(); break;
            case GREEN: state = Blocks.GREEN_BANNER.getDefaultState(); break;
            case RED: state = Blocks.RED_BANNER.getDefaultState(); break;
            case BLACK: state = Blocks.BLACK_BANNER.getDefaultState();
              break;
          } 
          state = (BlockState)state.with((Property)BannerBlock.ROTATION, rotation);
        } 
      } 
      
      return state;
    };
  
  public static final IStateFixer FIXER_BANNER_WALL = (reader, state, pos) -> {
      CompoundTag tag = reader.getBlockEntityData(pos);
      
      if (tag != null) {
        
        DyeColor colorOrig = ((AbstractBannerBlock)state.getBlock()).getColor();
        DyeColor colorFromData = DyeColor.byId(15 - tag.getInt("Base"));
        
        if (colorOrig != colorFromData) {
          
          Direction facing = (Direction)state.get((Property)WallBannerBlock.FACING);
          
          switch (colorFromData) {
            case WHITE:
              state = Blocks.WHITE_WALL_BANNER.getDefaultState(); break;
            case ORANGE: state = Blocks.ORANGE_WALL_BANNER.getDefaultState(); break;
            case MAGENTA: state = Blocks.MAGENTA_WALL_BANNER.getDefaultState(); break;
            case LIGHT_BLUE: state = Blocks.LIGHT_BLUE_WALL_BANNER.getDefaultState(); break;
            case YELLOW: state = Blocks.YELLOW_WALL_BANNER.getDefaultState(); break;
            case LIME: state = Blocks.LIME_WALL_BANNER.getDefaultState(); break;
            case PINK: state = Blocks.PINK_WALL_BANNER.getDefaultState(); break;
            case GRAY: state = Blocks.GRAY_WALL_BANNER.getDefaultState(); break;
            case LIGHT_GRAY: state = Blocks.LIGHT_GRAY_WALL_BANNER.getDefaultState(); break;
            case CYAN: state = Blocks.CYAN_WALL_BANNER.getDefaultState(); break;
            case PURPLE: state = Blocks.PURPLE_WALL_BANNER.getDefaultState(); break;
            case BLUE: state = Blocks.BLUE_WALL_BANNER.getDefaultState(); break;
            case BROWN: state = Blocks.BROWN_WALL_BANNER.getDefaultState(); break;
            case GREEN: state = Blocks.GREEN_WALL_BANNER.getDefaultState(); break;
            case RED: state = Blocks.RED_WALL_BANNER.getDefaultState(); break;
            case BLACK: state = Blocks.BLACK_WALL_BANNER.getDefaultState();
              break;
          } 
          state = (BlockState)state.with((Property)WallBannerBlock.FACING, (Comparable)facing);
        } 
      } 
      
      return state;
    };
  
  public static final IStateFixer FIXER_BED = (reader, state, pos) -> {
      CompoundTag tag = reader.getBlockEntityData(pos);
      
      if (tag != null && tag.contains("color", 3)) {
        
        int colorId = tag.getInt("color");
        Direction facing = (Direction)state.get((Property)BedBlock.FACING);
        BedPart part = (BedPart)state.get((Property)BedBlock.PART);
        Boolean occupied = (Boolean)state.get((Property)BedBlock.OCCUPIED);
        
        switch (colorId) {
          case 0:
            state = Blocks.WHITE_BED.getDefaultState(); break;
          case 1: state = Blocks.ORANGE_BED.getDefaultState(); break;
          case 2: state = Blocks.MAGENTA_BED.getDefaultState(); break;
          case 3: state = Blocks.LIGHT_BLUE_BED.getDefaultState(); break;
          case 4: state = Blocks.YELLOW_BED.getDefaultState(); break;
          case 5: state = Blocks.LIME_BED.getDefaultState(); break;
          case 6: state = Blocks.PINK_BED.getDefaultState(); break;
          case 7: state = Blocks.GRAY_BED.getDefaultState(); break;
          case 8: state = Blocks.LIGHT_GRAY_BED.getDefaultState(); break;
          case 9: state = Blocks.CYAN_BED.getDefaultState(); break;
          case 10: state = Blocks.PURPLE_BED.getDefaultState(); break;
          case 11: state = Blocks.BLUE_BED.getDefaultState(); break;
          case 12: state = Blocks.BROWN_BED.getDefaultState(); break;
          case 13: state = Blocks.GREEN_BED.getDefaultState(); break;
          case 14: state = Blocks.RED_BED.getDefaultState(); break;
          case 15: state = Blocks.BLACK_BED.getDefaultState(); break;
          default: return state;
        } 


        
        state = (BlockState)((BlockState)((BlockState)state.with((Property)BedBlock.FACING, (Comparable)facing)).with((Property)BedBlock.PART, (Comparable)part)).with((Property)BedBlock.OCCUPIED, occupied);
      } 
      
      return state;
    };

  
  public static final IStateFixer FIXER_CHRORUS_PLANT = (reader, state, pos) -> ((ChorusPlantBlock)state.getBlock()).withConnectionProperties(reader, pos);

  
  public static final IStateFixer FIXER_DIRT_SNOWY = (reader, state, pos) -> {
      Block block = reader.getBlockState(pos.up()).getBlock();
      return (BlockState)state.with((Property)SnowyBlock.SNOWY, Boolean.valueOf((block == Blocks.SNOW_BLOCK || block == Blocks.SNOW)));
    };
  
  public static final IStateFixer FIXER_DOOR = (reader, state, pos) -> {
      if (state.get((Property)DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
        
        BlockState stateLower = reader.getBlockState(pos.down());
        
        if (stateLower.getBlock() == state.getBlock())
        {
          state = (BlockState)state.with((Property)DoorBlock.FACING, stateLower.get((Property)DoorBlock.FACING));
          state = (BlockState)state.with((Property)DoorBlock.OPEN, stateLower.get((Property)DoorBlock.OPEN));
        }
      
      } else {
        
        BlockState stateUpper = reader.getBlockState(pos.up());
        
        if (stateUpper.getBlock() == state.getBlock()) {
          
          state = (BlockState)state.with((Property)DoorBlock.HINGE, stateUpper.get((Property)DoorBlock.HINGE));
          state = (BlockState)state.with((Property)DoorBlock.POWERED, stateUpper.get((Property)DoorBlock.POWERED));
        } 
      } 
      
      return state;
    };
  
  public static final IStateFixer FIXER_DOUBLE_PLANT = (reader, state, pos) -> {
      if (state.get((Property)TallPlantBlock.HALF) == DoubleBlockHalf.UPPER) {
        
        BlockState stateLower = reader.getBlockState(pos.down());
        
        if (stateLower.getBlock() instanceof TallPlantBlock)
        {
          state = (BlockState)stateLower.with((Property)TallPlantBlock.HALF, (Comparable)DoubleBlockHalf.UPPER);
        }
      } 
      
      return state;
    };
  
  public static final IStateFixer FIXER_FENCE = (reader, state, pos) -> {
      FenceBlock fence = (FenceBlock)state.getBlock();
      
      for (Direction side : PositionUtils.FACING_HORIZONTALS) {
        
        BlockPos posAdj = pos.offset(side);
        BlockState stateAdj = reader.getBlockState(posAdj);
        Direction sideOpposite = side.getOpposite();
        boolean flag = stateAdj.isSideSolidFullSquare(reader, posAdj, sideOpposite);
        state = (BlockState)state.with((Property)FENCE_WALL_PROP_MAP[side.getId()], Boolean.valueOf(fence.canConnect(stateAdj, flag, sideOpposite)));
      } 
      
      return state;
    };
  
  public static final IStateFixer FIXER_FENCE_GATE = (reader, state, pos) -> {
      FenceGateBlock gate = (FenceGateBlock)state.getBlock();
      Direction facing = (Direction)state.get((Property)FenceGateBlock.FACING);
      boolean inWall = false;
      
      if (facing.getAxis() == Direction.Axis.X) {

        
        inWall = (((IMixinFenceGateBlock)gate).invokeIsWall(reader.getBlockState(pos.offset(Direction.NORTH))) || ((IMixinFenceGateBlock)gate).invokeIsWall(reader.getBlockState(pos.offset(Direction.SOUTH))));
      
      }
      else {
        
        inWall = (((IMixinFenceGateBlock)gate).invokeIsWall(reader.getBlockState(pos.offset(Direction.WEST))) || ((IMixinFenceGateBlock)gate).invokeIsWall(reader.getBlockState(pos.offset(Direction.EAST))));
      } 
      
      return (BlockState)state.with((Property)FenceGateBlock.IN_WALL, Boolean.valueOf(inWall));
    };
  
  public static final IStateFixer FIXER_FIRE = (reader, state, pos) -> {
      FireBlock fire = (FireBlock)state.getBlock();
      return fire.getStateForPosition(reader, pos);
    };
  
  public static final IStateFixer FIXER_FLOWER_POT = (reader, state, pos) -> {
      CompoundTag tag = reader.getBlockEntityData(pos);
      
      if (tag != null)
      
      { String itemName = tag.getString("Item");
        
        if (itemName.length() > 0)
        
        { int meta = tag.getInt("Data");
          
                switch (itemName)
                {
                case "minecraft:sapling":
                    if (meta == 0)
                        return Blocks.POTTED_OAK_SAPLING.getDefaultState();
                    if (meta == 1)
                        return Blocks.POTTED_SPRUCE_SAPLING.getDefaultState();
                    if (meta == 2)
                        return Blocks.POTTED_BIRCH_SAPLING.getDefaultState();
                    if (meta == 3)
                        return Blocks.POTTED_JUNGLE_SAPLING.getDefaultState();
                    if (meta == 4)
                        return Blocks.POTTED_ACACIA_SAPLING.getDefaultState();
                    if (meta != 5) break;
                    return Blocks.POTTED_DARK_OAK_SAPLING.getDefaultState();
                case "minecraft:tallgrass":
                    if (meta == 0)
                        return Blocks.POTTED_DEAD_BUSH.getDefaultState();
                    if (meta != 2) break;
                    return Blocks.POTTED_FERN.getDefaultState();
                case "minecraft:red_flower":
                    if (meta == 0)
                        return Blocks.POTTED_POPPY.getDefaultState();
                    if (meta == 1)
                        return Blocks.POTTED_BLUE_ORCHID.getDefaultState();
                    if (meta == 2)
                        return Blocks.POTTED_ALLIUM.getDefaultState();
                    if (meta == 3)
                        return Blocks.POTTED_AZURE_BLUET.getDefaultState();
                    if (meta == 4)
                        return Blocks.POTTED_RED_TULIP.getDefaultState();
                    if (meta == 5)
                        return Blocks.POTTED_ORANGE_TULIP.getDefaultState();
                    if (meta == 6)
                        return Blocks.POTTED_WHITE_TULIP.getDefaultState();
                    if (meta == 7)
                        return Blocks.POTTED_PINK_TULIP.getDefaultState();
                    if (meta != 8) break;
                    return Blocks.POTTED_OXEYE_DAISY.getDefaultState();
                case "minecraft:yellow_flower":
                    return Blocks.POTTED_DANDELION.getDefaultState();
                case "minecraft:brown_mushroom":
                    return Blocks.POTTED_BROWN_MUSHROOM.getDefaultState();
                case "minecraft:red_mushroom":
                    return Blocks.POTTED_RED_MUSHROOM.getDefaultState();
                case "minecraft:deadbush":
                    return Blocks.POTTED_DEAD_BUSH.getDefaultState();
                case "minecraft:cactus":
                    return Blocks.POTTED_CACTUS.getDefaultState();
                default:
                    return state;
                
                }
            }
        }
        return state;
    };
  
  public static final IStateFixer FIXER_NOTE_BLOCK = (reader, state, pos) -> {
      CompoundTag tag = reader.getBlockEntityData(pos);
      
      if (tag != null)
      {


        
            state = (BlockState) ((BlockState) ((BlockState) state.with((Property) NoteBlock.POWERED,
                    Boolean.valueOf(tag.getBoolean("powered")))).with((Property) NoteBlock.NOTE,
                            Integer.valueOf(MathHelper.clamp(tag.getByte("note"), 0, 24)))).with(
                                    (Property) NoteBlock.INSTRUMENT,
                                    (Comparable) Instrument.fromBlockState(reader.getBlockState(pos.down())));
        }
      
      return state;
    };
  
  public static final IStateFixer FIXER_PANE = (reader, state, pos) -> {
      PaneBlock pane = (PaneBlock)state.getBlock();
      
      for (Direction side : PositionUtils.FACING_HORIZONTALS) {
        
        BlockPos posAdj = pos.offset(side);
        BlockState stateAdj = reader.getBlockState(posAdj);
        Direction sideOpposite = side.getOpposite();
        boolean flag = stateAdj.isSideSolidFullSquare(reader, posAdj, sideOpposite);
        state = (BlockState)state.with((Property)FENCE_WALL_PROP_MAP[side.getId()], Boolean.valueOf(pane.connectsTo(stateAdj, flag)));
      } 
      
      return state;
    };

  
  public static final IStateFixer FIXER_REDSTONE_REPEATER = (reader, state, pos) -> (BlockState)state.with((Property)RepeaterBlock.LOCKED, Boolean.valueOf(getIsRepeaterPoweredOnSide(reader, pos, state)));

  
  public static final IStateFixer FIXER_REDSTONE_WIRE = (reader, state, pos) -> {
      RedstoneWireBlock wire = (RedstoneWireBlock)state.getBlock();
      
      return (BlockState)((BlockState)((BlockState)((BlockState)state
        .with((Property)RedstoneWireBlock.WIRE_CONNECTION_WEST, (Comparable)((IMixinRedstoneWireBlock)wire).invokeGetSide(reader, pos, Direction.WEST)))
        .with((Property)RedstoneWireBlock.WIRE_CONNECTION_EAST, (Comparable)((IMixinRedstoneWireBlock)wire).invokeGetSide(reader, pos, Direction.EAST)))
        .with((Property)RedstoneWireBlock.WIRE_CONNECTION_NORTH, (Comparable)((IMixinRedstoneWireBlock)wire).invokeGetSide(reader, pos, Direction.NORTH)))
        .with((Property)RedstoneWireBlock.WIRE_CONNECTION_SOUTH, (Comparable)((IMixinRedstoneWireBlock)wire).invokeGetSide(reader, pos, Direction.SOUTH));
    };
  
  public static final IStateFixer FIXER_SKULL = (reader, state, pos) -> {
      CompoundTag tag = reader.getBlockEntityData(pos);
      
      if (tag != null) {
        
        int id = MathHelper.clamp(tag.getByte("SkullType"), 0, 5);

        
        if (id == 2) { id = 3; } else if (id == 3) { id = 2; }
        
        SkullBlock.SkullType typeOrig = ((AbstractSkullBlock)state.getBlock()).getSkullType();
        SkullBlock.Type type = SkullBlock.Type.values()[id];
        
        if (typeOrig != type)
        {
          if (type == SkullBlock.Type.SKELETON) {
            
            state = Blocks.SKELETON_SKULL.getDefaultState();
          }
          else if (type == SkullBlock.Type.WITHER_SKELETON) {
            
            state = Blocks.WITHER_SKELETON_SKULL.getDefaultState();
          }
          else if (type == SkullBlock.Type.PLAYER) {
            
            state = Blocks.PLAYER_HEAD.getDefaultState();
          }
          else if (type == SkullBlock.Type.ZOMBIE) {
            
            state = Blocks.ZOMBIE_HEAD.getDefaultState();
          }
          else if (type == SkullBlock.Type.CREEPER) {
            
            state = Blocks.CREEPER_HEAD.getDefaultState();
          }
          else if (type == SkullBlock.Type.DRAGON) {
            
            state = Blocks.DRAGON_HEAD.getDefaultState();
          } 
        }
        
        state = (BlockState)state.with((Property)BannerBlock.ROTATION, Integer.valueOf(MathHelper.clamp(tag.getByte("Rot"), 0, 15)));
      } 
      
      return state;
    };
  
  public static final IStateFixer FIXER_SKULL_WALL = (reader, state, pos) -> {
      CompoundTag tag = reader.getBlockEntityData(pos);
      
      if (tag != null) {
        
        int id = MathHelper.clamp(tag.getByte("SkullType"), 0, 5);

        
        if (id == 2) { id = 3; } else if (id == 3) { id = 2; }
        
        SkullBlock.SkullType typeOrig = ((AbstractSkullBlock)state.getBlock()).getSkullType();
        SkullBlock.Type type = SkullBlock.Type.values()[id];
        
        if (typeOrig != type) {
          
          Direction facing = (Direction)state.get((Property)WallSkullBlock.FACING);
          
          if (type == SkullBlock.Type.SKELETON) {
            
            state = Blocks.SKELETON_WALL_SKULL.getDefaultState();
          }
          else if (type == SkullBlock.Type.WITHER_SKELETON) {
            
            state = Blocks.WITHER_SKELETON_WALL_SKULL.getDefaultState();
          }
          else if (type == SkullBlock.Type.PLAYER) {
            
            state = Blocks.PLAYER_WALL_HEAD.getDefaultState();
          }
          else if (type == SkullBlock.Type.ZOMBIE) {
            
            state = Blocks.ZOMBIE_WALL_HEAD.getDefaultState();
          }
          else if (type == SkullBlock.Type.CREEPER) {
            
            state = Blocks.CREEPER_WALL_HEAD.getDefaultState();
          }
          else if (type == SkullBlock.Type.DRAGON) {
            
            state = Blocks.DRAGON_WALL_HEAD.getDefaultState();
          } 
          
          state = (BlockState)state.with((Property)WallSkullBlock.FACING, (Comparable)facing);
        } 
      } 
      
      return state;
    };

  
    public static final IStateFixer FIXER_STAIRS = (reader, state, pos) -> (BlockState) state.with(
            (Property) StairsBlock.SHAPE, (Comparable) IMixinStairsBlock.invokeGetStairShape(state, reader, pos));
  
  public static final IStateFixer FIXER_STEM = (reader, state, pos) -> {
      StemBlock stem = (StemBlock)state.getBlock();
      GourdBlock crop = stem.getGourdBlock();
      
      for (Direction side : PositionUtils.FACING_HORIZONTALS) {
        
        BlockPos posAdj = pos.offset(side);
        BlockState stateAdj = reader.getBlockState(posAdj);
        Block blockAdj = stateAdj.getBlock();
        
        if (blockAdj == crop || (stem == Blocks.PUMPKIN_STEM && blockAdj == Blocks.CARVED_PUMPKIN))
        {
          return (BlockState)crop.getAttachedStem().getDefaultState().with((Property)AttachedStemBlock.FACING, (Comparable)side);
        }
      } 
      
      return state;
    };
  
  public static final IStateFixer FIXER_TRIPWIRE = (reader, state, pos) -> {
      TripwireBlock wire = (TripwireBlock)state.getBlock();
      
      return (BlockState)((BlockState)((BlockState)((BlockState)state
        .with((Property)TripwireBlock.NORTH, Boolean.valueOf(wire.shouldConnectTo(reader.getBlockState(pos.north()), Direction.NORTH))))
        .with((Property)TripwireBlock.SOUTH, Boolean.valueOf(wire.shouldConnectTo(reader.getBlockState(pos.south()), Direction.SOUTH))))
        .with((Property)TripwireBlock.WEST, Boolean.valueOf(wire.shouldConnectTo(reader.getBlockState(pos.west()), Direction.WEST))))
        .with((Property)TripwireBlock.EAST, Boolean.valueOf(wire.shouldConnectTo(reader.getBlockState(pos.east()), Direction.EAST)));
    };
  
  public static final IStateFixer FIXER_VINE = (reader, state, pos) -> {
      VineBlock vine = (VineBlock)state.getBlock();
      return (BlockState)state.with((Property)VineBlock.UP, Boolean.valueOf(((IMixinVineBlock)vine).invokeShouldConnectUp(reader, pos.up(), Direction.UP)));
    };
  
  public static final IStateFixer FIXER_WALL = (reader, state, pos) -> {
      boolean[] sides = new boolean[6];
      
      for (Direction side : PositionUtils.FACING_HORIZONTALS) {
        
        BlockPos posAdj = pos.offset(side);
        BlockState stateAdj = reader.getBlockState(posAdj);
        
        boolean val = wallAttachesTo(stateAdj, side.getOpposite(), reader, posAdj);
        state = (BlockState)state.with((Property)FENCE_WALL_PROP_MAP[side.getId()], Boolean.valueOf(val));
        sides[side.getId()] = val;
      } 
      
      boolean south = sides[Direction.SOUTH.getId()];
      boolean west = sides[Direction.WEST.getId()];
      boolean north = sides[Direction.NORTH.getId()];
      boolean east = sides[Direction.EAST.getId()];
      boolean up = (((!south || west || !north || east) && (south || !west || north || !east)) || !reader.getBlockState(pos.up()).isAir());
      
      return (BlockState)state.with((Property)WallBlock.UP, Boolean.valueOf(up));
    };

  
  private static boolean wallAttachesTo(BlockState state, Direction side, BlockView world, BlockPos pos) {
    Block block = state.getBlock();
    boolean flag1 = state.isSideSolidFullSquare(world, pos, side);
    boolean flag2 = (block.matches(BlockTags.WALLS) || (block instanceof FenceGateBlock && FenceGateBlock.canWallConnect(state, side)));
    return ((!WallBlock.canConnect(block) && flag1) || flag2);
  }

  
  private static boolean getIsRepeaterPoweredOnSide(BlockView reader, BlockPos pos, BlockState stateRepeater) {
    Direction facing = (Direction)stateRepeater.get((Property)RepeaterBlock.FACING);
    Direction sideLeft = facing.rotateYCounterclockwise();
    Direction sideRight = facing.rotateYClockwise();
    
    return (getRepeaterPowerOnSide(reader, pos.offset(sideLeft), sideLeft) > 0 || 
      getRepeaterPowerOnSide(reader, pos.offset(sideRight), sideRight) > 0);
  }

  
  private static int getRepeaterPowerOnSide(BlockView reader, BlockPos pos, Direction side) {
    BlockState state = reader.getBlockState(pos);
    Block block = state.getBlock();
    
    if (AbstractRedstoneGateBlock.isRedstoneGate(state)) {
      
      if (block == Blocks.REDSTONE_BLOCK)
      {
        return 15;
      }

      
      return (block == Blocks.REDSTONE_WIRE) ? ((Integer)state.get((Property)RedstoneWireBlock.POWER)).intValue() : state.getStrongRedstonePower(reader, pos, side);
    } 


    
    return 0;
  }
  
  public static interface IStateFixer {
    BlockState fixState(IBlockReaderWithData param1IBlockReaderWithData, BlockState param1BlockState, BlockPos param1BlockPos);
  }
}