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

/**
 * Schindler PORT-inspired destination-dispatch UI.
 *
 * All components are rendered onto the physical block face. The nested
 * surfaces, clear typography and restrained accent colors are inspired
 * by Material Design, while all input remains handled by YTE's ButtonView.
 */
public class RenderSchindlerPortTerminal extends BlockEntityRenderer<SchindlerPortTerminal.BlockEntity> implements DirectionHelper, IGui, IBlock {
    private static final int BACKGROUND = 0xFF111B24;
    private static final int SURFACE = 0xFF1B2A36;
    private static final int SURFACE_VARIANT = 0xFF263C49;
    private static final int PRIMARY_CONTAINER = 0xFF164C60;
    private static final int PRIMARY = 0xFF77D8EA;
    private static final int ON_SURFACE = 0xFFF3F7F9;
    private static final int ON_SURFACE_VARIANT = 0xFFB0C4CD;
    private static final int SUCCESS = 0xFF80DDB7;
    private static final int WARNING = 0xFFFFD18A;
    private static final int BUTTON_HOVER = 0xFF35677A;

    private static final float FLOOR_BUTTON_WIDTH = 0.26F;
    private static final float FLOOR_BUTTON_HEIGHT = 0.105F;

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
        root.setBackgroundColor(BACKGROUND);

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
        final int pageCount = Math.max(1, (floors.size() + 8) / 9);
        final int linkedCars = data.getLinkedLiftCount(world);

        final LinearLayout header = panel(world, pos, SURFACE);
        header.addChild(text(world, pos, "port_brand", "SCHINDLER PORT", 0.90F, 0.065F, ON_SURFACE, 6.0F, TextView.HorizontalTextAlign.CENTER, 0.02F, 0.004F));
        header.addChild(text(world, pos, "port_brand_subtitle", "DESTINATION DISPATCH", 0.90F, 0.040F, PRIMARY, 3.35F, TextView.HorizontalTextAlign.CENTER, 0.02F, 0.000F));
        header.addChild(text(world, pos, "port_online", linkedCars > 0
                ? linkedCars + (linkedCars == 1 ? " CAR CONNECTED  •  SYSTEM ONLINE" : " CARS CONNECTED  •  SYSTEM ONLINE")
                : "NO LIFTS CONNECTED", 0.90F, 0.038F, linkedCars > 0 ? SUCCESS : WARNING,
                2.85F, TextView.HorizontalTextAlign.CENTER, 0.02F, 0.000F));
        root.addChild(header);

        root.addChild(text(world, pos, "port_prompt", "WHERE WOULD YOU LIKE TO GO?", 0.90F, 0.048F,
                ON_SURFACE_VARIANT, 3.8F, TextView.HorizontalTextAlign.CENTER, 0.05F, 0.006F));

        if (floors.isEmpty()) {
            final LinearLayout emptyPanel = panel(world, pos, SURFACE_VARIANT);
            emptyPanel.addChild(text(world, pos, "port_no_floors", "CONNECT LIFT FLOOR TRACKS", 0.88F, 0.075F,
                    WARNING, 4.0F, TextView.HorizontalTextAlign.CENTER, 0.03F, 0.018F));
            emptyPanel.addChild(text(world, pos, "port_no_floors_hint", "USE THE YTE LIFT LINKER", 0.88F, 0.045F,
                    ON_SURFACE_VARIANT, 3.0F, TextView.HorizontalTextAlign.CENTER, 0.03F, 0.000F));
            root.addChild(emptyPanel);
            return;
        }

        for (int rowIndex = 0; rowIndex < 3; rowIndex++) {
            final LinearLayout floorRow = row(world, pos);
            // LinearLayout lays horizontal children from right to left; add the
            // last visual column first so passengers see floor order left-to-right.
            for (int column = 2; column >= 0; column--) {
                final int visibleIndex = rowIndex * 3 + column;
                final int floorIndex = start + visibleIndex;
                if (floorIndex >= floors.size()) {
                    break;
                }

                final SchindlerPortTerminal.FloorOption floor = floors.get(floorIndex);
                final String subtitle = floor.description == null || floor.description.trim().isEmpty()
                        ? "LEVEL" : floor.description.trim();
                floorRow.addChild(button(world, pos, data.getKeyMapping(), light,
                        "floor_" + visibleIndex, floor.number, subtitle, SURFACE_VARIANT, FLOOR_BUTTON_WIDTH, FLOOR_BUTTON_HEIGHT));
            }
            root.addChild(floorRow);
        }

