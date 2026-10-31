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
 * Floor categories can be assigned while the physical terminal is unlocked with a brush.
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
    private static final float FLOOR_BUTTON_HEIGHT = 0.092F;

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
        final boolean unlocked = IBlock.getStatePropertySafe(blockState, SchindlerPortTerminal.UNLOCKED);

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
            renderHome(root, world, blockPos, blockEntity, light, unlocked);
        } else if (screenId != null && screenId.startsWith(SchindlerPortTerminal.MANAGE_PREFIX)) {
            renderManage(root, world, blockPos, blockEntity, light);
        } else if (screenId != null && screenId.startsWith(SchindlerPortTerminal.ASSIGN_PREFIX)) {
            renderAssign(root, world, blockPos, blockEntity, light);
        } else if (SchindlerPortTerminal.CONFIRM_SCREEN.equals(screenId)) {
            renderConfirm(root, world, blockPos, blockEntity, light);
        } else {
            renderResult(root, world, blockPos, blockEntity, light);
        }

        root.render();
    }

    private void renderHome(LinearLayout root, World world, BlockPos pos, SchindlerPortTerminal.BlockEntity data, int light, boolean unlocked) {
        final List<SchindlerPortTerminal.FloorOption> floors = data.getVisibleFloors(world);
        final int page = data.getPageIndex();
        final int start = page * 9;
        final int pageCount = Math.max(1, (floors.size() + 8) / 9);
        final int linkedCars = data.getLinkedLiftCount(world);

        addBrandHeader(root, world, pos, linkedCars, unlocked);
        root.addChild(text(world, pos, "port_prompt", "WHERE WOULD YOU LIKE TO GO?", 0.90F, 0.036F,
                ON_SURFACE_VARIANT, 3.5F, TextView.HorizontalTextAlign.CENTER, 0.04F, 0.002F));

        final LinearLayout groups = row(world, pos);
        final String[] labels = {"ALL", "PUBLIC", "OFFICE", "PARKING", "RESIDENTIAL", "SERVICE", "OTHER"};
        // Horizontal LinearLayout places the first child at the right edge.
        for (int i = labels.length - 1; i >= 0; i--) {
            final String group = labels[i];
            final boolean selected = group.equals(data.getActiveGroup());
            groups.addChild(button(world, pos, data.getKeyMapping(), light,
                    "group_" + group, compactGroupLabel(group), "",
                    selected ? PRIMARY_CONTAINER : SURFACE, 0.112F, 0.050F));
        }
        root.addChild(groups);

        if (floors.isEmpty()) {
            final LinearLayout emptyPanel = panel(world, pos, SURFACE_VARIANT);
            final int tracks = data.getTrackPositionCount();
            final int loadedLifts = data.getClientLiftCount();
            final int matchedLifts = data.getLinkedLiftCount(world);
            final String title;
            final String hint;
            if (tracks == 0) {
                title = "NO FLOOR TRACKS LINKED";
                hint = "USE THE YTE LIFT LINKER ON THIS TERMINAL AND FLOOR TRACKS";
            } else if (loadedLifts == 0) {
                title = "WAITING FOR MTR LIFT DATA";
                hint = "MAKE SURE THE LIFT IS BUILT AND LOADED";
            } else if (matchedLifts == 0) {
                title = "NO MATCHING LIFT FOUND";
                hint = "RELINK THIS TERMINAL TO THE TRACK FLOOR BLOCKS";
            } else if (!"ALL".equals(data.getActiveGroup())) {
                title = "NO FLOORS IN " + data.getActiveGroup();
                hint = "CHOOSE ALL OR ASSIGN FLOORS TO THIS GROUP";
            } else {
                title = "NO FLOOR NUMBERS FOUND";
                hint = "SET FLOOR NUMBERS ON THE MTR FLOOR TRACK BLOCKS";
            }
            emptyPanel.addChild(text(world, pos, "port_no_floors", title, 0.88F, 0.062F,
                    WARNING, 3.8F, TextView.HorizontalTextAlign.CENTER, 0.03F, 0.012F));
            emptyPanel.addChild(text(world, pos, "port_no_floors_hint", hint, 0.88F, 0.055F,
                    ON_SURFACE_VARIANT, 2.6F, TextView.HorizontalTextAlign.CENTER, 0.03F, 0.000F));
            emptyPanel.addChild(text(world, pos, "port_debug_counts",
                    "TRACK LINKS " + tracks + "  •  MTR LIFTS " + loadedLifts + "  •  MATCHED " + matchedLifts,
                    0.88F, 0.032F, ON_SURFACE_VARIANT, 2.45F, TextView.HorizontalTextAlign.CENTER, 0.03F, 0.000F));
            root.addChild(emptyPanel);
            if (unlocked) {
                root.addChild(button(world, pos, data.getKeyMapping(), light,
                        "manage_groups", "MANAGE GROUPS", "ASSIGN FLOOR CATEGORIES", PRIMARY_CONTAINER, 0.48F, 0.065F));
            }
            return;
        }

        for (int rowIndex = 0; rowIndex < 3; rowIndex++) {
            final LinearLayout floorRow = row(world, pos);
            // LinearLayout lays horizontal children from right to left; add the last visual column first.
            for (int column = 2; column >= 0; column--) {
                final int visibleIndex = rowIndex * 3 + column;
                final int floorIndex = start + visibleIndex;
                if (floorIndex >= floors.size()) {
                    break;
                }
                final SchindlerPortTerminal.FloorOption floor = floors.get(floorIndex);
                final String subtitle = floor.description == null || floor.description.trim().isEmpty()
                        ? floor.group : floor.group + " • " + floor.description.trim();
                floorRow.addChild(button(world, pos, data.getKeyMapping(), light,
                        "floor_" + visibleIndex, floor.number, subtitle, SURFACE_VARIANT,
                        FLOOR_BUTTON_WIDTH, FLOOR_BUTTON_HEIGHT));
            }
            root.addChild(floorRow);
        }

        final LinearLayout navigation = row(world, pos);
        if (page + 1 < pageCount) {
            navigation.addChild(button(world, pos, data.getKeyMapping(), light,
                    "next", "NEXT", "PAGE", PRIMARY_CONTAINER, 0.25F, 0.062F));
        } else {
            navigation.addChild(text(world, pos, "port_nav_empty_right", " ", 0.25F, 0.062F,
                    ON_SURFACE_VARIANT, 2.8F, TextView.HorizontalTextAlign.CENTER, 0.02F, 0.002F));
        }
        navigation.addChild(text(world, pos, "port_page", "PAGE " + (page + 1) + " OF " + pageCount, 0.30F, 0.062F,
                ON_SURFACE_VARIANT, 2.8F, TextView.HorizontalTextAlign.CENTER, 0.01F, 0.002F));
        if (page > 0) {
            navigation.addChild(button(world, pos, data.getKeyMapping(), light,
                    "previous", "PREVIOUS", "PAGE", SURFACE, 0.25F, 0.062F));
        } else {
            navigation.addChild(text(world, pos, "port_nav_empty_left", " ", 0.25F, 0.062F,
                    ON_SURFACE_VARIANT, 2.8F, TextView.HorizontalTextAlign.CENTER, 0.02F, 0.002F));
        }
        root.addChild(navigation);

        if (unlocked) {
            root.addChild(button(world, pos, data.getKeyMapping(), light,
                    "manage_groups", "MANAGE GROUPS", "ASSIGN FLOOR CATEGORIES", PRIMARY_CONTAINER, 0.48F, 0.062F));
        } else {
            root.addChild(text(world, pos, "port_footer", "SELECT A FLOOR TO CONTINUE", 0.90F, 0.028F,
                    ON_SURFACE_VARIANT, 2.5F, TextView.HorizontalTextAlign.CENTER, 0.04F, 0.002F));
        }
    }

    private String compactGroupLabel(String group) {
        switch (group) {
            case "PUBLIC": return "PUB";
            case "OFFICE": return "OFF";
            case "PARKING": return "PARK";
            case "RESIDENTIAL": return "RES";
            case "SERVICE": return "SERV";
            default: return group;
        }
    }

    private void addBrandHeader(LinearLayout root, World world, BlockPos pos, int linkedCars, boolean unlocked) {
        final LinearLayout header = panel(world, pos, SURFACE);
        header.addChild(text(world, pos, "port_brand", "SCHINDLER PORT", 0.90F, 0.046F,
                ON_SURFACE, 5.7F, TextView.HorizontalTextAlign.CENTER, 0.02F, 0.002F));
        header.addChild(text(world, pos, "port_brand_subtitle", "DESTINATION DISPATCH", 0.90F, 0.028F,
                PRIMARY, 3.0F, TextView.HorizontalTextAlign.CENTER, 0.02F, 0.000F));
        final String status = linkedCars > 0
                ? linkedCars + (linkedCars == 1 ? " CAR ONLINE" : " CARS ONLINE")
                : "NO LIFTS CONNECTED";
        header.addChild(text(world, pos, "port_online", unlocked ? "GROUP EDITING UNLOCKED  •  " + status : status,
                0.90F, 0.025F, linkedCars > 0 ? SUCCESS : WARNING,
                2.55F, TextView.HorizontalTextAlign.CENTER, 0.02F, 0.000F));
        root.addChild(header);
    }

    private void renderManage(LinearLayout root, World world, BlockPos pos, SchindlerPortTerminal.BlockEntity data, int light) {
        final List<SchindlerPortTerminal.FloorOption> floors = data.getAvailableFloors(world);
        final int page = data.getManagePage();
        final int pageCount = Math.max(1, (floors.size() + 8) / 9);
        final int start = page * 9;

        addBrandHeader(root, world, pos, data.getLinkedLiftCount(world), true);
        root.addChild(text(world, pos, "port_manage_title", "MANAGE FLOOR GROUPS", 0.90F, 0.042F,
                PRIMARY, 3.7F, TextView.HorizontalTextAlign.CENTER, 0.04F, 0.004F));
        root.addChild(text(world, pos, "port_manage_hint", "SELECT A FLOOR TO CHANGE ITS CATEGORY", 0.90F, 0.032F,
                ON_SURFACE_VARIANT, 2.55F, TextView.HorizontalTextAlign.CENTER, 0.04F, 0.000F));

        if (floors.isEmpty()) {
            root.addChild(text(world, pos, "port_manage_empty", "NO FLOORS AVAILABLE TO GROUP", 0.90F, 0.09F,
                    WARNING, 3.5F, TextView.HorizontalTextAlign.CENTER, 0.04F, 0.03F));
            root.addChild(button(world, pos, data.getKeyMapping(), light,
                    "manage_back", "BACK", "DESTINATIONS", SURFACE, 0.42F, 0.065F));
            return;
        }

        for (int rowIndex = 0; rowIndex < 3; rowIndex++) {
            final LinearLayout floorRow = row(world, pos);
            for (int column = 2; column >= 0; column--) {
                final int visibleIndex = rowIndex * 3 + column;
                final int floorIndex = start + visibleIndex;
                if (floorIndex >= floors.size()) {
                    break;
                }
                final SchindlerPortTerminal.FloorOption floor = floors.get(floorIndex);
                floorRow.addChild(button(world, pos, data.getKeyMapping(), light,
                        "manage_floor_" + visibleIndex, floor.number, floor.group,
                        SURFACE_VARIANT, FLOOR_BUTTON_WIDTH, FLOOR_BUTTON_HEIGHT));
            }
            root.addChild(floorRow);
        }

        final LinearLayout navigation = row(world, pos);
        if (page + 1 < pageCount) {
            navigation.addChild(button(world, pos, data.getKeyMapping(), light,
                    "manage_next", "NEXT", "PAGE", PRIMARY_CONTAINER, 0.25F, 0.060F));
        } else {
            navigation.addChild(text(world, pos, "manage_empty_right", " ", 0.25F, 0.060F,
                    ON_SURFACE_VARIANT, 2.8F, TextView.HorizontalTextAlign.CENTER, 0.02F, 0.002F));
        }
        navigation.addChild(text(world, pos, "manage_page", (page + 1) + " / " + pageCount, 0.30F, 0.060F,
                ON_SURFACE_VARIANT, 2.8F, TextView.HorizontalTextAlign.CENTER, 0.01F, 0.002F));
        if (page > 0) {
            navigation.addChild(button(world, pos, data.getKeyMapping(), light,
                    "manage_previous", "PREVIOUS", "PAGE", SURFACE, 0.25F, 0.060F));
        } else {
            navigation.addChild(text(world, pos, "manage_empty_left", " ", 0.25F, 0.060F,
                    ON_SURFACE_VARIANT, 2.8F, TextView.HorizontalTextAlign.CENTER, 0.02F, 0.002F));
        }
        root.addChild(navigation);
        root.addChild(button(world, pos, data.getKeyMapping(), light,
                "manage_back", "BACK", "DESTINATIONS", SURFACE, 0.40F, 0.060F));
    }

    private void renderAssign(LinearLayout root, World world, BlockPos pos, SchindlerPortTerminal.BlockEntity data, int light) {
        final String currentGroup = data.getSelectedGroup();
        addBrandHeader(root, world, pos, data.getLinkedLiftCount(world), true);
        root.addChild(text(world, pos, "port_assign_title", "CHOOSE FLOOR CATEGORY", 0.90F, 0.048F,
                PRIMARY, 3.8F, TextView.HorizontalTextAlign.CENTER, 0.04F, 0.004F));
        root.addChild(text(world, pos, "port_assign_floor", "FLOOR " + data.getDisplayText(), 0.90F, 0.10F,
                ON_SURFACE, 7.0F, TextView.HorizontalTextAlign.CENTER, 0.04F, 0.008F));
        root.addChild(text(world, pos, "port_assign_current", "CURRENT GROUP  •  " + currentGroup, 0.90F, 0.035F,
                ON_SURFACE_VARIANT, 2.8F, TextView.HorizontalTextAlign.CENTER, 0.04F, 0.000F));

        final String[] groups = SchindlerPortTerminal.FLOOR_GROUPS;
        for (int rowIndex = 0; rowIndex < 2; rowIndex++) {
            final LinearLayout groupRow = row(world, pos);
            for (int column = 2; column >= 0; column--) {
                final int index = rowIndex * 3 + column;
                final String group = groups[index];
                groupRow.addChild(button(world, pos, data.getKeyMapping(), light,
                        "assign_" + group, compactGroupLabel(group), currentGroup.equals(group) ? "SELECTED" : "SET GROUP",
                        currentGroup.equals(group) ? PRIMARY_CONTAINER : SURFACE_VARIANT, 0.26F, 0.082F));
            }
            root.addChild(groupRow);
        }

        root.addChild(button(world, pos, data.getKeyMapping(), light,
                "assign_back", "BACK", "FLOOR LIST", SURFACE, 0.40F, 0.060F));
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

        addBrandHeader(root, world, pos, data.getLinkedLiftCount(world), false);
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
        addBrandHeader(root, world, pos, data.getLinkedLiftCount(world), false);
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
