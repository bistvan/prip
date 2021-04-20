package prip.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import prip.utils.StringUtils;

import java.util.ArrayList;
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

    public String[][] activities() {
        String s = getActivities();
        if (StringUtils.isEmpty(s))
            return new String[0][];
        ArrayList<String[]> r = new ArrayList<>();
        for (String row : s.split("\n")) {
            if (StringUtils.isEmpty(row = row.trim()))
                continue;
            String[] a = row.split(",", 3);
            if (a.length < 3) {
                String[] b = new String[3];
                System.arraycopy(a, 0, b, 0, a.length);
            }
            r.add(a);
        }
        return r.toArray(new String[r.size()][]);
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
