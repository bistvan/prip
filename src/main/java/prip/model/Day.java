package prip.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import prip.utils.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Day implements Comparable {
    protected static Logger log = Logger.getLogger(Day.class.getName());

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

    public ArrayList<Activity> activities() {
        String s = getActivities();
        ArrayList<Activity> r = new ArrayList<>();
        if (StringUtils.isEmpty(s))
            return r;

        for (String row : s.split("\n")) {
            if (StringUtils.isEmpty(row = row.trim()))
                continue;
            String[] a = row.split(",", 3);
            try {
                Activity act = new Activity();
                act.setTask(Integer.parseInt(a[0]));
                if (a.length > 1 && !StringUtils.isEmpty(a[1])) {
                    String time = a[1];
                    int mul = 1;
                    if (time.endsWith("m"))
                        mul = 60;
                    if (time.endsWith("h"))
                        mul = 3600;
                    if (mul > 1)
                        time = time.substring(0, time.length() - 1);
                    act.setSeconds(Math.round(mul * Float.parseFloat(time)));
                }
                if (a.length > 2)
                    act.setText(a[2]);
                r.add(act);
            }
            catch (Exception ex) {
                // don't care too much
                log.warning("erAXD " + ex.getClass().getName() + ": " + ex.getMessage());
            }
        }
        return r;
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
