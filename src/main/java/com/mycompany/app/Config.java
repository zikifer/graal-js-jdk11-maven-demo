package com.mycompany.app;

public class Config {

    private static boolean debugLogging = false;
    private static boolean attachEngine = false;
    private static boolean releaseContextImmediate = false;

    public static void init(String[] args) {
        for (String arg : args) {
            switch (arg) {
                case "debugLogging":
                    debugLogging = true;
                    break;

                case "attachEngine":
                    attachEngine = true;
                    break;

                case "releaseContextImmediate":
                    releaseContextImmediate = true;
                    break;
            }
        }
    }

    public static void print() {
        Logger.println(Logger.Level.INFO, "Running with configuration:");
        Logger.println(Logger.Level.INFO, "-- debugLogging: " + debugLogging);
        Logger.println(Logger.Level.INFO, "-- attachEngine: " + attachEngine);
        Logger.println(Logger.Level.INFO, "-- releaseContextImmediate: " + releaseContextImmediate);
    }

    public static boolean isDebugLogging() {
        return debugLogging;
    }

    public static boolean isAttachEngine() {
        return attachEngine;
    }

    public static boolean isReleaseContextImmediate() {
        return releaseContextImmediate;
    }
}
