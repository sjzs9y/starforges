package com.starforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.starforge.whisper.WhisperAccelDisplay;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 画「加速」的悬浮文字：上面一行倍率，下面一行剩余秒数，剩余不足 10 秒变红。
 *
 * <p>写法参考 time-in-a-bottle / 无用模组：把实体当写字板，渲染时先朝向摄像机，再按比例缩放画字。</p>
 */
public class WhisperAccelDisplayRenderer extends EntityRenderer<WhisperAccelDisplay> {

    private static final float TEXT_SCALE = 0.02F;
    private static final int RED_THRESHOLD_TICKS = 200;

    private final Font font;

    public WhisperAccelDisplayRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.font = context.getFont();
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(WhisperAccelDisplay entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        String rateText = "x" + entity.getMultiplier();
        int remainingTicks = Math.max(0, entity.getRemainingTicks());
        String timeText = (remainingTicks / 20) + "s";
        int timeColor = remainingTicks <= RED_THRESHOLD_TICKS ? 0xFF5555 : 0xFFFFFF;

        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
        drawLine(poseStack, buffer, rateText, -0.75F, 0xFFFFFF, packedLight);
        drawLine(poseStack, buffer, timeText, 0.9F, timeColor, packedLight);
        poseStack.popPose();
    }

    private void drawLine(PoseStack poseStack, MultiBufferSource buffer, String text,
                          float y, int color, int packedLight) {
        float x = -this.font.width(text) / 2.0F;
        this.font.drawInBatch(text, x, y, color, false, poseStack.last().pose(), buffer,
                Font.DisplayMode.SEE_THROUGH, 0, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(WhisperAccelDisplay entity) {
        return null;
    }
}
