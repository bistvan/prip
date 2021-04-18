package prip.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Day implements Comparable {
    private Date date;
    private String activities;

    public Day() {
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getActivities() {
        return activities;
    }

    public void setActivities(String activities) {
        this.activities = activities;
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof Day) {
            Day d = (Day) o;
            if (d.date != null) {
                return date == null ? 1 : date.compareTo(d.date);
            }
            else if (date == null)
                return 0;
        }
        return -1;
    }
}
