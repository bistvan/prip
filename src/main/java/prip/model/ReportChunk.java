package prip.model;

import prip.utils.Jsonable;

import java.util.ArrayList;
import java.util.Objects;

public class ReportChunk implements Jsonable {
    private ArrayList<ReportDay> days = new ArrayList<>();
    private ArrayList<ReportTask> tasks = new ArrayList<>();

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
}
