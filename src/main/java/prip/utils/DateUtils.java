package prip.utils;

import java.text.FieldPosition;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtils {
    public static final long MILLIS_PER_DAY = 1000l * 60 * 60 * 24;

    private static DateUtils INSTANCE = new DateUtils();

    private SimpleDateFormat dateSimpleTime;
    private SimpleDateFormat date;
    private Calendar cal;

    public static DateUtils instance() {
        return INSTANCE;
    }

    public SimpleDateFormat getDateFmt() {
        SimpleDateFormat f = dateSimpleTime;
        if (f == null)
            dateSimpleTime = f = new DF("yyyy-MM-dd");
        return f;
    }

    public SimpleDateFormat getDateSimpleTimeFmt() {
        SimpleDateFormat f = date;
        if (f == null)
            date = f = new DF("yyyy-MM-dd'T'HH:mm");
        return f;
    }

    static final class DF extends SimpleDateFormat {
        public DF(String pattern) {
            super(pattern);
        }


        @Override
        public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition pos) {
            return super.format(date, toAppendTo, pos);
        }
    }

    public Calendar calendar() {
        Calendar c = cal;
        if (c == null)
            cal = c = Calendar.getInstance();
        return c;
    }


    /** String representation of the date */
    public String date(Date d) {
        return getDateFmt().format(d);
    }

    /** Converts the given string to date */
    public Date parseDate(String s) {
        try {
            return getDateFmt().parse(s);
        }
        catch (ParseException e) {
            throw new IllegalArgumentException("Parse failed:" + e.getMessage(), e);
        }
    }

    /** String representation of the date */
    public String dateSimpleTime(Date d) {
        return getDateSimpleTimeFmt().format(d);
    }

    /** Converts the given string to date */
    public Date parseDateSimpleTime(String s) {
        try {
            return getDateSimpleTimeFmt().parse(s);
        }
        catch (ParseException e) {
            throw new IllegalArgumentException("Parse failed:" + e.getMessage(), e);
        }
    }

    public Calendar clearTime(Date d) {
        Calendar c = calendar();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    public Date getWeek(Date d) {
        Calendar c = clearTime(d);
        int day = c.get(Calendar.DAY_OF_WEEK);
        if (day > 0)
            c.add(Calendar.DAY_OF_WEEK, -day + 1);
        return c.getTime();
    }

    public boolean sameWeek(Date d1, Date d2) {
        return getWeek(d1).equals(getWeek(d2));
    }

    public boolean sameDay(Date d1, Date d2) {
        Calendar d = clearTime(d1);
        long start = d.getTime().getTime();
        long end = start + MILLIS_PER_DAY;
        long x = d2.getTime();
        return start <= x && x < end;
    }

    public boolean isToday(Date d) {
        return sameDay(d, new Date());
    }
}
