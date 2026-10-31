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
import top.xfunny.mod.util.GetLiftDetails;
import top.xfunny.mod.util.TransformPositionX;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SchindlerPortTerminal extends LiftDestinationDispatchTerminalBase {
    public static final String HOME_PREFIX = "schindler_port_home_";
    public static final String CONFIRM_SCREEN = "schindler_port_confirm";
    public static final String RESULT_SCREEN = "schindler_port_result";

    public SchindlerPortTerminal() {
        super(true);
    }

    @Nonnull
    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 11, 0.1, IBlock.getStatePropertySafe(state, FACING));
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
                IBlock.getStatePropertySafe(state, FACING)
        );
        final double hitY = MathHelper.fractionalPart(hit.getPos().getYMapped());
        final String button = data.getKeyMapping().mapping(hitX, hitY);
        final String screenId = data.getScreenId();

        if (screenId != null && screenId.startsWith(HOME_PREFIX)) {
            if (button.startsWith("floor_")) {
                try {
                    final int visibleIndex = Integer.parseInt(button.substring("floor_".length()));
                    final List<FloorOption> floors = data.getAvailableFloors(world);
                    final int floorIndex = data.getPageIndex() * BlockEntity.FLOORS_PER_PAGE + visibleIndex;
                    if (floorIndex >= 0 && floorIndex < floors.size()) {
                        data.selectDestination(floors.get(floorIndex).number, world, pos);
                    }
                } catch (NumberFormatException ignored) {
                }
            } else if ("next".equals(button)) {
                data.setHomePage(data.getPageIndex() + 1, world, pos);
            } else if ("previous".equals(button)) {
                data.setHomePage(data.getPageIndex() - 1, world, pos);
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
                data.setHomePage(0, world, pos);
            }
        } else if (RESULT_SCREEN.equals(screenId) && "home".equals(button)) {
            data.setHomePage(0, world, pos);
        }

        return ActionResult.SUCCESS;
    }

    public static final class FloorOption {
        public final String number;
        public final String description;

        public FloorOption(String number, String description) {
            this.number = number;
            this.description = description;
        }
    }

    public static class BlockEntity extends LiftDestinationDispatchTerminalBase.BlockEntityBase {
        private static final int FLOORS_PER_PAGE = 9;

        public BlockEntity(BlockPos pos, BlockState state) {
            super(BlockEntityTypes.SCHINDLER_PORT_TERMINAL.get(), pos, state);
            registerScreenId(HOME_PREFIX + "0");
            setDisplayText("SELECT A DESTINATION");
        }

        public int getPageIndex() {
            final String screenId = getScreenId();
            if (screenId != null && screenId.startsWith(HOME_PREFIX)) {
                try {
                    return Math.max(0, Integer.parseInt(screenId.substring(HOME_PREFIX.length())));
                } catch (NumberFormatException ignored) {
                }
            }
            return 0;
        }

        public String getSelectedFloor() {
            return CONFIRM_SCREEN.equals(getScreenId()) ? getDisplayText().trim() : "";
        }

        public void setHomePage(int page, World world, BlockPos pos) {
            final int pageCount = Math.max(1, (getAvailableFloors(world).size() + FLOORS_PER_PAGE - 1) / FLOORS_PER_PAGE);
            final int clampedPage = Math.max(0, Math.min(page, pageCount - 1));
            registerScreenId(HOME_PREFIX + clampedPage);
            setDisplayText("SELECT A DESTINATION");
            syncDisplayState(world, pos);
        }

        public void selectDestination(String floor, World world, BlockPos pos) {
            registerScreenId(CONFIRM_SCREEN);
            setDisplayText(floor);
            syncDisplayState(world, pos);
        }

        public void showResult(String result, World world, BlockPos pos) {
            registerScreenId(RESULT_SCREEN);
            setDisplayText(result);
            syncDisplayState(world, pos);
        }

        /**
         * Floors are taken from lifts actually linked to this terminal, not from a hard-coded list.
         */
        public List<FloorOption> getAvailableFloors(World world) {
            final Map<String, FloorOption> floors = new LinkedHashMap<>();
            forEachTrackPosition(trackPosition -> MinecraftClientData.getInstance().lifts.forEach(lift -> {
                if (lift.getFloorIndex(Init.blockPosToPosition(trackPosition)) < 0) {
                    return;
                }

                lift.iterateFloors(liftFloor -> {
                    final BlockPos floorPos = Init.positionToBlockPos(liftFloor.getPosition());
                    final org.mtr.mapping.holder.BlockEntity floorEntity = world.getBlockEntity(floorPos);
                    final String floorNumber;
                    final String floorDescription;
                    if (floorEntity != null && floorEntity.data instanceof BlockLiftTrackFloor.BlockEntity) {
                        final org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair<String, String> details =
                                GetLiftDetails.getLiftDetails(world, lift, floorPos).right();
                        floorNumber = details.left();
                        floorDescription = details.right();
                    } else {
                        floorNumber = liftFloor.getNumber();
                        floorDescription = liftFloor.getDescription();
                    }
                    if (floorNumber != null && !floorNumber.trim().isEmpty()) {
                        floors.putIfAbsent(floorNumber, new FloorOption(floorNumber, floorDescription == null ? "" : floorDescription));
                    }
                });
            }));
            return new ArrayList<>(floors.values());
        }
    }
}
