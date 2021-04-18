package prip.utils;

import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.MimeTypes;

import java.io.IOException;
import java.lang.reflect.Method;

public abstract class HttpAction {
    private final MimeTypes.Type mime;

    HttpAction(MimeTypes.Type mime) {
        this.mime = mime;
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

    public static HttpAction methodAction(Object o, Method m, MimeTypes.Type mime) {
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
                    return new StringAction(new ContentProvider.StringMethod(m, o), mime);
                }
                throw new UnsupportedOperationException("Unhandled method: " + m);
            default:
                throw new UnsupportedOperationException("Unhandled mime: " + mime);
        }
    }

    public static HttpAction staticAction(MimeTypes.Type mime, Object val) {
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
                    return new StringAction(new ContentProvider.StringConstant((String) val), mime);
                if (val instanceof Resource)
                    return new StringAction(new ContentProvider.StringResource((Resource) val), mime);
                throw new UnsupportedOperationException("Unhandled value: " + val);
            default:
                throw new UnsupportedOperationException("Unhandled mime: " + mime);
        }
    }

    public static class StringAction extends HttpAction {
        private final ContentProvider cp;

        public StringAction(ContentProvider cp, MimeTypes.Type mime) {
            super(mime);
            this.cp = cp;
        }

        @Override
        public void process(HtContext ctx) throws IOException {
            ctx.response.setContentType(getMimeType().asString());
            ctx.response.setStatus(HttpServletResponse.SC_OK);
            ctx.baseRequest.setHandled(true);
            ctx.response.getWriter().println(cp.asString(ctx));
        }
    }
}