        final LinearLayout navigation = row(world, pos);
        // Horizontal LinearLayout places the first child at the right edge.
        // Insert NEXT first and PREVIOUS last so the visual order is conventional.
        if (page + 1 < pageCount) {
            navigation.addChild(button(world, pos, data.getKeyMapping(), light,
                    "next", "NEXT", "PAGE", PRIMARY_CONTAINER, 0.25F, 0.075F));
        } else {
            navigation.addChild(text(world, pos, "port_nav_empty_right", " ", 0.25F, 0.075F,
                    ON_SURFACE_VARIANT, 3.0F, TextView.HorizontalTextAlign.CENTER, 0.02F, 0.002F));
        }
        navigation.addChild(text(world, pos, "port_page", "PAGE " + (page + 1) + " OF " + pageCount, 0.30F, 0.075F,
                ON_SURFACE_VARIANT, 3.0F, TextView.HorizontalTextAlign.CENTER, 0.01F, 0.002F));
        if (page > 0) {
            navigation.addChild(button(world, pos, data.getKeyMapping(), light,
                    "previous", "PREVIOUS", "PAGE", SURFACE, 0.25F, 0.075F));
        } else {
            navigation.addChild(text(world, pos, "port_nav_empty_left", " ", 0.25F, 0.075F,
                    ON_SURFACE_VARIANT, 3.0F, TextView.HorizontalTextAlign.CENTER, 0.02F, 0.002F));
        }
        root.addChild(navigation);
        root.addChild(text(world, pos, "port_footer", "SELECT A FLOOR TO CONTINUE", 0.90F, 0.035F,
                ON_SURFACE_VARIANT, 2.75F, TextView.HorizontalTextAlign.CENTER, 0.04F, 0.002F));
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

        root.addChild(text(world, pos, "port_confirm_step", "STEP 2 OF 2  •  REVIEW", 0.90F, 0.045F,
                PRIMARY, 3.2F, TextView.HorizontalTextAlign.CENTER, 0.05F, 0.012F));

        final LinearLayout destinationCard = panel(world, pos, SURFACE_VARIANT);
        destinationCard.addChild(text(world, pos, "port_confirm_label", "YOUR DESTINATION", 0.88F, 0.050F,
                ON_SURFACE_VARIANT, 3.25F, TextView.HorizontalTextAlign.CENTER, 0.03F, 0.012F));
        destinationCard.addChild(text(world, pos, "port_confirm_floor", selectedFloor, 0.88F, 0.145F,
                PRIMARY, 10.5F, TextView.HorizontalTextAlign.CENTER, 0.03F, 0.004F));
        if (!description.isEmpty()) {
            destinationCard.addChild(text(world, pos, "port_confirm_description", description, 0.86F, 0.055F,
                    ON_SURFACE, 3.4F, TextView.HorizontalTextAlign.CENTER, 0.04F, 0.000F));
        }
        root.addChild(destinationCard);

        root.addChild(text(world, pos, "port_confirm_hint", "CONFIRM TO RECEIVE YOUR CAR ASSIGNMENT", 0.90F, 0.045F,
                ON_SURFACE_VARIANT, 2.85F, TextView.HorizontalTextAlign.CENTER, 0.04F, 0.008F));

