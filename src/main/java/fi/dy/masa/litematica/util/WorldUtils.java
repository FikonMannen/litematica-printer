package fi.dy.masa.litematica.util;

import net.minecraft.block.enums.WallMountLocation;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import com.mojang.datafixers.DataFixer;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ComparatorBlock;
import net.minecraft.block.RepeaterBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.structure.Structure;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialCache;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.SchematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.tool.ToolMode;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.hotkeys.KeybindMulti;
import fi.dy.masa.malilib.interfaces.IStringConsumer;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.SubChunkPos;

import net.minecraft.block.AbstractButtonBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.DropperBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.FurnaceBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.LogBlock;
import net.minecraft.block.ObserverBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.PumpkinBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.SignBlock;
import net.minecraft.block.TorchBlock;
import net.minecraft.block.EndPortalFrameBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.TripwireHookBlock;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.WallRedstoneTorchBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.block.BellBlock;
import net.minecraft.block.GrindstoneBlock;
import java.util.LinkedHashMap;
import java.util.Map;

public class WorldUtils {

    private static class FacingData {
        public int type;
        public boolean isReversed;

        FacingData(int type, boolean isrev) {
            this.type = type;
            this.isReversed = isrev;
        }
    }

    private static final Map<Class<? extends Block>, FacingData> facingMap = new LinkedHashMap<Class<? extends Block>, FacingData>();

    private static final List<PositionCache> EASY_PLACE_POSITIONS = new ArrayList<>();
    private static boolean preventOnBlockAdded;
    private static boolean setupFacing = false;

    private static void addFD(final Class<? extends Block> c, FacingData data) {
        facingMap.put(c, data);
    }

    private static void setUpFacingData() {
        setupFacing = true;

        /*
         * 0 = Normal up/down/east/west/south/north directions 1 = Horizontal directions
         * 2 = Wall Attactchable block
         * 
         * 
         * TODO: THIS CODE MUST BE CLEANED UP. 
         */

        // All directions, reverse of what player is facing
        addFD(PistonBlock.class, new FacingData(0, true));
        addFD(DispenserBlock.class, new FacingData(0, true));
        addFD(DropperBlock.class, new FacingData(0, true));

        // All directions, normal direction of player
        addFD(ObserverBlock.class, new FacingData(0, false));

        // Horizontal directions, normal direction
        addFD(StairsBlock.class, new FacingData(1, false));
        addFD(DoorBlock.class, new FacingData(1, false));
        addFD(BedBlock.class, new FacingData(1, false));
        addFD(FenceGateBlock.class, new FacingData(1, false));

        // Horizontal directions, reverse of what player is facing
        addFD(ChestBlock.class, new FacingData(1, true));
        addFD(RepeaterBlock.class, new FacingData(1, true));
        addFD(ComparatorBlock.class, new FacingData(1, true));
        addFD(EnderChestBlock.class, new FacingData(1, true));
        addFD(FurnaceBlock.class, new FacingData(1, true));
        addFD(PumpkinBlock.class, new FacingData(1, true));
        addFD(EndPortalFrameBlock.class, new FacingData(1, true));

        // Top/bottom placable side mountable blocks
        addFD(LeverBlock.class, new FacingData(2, false));
        addFD(AbstractButtonBlock.class, new FacingData(2, false));
        addFD(BellBlock.class, new FacingData(2, false));
        addFD(GrindstoneBlock.class, new FacingData(2, false));

    }

    // TODO: This must be moved to another class and not be static.
    private static FacingData getFacingData(BlockState state) {
        if (!setupFacing) {
            setUpFacingData();
        }
        Block block = state.getBlock();
        for (final Class<? extends Block> c : facingMap.keySet()) {
            if (c.isInstance(block)) {
                return facingMap.get(c);
            }
        }
        return null;
    }

    public static boolean shouldPreventOnBlockAdded() 
    {
        return preventOnBlockAdded;
    }

    public static void setShouldPreventOnBlockAdded(boolean prevent) 
    {
        preventOnBlockAdded = prevent;
    }
    
    public static boolean convertSchematicaSchematicToLitematicaSchematic(
            File inputDir, String inputFileName, File outputDir, String outputFileName, boolean ignoreEntities, boolean override, IStringConsumer feedback)
    {
        LitematicaSchematic litematicaSchematic = convertSchematicaSchematicToLitematicaSchematic(inputDir, inputFileName, ignoreEntities, feedback);
        return litematicaSchematic != null && litematicaSchematic.writeToFile(outputDir, outputFileName, override);
    }

    @Nullable
    public static LitematicaSchematic convertSchematicaSchematicToLitematicaSchematic(File inputDir, String inputFileName,
            boolean ignoreEntities, IStringConsumer feedback)
    {
        SchematicaSchematic schematic = SchematicaSchematic.createFromFile(new File(inputDir, inputFileName));

        if (schematic == null)
        {
            feedback.setString("litematica.error.schematic_conversion.schematic_to_litematica.failed_to_read_schematic");
            return null;
        }

        WorldSchematic world = SchematicWorldHandler.createSchematicWorld();

        loadChunksSchematicWorld(world, BlockPos.ORIGIN, schematic.getSize());
        StructurePlacementData placementSettings = new StructurePlacementData();
        placementSettings.setIgnoreEntities(ignoreEntities);
        schematic.placeSchematicDirectlyToChunks(world, BlockPos.ORIGIN, placementSettings);

        String subRegionName = FileUtils.getNameWithoutExtension(inputFileName) + " (Converted Schematic)";
        AreaSelection area = new AreaSelection();
        area.setName(subRegionName);
        subRegionName = area.createNewSubRegionBox(BlockPos.ORIGIN, subRegionName);
        area.setSelectedSubRegionBox(subRegionName);
        Box box = area.getSelectedSubRegionBox();
        area.setSubRegionCornerPos(box, Corner.CORNER_1, BlockPos.ORIGIN);
        area.setSubRegionCornerPos(box, Corner.CORNER_2, (new BlockPos(schematic.getSize())).add(-1, -1, -1));

        LitematicaSchematic litematicaSchematic = LitematicaSchematic.createFromWorld(world, area, false, "?", feedback);

        if (litematicaSchematic != null && ignoreEntities == false)
        {
            litematicaSchematic.takeEntityDataFromSchematicaSchematic(schematic, subRegionName);
        }
        else
        {
            feedback.setString("litematica.error.schematic_conversion.schematic_to_litematica.failed_to_create_schematic");
        }

        return litematicaSchematic;
    }

