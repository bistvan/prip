package prip;

import jakarta.servlet.http.Cookie;
import org.eclipse.jetty.http.MimeTypes;
import org.stringtemplate.v4.ST;

import java.io.IOException;
import java.util.Date;
import java.util.Random;

public class WorkspaceActions {
    public static final String COOKIE_FAVORITE_WS = "favorite-ws";
    public final Resource ws = new Resource("/static/workspace.html");

    public String workspace(HtContext ctx) throws IOException {
        String wsid = ctx.target.substring(ctx.target.lastIndexOf('/') + 1);
        boolean foundCookie = false;
        Cookie[] cookies = ctx.request.getCookies();
        if (cookies != null)
            for (Cookie cookie : ctx.request.getCookies()) {
                if (cookie.getName().equals(COOKIE_FAVORITE_WS)) {
                    if (wsid.equals(cookie.getValue())) {
                        // ok, later update the date
                        foundCookie = true;
                    }
                    else {
                        cookie.setMaxAge(0);
                        ctx.response.addCookie(cookie);
                    }
                }
            }

        if (!foundCookie) {
            Cookie c = new Cookie(COOKIE_FAVORITE_WS, wsid);
            c.setMaxAge(3600 * 24 * 365);
            c.setPath("/");
            ctx.response.addCookie(c);
        }

        ST t = new ST(ws.asString(), '$', '$');
        t.add("workspace", wsid);
        t.add("now", new Date().toString());
        return t.render();
    }

    @HtAction(path = "/workspace")
    public String goWorkspace(HtContext ctx) throws IOException, NoSuchMethodException {
        Cookie[] cookies = ctx.request.getCookies();
        if (cookies != null)
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(COOKIE_FAVORITE_WS)) {
                    ctx.response.sendRedirect("/workspace/" + cookie.getValue());
                    return "redirected to workspace";
                }
            }
        Random rnd = new Random();
        long l = 0;
        while (l < Integer.MAX_VALUE)
            l = rnd.nextLong();
        String path = "/workspace/" + Long.toString(l, 16);
        ctx.handler.register(this.getClass().getDeclaredMethod("workspace", HtContext.class), this,
                MimeTypes.Type.TEXT_HTML_UTF_8, path, "GET");
        ctx.response.sendRedirect(path);
        return "redirect to new workspace";
    }
}
