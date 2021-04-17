package prip.model;

import java.util.ArrayList;
import java.util.Date;

public class Day {
    private Date date;
    private ArrayList<Activity> activities;

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public ArrayList<Activity> getActivities() {
        return activities;
    }

    public void setActivities(ArrayList<Activity> activities) {
        this.activities = activities;
    }
}