    public static boolean convertStructureToLitematicaSchematic(File structureDir, String structureFileName,
            File outputDir, String outputFileName, boolean ignoreEntities, boolean override, IStringConsumer feedback)
    {
        LitematicaSchematic litematicaSchematic = convertStructureToLitematicaSchematic(structureDir, structureFileName, ignoreEntities, feedback);
        return litematicaSchematic != null && litematicaSchematic.writeToFile(outputDir, outputFileName, override);
    }

    @Nullable
    public static LitematicaSchematic convertStructureToLitematicaSchematic(File structureDir, String structureFileName,
            boolean ignoreEntities, IStringConsumer feedback)
    {
        DataFixer fixer = MinecraftClient.getInstance().getDataFixer();
        File file = new File(structureDir, structureFileName);

        try
        {
            InputStream is = new FileInputStream(file);
            Structure template = readTemplateFromStream(is, fixer);
            is.close();

            WorldSchematic world = SchematicWorldHandler.createSchematicWorld();

            loadChunksSchematicWorld(world, BlockPos.ORIGIN, template.getSize());

            StructurePlacementData placementSettings = new StructurePlacementData();
            placementSettings.setIgnoreEntities(ignoreEntities);
            template.method_15172(world, BlockPos.ORIGIN, placementSettings, 0x12);

            String subRegionName = FileUtils.getNameWithoutExtension(structureFileName) + " (Converted Structure)";
            AreaSelection area = new AreaSelection();
            area.setName(subRegionName);
            subRegionName = area.createNewSubRegionBox(BlockPos.ORIGIN, subRegionName);
            area.setSelectedSubRegionBox(subRegionName);
            Box box = area.getSelectedSubRegionBox();
            area.setSubRegionCornerPos(box, Corner.CORNER_1, BlockPos.ORIGIN);
            area.setSubRegionCornerPos(box, Corner.CORNER_2, template.getSize().add(-1, -1, -1));

            LitematicaSchematic litematicaSchematic = LitematicaSchematic.createFromWorld(world, area, ignoreEntities, template.getAuthor(), feedback);

            if (litematicaSchematic != null)
            {
                //litematicaSchematic.takeEntityDataFromVanillaStructure(template, subRegionName); // TODO
            }
            else
            {
                feedback.setString("litematica.error.schematic_conversion.structure_to_litematica_failed");
            }

            return litematicaSchematic;
        }
        catch (Throwable t)
        {
        }

        return null;
    }

    public static boolean convertLitematicaSchematicToSchematicaSchematic(
            File inputDir, String inputFileName, File outputDir, String outputFileName, boolean ignoreEntities, boolean override, IStringConsumer feedback)
    {
        //SchematicaSchematic schematic = convertLitematicaSchematicToSchematicaSchematic(inputDir, inputFileName, ignoreEntities, feedback);
        //return schematic != null && schematic.writeToFile(outputDir, outputFileName, override, feedback);
        // TODO 1.13
        return false;
    }

    @Nullable
    public static SchematicaSchematic convertLitematicaSchematicToSchematicaSchematic(File inputDir, String inputFileName, boolean ignoreEntities, IStringConsumer feedback)
    {
        LitematicaSchematic litematicaSchematic = LitematicaSchematic.createFromFile(inputDir, inputFileName);

        if (litematicaSchematic == null)
        {
            feedback.setString("litematica.error.schematic_conversion.litematica_to_schematic.failed_to_read_schematic");
            return null;
        }

        WorldSchematic world = SchematicWorldHandler.createSchematicWorld();

        BlockPos size = new BlockPos(litematicaSchematic.getTotalSize());
        loadChunksSchematicWorld(world, BlockPos.ORIGIN, size);
        SchematicPlacement schematicPlacement = SchematicPlacement.createForSchematicConversion(litematicaSchematic, BlockPos.ORIGIN);
        litematicaSchematic.placeToWorld(world, schematicPlacement, false); // TODO use a per-chunk version for a bit more speed

        SchematicaSchematic schematic = SchematicaSchematic.createFromWorld(world, BlockPos.ORIGIN, size, ignoreEntities);

        if (schematic == null)
        {
            feedback.setString("litematica.error.schematic_conversion.litematica_to_schematic.failed_to_create_schematic");
        }

        return schematic;
    }

    public static boolean convertLitematicaSchematicToVanillaStructure(
            File inputDir, String inputFileName, File outputDir, String outputFileName, boolean ignoreEntities, boolean override, IStringConsumer feedback)
    {
        Structure template = convertLitematicaSchematicToVanillaStructure(inputDir, inputFileName, ignoreEntities, feedback);
        return writeVanillaStructureToFile(template, outputDir, outputFileName, override, feedback);
    }

    @Nullable
    public static Structure convertLitematicaSchematicToVanillaStructure(File inputDir, String inputFileName, boolean ignoreEntities, IStringConsumer feedback)
    {
        LitematicaSchematic litematicaSchematic = LitematicaSchematic.createFromFile(inputDir, inputFileName);

        if (litematicaSchematic == null)
        {
            feedback.setString("litematica.error.schematic_conversion.litematica_to_schematic.failed_to_read_schematic");
            return null;
        }

        WorldSchematic world = SchematicWorldHandler.createSchematicWorld();

        BlockPos size = new BlockPos(litematicaSchematic.getTotalSize());
        loadChunksSchematicWorld(world, BlockPos.ORIGIN, size);
        SchematicPlacement schematicPlacement = SchematicPlacement.createForSchematicConversion(litematicaSchematic, BlockPos.ORIGIN);
        litematicaSchematic.placeToWorld(world, schematicPlacement, false); // TODO use a per-chunk version for a bit more speed

        Structure template = new Structure();
        template.method_15174(world, BlockPos.ORIGIN, size, ignoreEntities == false, Blocks.STRUCTURE_VOID); // takeBlocksFromWorld

        return template;
    }

