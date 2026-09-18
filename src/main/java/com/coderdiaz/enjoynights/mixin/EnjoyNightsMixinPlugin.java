package com.coderdiaz.enjoynights.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class EnjoyNightsMixinPlugin implements IMixinConfigPlugin {

    private static final boolean IS_IRIS_PRESENT = isIrisAvailable();

    private static boolean isIrisAvailable() {
        try {
            Class.forName("net.irisshaders.iris.Iris", false, Thread.currentThread().getContextClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // If the mixin targets Iris, only apply it if Iris is actually installed and present on the classpath.
        if (mixinClassName.contains("Iris")) {
            return IS_IRIS_PRESENT;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
