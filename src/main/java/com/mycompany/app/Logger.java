package com.mycompany.app;

public class Logger {

    public enum Level {
        DEBUG,
        INFO
    }

    public static void printf(Level level, String format, Object ... args) {
        if (doPrint(level)) {
            System.out.println(prefix() + String.format(format, args));
        }
    }

    public static void println(Level level, String message) {
        if (doPrint(level)) {
            System.out.println(prefix() + message);
        }
    }

    public static void print(String message, Throwable ex) {
        StringBuilder builder = new StringBuilder(message).append("\n").append(ex).append("\n");
        StackTraceElement[] trace = ex.getStackTrace();
        for (StackTraceElement traceElement : trace) {
            builder.append("\tat ").append(traceElement.toString()).append("\n");
        }
        printf(Level.INFO, builder.toString());
    }

    private static boolean doPrint(Level level) {
        return level == Level.INFO || Config.isDebugLogging();
    }

    private static String prefix() {
        return String.format("[%d] [%s] ", System.currentTimeMillis(), Thread.currentThread().getName());
    }
}
