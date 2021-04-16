package prip;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;

public class HtContext {
    final String target;
    final Request baseRequest;
    final HttpServletRequest request;
    final HttpServletResponse response;
    final PripHandler handler;

    public HtContext(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response, PripHandler pripHandler) {
        this.target = target;
        this.baseRequest = baseRequest;
        this.request = request;
        this.response = response;
        this.handler = pripHandler;
    }
}
