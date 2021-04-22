package prip.utils;

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {
    /**
     * Converts the given expression to camelcase, using the delimiters defined by regexp.
     * @param s the string to be converted
     * @param regexp the delimiter pattern
     * @param startLower true starts with lowerCase
     * @return the camelcased string
     */
    public static String toCamelCase(String s, String regexp, boolean startLower) {
        StringBuilder b = new StringBuilder(s.length());
        Matcher m = Pattern.compile(regexp).matcher(s);
        int end = 0;
        while (m.find()) {
            int start = m.start();
            if (start > end) {
                int i;
                if (startLower && b.length() == 0)
                    i = end;
                else {
                    i = end + 1;
                    b.append(Character.toUpperCase(s.charAt(end)));
                }
                while (i < start)
                    b.append(Character.toLowerCase(s.charAt(i++)));
            }
            end = m.end();
        }
        return b.toString();
    }

    /**
     * Converts the camelcase to a separated string
     * @param s the camelcased string
     * @param newDelim the new delimiters to be inserted
     * @return separated string
     */
    public static String deCamelCase(String s, String newDelim) {
        StringBuilder b = new StringBuilder(s.length() + newDelim.length() *4); // estimating the expected capacity
        for (int i = 0, n = s.length(); i < n; i++) {
            char ch = s.charAt(i);
            if (i > 0 && ch == Character.toUpperCase(ch))
                b.append(newDelim);
            b.append(Character.toLowerCase(ch));
        }
        return b.toString();
    }

    /**
     * Returns true if the string is null, or empty.
     */
    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    /**
     * Returns the name of the setter generated from the property name.
     */
    public static String setterName(String name) {
        return genMethodName(name, "set", null);
    }

    /**
     * Returns the name of the getter generated from the property name.
     */
    public static String getterName(String name, boolean isBoolean) {
        return genMethodName(name, isBoolean ? "is" : "get", null);
    }

    /**
     * Generates a method name from a property name.
     * @param name property name
     * @param prefix possibly null prefix prepended before the name
     * @param suffix possibly null suffix appended to the name
     * @return the generated method name
     */
    public static String genMethodName(String name, String prefix, String suffix) {
        StringBuilder b = new StringBuilder();
        if (prefix != null)
            b.append(prefix);

        int l = name.length();
        int start = 0;
        while (true) {
            int index = name.indexOf('_', start);
            int end = index == -1 ? l : index;
            if (end > start) {
                if (b.length() > 0)
                    b.append(Character.toUpperCase(name.charAt(start++)));
                if (end > start)
                    b.append(name, start, end);
            }
            if (index == -1)
                break;
            else
                start = index + 1;
        }

        if (suffix != null)
            b.append(suffix);
        return b.toString();
    }

    public static String eatString(String from, int index) {
        char endChar;
        switch (from.charAt(index)) {
            case '\"': endChar = '\"'; break;
            case '\'': endChar = '\''; break;
            default:
                throw new IllegalArgumentException("No Quote char found at: " + index);
        }

        for (int i = index + 1, l = from.length(); i < l; i++) {
            if (from.charAt(i) == endChar && from.charAt(i - 1) != '\\')
                return from.substring(index + 1, i);
        }

        throw new IllegalArgumentException("No [" + endChar + "] char found until the end");
    }

    /** Encodes the given number into text id */
    public static String idToString(int id) {
        return Integer.toString(id, Character.MAX_RADIX);
    }

    /** Decodes the given text id into number */
    public static int parseId(String id) {
        try {
            return Integer.parseInt(id, Character.MAX_RADIX);
        }
        catch (NumberFormatException e) {
            throw e; // just to put a break point
        }
    }

    public static String unquote(String s) {
        if (isEmpty(s) || s.length() < 2)
            return s;
        char end = s.charAt(0);
        if (end != '\"' && end != '"')
            return s;
        if (s.charAt(s.length() - 1) != end)
            return s;
        return s.substring(1, s.length() - 1);
    }

    public static String escape(String raw) {
        String escaped = raw;
        escaped = escaped.replace("\\", "\\\\");
        escaped = escaped.replace("\"", "\\\"");
        escaped = escaped.replace("\b", "\\b");
        escaped = escaped.replace("\f", "\\f");
        escaped = escaped.replace("\n", "\\n");
        escaped = escaped.replace("\r", "\\r");
        escaped = escaped.replace("\t", "\\t");
        return escaped;
    }

    public static String replacePattern(String s, Pattern p, MessageFormat format) {
        if (p != null && format != null) {
            Matcher m = p.matcher(s);
            int index = 0;
            StringBuilder b = null;
            while (m.find()) {
                if (b == null)
                    b = new StringBuilder();
                if (m.start() > index)
                    b.append(s, index, m.start());
                index = m.end();

                // collecting all groups including the whole, then the pattern decides which one will be used
                int n = m.groupCount() + 1;
                String [] params = new String[n];
                for (int i = 0; i < n; i++)
                    params[i] = m.group(i);
                b.append(format.format(params));
            }
            if (b != null) {
                if (index >= 0 && index < s.length())
                    b.append(s, index, s.length());
                return b.toString();
            }
        }
        return s;
    }
}
