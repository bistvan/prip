package prip;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PripHandler extends AbstractHandler {
    private static Logger log = Logger.getLogger(PripServer.class.getName());

    private final HashMap<String, HashMap<String, HttpAction>> actions;

    public PripHandler(Object... contents) {
        this.actions = new HashMap<>();
        for (Object o : contents) {
            Class<?> clazz = o.getClass();
            for (Field f : clazz.getDeclaredFields()) {
                HtAction anno = f.getAnnotation(HtAction.class);
                if (anno != null && f.canAccess(o)) {
                    try {
                        Object val = f.get(o);
                        if (val == null)
                            throw new IllegalArgumentException("Value is null");

                        HttpAction action = HttpAction.staticAction(anno.mime(), val);
                        register(action, anno);
                    } catch (IllegalAccessException e) {
                        log.log(Level.WARNING, "Couldn't process: " + anno.path(), e);
                    }
                }
            }
            for (Method m : clazz.getDeclaredMethods()) {
                register(m, o);
            }
        }
    }

    private void register(Method m, Object o) {
        HtAction anno = m.getAnnotation(HtAction.class);
        if (anno != null && m.canAccess(o)) {
            HttpAction action = HttpAction.methodAction(o, m, anno.mime());
            register(action, anno);
        }
    }

    void register(Method m, Object o, MimeTypes.Type mime, String path, String... method) {
        if (m.canAccess(o)) {
            HttpAction action = HttpAction.methodAction(o, m, mime);
            register(action, path, method);
        }
    }

    private void register(HttpAction action, HtAction anno) {
        register(action, anno.path(), anno.method());
    }

    private void register(HttpAction action, String path, String... methods) {
        for (String method : methods) {
            HashMap<String, HttpAction> m = actions.get(method);
            if (m == null)
                actions.put(method, m = new HashMap<>());
            m.put(path, action);
        }
    }


    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HashMap<String, HttpAction> m = actions.get(baseRequest.getMethod());
        if (m != null) {
            HttpAction action = m.get(target);
            if (action != null) {
                HtContext ctx = new HtContext(target, baseRequest, request, response, this);
                try {
                    action.process(ctx);
                }
                catch (Exception e) {
                    log.log(Level.SEVERE, "Process went wrong", e);
                    ctx.response.getWriter().println(e.getMessage());
                }
            }
        }
    }
}
