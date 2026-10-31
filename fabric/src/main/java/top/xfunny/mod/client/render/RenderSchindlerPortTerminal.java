package top.xfunny.mod.client.render;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityRenderer;
import org.mtr.mapping.mapper.DirectionHelper;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.PlayerHelper;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.data.IGui;
import org.mtr.mod.render.StoredMatrixTransformations;
import top.xfunny.mod.Init;
import top.xfunny.mod.block.SchindlerPortTerminal;
import top.xfunny.mod.client.resource.FontList;
import top.xfunny.mod.client.view.LineComponent;
import top.xfunny.mod.client.view.PortTouchButtonView;
import top.xfunny.mod.client.view.TextView;
import top.xfunny.mod.client.view.view_group.LinearLayout;
import top.xfunny.mod.item.YteGroupLiftButtonsLinker;
import top.xfunny.mod.item.YteLiftButtonsLinker;
import top.xfunny.mod.keymapping.DefaultButtonsKeyMapping;
import top.xfunny.mod.client.view.LayoutSize;

import java.util.List;

public class RenderSchindlerPortTerminal extends BlockEntityRenderer<SchindlerPortTerminal.BlockEntity> implements DirectionHelper, IGui, IBlock {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int MUTED = 0xFF9FB4C2;
    private static final int ACCENT = 0xFF54C5D8;
    private static final int BUTTON = 0xFF203747;
    private static final int BUTTON_HOVER = 0xFF39758A;
    private static final int BUTTON_SELECTED = 0xFF2C6678;
    private static final float BUTTON_WIDTH = 0.235F;
    private static final float BUTTON_HEIGHT = 0.105F;

    public RenderSchindlerPortTerminal(Argument dispatcher) {
        super(dispatcher);
    }

    @Override
    public void render(SchindlerPortTerminal.BlockEntity blockEntity, float tickDelta, GraphicsHolder graphicsHolder, int light, int overlay) {
        final World world = blockEntity.getWorld2();
        if (world == null) {
            return;
        }

        final ClientPlayerEntity player = MinecraftClient.getInstance().getPlayerMapped();
        if (player == null) {
            return;
        }

        final BlockPos blockPos = blockEntity.getPos2();
        final BlockState blockState = world.getBlockState(blockPos);
        final Direction facing = IBlock.getStatePropertySafe(blockState, FACING);
        final boolean holdingLinker = PlayerHelper.isHolding(PlayerEntity.cast(player),
                item -> item.data instanceof YteLiftButtonsLinker || item.data instanceof YteGroupLiftButtonsLinker);

        final DefaultButtonsKeyMapping keyMapping = blockEntity.getKeyMapping();
        keyMapping.clearButtons();

        final StoredMatrixTransformations transform = new StoredMatrixTransformations(0.5, 0, 0.5);
        transform.add(graphics -> {
            graphics.rotateYDegrees(-facing.asRotation());
            graphics.translate(0, 0, 7.9F / 16 - SMALL_OFFSET);
        });

        final LineComponent line = new LineComponent();
        line.setBasicsAttributes(world, blockPos);
        blockEntity.forEachTrackPosition(trackPosition -> line.RenderLine(holdingLinker, trackPosition));

        final LinearLayout root = new LinearLayout(true);
        root.setBasicsAttributes(world, blockPos);
        root.setStoredMatrixTransformations(transform);
        root.setParentDimensions(1, 11F / 16);
        root.setPosition(-8F / 16, 0);
        root.setWidth(LayoutSize.MATCH_PARENT);
        root.setHeight(LayoutSize.MATCH_PARENT);

        final String screenId = blockEntity.getScreenId();
        if (screenId != null && screenId.startsWith(SchindlerPortTerminal.HOME_PREFIX)) {
            renderHome(root, world, blockPos, blockEntity, light);
        } else if (SchindlerPortTerminal.CONFIRM_SCREEN.equals(screenId)) {
            renderConfirm(root, world, blockPos, blockEntity, light);
        } else {
            renderResult(root, world, blockPos, blockEntity, light);
        }

        root.render();
    }

    private void renderHome(LinearLayout root, World world, BlockPos pos, SchindlerPortTerminal.BlockEntity data, int light) {
        final List<SchindlerPortTerminal.FloorOption> floors = data.getAvailableFloors(world);
        final int page = data.getPageIndex();
        final int start = page * 9;

        root.addChild(text(world, pos, "port_title", "SCHINDLER PORT", 0.90F, 0.075F, WHITE, 5.8F, TextView.HorizontalTextAlign.CENTER, 0.05F, 0.012F));
        root.addChild(text(world, pos, "port_prompt", "SELECT YOUR DESTINATION", 0.90F, 0.060F, ACCENT, 4.0F, TextView.HorizontalTextAlign.CENTER, 0.05F, 0.005F));

        if (floors.isEmpty()) {
            root.addChild(text(world, pos, "port_no_floors", "CONNECT LIFT FLOOR TRACKS", 0.88F, 0.14F, MUTED, 4.2F, TextView.HorizontalTextAlign.CENTER, 0.06F, 0.06F));
            return;
        }

        for (int rowIndex = 0; rowIndex < 3; rowIndex++) {
            final LinearLayout row = row(world, pos);
            for (int column = 0; column < 3; column++) {
                final int visibleIndex = rowIndex * 3 + column;
                final int floorIndex = start + visibleIndex;
                if (floorIndex >= floors.size()) {
                    break;
                }
                final SchindlerPortTerminal.FloorOption floor = floors.get(floorIndex);
                row.addChild(button(world, pos, data.getKeyMapping(), light,
                        "floor_" + visibleIndex, floor.number, BUTTON));
            }
            root.addChild(row);
        }

        final int pageCount = Math.max(1, (floors.size() + 8) / 9);
        if (pageCount > 1) {
            final LinearLayout navigation = row(world, pos);
            if (page > 0) {
                navigation.addChild(button(world, pos, data.getKeyMapping(), light, "previous", "PREV", BUTTON));
            } else {
                navigation.addChild(text(world, pos, "port_nav_empty_left", "", 0.235F, 0.075F, MUTED, 3.5F, TextView.HorizontalTextAlign.CENTER, 0.02F, 0.004F));
            }
            navigation.addChild(text(world, pos, "port_page", (page + 1) + " / " + pageCount, 0.235F, 0.075F, MUTED, 3.5F, TextView.HorizontalTextAlign.CENTER, 0.02F, 0.004F));
            if (page + 1 < pageCount) {
                navigation.addChild(button(world, pos, data.getKeyMapping(), light, "next", "NEXT", BUTTON));
            } else {
                navigation.addChild(text(world, pos, "port_nav_empty_right", "", 0.235F, 0.075F, MUTED, 3.5F, TextView.HorizontalTextAlign.CENTER, 0.02F, 0.004F));
            }
            root.addChild(navigation);
        } else {
            root.addChild(text(world, pos, "port_footer", "TOUCH A FLOOR TO CONTINUE", 0.88F, 0.075F, MUTED, 3.2F, TextView.HorizontalTextAlign.CENTER, 0.06F, 0.004F));
        }
    }

