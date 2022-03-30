/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.mycompany.app;

import org.apache.commons.io.IOUtils;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Simple benchmark for Graal.js via GraalVM Polyglot Context and ScriptEngine.
 */
public class App {

    public static final String LANG = "js";

    public static void main(String[] args) throws Exception {
        String sourceName = "/js/handleEvent.js";
        String handleEventSource = loadResource(sourceName);

        Config.init(args);
        Config.print();

        Logger.println(Logger.Level.INFO, "Press <Enter> to begin");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        int iterations = 10000;
        CountDownLatch countDownLatch = new CountDownLatch(iterations);

        long start = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            int index = i;
            executorService.execute(() -> {
                try {
                    run(sourceName, handleEventSource, index);
                } catch (Exception ex) {
                    Logger.print("run() failed", ex);
                }
                countDownLatch.countDown();
            });
        }

        countDownLatch.await(5, TimeUnit.MINUTES);
        Logger.printf(Logger.Level.INFO, "Done.  Took %d ms.", System.currentTimeMillis() - start);
        scanner.nextLine();
        ContextPool.stop();
        System.exit(0);
    }

    static String loadResource(String location) throws IOException {
        try (InputStream stream = App.class.getResourceAsStream(location)) {
            if (stream == null) {
                throw new IOException("Resource [" + location + "] not found");
            }
            return IOUtils.toString(stream, Charset.defaultCharset());
        }
    }

    static Source loadSource(String location) throws IOException {
        String contents = loadResource(location);
        return Source.newBuilder(LANG, contents, location).cached(false).internal(true).buildLiteral();
    }

    static void run(String sourceName,
                    String handleEventSource,
                    int index) throws IOException {
        Source jvmNpmSource = loadSource("/js/jvm-npm.js");

        Context context = ContextPool.borrow();
        try {
            context.enter();

            /* Mirror GraalEngine.java */
            Value bindings = context.getBindings(LANG);

            String rootDirectory = Path.of(".").toAbsolutePath().toString();
            ModuleIO io = new ModuleIO(rootDirectory);

            GraalSourceLoadFunction loader = new GraalSourceLoadFunction(context);

            bindings.putMember("load", loader);
            bindings.putMember("io", io);

            Value jvmNPM = context.eval(jvmNpmSource);
            jvmNPM.getMember("createRoot").execute(io);
            bindings.putMember("runtime", jvmNPM);

            /* Mirror GraalContext.java */
            Value loadFunction = bindings.getMember("runtime").getMember("_loadScript");
            Value entryPoint = loadFunction.execute(sourceName, handleEventSource);

            /* Mirror JsExecutor.java */
            Value jsFunction = entryPoint.getMember("handleEvent");
            Value result = jsFunction.execute("index_" + index);
            Logger.println(Logger.Level.DEBUG, "Result: " + result.asString());
        } finally {
            context.leave();
            ContextPool.release(context);
        }
    }
}
