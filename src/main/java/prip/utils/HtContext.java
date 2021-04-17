package prip.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;

public class HtContext {
    public final String target;
    public final Request baseRequest;
    public final HttpServletRequest request;
    public final HttpServletResponse response;
    public final ActionHandler handler;

    public HtContext(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response, ActionHandler handler) {
        this.target = target;
        this.baseRequest = baseRequest;
        this.request = request;
        this.response = response;
        this.handler = handler;
    }

    public String getString(String id) {
        String s = request.getParameter(id);
        if (s == null)
            throw new AppException("No input parameter: '" + id + '\'');
        return s;
    }

    public String getString(String id, int max) {
        String s = getString(id);
        if (s.length() > max)
            throw new AppException("Input parameter '" + id + "' longer than " + max);
        return s;
    }

    public String optString(String id) {
        String s = request.getParameter(id);
        return s;
    }

    public String optString(String id, int max) {
        String s = optString(id);
        if (s != null && s.length() > max)
            throw new AppException("Input parameter '" + id + "' longer than " + max);
        return s;
    }
}
