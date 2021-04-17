package prip.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import prip.PripServer;
import prip.utils.AppException;
import prip.utils.FileUtils;
import prip.utils.IORuntimeException;
import prip.utils.WrapperException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class Workspace {
    private static final HashMap<String, Workspace> CACHE = new HashMap<>();

    private String id;
    private int nextTaskId;
    private String firstName;
    private String lastName;
    private String supervisor;
    private ArrayList<Task> tasks;
    private ArrayList<Day> days;

    public Workspace() {

    }

    public Workspace(Workspace ws) {
        this.id = ws.id;
        this.firstName = ws.firstName;
        this.lastName = ws.lastName;
        this.supervisor = ws.supervisor;
    }

    public Workspace(String id) {
        this.id = id;
    }



    public static Workspace getInstance(String id) {
        Workspace ws = optInstance(id);
        if (ws == null)
            throw new AppException("No Workspace found: " + id);
        return ws;
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
                    res = new ObjectMapper().reader(Workspace.class).readValue(wsjson);
                    CACHE.put(id, res);
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

    public String toJson() {
        try {
            return new ObjectMapper()
                    .writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
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

    public void addTask(Task t) {
        if (tasks == null)
            tasks = new ArrayList<>();
        tasks.add(0, t);
    }

    public void addPinnedTasks(Workspace ws) {
        if (this.tasks == null)
            this.tasks = new ArrayList<>();
        if (ws.tasks != null) {
            for (int i = 0, n = ws.tasks.size(); i < n; i++) {
                Task t = ws.tasks.get(i);
                if (t.isPinned())
                    this.tasks.add(t);
            }
        }
    }

    public ArrayList<Task> getTasks() {
        return tasks;
    }

    public void setTasks(ArrayList<Task> tasks) {
        this.tasks = tasks;
    }

    public ArrayList<Day> getDays() {
        return days;
    }

    public void setDays(ArrayList<Day> days) {
        this.days = days;
    }
}
