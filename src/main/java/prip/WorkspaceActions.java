package prip;

import jakarta.servlet.http.Cookie;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.MimeTypes;
import org.stringtemplate.v4.ST;
import prip.model.Day;
import prip.model.ReportChunk;
import prip.utils.JsonResult;
import prip.model.Task;
import prip.model.Workspace;
import prip.utils.*;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;

public class WorkspaceActions implements ActionHolder {
    public static final String COOKIE_FAVORITE_WS = "favorite-ws";
    public static final String WORKSPACE_ID = "wsid";

    public final Resource wsPage = new Resource("/static/workspace.html");
    public final Resource mainPage = new Resource("/static/main.html");

    @HtAction(path = {"/", "/index.html"})
    public String main(HtContext ctx) {
        String noWs = ctx.request.getParameter("noWs");
        ST t = new ST(mainPage.asString(), '$', '$');
        if (noWs != null)
            t.add("error", "No Workspace found: " + noWs);
        return t.render();
    }

    public String workspace(HtContext ctx) throws IOException {
        String wsid = ctx.target.substring(ctx.target.lastIndexOf('/') + 1);
        Cookie foundCookie = getCookie(ctx);
        if (foundCookie != null) {
            if (wsid.equals(foundCookie.getValue())) {
                // ok, later update the date
            }
            else {
                foundCookie.setMaxAge(0);
                ctx.response.addCookie(foundCookie);
                foundCookie = null;
            }
        }
        if (foundCookie == null) {
            Cookie c = new Cookie(COOKIE_FAVORITE_WS, wsid);
            c.setMaxAge(3600 * 24 * 365);
            c.setPath("/");
            ctx.response.addCookie(c);
        }

        ST t = new ST(wsPage.asString(), '$', '$');
        t.add("workspace", wsid);
        t.add("now", new Date().toString());
        return t.render();
    }

    @HtAction(path = "/workspace")
    public String goWorkspace(HtContext ctx) throws IOException, NoSuchMethodException {
        Workspace ws = getCurrentWorkspace(ctx, false);
        if (ws == null) {
            String wsid = deleteCookie(ctx);
            if (wsid != null) {
                ctx.response.sendRedirect("/?noWs=" + wsid);
                return "redirected to main";
            }
            ws = Workspace.create();
        }

        String path = "/workspace/" + ws.getId();
        ctx.handler.register(this.getClass().getDeclaredMethod("workspace", HtContext.class), this,
                MimeTypes.Type.TEXT_HTML_UTF_8, path, HttpMethod.GET);
        ctx.response.sendRedirect(path);
        return "redirect to workspace";
    }

    @HtAction(path = "/workspace/data", mime = MimeTypes.Type.APPLICATION_JSON_UTF_8)
    public String workspaceData(HtContext ctx) {
        Workspace ws = getCurrentWorkspace(ctx, true);
        Date date = ctx.optDate("date", DateUtils.instance().getDateFmt());
        Workspace res = new Workspace(ws);
        res.addWeekData(ws, date);
        return res.toJson();
    }

    @HtAction(path = "/workspace/task", mime = MimeTypes.Type.APPLICATION_JSON_UTF_8)
    public String findTask(HtContext ctx) {
        Workspace ws = getCurrentWorkspace(ctx, true);
        String name = ctx.getString("name", 250);
        for (Task t : ws.getTasks()) {
            if (name.equals(t.getName()))
                return t.toJson();
        }
        return null;
    }

    @HtAction(path = "/workspace/save", mime = MimeTypes.Type.APPLICATION_JSON_UTF_8, method = HttpMethod.POST)
    public String saveWorkspace(HtContext ctx) throws IOException {
        StringBuilder json = StreamUtils.toString(ctx.request.getReader(), "utf-8");
        Workspace ws = getCurrentWorkspace(ctx, true);
        Workspace nws = Workspace.read(json);
        for (Task task: nws.getTasks()) {
            Task wt;
            if (task.getId() == 0 || (wt = ws.getTask(task.getId())) == null)
                ws.addTask(task);
            else {
                wt.update(task);
            }
        }
        for (Day day : nws.getDays()) {
            Day found = null;
            for (Day wd : ws.getDays()) {
                if (DateUtils.instance().sameDay(day.getDate(), wd.getDate())) {
                    found = wd;
                    break;
                }
            }
            if (found != null)
                found.setActivities(day.getActivities());
            else
                ws.addDay(day);
        }
        if (!ws.getDays().isEmpty())
            Collections.sort(ws.getDays());

        ws.update(nws);
        ws.save();

        return new JsonResult().toJson();
    }


    @HtAction(path = "/workspace/report", mime = MimeTypes.Type.APPLICATION_JSON_UTF_8, method = HttpMethod.POST)
    public String getReport(HtContext ctx) throws IOException {
        ReportChunk chunk = new ReportChunk();

        return chunk.toJson();
    }

    private static Cookie getCookie(HtContext ctx) {
        Cookie[] cookies = ctx.request.getCookies();
        if (cookies != null)
            for (Cookie c : cookies) {
                if (c.getName().equals(COOKIE_FAVORITE_WS))
                    return c;
            }
        return null;
    }

    private static Workspace getCurrentWorkspace(HtContext ctx, boolean escalate) {
        String wsid = ctx.optString(WORKSPACE_ID, 100);
        if (wsid == null) {
            Cookie cookie = getCookie(ctx);
            if (cookie != null)
                wsid = cookie.getValue();
        }

        return escalate ? Workspace.getInstance(wsid) : Workspace.optInstance(wsid);
    }

    private static String deleteCookie(HtContext ctx) {
        String found = null;
        Cookie[] cookies = ctx.request.getCookies();
        for (Cookie c : cookies) {
            if (c.getName().equals(COOKIE_FAVORITE_WS)) {
                c.setMaxAge(0);
                ctx.response.addCookie(c);
                found = c.getValue();
            }
        }
        return found;
    }
}
