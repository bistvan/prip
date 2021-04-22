package prip;

import jakarta.servlet.http.Cookie;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.MimeTypes;
import org.stringtemplate.v4.ST;
import prip.model.*;
import prip.utils.JsonResult;
import prip.utils.*;

import java.io.IOException;
import java.text.MessageFormat;
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
            throw new AppException("Couldn't compile JIRA Number pattern: " + nws.getJiraNumber() + " : " + ex.getMessage());
        }

        try {
            if (!StringUtils.isEmpty(nws.getGitHash())) {
                Pattern.compile(nws.getGitHash());
                if (StringUtils.isEmpty(nws.getGitUrl()))
                    throw new AppException("JIRA Pattern provided but URL is empty");
            }
        }
        catch (PatternSyntaxException ex) {
            throw new AppException("Couldn't compile Commit pattern: " + nws.getGitHash() + " : " + ex.getMessage());
        }

        for (Task task: nws.getTasks()) {
            Task wt;
            if (StringUtils.isEmpty(task.getName()))
                throw new AppException("Task name is empty");
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
        ReportChunk primary = getReportChunk(date, ws);
        prip.getChunks().add(primary);
        ReportChunk next = getReportChunk(DateUtils.instance().addWeek(date, 1), ws);
        if (next.getTotalActivities() > 0)
            prip.getChunks().add(next);

        if (!StringUtils.isEmpty(ws.getPripSubject())) {
            try {
                MessageFormat fmt = new MessageFormat(ws.getPripSubject());
                prip.setPripSubject(fmt.format(new String[] {ws.getDevName(), primary.getInterval()}));
            }
            catch (Exception ex) {
                prip.setPripSubject(ex.getClass().getName() + ": " + ex.getMessage());
            }
        }

        return prip.toJson();
    }

    private ReportChunk getReportChunk(Date date, Workspace ws) {
        Pattern jiraPattern = StringUtils.isEmpty(ws.getJiraNumber()) ? null : Pattern.compile(ws.getJiraNumber());
        Pattern commitPattern = StringUtils.isEmpty(ws.getGitHash()) ? null : Pattern.compile(ws.getGitHash());
        ReportChunk chunk = new ReportChunk();
        HashMap<Integer, Task> taskLookup = ws.taskLookup();
        LinkedHashMap<Integer, ReportTask> tasks = new LinkedHashMap<>();
        ReportDay[] days = new ReportDay[7];
        for (Day d : ws.getWeekDays(date)) {
            ReportDay rd = new ReportDay(d.getDate());
            days[DateUtils.instance().getDayOfWeek(d.getDate()) - 1] = rd;

            IdentityHashMap<Task, Integer> dayTask = new IdentityHashMap<>();
            try {
                for (Activity act : d.activities()) {
                    Task t = taskLookup.get(act.getTask());
                    if (t == null)
                        throw new Exception("Unknown task ID: '" + act.getTask() + '\'');

                    ReportTask rt = tasks.computeIfAbsent(t.getId(), integer -> {
                        String title = StringUtils.replacePattern(t.title(), jiraPattern, ws.jiraUrlFmt());
                        ReportTask r = new ReportTask(title, t.getEstimate());
                        chunk.addTask(r);
                        return r;
                    });

                    chunk.addActivity();
                    rt.addSpentTime(act.getSeconds());
                    chunk.addSpentTime(act.getSeconds());

                    String text;
                    if (!StringUtils.isEmpty(text = act.getText())) {
                        String x = StringUtils.replacePattern(text, commitPattern, ws.gitUrlFmt());
                        if (x != text) // replace returned the same instance if no match
                            rt.addCommit(x);
                        else {
                            x = StringUtils.replacePattern(text, jiraPattern, ws.jiraUrlFmt());
                            rt.addActivity(x);
                        }
                    }
                    if (dayTask.putIfAbsent(t, 1) == null)
                        rd.addActivity(StringUtils.replacePattern(t.getName(), jiraPattern, ws.jiraUrlFmt()));
                }

            }
            catch (Exception e) {
                rd.addActivity(e.getClass().getName() + ": " + e.getMessage());
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
            chunk.setInterval(DateUtils.instance().getDateDotFmt().format(first) + " - "
                + DateUtils.instance().getDayOfMonthDotFmt().format(last));

        return chunk;
    }

    @HtAction(path = "/workspace/monthlyReport.xls", customMime = "application/vnd.ms-excel")
    public void monthlyExcelReport(HtContext ctx) throws IOException {
        Workspace ws = getCurrentWorkspace(ctx, true);
        Date d = ctx.optDate("date", DateUtils.instance().getDateFmt());
        if (d == null)
            d = new Date();
        Workbook wb = new HSSFWorkbook();
        Sheet sheet = wb.createSheet("Monthly report");

        int rowCount = 0;
        // header
        Row row = sheet.createRow(rowCount++);
        Cell cell = row.createCell(0);
        cell.setCellValue("Date");
        cell = row.createCell(1);
        cell.setCellValue("Task");
        cell = row.createCell(2);
        cell.setCellValue("Hours");

        CellStyle wrap = wb.createCellStyle();
        wrap.setWrapText(true);
        cell.setCellStyle(wrap);

        Date month = DateUtils.instance().getMonth(d);
        Date next = DateUtils.instance().addMonth(month, 1);

        ArrayList<Day> days = ws.getMonthDays(month, next);
        int dsize = days.size();
        HashMap<Integer, Task> tasks = ws.taskLookup();
        int index = 0;
        int taskChars = 60;
        for (long dtime = month.getTime(); dtime < next.getTime(); dtime += DateUtils.MILLIS_PER_DAY, rowCount++) {
            row = sheet.createRow(rowCount);
            Date dayDate = new Date(dtime);
            while (index < dsize && days.get(index).getDate().getTime() < dtime)
                index++;
            Day day =  index < dsize && DateUtils.instance().sameDay(days.get(index).getDate(), dayDate) ?
                days.get(index) : null;

            row.createCell(0).setCellValue(DateUtils.instance().getDateFmt().format(dayDate));
            StringBuilder b = new StringBuilder();
            double spent = 0;
            int lines = 0;
            if (day != null) {
                IdentityHashMap<Task, StringBuilder> daytask = new IdentityHashMap<>();
                ArrayList<StringBuilder> bl = new ArrayList<>();
                for (Activity act : day.activities()) {
                    Task task = tasks.get(act.getTask());
                    if (task == null)
                        continue;

                    StringBuilder tb = daytask.get(task);
                    if (tb == null) {
                        daytask.put(task, tb = new StringBuilder().append(task.title()));
                        bl.add(tb);
                    }
                    spent += act.getSeconds();
                    if (!StringUtils.isEmpty(act.getText()))
                        tb.append("; ").append(act.getText());
                }
                int n = lines = bl.size();
                for (int i = 0; i < n; i++) {
                    if (b.length() != 0)
                        b.append('\n');
                    String line = bl.get(i).toString();
                    b.append(line);
                    lines += line.length() / taskChars;
                }
            }
            cell = row.createCell(1);
            cell.setCellValue(b.toString());
            if (lines > 1) {
                cell.setCellStyle(wrap);
                row.setHeightInPoints(lines * sheet.getDefaultRowHeightInPoints());
            }
            row.createCell(2).setCellValue(Math.round(spent / 360) / 10d);
        }
        sheet.setColumnWidth(0, 256 * 15);
        sheet.setColumnWidth(1, 256 * taskChars);
        sheet.setColumnWidth(2, 256 * 8);

        wb.write(ctx.response.getOutputStream());
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
