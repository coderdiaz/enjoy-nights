package com.coderdiaz.enjoynights.mixin;

import com.coderdiaz.enjoynights.util.NightAndMoonHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SkyRenderer;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(SkyRenderer.class)
public class SkyRendererMixin {

    @ModifyArg(
            method = "renderMoon",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/DynamicUniforms;writeTransform(Lorg/joml/Matrix4fc;Lorg/joml/Vector4fc;Lorg/joml/Vector3fc;Lorg/joml/Matrix4fc;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"
            ),
            index = 1
    )
    private Vector4fc enjoynights$tintMoonRed(Vector4fc color) {
        if (NightAndMoonHelper.isBloodMoon(Minecraft.getInstance().level)) {
            // Subtle, atmospheric crimson tint for Blood Moon:
            // R=1.0, G=0.35, B=0.35 preserves crater contrast while casting a sinister red hue over the moon
            return new Vector4f(1.0F, 0.35F, 0.35F, color.w());
        }
        return color;
    }
}