        final LinearLayout actions = row(world, pos);
        actions.addChild(button(world, pos, data.getKeyMapping(), light,
                "confirm", "CONFIRM", "CALL LIFT", PRIMARY_CONTAINER, 0.39F, 0.105F));
        actions.addChild(button(world, pos, data.getKeyMapping(), light,
                "back", "CHANGE", "DESTINATION", SURFACE, 0.39F, 0.105F));
        root.addChild(actions);
    }

    private void renderResult(LinearLayout root, World world, BlockPos pos, SchindlerPortTerminal.BlockEntity data, int light) {
        final String assignment = data.getDisplayText();
        final boolean assigned = assignment != null && !assignment.contains("NO LIFT");
        root.addChild(text(world, pos, "port_result_status", assigned ? "REQUEST ACCEPTED" : "NO CAR AVAILABLE",
                0.90F, 0.065F, assigned ? SUCCESS : WARNING, 4.8F, TextView.HorizontalTextAlign.CENTER, 0.05F, 0.018F));

        final LinearLayout carCard = panel(world, pos, SURFACE_VARIANT);
        carCard.addChild(text(world, pos, "port_result_label", assigned ? "YOUR ASSIGNED CAR" : "PLEASE CHECK THE LIFT LINKS",
                0.88F, 0.050F, ON_SURFACE_VARIANT, 3.2F, TextView.HorizontalTextAlign.CENTER, 0.03F, 0.012F));
        carCard.addChild(text(world, pos, "port_result_assignment", assigned ? assignment.replace("CAR ", "") : "—",
                0.90F, 0.16F, assigned ? PRIMARY : WARNING, 9.2F, TextView.HorizontalTextAlign.CENTER, 0.025F, 0.008F));
        if (assigned) {
            carCard.addChild(text(world, pos, "port_result_instruction", "PROCEED TO YOUR CAR", 0.88F, 0.045F,
                    ON_SURFACE, 3.2F, TextView.HorizontalTextAlign.CENTER, 0.03F, 0.000F));
        }
        root.addChild(carCard);

        final LinearLayout actions = row(world, pos);
        actions.addChild(button(world, pos, data.getKeyMapping(), light,
                "home", "DONE", "NEW JOURNEY", PRIMARY_CONTAINER, 0.42F, 0.095F));
        root.addChild(actions);
    }

    private LinearLayout panel(World world, BlockPos pos, int color) {
        final LinearLayout panel = new LinearLayout(true);
        panel.setBasicsAttributes(world, pos);
        panel.setWidth(LayoutSize.MATCH_PARENT);
        panel.setHeight(LayoutSize.WRAP_CONTENT);
        panel.setMargin(0.035F, 0.006F, 0.035F, 0.006F);
        panel.setBackgroundColor(color);
        return panel;
    }

    private LinearLayout row(World world, BlockPos pos) {
        final LinearLayout row = new LinearLayout(false);
        row.setBasicsAttributes(world, pos);
        row.setWidth(LayoutSize.MATCH_PARENT);
        row.setHeight(LayoutSize.WRAP_CONTENT);
        row.setMargin(0.015F, 0.003F, 0.015F, 0.003F);
        return row;
    }

    private PortTouchButtonView button(World world, BlockPos pos, DefaultButtonsKeyMapping keyMapping,
                                       int light, String id, String caption, String subtitle, int color,
                                       float width, float height) {
        final PortTouchButtonView button = new PortTouchButtonView();
        button.setId(id);
        button.setCaption(caption);
        button.setSubtitle(subtitle);
        button.setBasicsAttributes(world, pos, keyMapping);
        button.setTexture(new Identifier(Init.MOD_ID, "textures/block/port/rounded_card.png"));
        button.setWidth(width);
        button.setHeight(height);
        button.setLight(light);
        button.setDefaultColor(color);
        button.setHoverColor(BUTTON_HOVER);
        button.setPressedColor(PRIMARY_CONTAINER);
        button.setMargin(0.012F, 0.004F, 0.012F, 0.004F);
        return button;
    }

    private TextView text(World world, BlockPos pos, String id, String value, float width, float height,
                          int color, float fontSize, TextView.HorizontalTextAlign align, float marginLeft, float marginTop) {
        final TextView text = new TextView();
        text.setId(id);
        text.setBasicsAttributes(world, pos, FontList.instance.getFont("nimbus_sans_bold"), fontSize, color);
        text.setTextureId("schindler_port_text_" + id);
        text.setText(value == null ? " " : value);
        text.setDisplayLength(36, 0);
        text.setAdaptMode(TextView.AdaptMode.FIT_WIDTH);
        text.setTextAlign(align);
        text.setWidth(width);
        text.setHeight(height);
        text.setMargin(marginLeft, marginTop, marginLeft, 0);
        return text;
    }
}
