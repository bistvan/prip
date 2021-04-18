package prip.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import prip.PripServer;
import prip.utils.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Workspace implements Jsonable {
    public static final List EMPTY = Arrays.asList();
    private static final HashMap<String, Workspace> CACHE = new HashMap<>();

    private String id;
    private int nextTaskId;
    private String firstName;
    private String lastName;
    private String supervisor;
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
        this.firstName = ws.firstName;
        this.lastName = ws.lastName;
        this.supervisor = ws.supervisor;
        this.started = ws.started;
        this.stopped = ws.stopped;
        this.current = ws.current;
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

        res.save();

        CACHE.put(res.id, res);
        return res;
    }

    public void save() {
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

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getSupervisor() {
        return supervisor;
    }

    public void setSupervisor(String supervisor) {
        this.supervisor = supervisor;
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

    public void addTask(Task t) {
        Objects.requireNonNull(t);
        if (t.getId() < 1)
            t.setId(nextTaskId());
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
        }
        else
            date = new Date();

        long start = DateUtils.instance().getWeek(date).getTime();
        long end = DateUtils.instance().getWeek(date).getTime() + (7 * DateUtils.MILLIS_PER_DAY);
        if (this.days == null)
            this.days = new ArrayList<>();
        if (ws.days != null) {
            for (int i = 0, n = ws.days.size(); i < n; i++) {
                Day d = ws.days.get(i);
                long t = d.getDate().getTime();
                if (t >= start && t < end) {
                    this.days.add(d);
                }
            }
        }
    }

    public List<Task> getTasks() {
        return tasks == null ? EMPTY : tasks;
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

    public void setDays(ArrayList<Day> days) {
        this.days = days;
    }

    public void addDay(Day day) {
        if (days == null)
            days = new ArrayList<>();
        days.add(Objects.requireNonNull(day));
    }

}
