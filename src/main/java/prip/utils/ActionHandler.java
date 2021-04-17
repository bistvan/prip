package prip.utils;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import prip.PripServer;
import prip.utils.HtAction;
import prip.utils.HtContext;
import prip.utils.HttpAction;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ActionHandler extends AbstractHandler {
    private static Logger log = Logger.getLogger(PripServer.class.getName());

    private final EnumMap<HttpMethod, HashMap<String, HttpAction>> actions;

    public ActionHandler(ActionHolder... contents) {
        this.actions = new EnumMap<>(HttpMethod.class);
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

    public void register(Method m, Object o, MimeTypes.Type mime, String path, HttpMethod... method) {
        if (m.canAccess(o)) {
            HttpAction action = HttpAction.methodAction(o, m, mime);
            register(action, path, method);
        }
    }

    private void register(HttpAction action, HtAction anno) {
        register(action, anno.path(), anno.method());
    }

    private synchronized void register(HttpAction action, String path, HttpMethod... methods) {
        for (HttpMethod method : methods) {
            HashMap<String, HttpAction> m = actions.get(method);
            if (m == null)
                actions.put(method, m = new HashMap<>());
            m.put(path, action);
        }
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HashMap<String, HttpAction> m = actions.get(HttpMethod.valueOf(baseRequest.getMethod()));
        HttpAction action = null;
        if (m != null) {
            action = m.get(target);
            if (action != null) {
                HtContext ctx = new HtContext(target, baseRequest, request, response, this);
                try {
                    action.process(ctx);
                }
                catch (Exception e) {
                    Throwable t = e instanceof WrapperException ? ((WrapperException) e).unwrap() : e;

                    log.log(Level.SEVERE, "Process went wrong", t);
                    response.getWriter()
                        .append("ERROR: ").append(t.getClass().getName()).append(": ")
                        .append(t.getMessage());
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    baseRequest.setHandled(true);
                }
            }
        }
        if (action == null) {
            m = actions.get(HttpMethod.GET);
            action = m.get("/");
            if (action != null) {
                response.sendRedirect("/");
                baseRequest.setHandled(true);
            }
        }
    }
}
