package com.example.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class VulkanCompatibleJumpCircle {
    private static float circleRadius = 0f;
    private static boolean isExpanding = false;
    private static double circleX, circleY, circleZ;

    @Inject(method = "render", at = @At("TAIL"))
    private void renderCircleSafe(RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, 
                                  GameRenderer gameRenderer, Matrix4f lightmapTextureManager, CallbackInfo ci) {
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        if (client.player.jumping && !isExpanding && client.player.isOnGround()) {
            isExpanding = true;
            circleRadius = 0.2f;
            circleX = client.player.getX();
            circleY = client.player.getY() + 0.02;
            circleZ = client.player.getZ();
        }

        if (isExpanding) {
            circleRadius += 0.12f * tickCounter.getTickDelta(true);
            if (circleRadius > 2.2f) {
                isExpanding = false;
            } else {
                VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();
                VertexConsumer buffer = immediate.getBuffer(RenderLayer.getLines());

                MatrixStack matrices = new MatrixStack();
                matrices.push();
                matrices.translate(circleX - camera.getPos().x, circleY - camera.getPos().y, circleZ - camera.getPos().z);
                
                int points = 40;
                float alpha = 1.0f - (circleRadius / 2.2f);
                
                for (int i = 0; i < points; i++) {
                    float angle = (float) (i * 2.0 * Math.PI / points);
                    float nextAngle = (float) ((i + 1) * 2.0 * Math.PI / points);

                    float x1 = MathHelper.sin(angle) * circleRadius;
                    float z1 = MathHelper.cos(angle) * circleRadius;
                    float x2 = MathHelper.sin(nextAngle) * circleRadius;
                    float z2 = MathHelper.cos(nextAngle) * circleRadius;

                    MatrixStack.Entry entry = matrices.peek();
                    buffer.vertex(entry.getPositionMatrix(), x1, 0, z1).color(0.0f, 0.8f, 1.0f, alpha).normal(entry, 0, 1, 0);
                    buffer.vertex(entry.getPositionMatrix(), x2, 0, z2).color(0.0f, 0.8f, 1.0f, alpha).normal(entry, 0, 1, 0);
                }

                matrices.pop();
                immediate.draw();
            }
        }
    }
}

