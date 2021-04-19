package prip.model;

import prip.utils.Jsonable;

import java.util.ArrayList;
import java.util.Objects;

public class ReportChunk implements Jsonable {
    private ArrayList<ReportDay> days = new ArrayList<>();
    private ArrayList<ReportTask> tasks = new ArrayList<>();
    private Integer totalSpent;
    private int totalActivities;
    private String interval;

    public void addDay(ReportDay d) {
        days.add(Objects.requireNonNull(d));
    }
    public ArrayList<ReportDay> getDays() {
        return days;
    }

    public void addTask(ReportTask t) {
        tasks.add(Objects.requireNonNull(t));
    }
    public ArrayList<ReportTask> getTasks() {
        return tasks;
    }

    public void setDays(ArrayList<ReportDay> days) {
        this.days = days;
    }

    public void setTasks(ArrayList<ReportTask> tasks) {
        this.tasks = tasks;
    }

    public Integer getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(Integer totalSpent) {
        this.totalSpent = totalSpent;
    }

    public void addSpentTime(int spent) {
        totalSpent = totalSpent == null ? spent : totalSpent + spent;
    }

    public void addActivity() {
        totalActivities++;
    }

    public int getTotalActivities() {
        return totalActivities;
    }

    public void setTotalActivities(int totalActivities) {
        this.totalActivities = totalActivities;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }
}