    private static boolean writeVanillaStructureToFile(Structure template, File dir, String fileNameIn, boolean override, IStringConsumer feedback)
    {
        String fileName = fileNameIn;
        String extension = ".nbt";

        if (fileName.endsWith(extension) == false)
        {
            fileName = fileName + extension;
        }

        File file = new File(dir, fileName);
        FileOutputStream os = null;

        try
        {
            if (dir.exists() == false && dir.mkdirs() == false)
            {
                feedback.setString(StringUtils.translate("litematica.error.schematic_write_to_file_failed.directory_creation_failed", dir.getAbsolutePath()));
                return false;
            }

            if (override == false && file.exists())
            {
                feedback.setString(StringUtils.translate("litematica.error.structure_write_to_file_failed.exists", file.getAbsolutePath()));
                return false;
            }

            CompoundTag tag = template.toTag(new CompoundTag());
            os = new FileOutputStream(file);
            NbtIo.writeCompressed(tag, os);
            os.close();

            return true;
        }
        catch (Exception e)
        {
            feedback.setString(StringUtils.translate("litematica.error.structure_write_to_file_failed.exception", file.getAbsolutePath()));
        }

        return false;
    }

    private static Structure readTemplateFromStream(InputStream stream, DataFixer fixer) throws IOException
    {
        CompoundTag nbt = NbtIo.readCompressed(stream);
        Structure template = new Structure();
        //template.read(fixer.process(FixTypes.STRUCTURE, nbt));
        template.fromTag(nbt);

        return template;
    }

    public static boolean isClientChunkLoaded(ClientWorld world, int chunkX, int chunkZ)
    {
        return ((ClientChunkManager) world.getChunkManager()).method_2857(chunkX, chunkZ, ChunkStatus.FULL, false) != null;
    }

    public static void loadChunksSchematicWorld(WorldSchematic world, BlockPos origin, Vec3i areaSize)
    {
        BlockPos posEnd = origin.add(PositionUtils.getRelativeEndPositionFromAreaSize(areaSize));
        BlockPos posMin = PositionUtils.getMinCorner(origin, posEnd);
        BlockPos posMax = PositionUtils.getMaxCorner(origin, posEnd);
        final int cxMin = posMin.getX() >> 4;
        final int czMin = posMin.getZ() >> 4;
        final int cxMax = posMax.getX() >> 4;
        final int czMax = posMax.getZ() >> 4;

        for (int cz = czMin; cz <= czMax; ++cz)
        {
            for (int cx = cxMin; cx <= cxMax; ++cx)
            {
                world.getChunkProvider().loadChunk(cx, cz);
            }
        }
    }

    public static void setToolModeBlockState(ToolMode mode, boolean primary, MinecraftClient mc)
    {
        HitResult trace = RayTraceUtils.getRayTraceFromEntity(mc.world, mc.player, true, 6);
        BlockState state = Blocks.AIR.getDefaultState();

        if (trace != null &&
            trace.getType() == HitResult.Type.BLOCK)
        {
            state = mc.world.getBlockState(((BlockHitResult) trace).getBlockPos());
        }

        if (primary)
        {
            mode.setPrimaryBlock(state);
        }
        else
        {
            mode.setSecondaryBlock(state);
        }
    }
    /**
     * New doSchematicWorldPickBlock that allows you to choose which block you want
     */
    public static boolean doSchematicWorldPickBlock(boolean closest, MinecraftClient mc, BlockState preference, BlockPos pos)
    {

        World world = SchematicWorldHandler.getSchematicWorld();

        ItemStack stack = MaterialCache.getInstance().getItemForState(preference, world, pos);

        if (stack.isEmpty() == false)
        {
            PlayerInventory inv = mc.player.inventory;

            if (mc.player.abilities.creativeMode)
            {
                // BlockEntity te = world.getBlockEntity(pos);

                // The creative mode pick block with NBT only works correctly
                // if the server world doesn't have a TileEntity in that position.
                // Otherwise it would try to write whatever that TE is into the picked
                // ItemStack.
                // if (GuiBase.isCtrlDown() && te != null && mc.world.isAir(pos)) {
                // ItemUtils.storeTEInStack(stack, te);
                // }

                // InventoryUtils.setPickedItemToHand(stack, mc);

                // NOTE: I dont know why we have to pick block in creative mode. You can simply just set the block
                mc.interactionManager.clickCreativeStack(stack, 36 + inv.selectedSlot);

                return true;
            }
            else
            {
                int slot = inv.getSlotWithStack(stack);
                boolean shouldPick = inv.selectedSlot != slot;
                boolean canPick = slot != -1;

                if (shouldPick && canPick)
                {
                    InventoryUtils.setPickedItemToHand(stack, mc);
                }

                // return shouldPick == false || canPick;
            }
        }

        return true;
    }

    /**
     * Does a ray trace to the schematic world, and returns either the closest or
     * the furthest hit block.
     * 
     * @param closest
     * @param mc
     * @return true if the correct item was or is in the player's hand after the
     *         pick block
     */

    public static boolean doSchematicWorldPickBlock(boolean closest, MinecraftClient mc)
    {
        BlockPos pos = null;

        if (closest)
        {
            pos = RayTraceUtils.getSchematicWorldTraceIfClosest(mc.world, mc.player, 6);
        }
        else
        {
            pos = RayTraceUtils.getFurthestSchematicWorldTrace(mc.world, mc.player, 6);
        }

        if (pos != null)
        {
            World world = SchematicWorldHandler.getSchematicWorld();
            BlockState state = world.getBlockState(pos);
            ItemStack stack = MaterialCache.getInstance().getItemForState(state, world, pos);

            if (stack.isEmpty() == false)
            {
                PlayerInventory inv = mc.player.inventory;

                if (mc.player.abilities.creativeMode)
                {
                    BlockEntity te = world.getBlockEntity(pos);

                    // The creative mode pick block with NBT only works correctly
                    // if the server world doesn't have a TileEntity in that position.
                    // Otherwise it would try to write whatever that TE is into the picked ItemStack.
                    if (GuiBase.isCtrlDown() && te != null && mc.world.isAir(pos))
                    {
                        ItemUtils.storeTEInStack(stack, te);
                    }

                    InventoryUtils.setPickedItemToHand(stack, mc);
                    mc.interactionManager.clickCreativeStack(mc.player.getStackInHand(Hand.MAIN_HAND), 36 + inv.selectedSlot);

                    //return true;
                }
                else
                {
                    int slot = inv.getSlotWithStack(stack);
                    boolean shouldPick = inv.selectedSlot != slot;
                    boolean canPick = slot != -1;

                    if (shouldPick && canPick)
                    {
                        InventoryUtils.setPickedItemToHand(stack, mc);
                    }

                    //return shouldPick == false || canPick;
                }
            }

            return true;
        }

        return false;
    }