    private void renderConfirm(LinearLayout root, World world, BlockPos pos, SchindlerPortTerminal.BlockEntity data, int light) {
        final String selectedFloor = data.getSelectedFloor();
        String description = "";
        for (SchindlerPortTerminal.FloorOption floor : data.getAvailableFloors(world)) {
            if (floor.number.equals(selectedFloor)) {
                description = floor.description;
                break;
            }
        }

        root.addChild(text(world, pos, "port_confirm_title", "CONFIRM DESTINATION", 0.90F, 0.075F, WHITE, 5.4F, TextView.HorizontalTextAlign.CENTER, 0.05F, 0.012F));
        root.addChild(text(world, pos, "port_confirm_floor", selectedFloor, 0.86F, 0.18F, ACCENT, 10.0F, TextView.HorizontalTextAlign.CENTER, 0.07F, 0.035F));
        if (!description.isEmpty()) {
            root.addChild(text(world, pos, "port_confirm_description", description, 0.86F, 0.065F, MUTED, 4.0F, TextView.HorizontalTextAlign.CENTER, 0.07F, 0.012F));
        }
        final LinearLayout actions = row(world, pos);
        actions.addChild(button(world, pos, data.getKeyMapping(), light, "confirm", "CONFIRM", BUTTON_SELECTED));
        actions.addChild(button(world, pos, data.getKeyMapping(), light, "back", "BACK", BUTTON));
        root.addChild(actions);
    }

    private void renderResult(LinearLayout root, World world, BlockPos pos, SchindlerPortTerminal.BlockEntity data, int light) {
        root.addChild(text(world, pos, "port_result_title", "DESTINATION REGISTERED", 0.90F, 0.085F, WHITE, 4.8F, TextView.HorizontalTextAlign.CENTER, 0.05F, 0.015F));
        root.addChild(text(world, pos, "port_result_assignment", data.getDisplayText(), 0.90F, 0.18F, ACCENT, 7.0F, TextView.HorizontalTextAlign.CENTER, 0.05F, 0.04F));
        final LinearLayout actions = row(world, pos);
        actions.addChild(button(world, pos, data.getKeyMapping(), light, "home", "DONE", BUTTON_SELECTED));
        root.addChild(actions);
    }

    private LinearLayout row(World world, BlockPos pos) {
        final LinearLayout row = new LinearLayout(false);
        row.setBasicsAttributes(world, pos);
        row.setWidth(LayoutSize.MATCH_PARENT);
        row.setHeight(LayoutSize.WRAP_CONTENT);
        row.setMargin(0, 0.006F, 0, 0.004F);
        return row;
    }

    private PortTouchButtonView button(World world, BlockPos pos, DefaultButtonsKeyMapping keyMapping,
                                       int light, String id, String caption, int color) {
        final PortTouchButtonView button = new PortTouchButtonView();
        button.setId(id);
        button.setCaption(caption);
        button.setBasicsAttributes(world, pos, keyMapping);
        button.setTexture(new Identifier(Init.MOD_ID, "textures/block/white.png"));
        button.setWidth(BUTTON_WIDTH);
        button.setHeight(BUTTON_HEIGHT);
        button.setLight(light);
        button.setDefaultColor(color);
        button.setHoverColor(BUTTON_HOVER);
        button.setPressedColor(BUTTON_SELECTED);
        button.setMargin(0.02F, 0.006F, 0.02F, 0.006F);
        return button;
    }

    private TextView text(World world, BlockPos pos, String id, String value, float width, float height,
                          int color, float fontSize, TextView.HorizontalTextAlign align, float marginLeft, float marginTop) {
        final TextView text = new TextView();
        text.setId(id);
        text.setBasicsAttributes(world, pos, FontList.instance.getFont("nimbus_sans_bold"), fontSize, color);
        text.setTextureId("schindler_port_text_" + id);
        text.setText(value == null ? "" : value);
        text.setDisplayLength(32, 0);
        text.setAdaptMode(TextView.AdaptMode.FIT_WIDTH);
        text.setTextAlign(align);
        text.setWidth(width);
        text.setHeight(height);
        text.setMargin(marginLeft, marginTop, marginLeft, 0);
        return text;
    }
}
