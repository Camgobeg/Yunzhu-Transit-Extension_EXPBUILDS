package top.xfunny.mod.client.view;

import org.mtr.mod.render.StoredMatrixTransformations;
import top.xfunny.mod.client.resource.FontList;

/**
 * A Material-style in-world touch card. The original ButtonView still owns
 * the physical hit-test, hover state and click cooldown.
 */
public final class PortTouchButtonView extends ButtonView {
    @Override
    protected org.mtr.mapping.holder.Direction getButtonFacing(org.mtr.mapping.holder.BlockState blockState) {
        return org.mtr.mod.block.IBlock.getStatePropertySafe(blockState,
                org.mtr.mapping.mapper.DirectionHelper.FACING).getOpposite();
    }

    private String caption = "";
    private String subtitle = "";
    private StoredMatrixTransformations captionTransformations;

    public void setCaption(String caption) {
        this.caption = caption == null ? "" : caption;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle == null ? "" : subtitle;
    }

    @Override
    public void setStoredMatrixTransformations(StoredMatrixTransformations transformations) {
        super.setStoredMatrixTransformations(transformations);
        captionTransformations = transformations == null ? null : transformations.copy();
        if (captionTransformations != null) {
            captionTransformations.add(graphicsHolder -> graphicsHolder.translate(0, 0, 0.0002F));
        }
    }

    @Override
    public void render() {
        // Draw the card background and let ButtonView register its live hit area.
        super.render();

        if (caption.isEmpty() || captionTransformations == null) {
            return;
        }

        final boolean hasSubtitle = !subtitle.isEmpty();
        final TextView titleView = new TextView();
        titleView.setId(getId() + "_title");
        titleView.setBasicsAttributes(world, blockPos, FontList.instance.getFont("nimbus_sans_bold"),
                hasSubtitle ? 4.35F : 4.5F, 0xFFF5FAFC);
        titleView.setTextureId("schindler_port_title_" + getId());
        titleView.setText(caption);
        titleView.setTextAlign(TextView.HorizontalTextAlign.CENTER);
        titleView.setDisplayLength(18, 0);
        titleView.setAdaptMode(TextView.AdaptMode.FIT_WIDTH);
        titleView.setPosition(x + width * 0.04F, y + (hasSubtitle ? height * 0.43F : height * 0.04F));
        titleView.setWidth(width * 0.92F);
        titleView.setHeight(hasSubtitle ? height * 0.50F : height * 0.92F);
        titleView.setStoredMatrixTransformations(captionTransformations);
        titleView.render();

        if (hasSubtitle) {
            final TextView subtitleView = new TextView();
            subtitleView.setId(getId() + "_subtitle");
            subtitleView.setBasicsAttributes(world, blockPos, FontList.instance.getFont("nimbus_sans_bold"),
                    2.35F, 0xFFBED0D8);
            subtitleView.setTextureId("schindler_port_subtitle_" + getId());
            subtitleView.setText(subtitle);
            subtitleView.setTextAlign(TextView.HorizontalTextAlign.CENTER);
            subtitleView.setDisplayLength(28, 0);
            subtitleView.setAdaptMode(TextView.AdaptMode.FIT_WIDTH);
            subtitleView.setPosition(x + width * 0.04F, y + height * 0.06F);
            subtitleView.setWidth(width * 0.92F);
            subtitleView.setHeight(height * 0.32F);
            subtitleView.setStoredMatrixTransformations(captionTransformations);
            subtitleView.render();
        }
    }
}
