package prip.model;

import java.util.ArrayList;
import java.util.Date;

public class ReportDay {
    private Date date;
    private ArrayList<String> activities = new ArrayList<>();

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public ArrayList<String> getActivities() {
        return activities;
    }

    public void addActivity(String act) {
        this.activities.add(act);
    }
}
