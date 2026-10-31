package top.xfunny.mod.block;

import org.mtr.core.data.Lift;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.block.BlockLiftTrackFloor;
import top.xfunny.mod.BlockEntityTypes;
import top.xfunny.mod.Init;
import top.xfunny.mod.Items;
import top.xfunny.mod.block.base.LiftDestinationDispatchTerminalBase;
import top.xfunny.mod.packet.PacketSyncLiftDestinationDispatchTerminal;
import top.xfunny.mod.util.TransformPositionX;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SchindlerPortTerminal extends LiftDestinationDispatchTerminalBase {
    public static final String HOME_PREFIX = "schindler_port_home_";
    public static final String MANAGE_PREFIX = "schindler_port_manage_";
    public static final String ASSIGN_PREFIX = "schindler_port_assign_";
    public static final String CONFIRM_SCREEN = "schindler_port_confirm";
    public static final String RESULT_SCREEN = "schindler_port_result";

    public static final String[] FLOOR_GROUPS = {
            "PUBLIC", "OFFICE", "PARKING", "RESIDENTIAL", "SERVICE", "OTHER"
    };

    public SchindlerPortTerminal() {
        super(true);
    }

    @Nonnull
    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        // The terminal is a wall-mounted 3D housing (3.2 px deep), not a paper-thin panel.
        // FACING points into the mounting wall; the housing/display project out from its opposite side.
        return IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 11, 3.2, IBlock.getStatePropertySafe(state, FACING).getOpposite());
    }

    @Nonnull
    @Override
    public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new SchindlerPortTerminal.BlockEntity(blockPos, blockState);
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        properties.add(FACING);
        properties.add(UNLOCKED);
    }

    @Nonnull
    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        final ActionResult brushResult = IBlock.checkHoldingBrush(world, player, () -> {
            final boolean unlocked = !IBlock.getStatePropertySafe(state, UNLOCKED);
            world.setBlockState(pos, state.with(new Property<>(UNLOCKED.data), unlocked));
            player.sendMessage(Text.cast(TextHelper.translatable(unlocked ? "hint.yte.unlocked" : "hint.yte.locked")), true);
        });
        if (brushResult == ActionResult.SUCCESS) {
            return ActionResult.SUCCESS;
        }

        if (player.isHolding(Items.YTE_LIFT_BUTTONS_LINK_CONNECTOR.get())
                || player.isHolding(Items.YTE_LIFT_BUTTONS_LINK_REMOVER.get())
                || player.isHolding(Items.YTE_GROUP_LIFT_BUTTONS_LINK_CONNECTOR.get())
                || player.isHolding(Items.YTE_GROUP_LIFT_BUTTONS_LINK_REMOVER.get())) {
            return ActionResult.PASS;
        }

        if (!world.isClient()) {
            return ActionResult.SUCCESS;
        }

        final org.mtr.mapping.holder.BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity == null || !(blockEntity.data instanceof SchindlerPortTerminal.BlockEntity)) {
            return ActionResult.SUCCESS;
        }

        final SchindlerPortTerminal.BlockEntity data = (SchindlerPortTerminal.BlockEntity) blockEntity.data;
        final double hitX = TransformPositionX.transform(
                MathHelper.fractionalPart(hit.getPos().getXMapped()),
                MathHelper.fractionalPart(hit.getPos().getZMapped()),
                IBlock.getStatePropertySafe(state, FACING).getOpposite()
        );
        final double hitY = MathHelper.fractionalPart(hit.getPos().getYMapped());
        final String button = data.getKeyMapping().mapping(hitX, hitY);
        final String screenId = data.getScreenId();

        if (screenId != null && screenId.startsWith(HOME_PREFIX)) {
            if (button.startsWith("group_")) {
                data.setHomePage(0, button.substring("group_".length()), world, pos);
            } else if ("manage_groups".equals(button) && IBlock.getStatePropertySafe(state, UNLOCKED)) {
                data.setManagePage(0, world, pos);
            } else if (button.startsWith("floor_")) {
                try {
                    final int visibleIndex = Integer.parseInt(button.substring("floor_".length()));
                    final List<FloorOption> floors = data.getVisibleFloors(world);
                    final int floorIndex = data.getPageIndex() * BlockEntity.FLOORS_PER_PAGE + visibleIndex;
                    if (floorIndex >= 0 && floorIndex < floors.size()) {
                        data.selectDestination(floors.get(floorIndex).number, world, pos);
                    }
                } catch (NumberFormatException ignored) {
                }
            } else if ("next".equals(button)) {
                data.setHomePage(data.getPageIndex() + 1, data.getActiveGroup(), world, pos);
            } else if ("previous".equals(button)) {
                data.setHomePage(data.getPageIndex() - 1, data.getActiveGroup(), world, pos);
            }
        } else if (screenId != null && screenId.startsWith(MANAGE_PREFIX)) {
            if (button.startsWith("manage_floor_")) {
                try {
                    final int visibleIndex = Integer.parseInt(button.substring("manage_floor_".length()));
                    final List<FloorOption> floors = data.getAvailableFloors(world);
                    final int floorIndex = data.getManagePage() * BlockEntity.FLOORS_PER_PAGE + visibleIndex;
                    if (floorIndex >= 0 && floorIndex < floors.size()) {
                        data.openGroupAssignment(floors.get(floorIndex), world, pos);
                    }
                } catch (NumberFormatException ignored) {
                }
            } else if ("manage_next".equals(button)) {
                data.setManagePage(data.getManagePage() + 1, world, pos);
            } else if ("manage_previous".equals(button)) {
                data.setManagePage(data.getManagePage() - 1, world, pos);
            } else if ("manage_back".equals(button)) {
                data.setHomePage(0, "ALL", world, pos);
            }
        } else if (screenId != null && screenId.startsWith(ASSIGN_PREFIX)) {
            if ("assign_back".equals(button)) {
                data.setManagePage(data.getAssignmentPage(), world, pos);
            } else if (button.startsWith("assign_")) {
                data.assignFloorGroup(button.substring("assign_".length()), world, pos);
            }
        } else if (CONFIRM_SCREEN.equals(screenId)) {
            if ("confirm".equals(button)) {
                final String selectedFloor = data.getSelectedFloor();
                if (!selectedFloor.isEmpty()) {
                    data.callLift(world, pos, selectedFloor);
                    final String assignment = data.formatLiftAssignment();
                    data.showResult("??".equals(assignment) ? "NO LIFT AVAILABLE" : "CAR " + assignment, world, pos);
                }
            } else if ("back".equals(button)) {
                data.setHomePage(0, "ALL", world, pos);
            }
        } else if (RESULT_SCREEN.equals(screenId) && "home".equals(button)) {
            data.setHomePage(0, "ALL", world, pos);
        }

        return ActionResult.SUCCESS;
    }

    public static final class FloorOption {
        public final long positionKey;
        public final String number;
        public final String description;
        public final String group;

        public FloorOption(long positionKey, String number, String description, String group) {
            this.positionKey = positionKey;
            this.number = number;
            this.description = description;
            this.group = group;
        }
    }

    public static class BlockEntity extends LiftDestinationDispatchTerminalBase.BlockEntityBase {
        private static final int FLOORS_PER_PAGE = 9;
        private static final String KEY_GROUP_COUNT = "port_floor_group_count";
        private static final String KEY_SELECTED_GROUP_FLOOR = "port_selected_group_floor";

        private final Map<Long, String> floorGroups = new LinkedHashMap<>();
        private long selectedGroupFloorPosition = Long.MIN_VALUE;

        public BlockEntity(BlockPos pos, BlockState state) {
            super(BlockEntityTypes.SCHINDLER_PORT_TERMINAL.get(), pos, state);
            registerScreenId(HOME_PREFIX + "0_ALL");
            setDisplayText("SELECT A DESTINATION");
        }

        @Override
        public void readCompoundTag(CompoundTag compoundTag) {
            super.readCompoundTag(compoundTag);
            final String savedDisplayText = compoundTag.getString("port_display_text");
            if (!savedDisplayText.isEmpty()) {
                setDisplayText(savedDisplayText);
            }

            floorGroups.clear();
            final int groupCount = Math.max(0, Math.min(compoundTag.getInt(KEY_GROUP_COUNT), 512));
            for (int i = 0; i < groupCount; i++) {
                final long floorPosition = compoundTag.getLong("port_group_pos_" + i);
                final String groupName = compoundTag.getString("port_group_name_" + i);
                floorGroups.put(floorPosition, normalizeGroup(groupName));
            }
            selectedGroupFloorPosition = compoundTag.contains(KEY_SELECTED_GROUP_FLOOR)
                    ? compoundTag.getLong(KEY_SELECTED_GROUP_FLOOR) : Long.MIN_VALUE;
        }

        @Override
        public void writeCompoundTag(CompoundTag compoundTag) {
            super.writeCompoundTag(compoundTag);
            compoundTag.putString("port_display_text", getDisplayText());
            compoundTag.putInt(KEY_GROUP_COUNT, floorGroups.size());
            int index = 0;
            for (Map.Entry<Long, String> entry : floorGroups.entrySet()) {
                compoundTag.putLong("port_group_pos_" + index, entry.getKey());
                compoundTag.putString("port_group_name_" + index, entry.getValue());
                index++;
            }
            compoundTag.putLong(KEY_SELECTED_GROUP_FLOOR, selectedGroupFloorPosition);
        }

        public int getPageIndex() {
            return readPage(getScreenId(), HOME_PREFIX);
        }

        public int getManagePage() {
            return readPage(getScreenId(), MANAGE_PREFIX);
        }

        public int getAssignmentPage() {
            return readPage(getScreenId(), ASSIGN_PREFIX);
        }

        private static int readPage(String screenId, String prefix) {
            if (screenId == null || !screenId.startsWith(prefix)) {
                return 0;
            }
            try {
                final String remainder = screenId.substring(prefix.length());
                final int separator = remainder.indexOf('_');
                return Math.max(0, Integer.parseInt(separator < 0 ? remainder : remainder.substring(0, separator)));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }

        public String getActiveGroup() {
            final String screenId = getScreenId();
            if (screenId == null || !screenId.startsWith(HOME_PREFIX)) {
                return "ALL";
            }
            final String remainder = screenId.substring(HOME_PREFIX.length());
            final int separator = remainder.indexOf('_');
            if (separator < 0) {
                return "ALL";
            }
            final String group = remainder.substring(separator + 1);
            return "ALL".equalsIgnoreCase(group) ? "ALL" : normalizeGroup(group);
        }

        public String getSelectedFloor() {
            return CONFIRM_SCREEN.equals(getScreenId()) ? getDisplayText().trim() : "";
        }

        public void setHomePage(int page, World world, BlockPos pos) {
            setHomePage(page, getActiveGroup(), world, pos);
        }

        public void setHomePage(int page, String group, World world, BlockPos pos) {
            final String normalizedGroup = "ALL".equalsIgnoreCase(group) ? "ALL" : normalizeGroup(group);
            final int visibleFloorCount = getFloorsForGroup(world, normalizedGroup).size();
            final int pageCount = Math.max(1, (visibleFloorCount + FLOORS_PER_PAGE - 1) / FLOORS_PER_PAGE);
            final int clampedPage = Math.max(0, Math.min(page, pageCount - 1));
            registerScreenId(HOME_PREFIX + clampedPage + "_" + normalizedGroup);
            setDisplayText("SELECT A DESTINATION");
            syncPortState(world, pos);
        }

        public void setManagePage(int page, World world, BlockPos pos) {
            final int pageCount = Math.max(1, (getAvailableFloors(world).size() + FLOORS_PER_PAGE - 1) / FLOORS_PER_PAGE);
            final int clampedPage = Math.max(0, Math.min(page, pageCount - 1));
            registerScreenId(MANAGE_PREFIX + clampedPage);
            setDisplayText("MANAGE FLOOR GROUPS");
            syncPortState(world, pos);
        }

        public void openGroupAssignment(FloorOption floor, World world, BlockPos pos) {
            selectedGroupFloorPosition = floor.positionKey;
            registerScreenId(ASSIGN_PREFIX + getManagePage());
            setDisplayText(floor.number);
            syncPortState(world, pos);
        }

        public void assignFloorGroup(String group, World world, BlockPos pos) {
            if (selectedGroupFloorPosition != Long.MIN_VALUE) {
                floorGroups.put(selectedGroupFloorPosition, normalizeGroup(group));
            }
            registerScreenId(MANAGE_PREFIX + getAssignmentPage());
            setDisplayText("GROUP SAVED");
            syncPortState(world, pos);
        }

        public String getSelectedGroup() {
            return floorGroups.getOrDefault(selectedGroupFloorPosition, "PUBLIC");
        }

        public String encodeGroupSync() {
            final StringBuilder result = new StringBuilder();
            floorGroups.forEach((position, group) -> {
                if (result.length() > 0) {
                    result.append(';');
                }
                result.append(position).append(':').append(group);
            });
            return result.toString();
        }

        public void applyGroupSync(String encodedGroups, long selectedFloorPosition) {
            if (encodedGroups != null && !encodedGroups.isEmpty()) {
                floorGroups.clear();
                for (String entry : encodedGroups.split(";")) {
                    final int separator = entry.indexOf(':');
                    if (separator <= 0) {
                        continue;
                    }
                    try {
                        final long position = Long.parseLong(entry.substring(0, separator));
                        floorGroups.put(position, normalizeGroup(entry.substring(separator + 1)));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            if (selectedFloorPosition != Long.MIN_VALUE) {
                selectedGroupFloorPosition = selectedFloorPosition;
            }
        }

        private void syncPortState(World world, BlockPos pos) {
            if (world.isClient()) {
                top.xfunny.mod.client.InitClient.REGISTRY_CLIENT.sendPacketToServer(
                        new PacketSyncLiftDestinationDispatchTerminal(pos, getScreenId(), getDisplayText(),
                                encodeGroupSync(), selectedGroupFloorPosition));
            }
        }

        public void selectDestination(String floor, World world, BlockPos pos) {
            registerScreenId(CONFIRM_SCREEN);
            setDisplayText(floor);
            syncPortState(world, pos);
        }

        public void showResult(String result, World world, BlockPos pos) {
            registerScreenId(RESULT_SCREEN);
            setDisplayText(result);
            syncPortState(world, pos);
        }

        public int getTrackPositionCount() {
            return getTrackPositions().size();
        }

        public int getClientLiftCount() {
            return MinecraftClientData.getInstance().lifts.size();
        }

        /** Number of distinct MTR cars connected to this terminal's floor tracks. */
        public int getLinkedLiftCount(World world) {
            final Set<Long> linkedLiftIds = new HashSet<>();
            forEachTrackPosition(trackPosition -> MinecraftClientData.getInstance().lifts.forEach(lift -> {
                if (lift.getFloorIndex(Init.blockPosToPosition(trackPosition)) >= 0) {
                    linkedLiftIds.add(lift.getId());
                }
            }));
            return linkedLiftIds.size();
        }

        /**
         * Reads each actual MTR LiftFloor entry. The previous helper accidentally
         * returned lift.getCurrentFloor() for every iteration, causing floor labels
         * to be duplicated or disappear when the current floor had no block entity.
         */
        public List<FloorOption> getAvailableFloors(World world) {
            final Map<Long, FloorOption> floors = new LinkedHashMap<>();
            final org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectOpenHashSet<BlockPos> linkedTracks = getTrackPositions();
            for (final BlockPos trackPosition : linkedTracks) {
                MinecraftClientData.getInstance().lifts.forEach(lift -> {
                    if (lift.getFloorIndex(Init.blockPosToPosition(trackPosition)) < 0) {
                        return;
                    }

                    lift.iterateFloors(liftFloor -> {
                        final BlockPos floorBlockPos = Init.positionToBlockPos(liftFloor.getPosition());
                        final org.mtr.mapping.holder.BlockEntity floorEntity = world.getBlockEntity(floorBlockPos);
                        String floorNumber = liftFloor.getNumber();
                        String floorDescription = liftFloor.getDescription();

                        if (floorEntity != null && floorEntity.data instanceof BlockLiftTrackFloor.BlockEntity) {
                            final BlockLiftTrackFloor.BlockEntity floorData = (BlockLiftTrackFloor.BlockEntity) floorEntity.data;
                            floorNumber = floorData.getFloorNumber();
                            floorDescription = floorData.getFloorDescription();
                        }

                        if (floorNumber == null || floorNumber.trim().isEmpty()) {
                            return;
                        }

                        final long floorKey = floorBlockPos.asLong();
                        final String group = floorGroups.getOrDefault(floorKey, "PUBLIC");
                        floors.putIfAbsent(floorKey, new FloorOption(floorKey, floorNumber.trim(),
                                floorDescription == null ? "" : floorDescription.trim(), group));
                    });
                });
            }
            return new ArrayList<>(floors.values());
        }

        public List<FloorOption> getVisibleFloors(World world) {
            return getFloorsForGroup(world, getActiveGroup());
        }

        private List<FloorOption> getFloorsForGroup(World world, String group) {
            final List<FloorOption> allFloors = getAvailableFloors(world);
            if ("ALL".equals(group)) {
                return allFloors;
            }
            final List<FloorOption> filtered = new ArrayList<>();
            for (FloorOption floor : allFloors) {
                if (group.equals(floor.group)) {
                    filtered.add(floor);
                }
            }
            return filtered;
        }

        private static String normalizeGroup(String group) {
            if (group == null) {
                return "PUBLIC";
            }
            final String normalized = group.trim().toUpperCase(java.util.Locale.ROOT);
            for (String knownGroup : FLOOR_GROUPS) {
                if (knownGroup.equals(normalized)) {
                    return knownGroup;
                }
            }
            return "PUBLIC";
        }
    }
}
