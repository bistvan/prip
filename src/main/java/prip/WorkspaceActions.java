package prip;

import jakarta.servlet.http.Cookie;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.MimeTypes;
import org.stringtemplate.v4.ST;
import prip.model.*;
import prip.utils.JsonResult;
import prip.utils.*;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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
        try {
            if (!StringUtils.isEmpty(nws.getJiraNumber())) {
                Pattern.compile(nws.getJiraNumber());
                if (StringUtils.isEmpty(nws.getJiraUrl()))
                    throw new AppException("JIRA Pattern provided but URL is empty");
            }
        }
        catch (PatternSyntaxException ex) {
            throw new AppException("Couldn't compile JIRA Number pattern: " + ex.getMessage());
        }

        try {
            if (!StringUtils.isEmpty(nws.getGitHash())) {
                Pattern.compile(nws.getGitHash());
                if (StringUtils.isEmpty(nws.getGitUrl()))
                    throw new AppException("JIRA Pattern provided but URL is empty");
            }
        }
        catch (PatternSyntaxException ex) {
            throw new AppException("Couldn't compile Commit pattern: " + ex.getMessage());
        }

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


    @HtAction(path = "/workspace/report", mime = MimeTypes.Type.APPLICATION_JSON_UTF_8, method = HttpMethod.GET)
    public String getReport(HtContext ctx) throws IOException {
        Workspace ws = getCurrentWorkspace(ctx, true);
        Date date = ctx.optDate("date", DateUtils.instance().getDateFmt());
        ProgressReport prip = new ProgressReport();
        prip.getChunks().add(getReportChunk(date, ws));
        ReportChunk next = getReportChunk(DateUtils.instance().addWeek(date, 1), ws);
        if (next.getTotalActivities() > 0)
            prip.getChunks().add(next);

        return prip.toJson();
    }

    private ReportChunk getReportChunk(Date date, Workspace ws) {
        Pattern jiraPattern = StringUtils.isEmpty(ws.getJiraNumber()) ? null : Pattern.compile(ws.getJiraNumber());
        Pattern commitPattern = StringUtils.isEmpty(ws.getGitHash()) ? null : Pattern.compile(ws.getGitHash());
        ReportChunk chunk = new ReportChunk();
        HashMap<Integer, Task> taskLookup = ws.getTaskLookup();
        LinkedHashMap<Integer, ReportTask> tasks = new LinkedHashMap<>();
        ReportDay[] days = new ReportDay[7];
        for (Day d : ws.getWeekDays(date)) {
            ReportDay rd = new ReportDay(d.getDate());
            days[DateUtils.instance().getDayOfWeek(d.getDate()) - 1] = rd;

            String s = d.getActivities();
            IdentityHashMap<Task, Integer> dayTask = new IdentityHashMap<>();
            if (!StringUtils.isEmpty(s)) {
                try {
                    for (String aRow : s.split("\n")) {
                        String[] act = aRow.split(",", 3);
                        Task t = taskLookup.get(Integer.parseInt(act[0]));
                        if (t == null)
                            throw new Exception("Unknown task ID: '" + act[0] + '\'');

                        ReportTask rt = tasks.computeIfAbsent(t.getId(), integer -> {
                            String title = jiraPattern != null ? title = StringUtils.replacePattern(t.title(), jiraPattern, ws.getJiraUrl()) : null;
                            ReportTask r = new ReportTask(title == null ? t.title() : title, t.getEstimate());
                            chunk.addTask(r);
                            return r;
                        });

                        chunk.addActivity();
                        if (act.length > 1) {
                            if (!StringUtils.isEmpty(act[1])) {
                                int spent = Integer.parseInt(act[1]);
                                rt.addSpentTime(spent);
                                chunk.addSpentTime(spent);
                            }
                            String text;
                            if (act.length > 2 && !StringUtils.isEmpty(text = act[2])) {
                                String x = null;
                                if (commitPattern != null)
                                    x = StringUtils.replacePattern(text, commitPattern, ws.getGitUrl());
                                if (x != null)
                                    rt.addCommit(x);
                                else {
                                    x = jiraPattern == null ? null : StringUtils.replacePattern(text, jiraPattern, ws.getJiraUrl());
                                    rt.addActivity(x == null ? text : x);
                                }
                            }
                        }
                        if (dayTask.putIfAbsent(t, 1) == null)
                            rd.addActivity(t.getName());
                    }

                }
                catch (Exception e) {
                    rd.addActivity(e.getClass().getName() + ": " + e.getMessage());
                }
            }
        }

        // add days
        Date start = DateUtils.instance().getWeek(date);
        Date first = null, last = null;
        for (int i = 0; i < days.length; i++) {
            ReportDay d = days[i];
            if (d == null)
                d = new ReportDay(DateUtils.instance().addDay(start, i));
            else {
                if (first == null)
                    first = d.getDate();
                last = d.getDate();
            }

            chunk.addDay(d);
        }
        if (first != null)
            chunk.setInterval(DateUtils.instance().getDateFmt().format(first) + " and " + DateUtils.instance().getDayOfMonthFmt().format(last));

        return chunk;
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
        if (cookies != null)
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
