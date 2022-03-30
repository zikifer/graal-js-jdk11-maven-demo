package com.mycompany.app;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.EnvironmentAccess;
import org.graalvm.polyglot.PolyglotAccess;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static com.mycompany.app.App.LANG;

public class ContextPool {

    private static final Engine engine;
    private static final BlockingDeque<Context> releaseQueue;
    private static final ExecutorService executorService;
    private static boolean running = true;

    static {
        engine = Engine.newBuilder()
                .option("engine.WarnInterpreterOnly", "false")
                .build();

        releaseQueue = new LinkedBlockingDeque<>();

        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(ContextPool::run);
    }

    public static Context borrow() {
        Context.Builder builder = Context.newBuilder(LANG)
                .allowCreateProcess(false)
                .allowCreateThread(false)
                .allowIO(true)
                .allowNativeAccess(false)
                .allowEnvironmentAccess(EnvironmentAccess.NONE)
                .allowPolyglotAccess(PolyglotAccess.ALL)
                .allowHostClassLoading(false)
                .option("js.intl-402", "true")
                .option("js.foreign-object-prototype", "true");

        if (Config.isAttachEngine()) {
            builder.engine(engine);
        }

        return builder.build();
    }

    public static void release(Context context) {
        if (Config.isReleaseContextImmediate()) {
            context.close();
            Logger.println(Logger.Level.DEBUG, "Closed context " + context.hashCode());
        } else {
            releaseQueue.offer(context);
        }
    }

    public static void stop() {
        running = false;
        try {
            engine.close(true);
            executorService.shutdown();
            boolean termination = executorService.awaitTermination(5, TimeUnit.SECONDS);
            Logger.println(Logger.Level.INFO, "Service terminated: " + termination);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void run() {
        Logger.println(Logger.Level.INFO, "Running context release thread");
        do {
            try {
                Context context = releaseQueue.poll(1, TimeUnit.SECONDS);
                if (context != null) {
                    context.close();
                    Logger.println(Logger.Level.DEBUG, "Closed context " + context.hashCode());
                }
            } catch (InterruptedException ex) {
                running = false;
            }
        } while (running);

        Logger.println(Logger.Level.INFO, "Context release thread ending");
    }
}