    public static void easyPlaceOnUseTick(MinecraftClient mc)
    {
        if (mc.player != null &&
            Configs.Generic.EASY_PLACE_HOLD_ENABLED.getBooleanValue() &&
            Configs.Generic.EASY_PLACE_MODE.getBooleanValue() &&
            Hotkeys.EASY_PLACE_ACTIVATION.getKeybind().isKeybindHeld() &&
            KeybindMulti.isKeyDown(KeybindMulti.getKeyCode(mc.options.keyUse)))
        {
            WorldUtils.doEasyPlaceAction(mc);
        }
    }

    public static boolean handleEasyPlace(MinecraftClient mc)
    {
        ActionResult result = doEasyPlaceAction(mc);

        if (result == ActionResult.FAIL)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.message.easy_place_fail");
            return true;
        }

        return result != ActionResult.PASS;
    }

    private static ActionResult doEasyPlaceAction(MinecraftClient mc) 
    {
        RayTraceWrapper traceWrapper = RayTraceUtils.getGenericTrace(mc.world, mc.player, 6, true);
        if (traceWrapper == null) 
        {
            return ActionResult.FAIL;
        }
        BlockHitResult trace = traceWrapper.getBlockHitResult();
        BlockPos tracePos = trace.getBlockPos();
        int posX = tracePos.getX();
        int posY = tracePos.getY();
        int posZ = tracePos.getZ();
        int rangeX = Configs.Generic.EASY_PLACE_MODE_RANGE_X.getIntegerValue();
        int rangeY = Configs.Generic.EASY_PLACE_MODE_RANGE_Y.getIntegerValue();
        int rangeZ = Configs.Generic.EASY_PLACE_MODE_RANGE_Z.getIntegerValue();
        Direction[] facingSides = Direction.getEntityFacingOrder(mc.player);
        Direction primaryFacing = facingSides[0];
        Direction horizontalFacing = primaryFacing; // For use in blocks with only horizontal rotation

        int index = 0;
        while (horizontalFacing.getAxis() == Direction.Axis.Y && index < facingSides.length) 
        {
            horizontalFacing = facingSides[index++];
        }

        World world = SchematicWorldHandler.getSchematicWorld();

        /*
        * TODO: THIS IS REALLY BAD IN TERMS OF EFFICIENCY.
        * I suggest using some form of search with a built in datastructure first
        * Maybe quadtree? (I dont know how MC works)
        */

        int maxInteract = Configs.Generic.EASY_PLACE_MODE_MAX_BLOCKS.getIntegerValue();
        int interact = 0;
        boolean hasPicked = false;
        Text pickedBlock = null;
        for (int x = -rangeX; x <= rangeX; x++) 
        {
            for (int y = -rangeY; y <= rangeY; y++) 
            {
                for (int z = -rangeZ; z <= rangeZ; z++) 
                {
                    int newX = posX + x;
                    int newY = Math.min(Math.max(posY + y, 0), 255);
                    int newZ = posZ + z;

                    int dx = newX - (int) mc.player.x;
                    int dy = newY - (int) mc.player.y;
                    int dz = newZ - (int) mc.player.z;

                    if (dx * dx + dy * dy + dz * dz > 6 * 6) // Check if within reach distance
                        continue;
                    BlockPos pos = new BlockPos(newX, newY, newZ);

                    BlockState stateSchematic = world.getBlockState(pos);

                    if (stateSchematic.isAir())
                        continue;

                    ItemStack stack = MaterialCache.getInstance().getItemForState(stateSchematic);
                    if (stack.isEmpty() == false) 
                    {
                        BlockState stateClient = mc.world.getBlockState(pos);

                        if (stateSchematic == stateClient) 
                        {
                            continue;
                        }

                        // Abort if there is already a block in the target position
                        if (easyPlaceBlockChecksCancel(stateSchematic, stateClient, mc.player, stack)) {

                            /* Sometimes, blocks have other states like the delay on a repeater. 
                             * So, this code clicks the block until the state is the same
                             * I don't know if Schematica does this too, I just did it because I work with a lot of redstone
                            */
                            if (!stateClient.isAir() && !mc.player.isSneaking() && !easyPlaceIsPositionCached(pos, true)) 
                            {
                                Block cBlock = stateClient.getBlock();
                                Block sBlock = stateSchematic.getBlock();

                                if (cBlock.getName().equals(sBlock.getName())) 
                                {
                                    Direction facingSchematic = fi.dy.masa.malilib.util.BlockUtils
                                            .getFirstPropertyFacingValue(stateSchematic);
                                    Direction facingClient = fi.dy.masa.malilib.util.BlockUtils
                                            .getFirstPropertyFacingValue(stateClient);

                                    if (facingSchematic == facingClient) 
                                    {
                                        int clickTimes = 0;
                                        Direction side = Direction.NORTH;
                                        if (sBlock instanceof RepeaterBlock) 
                                        {
                                            int clientDelay = stateClient.get(RepeaterBlock.DELAY);
                                            int schematicDelay = stateSchematic.get(RepeaterBlock.DELAY);
                                            if (clientDelay != schematicDelay) 
                                            {

                                                if (clientDelay < schematicDelay) 
                                                {
                                                    clickTimes = schematicDelay - clientDelay;
                                                } 
                                                else if (clientDelay > schematicDelay) 
                                                {
                                                    clickTimes = schematicDelay + (4 - clientDelay);
                                                }
                                            }
                                            side = Direction.UP;
                                        } 
                                        else if (sBlock instanceof ComparatorBlock) 
                                        {
                                            if (stateSchematic.get(ComparatorBlock.MODE) != stateClient.get(ComparatorBlock.MODE))
                                                clickTimes = 1;
                                            side = Direction.UP;
                                        } else if (sBlock instanceof LeverBlock) 
                                        {
                                            if (stateSchematic.get(LeverBlock.POWERED) != stateClient.get(LeverBlock.POWERED))
                                                clickTimes = 1;

                                            
                                            /*
                                             * I dont know if this direction code is needed. 
                                             * I am just doing it anyway to make it "make sense" to the server
                                             * (I am emulating what the client does so the server isn't confused)
                                             */
                                            if (stateClient.get(LeverBlock.FACE) == WallMountLocation.CEILING) 
                                            {
                                                side = Direction.DOWN;
                                            } 
                                            else if (stateClient.get(LeverBlock.FACE) == WallMountLocation.FLOOR) 
                                            {
                                                side = Direction.UP;
                                            } 
                                            else 
                                            {
                                                side = stateClient.get(LeverBlock.FACING);
                                            }

                                        } 
                                        else if (sBlock instanceof TrapdoorBlock) 
                                        {
                                            if (stateSchematic.getMaterial() != Material.METAL && stateSchematic.get(TrapdoorBlock.OPEN) != stateClient.get(TrapdoorBlock.OPEN))
                                                clickTimes = 1;
                                        } 
                                        else if (sBlock instanceof FenceGateBlock) 
                                        {
                                            if (stateSchematic.get(FenceGateBlock.OPEN) != stateClient.get(FenceGateBlock.OPEN))
                                                clickTimes = 1;
                                        } 
                                        else if (sBlock instanceof DoorBlock) 
                                        {
                                            if (stateClient.getMaterial() != Material.METAL && stateSchematic.get(DoorBlock.OPEN) != stateClient.get(DoorBlock.OPEN))
                                                clickTimes = 1;
                                        }

                                        for (int i = 0; i < clickTimes; i++)  // Click on the block a few times
                                        {
                                            Hand hand = Hand.MAIN_HAND;

                                            Vec3d hitPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5,
                                                    pos.getZ() + 0.5);

                                            BlockHitResult hitResult = new BlockHitResult(hitPos, side, pos, false);

                                            mc.interactionManager.interactBlock(mc.player, mc.world, hand, hitResult);
                                            interact++;
                                        }

                                        if (clickTimes > 0) 
                                        {
                                            cacheEasyPlacePosition(pos, true);
                                        }
                                    }
                                }
                            }
                            continue;
                        }

                        if (easyPlaceIsPositionCached(pos, false)) 
                        {
                            continue;
                        }
                        Direction facing = fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(stateSchematic);
                        if (facing != null) 
                        {
                            FacingData facedata = getFacingData(stateSchematic);
                            if (!canPlaceFace(facedata, stateSchematic, mc.player, primaryFacing, horizontalFacing))
                                continue;

                            if (
                                (stateSchematic.getBlock() instanceof DoorBlock && stateSchematic.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER) ||
                                (stateSchematic.getBlock() instanceof BedBlock && stateSchematic.get(BedBlock.PART) == BedPart.HEAD)
                                
                                ) {
                                continue;
                            }
                        }

                        // Exception for signs (edge case)
                        if (stateSchematic.getBlock() instanceof SignBlock && !(stateSchematic.getBlock() instanceof WallSignBlock)) {
                            if ((Math.floor((mc.player.yaw + 180.0) * 16.0 / 360.0 + 0.5) % 15) != stateSchematic
                                    .get(SignBlock.ROTATION))
                                continue;

                        }
                        double offX = 0.5; // We dont really need this. But I did it anyway so that I could experiment easily.
                        double offY = 0.5;
                        double offZ = 0.5;

                        Direction sideOrig = Direction.NORTH;
                        BlockPos npos = pos;
                        Direction side = applyPlacementFacing(stateSchematic, sideOrig, stateClient);
                        Block blockSchematic = stateSchematic.getBlock();
                        if (blockSchematic instanceof WallMountedBlock || blockSchematic instanceof TorchBlock
                                || blockSchematic instanceof LadderBlock || blockSchematic instanceof TrapdoorBlock
                                || blockSchematic instanceof TripwireHookBlock
                                || blockSchematic instanceof WallSignBlock) {


                            /* Some blocks, especially wall mounted blocks must be placed on another for directionality to work
                            * Basically, the block pos sent must be a "clicked" block.
                            */
                            int px = pos.getX();
                            int py = pos.getY();
                            int pz = pos.getZ();

                            if (side == Direction.DOWN) {
                                py += 1;
                            } else if (side == Direction.UP) {
                                py += -1;
                            } else if (side == Direction.NORTH) {
                                pz += 1;
                            } else if (side == Direction.SOUTH) {
                                pz += -1;
                            } else if (side == Direction.EAST) {
                                px += -1;
                            } else if (side == Direction.WEST) {
                                px += 1;
                            }

                            npos = new BlockPos(px, py, pz);

                            BlockState clientStateItem = mc.world.getBlockState(npos);

                            if (clientStateItem == null || clientStateItem.isAir()) {
                                if (!(blockSchematic instanceof TrapdoorBlock)) {
                                    continue;
                                }
                                BlockPos testPos;

                                /* Trapdoors are special. They can also be placed on top, or below another block
                                */
                                if (stateSchematic.get(TrapdoorBlock.HALF) == BlockHalf.TOP) {
                                    testPos = new BlockPos(pos.getX(), pos.getY() + 1, pos.getZ());
                                    side = Direction.DOWN;
                                } else {
                                    testPos = new BlockPos(pos.getX(), pos.getY() - 1, pos.getZ());
                                    side = Direction.UP;
                                }
                                BlockState clientStateItemTest = mc.world.getBlockState(testPos);

                                if (clientStateItemTest == null || clientStateItemTest.isAir()) {
                                    BlockState schematicNItem = world.getBlockState(npos);

                                    BlockState schematicTItem = world.getBlockState(testPos);

                                    /* If possible, it is always best to attatch the trapdoor to an
                                    * actual block that exists on the world
                                    * But other times, it can't be helped
                                    */
                                    if ((schematicNItem != null && !schematicNItem.isAir())
                                            || (schematicTItem != null && !schematicTItem.isAir()))
                                        continue;
                                    npos = pos;
                                } else
                                    npos = testPos;

                                // If trapdoor is placed from top or bottom, directionality is decided by player direction
                                if (stateSchematic.get(TrapdoorBlock.FACING).getOpposite() != horizontalFacing) {
                                    continue;
                                }

                            }

                        }

                        // Abort if the required item was not able to be pick-block'd
                        if (!hasPicked) {

                            if (doSchematicWorldPickBlock(true, mc, stateSchematic, pos) == false) {
                                return ActionResult.FAIL;
                            }
                            hasPicked = true;
                            pickedBlock = stateSchematic.getBlock().getName();
                        } else if (pickedBlock != null && !pickedBlock.equals(stateSchematic.getBlock().getName())) {
                            continue;
                        }
                       


                        Hand hand = EntityUtils.getUsedHandForItem(mc.player, stack);

                        // Abort if a wrong item is in the player's hand
                        if (hand == null) {
                            continue;
                        }

                        Vec3d hitPos = new Vec3d(offX, offY, offZ);
                        // Carpet Accurate Placement protocol support, plus BlockSlab support
                        hitPos = applyHitVec(npos, stateSchematic, hitPos, side);

                        // Mark that this position has been handled (use the non-offset position that is
                        // checked above)
                        cacheEasyPlacePosition(pos, false);

                        BlockHitResult hitResult = new BlockHitResult(hitPos, side, npos, false);

                        // System.out.printf("pos: %s side: %s, hit: %s\n", pos, side, hitPos);
                        // pos, side, hitPos

                        mc.interactionManager.interactBlock(mc.player, mc.world, hand, hitResult);
                        interact++;
                        if (stateSchematic.getBlock() instanceof SlabBlock
                                && stateSchematic.get(SlabBlock.TYPE) == SlabType.DOUBLE) {
                            stateClient = mc.world.getBlockState(npos);

                            if (stateClient.getBlock() instanceof SlabBlock
                                    && stateClient.get(SlabBlock.TYPE) != SlabType.DOUBLE) {
                                side = applyPlacementFacing(stateSchematic, sideOrig, stateClient);
                                hitResult = new BlockHitResult(hitPos, side, npos, false);
                                mc.interactionManager.interactBlock(mc.player, mc.world, hand, hitResult);
                                interact++;
                            }
                        }
                       

                        if (interact >= maxInteract) {
                            return ActionResult.SUCCESS;
                        }
                       
                    }

                }
            }

        }

        return (interact > 0) ? ActionResult.SUCCESS : ActionResult.FAIL;
    }

    /* Checks if the block can be placed in the correct orientation if player is facing a certain direction
    * Dont place block if orientation will be wrong
    */
    private static boolean canPlaceFace(FacingData facedata, BlockState stateSchematic, PlayerEntity player,
            Direction primaryFacing, Direction horizontalFacing) {
        Direction facing = fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(stateSchematic);
        if (facing != null && facedata != null) {

            switch (facedata.type) {
            case 0: // All directions (ie, observers and pistons)
                if (facedata.isReversed) {
                    return facing.getOpposite() == primaryFacing;
                } else {
                    return facing == primaryFacing;
                }

            case 1: // Only Horizontal directions (ie, repeaters and comparators)
                if (facedata.isReversed) {
                    return facing.getOpposite() == horizontalFacing;
                } else {
                    return facing == horizontalFacing;
                }
            case 2: // Wall mountable, such as a lever, only use player direction if not on wall.
                return stateSchematic.get(WallMountedBlock.FACE) == WallMountLocation.WALL
                        || facing == horizontalFacing;
            default: // Ignore rest -> TODO: Other blocks like anvils, etc...
                return true;
            }
        } else {
            return true;
        }
    }

    private static boolean easyPlaceBlockChecksCancel(BlockState stateSchematic, BlockState stateClient,
            PlayerEntity player, ItemStack stack) {
        Block blockSchematic = stateSchematic.getBlock();

        if (blockSchematic instanceof SlabBlock && stateSchematic.get(SlabBlock.TYPE) == SlabType.DOUBLE) {
            Block blockClient = stateClient.getBlock();

            if (blockClient instanceof SlabBlock && stateClient.get(SlabBlock.TYPE) != SlabType.DOUBLE) {
                return blockSchematic != blockClient;
            }
        }

        if (stateClient.isAir()) // This is a lot simpler than below. But slightly lacks functionality.
            return false;
        /*
         * if (trace.getType() != HitResult.Type.BLOCK) { return false; }
         */
        // BlockHitResult hitResult = (BlockHitResult) trace;
        // ItemPlacementContext ctx = new ItemPlacementContext(new
        // ItemUsageContext(player, Hand.MAIN_HAND, hitResult));

        // if (stateClient.canReplace(ctx) == false) {
        // return true;
        // }

        return true;
    }

    /**
     * Apply hit vectors (used to be Carpet hit vec protocol, but I think it is uneccessary now with orientation/states programmed in)
     * 
     * @param pos
     * @param state
     * @param hitVecIn
     * @return
     */
    public static Vec3d applyHitVec(BlockPos pos, BlockState state, Vec3d hitVecIn, Direction side) {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();

        double dx = hitVecIn.getX();
        double dy = hitVecIn.getY();
        double dz = hitVecIn.getZ();
        Block block = state.getBlock();

        /* I dont know if this is needed, just doing to mimick client
        * According to the MC protocol wiki, the protocol expects a 1 on a side
        * that is clicked
        */
        if (side == Direction.UP) { 
            dy = 1;
        } else if (side == Direction.DOWN) {
            dy = 0;
        } else if (side == Direction.EAST) {
            dx = 1;
        } else if (side == Direction.WEST) {
            dx = 0;
        } else if (side == Direction.SOUTH) {
            dz = 1;
        } else if (side == Direction.NORTH) {
            dz = 0;
        }


        if (block instanceof StairsBlock) {
            if (state.get(StairsBlock.HALF) == BlockHalf.TOP) {
                dy = 0.9;
            } else {
                dy = 0;
            }
        } else if (block instanceof SlabBlock && state.get(SlabBlock.TYPE) != SlabType.DOUBLE) {
            if (state.get(SlabBlock.TYPE) == SlabType.TOP) {
                dy = 0.9;
            } else {
                dy = 0;
            }
        } else if (block instanceof TrapdoorBlock) {
            if (state.get(TrapdoorBlock.HALF) == BlockHalf.TOP) {
                dy = 0.9;
            } else {
                dy = 0;
            }
        }
        return new Vec3d(x + dx, y + dy, z + dz);
    }

    /* Gets the direction necessary to build the block oriented correctly.
    * TODO: Need a better way to do this.
    */
    private static Direction applyPlacementFacing(BlockState stateSchematic, Direction side, BlockState stateClient) {
        Block blockSchematic = stateSchematic.getBlock();
        Block blockClient = stateClient.getBlock();

        if (blockSchematic instanceof SlabBlock) {
            if (stateSchematic.get(SlabBlock.TYPE) == SlabType.DOUBLE && blockClient instanceof SlabBlock
                    && stateClient.get(SlabBlock.TYPE) != SlabType.DOUBLE) {
                if (stateClient.get(SlabBlock.TYPE) == SlabType.TOP) {
                    return Direction.DOWN;
                } else {
                    return Direction.UP;
                }
            }
            // Single slab
            else {
                return Direction.NORTH;
            }
        } else if (blockSchematic instanceof LogBlock || blockSchematic instanceof PillarBlock) {
            Direction.Axis axis = stateSchematic.get(PillarBlock.AXIS);
            // Logs and pillars only have 3 directions that are important
            if (axis == Direction.Axis.X) {
                return Direction.WEST;
            } else if (axis == Direction.Axis.Y) {
                return Direction.DOWN;
            } else if (axis == Direction.Axis.Z) {
                return Direction.NORTH;
            }

        } else if (blockSchematic instanceof WallSignBlock) {
            return stateSchematic.get(WallSignBlock.FACING);
        } else if (blockSchematic instanceof WallMountedBlock) {
            WallMountLocation location = stateSchematic.get(WallMountedBlock.FACE);
            if (location == WallMountLocation.FLOOR) {
                return Direction.UP;
            } else if (location == WallMountLocation.CEILING) {
                return Direction.DOWN;
            } else {
                return stateSchematic.get(WallMountedBlock.FACING);

            }

        } else if (blockSchematic instanceof HopperBlock) {
            return stateSchematic.get(HopperBlock.FACING).getOpposite();
        } else if (blockSchematic instanceof TorchBlock) {

            if (blockSchematic instanceof WallTorchBlock) {
                return stateSchematic.get(WallTorchBlock.FACING);
            } else if (blockSchematic instanceof WallRedstoneTorchBlock) {
                return stateSchematic.get(WallRedstoneTorchBlock.FACING);
            } else {
                return Direction.UP;
            }
        } else if (blockSchematic instanceof LadderBlock) {
            return stateSchematic.get(LadderBlock.FACING);
        } else if (blockSchematic instanceof TrapdoorBlock) {
            return stateSchematic.get(TrapdoorBlock.FACING);
        } else if (blockSchematic instanceof TripwireHookBlock) {
            return stateSchematic.get(TripwireHookBlock.FACING);
        }

        // TODO: Add more for other blocks
        return side;
    }

   /**
     * Does placement restriction checks for the targeted position.
     * If the targeted position is outside of the current layer range, or should be air
     * in the schematic, or the player is holding the wrong item in hand, then true is returned
     * to indicate that the use action should be cancelled.
     * @param mc
     * @param doEasyPlace
     * @param restrictPlacement
     * @return
     */
    public static boolean handlePlacementRestriction(MinecraftClient mc)
    {
        boolean cancel = placementRestrictionInEffect(mc);

        if (cancel)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.message.placement_restriction_fail");
        }

        return cancel;
    }

    /**
     * Does placement restriction checks for the targeted position.
     * If the targeted position is outside of the current layer range, or should be air
     * in the schematic, or the player is holding the wrong item in hand, then true is returned
     * to indicate that the use action should be cancelled.
     * @param mc
     * @param doEasyPlace
     * @param restrictPlacement
     * @return true if the use action should be cancelled
     */
    private static boolean placementRestrictionInEffect(MinecraftClient mc)
    {
        HitResult trace = mc.hitResult;

        ItemStack stack = mc.player.getMainHandStack();

        if (stack.isEmpty())
        {
            stack = mc.player.getOffHandStack();
        }

        if (stack.isEmpty())
        {
            return false;
        }

        if (trace != null && trace.getType() == HitResult.Type.BLOCK)
        {
            BlockHitResult blockHitResult = (BlockHitResult) trace;
            BlockPos pos = blockHitResult.getBlockPos();
            ItemPlacementContext ctx = new ItemPlacementContext(new ItemUsageContext(mc.player, Hand.MAIN_HAND, blockHitResult));

            // Get the possibly offset position, if the targeted block is not replaceable
            pos = ctx.getBlockPos();

            BlockState stateClient = mc.world.getBlockState(pos);

            World worldSchematic = SchematicWorldHandler.getSchematicWorld();
            LayerRange range = DataManager.getRenderLayerRange();
            boolean schematicHasAir = worldSchematic.isAir(pos);

            // The targeted position is outside the current render range
            if (schematicHasAir == false && range.isPositionWithinRange(pos) == false)
            {
                return true;
            }

            // There should not be anything in the targeted position,
            // and the position is within or close to a schematic sub-region
            if (schematicHasAir && isPositionWithinRangeOfSchematicRegions(pos, 2))
            {
                return true;
            }

            blockHitResult = new BlockHitResult(blockHitResult.getPos(), blockHitResult.getSide(), pos, false);
            ctx = new ItemPlacementContext(new ItemUsageContext(mc.player, Hand.MAIN_HAND, (BlockHitResult) trace));

            // Placement position is already occupied
            if (stateClient.canReplace(ctx) == false)
            {
                return true;
            }

            BlockState stateSchematic = worldSchematic.getBlockState(pos);
            stack = MaterialCache.getInstance().getItemForState(stateSchematic);

            // The player is holding the wrong item for the targeted position
            if (stack.isEmpty() == false && EntityUtils.getUsedHandForItem(mc.player, stack) == null)
            {
                return true;
            }
        }

        return false;
    }

    public static boolean isPositionWithinRangeOfSchematicRegions(BlockPos pos, int range)
    {
        SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
        final int minCX = (pos.getX() - range) >> 4;
        final int minCY = (pos.getY() - range) >> 4;
        final int minCZ = (pos.getZ() - range) >> 4;
        final int maxCX = (pos.getX() + range) >> 4;
        final int maxCY = (pos.getY() + range) >> 4;
        final int maxCZ = (pos.getZ() + range) >> 4;
        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();

        for (int cy = minCY; cy <= maxCY; ++cy)
        {
            for (int cz = minCZ; cz <= maxCZ; ++cz)
            {
                for (int cx = minCX; cx <= maxCX; ++cx)
                {
                    List<IntBoundingBox> boxes = manager.getTouchedBoxesInSubChunk(new SubChunkPos(cx, cy, cz));

                    for (int i = 0; i < boxes.size(); ++i)
                    {
                        IntBoundingBox box = boxes.get(i);

                        if (x >= box.minX - range && x <= box.maxX + range &&
                            y >= box.minY - range && y <= box.maxY + range &&
                            z >= box.minZ - range && z <= box.maxZ + range)
                        {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Checks if the given one block thick slice has non-air blocks or not.
     * NOTE: The axis is the perpendicular axis (that goes through the plane).
     * @param axis
     * @param pos1
     * @param pos2
     * @return
     */
    public static boolean isSliceEmpty(World world, Direction.Axis axis, BlockPos pos1, BlockPos pos2)
    {
        BlockPos.Mutable posMutable = new BlockPos.Mutable();

        switch (axis)
        {
            case Z:
            {
                int x1 = Math.min(pos1.getX(), pos2.getX());
                int x2 = Math.max(pos1.getX(), pos2.getX());
                int y1 = Math.min(pos1.getY(), pos2.getY());
                int y2 = Math.max(pos1.getY(), pos2.getY());
                int z = pos1.getZ();
                int cxMin = (x1 >> 4);
                int cxMax = (x2 >> 4);

                for (int cx = cxMin; cx <= cxMax; ++cx)
                {
                    Chunk chunk = world.getChunk(cx, z >> 4);
                    int xMin = Math.max(x1,  cx << 4      );
                    int xMax = Math.min(x2, (cx << 4) + 15);
                    int yMax = Math.min(y2, chunk.getHighestNonEmptySectionYOffset() + 15);

                    for (int x = xMin; x <= xMax; ++x)
                    {
                        for (int y = y1; y <= yMax; ++y)
                        {
                            if (chunk.getBlockState(posMutable.set(x, y, z)).isAir() == false)
                            {
                                return false;
                            }
                        }
                    }
                }

                break;
            }

            case Y:
            {
                int x1 = Math.min(pos1.getX(), pos2.getX());
                int x2 = Math.max(pos1.getX(), pos2.getX());
                int y = pos1.getY();
                int z1 = Math.min(pos1.getZ(), pos2.getZ());
                int z2 = Math.max(pos1.getZ(), pos2.getZ());
                int cxMin = (x1 >> 4);
                int cxMax = (x2 >> 4);
                int czMin = (z1 >> 4);
                int czMax = (z2 >> 4);

                for (int cz = czMin; cz <= czMax; ++cz)
                {
                    for (int cx = cxMin; cx <= cxMax; ++cx)
                    {
                        Chunk chunk = world.getChunk(cx, cz);

                        if (y > chunk.getHighestNonEmptySectionYOffset() + 15)
                        {
                            continue;
                        }

                        int xMin = Math.max(x1,  cx << 4      );
                        int xMax = Math.min(x2, (cx << 4) + 15);
                        int zMin = Math.max(z1,  cz << 4      );
                        int zMax = Math.min(z2, (cz << 4) + 15);

                        for (int z = zMin; z <= zMax; ++z)
                        {
                            for (int x = xMin; x <= xMax; ++x)
                            {
                                if (chunk.getBlockState(posMutable.set(x, y, z)).isAir() == false)
                                {
                                    return false;
                                }
                            }
                        }
                    }
                }

                break;
            }

            case X:
            {
                int x = pos1.getX();
                int z1 = Math.min(pos1.getZ(), pos2.getZ());
                int z2 = Math.max(pos1.getZ(), pos2.getZ());
                int y1 = Math.min(pos1.getY(), pos2.getY());
                int y2 = Math.max(pos1.getY(), pos2.getY());
                int czMin = (z1 >> 4);
                int czMax = (z2 >> 4);

                for (int cz = czMin; cz <= czMax; ++cz)
                {
                    Chunk chunk = world.getChunk(x >> 4, cz);
                    int zMin = Math.max(z1,  cz << 4      );
                    int zMax = Math.min(z2, (cz << 4) + 15);
                    int yMax = Math.min(y2, chunk.getHighestNonEmptySectionYOffset() + 15);

                    for (int z = zMin; z <= zMax; ++z)
                    {
                        for (int y = y1; y <= yMax; ++y)
                        {
                            if (chunk.getBlockState(posMutable.set(x, y, z)).isAir() == false)
                            {
                                return false;
                            }
                        }
                    }
                }

                break;
            }
        }

        return true;
    }

    public static boolean easyPlaceIsPositionCached(BlockPos pos, boolean useClicked)
    {
        long currentTime = System.nanoTime();
        boolean cached = false;

        for (int i = 0; i < EASY_PLACE_POSITIONS.size(); ++i) {
            PositionCache val = EASY_PLACE_POSITIONS.get(i);
            boolean expired = val.hasExpired(currentTime);

            if (expired) {
                EASY_PLACE_POSITIONS.remove(i);
                --i;
            } else if (val.getPos().equals(pos)) {

                // Item placement and "using"/"clicking" (changing delay for repeaters) are diffferent
                if (!useClicked || val.hasClicked) {
                    cached = true;
                }

                // Keep checking and removing old entries if there are a fair amount
                if (EASY_PLACE_POSITIONS.size() < 16) {
                    break;
                }
            }
        }

        return cached;
    }

    private static void cacheEasyPlacePosition(BlockPos pos, boolean useClicked)
    {
        PositionCache item = new PositionCache(pos, System.nanoTime(), useClicked ? 1000000000 : 2000000000);
       // TODO: Create a separate cache for clickable items, as this just makes duplicates
        if (useClicked)
            item.hasClicked = true;
        EASY_PLACE_POSITIONS.add(item);
    }

    public static class PositionCache
    {
        private final BlockPos pos;
        private final long time;
        private final long timeout;
        public boolean hasClicked = false;

        private PositionCache(BlockPos pos, long time, long timeout)
        {
            this.pos = pos;
            this.time = time;
            this.timeout = timeout;
        }

        public BlockPos getPos()
        {
            return this.pos;
        }

        public boolean hasExpired(long currentTime)
        {
            return currentTime - this.time > this.timeout;
        }
    }
}
