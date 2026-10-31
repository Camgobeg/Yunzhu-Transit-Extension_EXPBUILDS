package top.xfunny.mod.client.view;

import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.render.StoredMatrixTransformations;
import top.xfunny.mod.client.resource.FontList;

/**
 * A clickable YTE button that paints its caption over the button texture.
 * The hit area remains the physical block-face area registered by ButtonView.
 */
public final class PortTouchButtonView extends ButtonView {
    private String caption = "";
    private StoredMatrixTransformations captionTransformations;

    public void setCaption(String caption) {
        this.caption = caption == null ? "" : caption;
    }

    @Override
    public void setStoredMatrixTransformations(StoredMatrixTransformations transformations) {
        super.setStoredMatrixTransformations(transformations);
        captionTransformations = transformations == null ? null : transformations.copy();
        if (captionTransformations != null) {
            captionTransformations.add(graphicsHolder -> graphicsHolder.translate(0, 0, -0.0002F));
        }
    }

    @Override
    public void render() {
        super.render();

        if (caption.isEmpty() || captionTransformations == null) {
            return;
        }

        final TextView captionView = new TextView();
        captionView.setId(getId() + "_caption");
        captionView.setBasicsAttributes(world, blockPos, FontList.instance.getFont("nimbus_sans_bold"), 4.2F, 0xFFFFFFFF);
        captionView.setTextureId("schindler_port_button_" + getId());
        captionView.setText(caption);
        captionView.setTextAlign(TextView.HorizontalTextAlign.CENTER);
        captionView.setDisplayLength(24, 0);
        captionView.setAdaptMode(TextView.AdaptMode.FIT_WIDTH);
        captionView.setPosition(x, y);
        captionView.setWidth(width);
        captionView.setHeight(height);
        captionView.setStoredMatrixTransformations(captionTransformations);
        captionView.render();
    }
}
