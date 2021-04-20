package prip.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import prip.PripServer;
import prip.utils.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Workspace implements Jsonable {
    public static final List EMPTY = Arrays.asList();
    private static final HashMap<String, Workspace> CACHE = new HashMap<>();

    private String id;

    private boolean autosave;
    private String jiraNumber;
    private String jiraUrl;
    private String gitHash;
    private String gitUrl;

    private int nextTaskId;
    private String devName;
    private String supervisor;
    private String pripSubject;
    private Date current;
    private Date started;
    private Date stopped;
    private ArrayList<Task> tasks;
    private ArrayList<Day> days;

    public Workspace() {

    }

    public Workspace(Workspace ws) {
        this.update(ws);
    }

    public Workspace(String id) {
        this.id = id;
    }

    public void update(Workspace ws) {
        this.id = ws.id;
        this.devName = ws.devName;
        this.supervisor = ws.supervisor;
        this.pripSubject = ws.pripSubject;
        this.started = ws.started;
        this.stopped = ws.stopped;
        this.current = ws.current;

        this.autosave = ws.autosave;
        this.jiraNumber = ws.jiraNumber;
        this.jiraUrl = ws.jiraUrl;
        this.gitHash = ws.gitHash;
        this.gitUrl = ws.gitUrl;
    }

    public static Workspace getInstance(String id) {
        Workspace ws = optInstance(id);
        if (ws == null)
            throw new AppException("No Workspace found: " + id);
        return ws;
    }

    public static Workspace read(CharSequence js) throws IOException {
        Workspace res = new ObjectMapper()
            .setDateFormat(DateUtils.instance().getDateSimpleTimeFmt())
            .reader(Workspace.class).readValue(js.toString());
        return res;
    }

    public synchronized static Workspace optInstance(String id) {
        if (id == null || (id = id.trim()).length() == 0)
            return null;
        Workspace res = CACHE.get(id);
        if (res == null && id != null && Long.parseLong(id, 16) > Integer.MAX_VALUE) {
            File f = new File(PripServer.WORK_FOLDER, id);
            if (f.exists()) {
                String wsjson = FileUtils.readFile(f);
                try {
                    CACHE.put(id, res = read(wsjson));
                } catch (IOException e) {
                    throw new IORuntimeException(e);
                }
            }
        }
        return res;
    }

    public synchronized static Workspace create() {
        Random rnd = new Random();
        long l = 0;
        while (l < Integer.MAX_VALUE)
            l = rnd.nextLong();

        Workspace res = new Workspace(Long.toString(l, 16));
        res.addTask(new Task(res.nextTaskId(), true, "Helping others"));
        res.addTask(new Task(res.nextTaskId(), true, "Review"));
        res.addTask(new Task(res.nextTaskId(), true, "Standup meeting"));
        res.addTask(new Task(res.nextTaskId(), true, "Status meeting"));

        res.setJiraNumber("(\bAAA\b)-[0-9]{1,6}");
        res.setGitHash("#[a-f0-9]{7,40}");
        res.setPripSubject("Progress Report; name: %s; date: %s");

        res.save();

        CACHE.put(res.id, res);
        return res;
    }

    public void save() {
        HashMap<Integer, Task> t = taskLookup();
        long limit = System.currentTimeMillis() - DateUtils.MILLIS_PER_DAY * 6 * 30;
        for (Iterator<Day> it = getDays().iterator(); it.hasNext();) {
            Day d = it.next();
            if (d.getDate().getTime() < limit) {
                it.remove();
            }
            else {
                for (String a : d.getActivities().split("\n")) {
                    if (StringUtils.isEmpty(a)) continue;
                    String [] b = a.split(",");
                    if (StringUtils.isEmpty(b[0])) continue;
                    try {
                        t.remove(Integer.parseInt(b[0]));
                    }
                    catch (Exception ex) {
                        // don't care
                    }
                }
            }
        }
        for (Iterator<Task> it = getTasks().iterator(); it.hasNext();) {
            Task task = it.next();
            if (!task.isPinned() && t.containsKey(task.getId()))
                it.remove();
        }

        File f = new File(PripServer.WORK_FOLDER, id);
        FileUtils.toFile(toJson(), f);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getNextTaskId() {
        return nextTaskId;
    }

    private int nextTaskId() {
        return ++nextTaskId;
    }

    public void setNextTaskId(int nextTaskId) {
        this.nextTaskId = nextTaskId;
    }

    public String getDevName() {
        return devName;
    }

    public void setDevName(String devName) {
        this.devName = devName;
    }

    public String getSupervisor() {
        return supervisor;
    }

    public void setSupervisor(String supervisor) {
        this.supervisor = supervisor;
    }

    public String getPripSubject() {
        return pripSubject;
    }

    public void setPripSubject(String pripSubject) {
        this.pripSubject = pripSubject;
    }

    public Date getCurrent() {
        return current;
    }

    public void setCurrent(Date current) {
        this.current = current;
    }

    public Date getStarted() {
        return started;
    }

    public void setStarted(Date started) {
        this.started = started;
    }

    public Date getStopped() {
        return stopped;
    }

    public void setStopped(Date stopped) {
        this.stopped = stopped;
    }

    public boolean isAutosave() {
        return autosave;
    }

    public void setAutosave(boolean autosave) {
        this.autosave = autosave;
    }

    public String getJiraNumber() {
        return jiraNumber;
    }

    public void setJiraNumber(String jiraNumber) {
        this.jiraNumber = jiraNumber;
    }

    public String getJiraUrl() {
        return jiraUrl;
    }

    public void setJiraUrl(String jiraUrl) {
        this.jiraUrl = jiraUrl;
    }

    public String getGitHash() {
        return gitHash;
    }

    public void setGitHash(String gitHash) {
        this.gitHash = gitHash;
    }

    public String getGitUrl() {
        return gitUrl;
    }

    public void setGitUrl(String gitUrl) {
        this.gitUrl = gitUrl;
    }

    public void addTask(Task t) {
        Objects.requireNonNull(t);
        if (t.getId() < 1)
            t.setId(nextTaskId());
        if (this.tasks == null)
            this.tasks = new ArrayList<>();
        tasks.add(0, t);
    }

    public void addWeekData(Workspace ws, Date date) {
        if (this.tasks == null)
            this.tasks = new ArrayList<>();
        if (ws.tasks != null) {
            for (int i = 0, n = ws.tasks.size(); i < n; i++) {
                Task t = ws.tasks.get(i);
                if (t.isPinned())
                    this.tasks.add(t);
            }
        }
        if (date != null) {
            if (!DateUtils.instance().isToday(date))
                this.setCurrent(date);
            else
                this.setCurrent(null);
        }
        else {
            date = ws.current;
            if (date == null)
                date = new Date();
        }
        if (this.started != null && !DateUtils.instance().isToday(this.started)) {
            this.setStarted(null);
            this.setStopped(null);
        }

        this.days = ws.getWeekDays(date);
    }

    public List<Task> getTasks() {
        return tasks == null ? EMPTY : tasks;
    }

    public HashMap<Integer, Task> taskLookup() {
        HashMap<Integer, Task> result = new HashMap<>();
        for (Task t : getTasks())
            result.put(t.getId(), t);
        return result;
    }

    public void setTasks(ArrayList<Task> tasks) {
        this.tasks = tasks;
    }

    public Task getTask(int id) {
        for (int i = getTasks().size(); --i >= 0;) {
            Task t = tasks.get(i);
            if (t.getId() == id)
                return t;
        }
        return null;
    }

    public List<Day> getDays() {
        return days == null ? EMPTY : days;
    }

    public ArrayList<Day> getWeekDays(Date date) {
        long start = DateUtils.instance().getWeek(date).getTime();
        long end = DateUtils.instance().getWeek(date).getTime() + (7 * DateUtils.MILLIS_PER_DAY);
        ArrayList<Day> result = new ArrayList<>();
        if (this.days != null) {
            for (int i = 0, n = this.days.size(); i < n; i++) {
                Day d = this.days.get(i);
                long t = d.getDate().getTime();
                if (t >= start && t < end)
                    result.add(d);
            }
        }
        return result;
    }

    public void setDays(ArrayList<Day> days) {
        this.days = days;
    }

    public void addDay(Day day) {
        if (days == null)
            days = new ArrayList<>();
        days.add(Objects.requireNonNull(day));
    }

}
