package com.mycompany.app;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.io.IOException;
import java.util.Map;

public class GraalSourceLoadFunction implements GraalFunction<Object, Value> {

    private final Context context;

    public GraalSourceLoadFunction(Context context) {
        this.context = context;
    }

    @Override
    @HostAccess.Export
    public Value apply(Object value) {
        final Source source;

        context.enter();
        try {
            if (value instanceof Map) {
                Map valueMap = (Map) value;

                final String script = (String) valueMap.get("script");
                final String name = (String) valueMap.get("name");

                source = Source.newBuilder(App.LANG, script, name).cached(false).build();
            } else {
                throw new RuntimeException("TypeError: cannot load [" + value.getClass() + "]");
            }

            Value result = context.eval(source);
            if (result.isException()) {
                throw result.throwException();
            }

            return result;
        } catch (Exception ex) {
            Logger.print("Failed to Load script", ex);
            throw new RuntimeException(ex);
        } finally {
            context.leave();
        }
    }
}
