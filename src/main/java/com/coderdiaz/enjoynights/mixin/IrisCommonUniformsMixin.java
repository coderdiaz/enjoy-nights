package com.coderdiaz.enjoynights.mixin;

import com.coderdiaz.enjoynights.client.ShaderDetectionHelper;
import com.coderdiaz.enjoynights.config.EnjoyNightsConfig;
import com.coderdiaz.enjoynights.util.NightAndMoonHelper;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.uniforms.CommonUniforms", remap = false)
public class IrisCommonUniformsMixin {

    @Inject(method = "getDarknessFactor", at = @At("RETURN"), cancellable = true)
    private static void enjoynights$boostDarknessFactorForShaders(CallbackInfoReturnable<Float> cir) {
        if (NightAndMoonHelper.isBloodMoon(Minecraft.getInstance().level)) {
            // Only inject if a shader pack is actively in use
            if (ShaderDetectionHelper.isShaderPackInUse()) {
                float current = cir.getReturnValueF();
                // Maintain a balanced baseline darknessFactor for shaders during Blood Moon.
                // This makes Complementary and Iris shaders render the rich crimson atmospheric sky
                // and clouds continuously without putting the Darkness/blindness effect on the player!
                // If a real Darkness micro-pulse occurs, current will naturally reach 1.0F.
                float target = EnjoyNightsConfig.CLIENT.bloodMoonShaderAtmosphereIntensity.get().floatValue();
                cir.setReturnValue(Math.max(current, target));
            }
        }
    }
}
