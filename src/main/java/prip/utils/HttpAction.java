package prip.utils;

import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.MimeTypes;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public abstract class HttpAction {
    private final MimeTypes.Type mime;
    private final String custMime;

    HttpAction(MimeTypes.Type mime, String custMime) {
        this.mime = mime;
        this.custMime = custMime;
    }

    public MimeTypes.Type getMimeType() {
        return mime;
    }

    public boolean isJson() {
        switch (mime) {
            case TEXT_JSON:
            case TEXT_JSON_8859_1:
            case TEXT_JSON_UTF_8:
            case APPLICATION_JSON:
            case APPLICATION_JSON_UTF_8:
            case APPLICATION_JSON_8859_1:
                return true;
        }
        return false;
    }

    public abstract void process(HtContext ctx) throws IOException;

    void done(HtContext ctx) {
        ctx.response.setContentType(
            StringUtils.isEmpty(custMime) ? getMimeType().asString() : custMime);
        ctx.response.setStatus(HttpServletResponse.SC_OK);
        ctx.baseRequest.setHandled(true);
    }

    public static HttpAction methodAction(Object o, Method m, MimeTypes.Type mime, String custMime) {
        Class<?> t = m.getReturnType();
        if (t == String.class) {
            switch (mime) {
                case TEXT_HTML:
                case TEXT_HTML_UTF_8:
                case TEXT_XML:
                case TEXT_XML_UTF_8:
                case TEXT_JSON:
                case TEXT_JSON_UTF_8:
                case APPLICATION_JSON:
                case APPLICATION_JSON_UTF_8:
                    Class<?> ret = m.getReturnType();
                    if (String.class.isAssignableFrom(ret)) {
                        return new StringAction(new ContentProvider.StringMethod(m, o), mime, custMime);
                    }
                    throw new UnsupportedOperationException("Unhandled method: " + m);
                default:
                    throw new UnsupportedOperationException("Unhandled mime: " + mime + " by " + m);
            }
        }
        else if (t == void.class) {
            return new VoidAction(mime, custMime, m, o);
        }
        else
            throw new UnsupportedOperationException("Unhandled method type: " + m);
    }

    public static HttpAction staticAction(MimeTypes.Type mime, String custMime, Object val) {
        switch (mime) {
            case TEXT_HTML:
            case TEXT_HTML_UTF_8:
            case TEXT_XML:
            case TEXT_XML_UTF_8:
            case TEXT_JSON:
            case TEXT_JSON_UTF_8:
            case APPLICATION_JSON:
            case APPLICATION_JSON_UTF_8:
                if (val instanceof String)
                    return new StringAction(new ContentProvider.StringConstant((String) val), mime, custMime);
                if (val instanceof Resource)
                    return new StringAction(new ContentProvider.StringResource((Resource) val), mime, custMime);
                throw new UnsupportedOperationException("Unhandled value: " + val);
            default:
                throw new UnsupportedOperationException("Unhandled mime: " + mime);
        }
    }

    public static class VoidAction extends HttpAction {
        private final Method method;
        private final boolean needsCtx;
        private final Object o;

        VoidAction(MimeTypes.Type mime, String custMime, Method m, Object o) {
            super(mime, custMime);
            this.method = m;
            this.o = o;
            int ct = m.getParameterCount();
            if (ct > 1 || (ct == 1 && !HtContext.class.isAssignableFrom(m.getParameterTypes()[0])))
                throw new IllegalArgumentException(m + " does not match the http action signature");
            this.needsCtx = ct == 1;
        }

        @Override
        public void process(HtContext ctx) throws IOException {
            try {
                if (needsCtx)
                    method.invoke(o, ctx);
                else
                    method.invoke(o);
                done(ctx);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new WrapperException(e);
            }
        }
    }

    public static class StringAction extends HttpAction {
        private final ContentProvider cp;

        public StringAction(ContentProvider cp, MimeTypes.Type mime, String custMime) {
            super(mime, custMime);
            this.cp = cp;
        }

        @Override
        public void process(HtContext ctx) throws IOException {
            ctx.response.getWriter().println(cp.asString(ctx));
            done(ctx);
        }
    }
}
