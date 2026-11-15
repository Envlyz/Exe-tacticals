package com.exetacticals.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.exetacticals.GCR;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.FastColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class Utils {

    public static void drawLine(GuiGraphics guiGraphics, float x1, float y1, float x2, float y2, float size, int color) {
        Matrix4f matrix4f = guiGraphics.pose().last().pose();
        VertexConsumer vertexConsumer = guiGraphics.bufferSource().getBuffer(RenderType.gui());

        float deltaX = x2 - x1;
        float deltaY = y2 - y1;

        float length = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);

        if (length == 0) {
            return;
        }

        float pX = -deltaY / length;
        float pY = deltaX / length;

        float halfSize = size / 2.0f;

        float v1x = x1 + pX * halfSize;
        float v1y = y1 + pY * halfSize;

        float v2x = x2 + pX * halfSize;
        float v2y = y2 + pY * halfSize;

        float v3x = x2 - pX * halfSize;
        float v3y = y2 - pY * halfSize;

        float v4x = x1 - pX * halfSize;
        float v4y = y1 - pY * halfSize;

        vertexConsumer.vertex(matrix4f, v1x, v1y, 0).color(color).endVertex();
        vertexConsumer.vertex(matrix4f, v2x, v2y, 0).color(color).endVertex();
        vertexConsumer.vertex(matrix4f, v3x, v3y, 0).color(color).endVertex();
        vertexConsumer.vertex(matrix4f, v4x, v4y, 0).color(color).endVertex();

        guiGraphics.flush();
    }

    public static void drawSolidCircle(GuiGraphics guiGraphics, float centerX, float centerY, float radius, int color, int segments) {
        Matrix4f matrix4f = guiGraphics.pose().last().pose();
        VertexConsumer vertexConsumer = guiGraphics.bufferSource().getBuffer(RenderType.gui());

        for (int i = 0; i < segments; i++) {
            double angle1 = 2.0 * Math.PI * i / segments;
            double angle2 = 2.0 * Math.PI * (i + 1) / segments;

            float x1 = centerX + radius * (float) Math.cos(angle1);
            float y1 = centerY + radius * (float) Math.sin(angle1);
            float x2 = centerX + radius * (float) Math.cos(angle2);
            float y2 = centerY + radius * (float) Math.sin(angle2);

            vertexConsumer.vertex(matrix4f, centerX, centerY, 0).color(color).endVertex();
            vertexConsumer.vertex(matrix4f, x1, y1, 0).color(color).endVertex();
            vertexConsumer.vertex(matrix4f, x2, y2, 0).color(color).endVertex();
        }

        guiGraphics.flush();
    }

    public static void drawVArrow(GuiGraphics graphics, float tipX, float tipY, float length, float thickness, float directionAngle, float armAngle, int color) {
        float directionRad = (float) Math.toRadians(directionAngle);
        float armAngleRad = (float) Math.toRadians(armAngle / 2.0f);

        float angle1 = directionRad - armAngleRad;
        float startX1 = tipX - length * (float) Math.cos(angle1);
        float startY1 = tipY - length * (float) Math.sin(angle1);

        float angle2 = directionRad + armAngleRad;
        float startX2 = tipX - length * (float) Math.cos(angle2);
        float startY2 = tipY - length * (float) Math.sin(angle2);

        drawLine(graphics, startX1, startY1, tipX, tipY, thickness, color);
        drawLine(graphics, startX2, startY2, tipX, tipY, thickness, color);
    }

    public static void drawDevHelperLayers(
            GuiGraphics guiGraphics, Font font, int mouseX, int mouseY, int width, int height, int leftPos, int topPos) {
        RenderSystem.disableDepthTest();
        if (GCR.IS_DEVELOPMENT) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0f, 0f, 1000f);
            guiGraphics.drawString(font, mouseX + " " + mouseY, mouseX + font.width("."), mouseY - font.lineHeight, 0xffffff);
            guiGraphics.hLine(0, width, mouseY, FastColor.ABGR32.color(255,0,255,0));
            guiGraphics.vLine(mouseX, 0, height, FastColor.ABGR32.color(255,0,0,255));

            guiGraphics.hLine(leftPos, leftPos + width, topPos, FastColor.ABGR32.color(255,0,255,255));
            guiGraphics.hLine(leftPos, leftPos + width, topPos + height, FastColor.ABGR32.color(255,0,255,255));
            guiGraphics.vLine(leftPos, topPos, topPos + height, FastColor.ABGR32.color(255,0,255,255));
            guiGraphics.vLine(leftPos + width, topPos, topPos + height, FastColor.ABGR32.color(255,0,255,255));

            guiGraphics.pose().popPose();
        }
        RenderSystem.enableDepthTest();
    }

}
