package prip;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public abstract class ContentProvider {
    public String asString(HtContext ctx) {
        throw new UnsupportedOperationException();
    }

    public static class StringConstant extends ContentProvider {
        private final String ct;

        public StringConstant(String ct) {
            this.ct = ct;
        }

        @Override
        public String asString(HtContext ctx) {
            return ct;
        }
    }

    public static class StringResource extends ContentProvider {
        private final Resource res;

        public StringResource(Resource res) {
            this.res = res;
        }

        @Override
        public String asString(HtContext ctx) {
            return res.asString();
        }
    }
    public static class StringMethod extends ContentProvider {
        private final Method m;
        private final Object o;
        private final boolean needsCtx;

        public StringMethod(Method m, Object o) {
            this.m = m;
            this.o = o;
            int ct = m.getParameterCount();
            if (ct > 1 || (ct == 1 && !HtContext.class.isAssignableFrom(m.getParameterTypes()[0])))
                throw new IllegalArgumentException(m + " does not match the http action signature");
            this.needsCtx = ct == 1;
        }

        @Override
        public String asString(HtContext ctx) {
            String s;
            try {
                if (needsCtx)
                    s = (String) m.invoke(o, ctx);
                else
                    s = (String) m.invoke(o);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            return s;
        }
    }

}
