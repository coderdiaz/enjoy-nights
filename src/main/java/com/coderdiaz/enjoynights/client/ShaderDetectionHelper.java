package com.coderdiaz.enjoynights.client;

public class ShaderDetectionHelper {
    private static Boolean irisLoaded = null;

    public static boolean isIrisLoaded() {
        if (irisLoaded == null) {
            try {
                Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                irisLoaded = true;
            } catch (Throwable t) {
                irisLoaded = false;
            }
        }
        return irisLoaded;
    }

    public static boolean isShaderPackInUse() {
        if (!isIrisLoaded()) {
            return false;
        }
        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            return (boolean) apiClass.getMethod("isShaderPackInUse").invoke(api);
        } catch (Throwable t) {
            return false;
        }
    }

    public static String getCurrentShaderPackName() {
        if (!isShaderPackInUse()) {
            return "";
        }
        try {
            Class<?> irisClass = Class.forName("net.irisshaders.iris.Iris");
            Object packName = irisClass.getMethod("getCurrentPackName").invoke(null);
            return packName != null ? packName.toString() : "";
        } catch (Throwable t) {
            return "";
        }
    }

    public static boolean isComplementaryActive() {
        String name = getCurrentShaderPackName().toLowerCase();
        return name.contains("complementary");
    }
}
