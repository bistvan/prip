package prip.model;

import prip.utils.DateUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class ReportDay {
    private static final String [] DAYS = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

    private String name;
    private Date date;
    private ArrayList<String> activities = new ArrayList<>();

    public ReportDay() {
    }

    public ReportDay(Date date) {
        setDate(date);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
        this.name = DateUtils.instance().calendar(date).get(Calendar.DAY_OF_MONTH) + " "
            + DAYS[DateUtils.instance().getDayOfWeek(date) - 1];
    }

    public ArrayList<String> getActivities() {
        return activities;
    }

    public void addActivity(String act) {
        this.activities.add(act);
    }
}
