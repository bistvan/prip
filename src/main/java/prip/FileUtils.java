package prip;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * File manipulation functions.
 */
public class FileUtils {
    private static final String DEFAULT_CHARSET = "UTF-8";
    private static final Predicate<File> ALL = f -> true;

    public static void toFile(String text, File dst) {
        toFile(text, dst, DEFAULT_CHARSET);
    }

    public static void toFile(String text, File dst, String charset) {
        try (FileOutputStream out = new FileOutputStream(dst);
             OutputStreamWriter w = new OutputStreamWriter(out, charset)
        ) {
            w.write(text);
            w.flush();
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    public static void toFileDiff(String text, File dst) {
        toFileDiff(text, dst, DEFAULT_CHARSET);
    }

    /** Writes the text to the given file, if it differs from the existing one */
    public static void toFileDiff(String text, File dst, String charset) {
        if (dst.exists() && readFile(dst, charset).equals(text))
            return;

        toFile(text, dst, charset);
    }

    /** Reads a file to string with default encoding */
    public static String readFile(File f) {
        return readFile(f, DEFAULT_CHARSET);
    }

    /** Reads a file to string with the given encoding */
    public static String readFile(File f, String charset) {
        try (
            FileInputStream in = new FileInputStream(f);
        ) {
            return StreamUtils.toString(in, charset).toString();
        }
        catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    /** Copy source file to the given destination (overwrites if already existed) */
    public static void copyFile(File src, File dst) {
        if (!src.isFile())
            throw new IORuntimeException(src + " is not a file to copy");
        if (dst.isDirectory())
            dst = new File(dst, src.getName());

        try (
            FileInputStream input = new FileInputStream(src);
            FileOutputStream output = new FileOutputStream(dst);
        ) {
            StreamUtils.copy(input, output, null);
            output.flush();
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        dst.setLastModified(src.lastModified());
    }

    /** Copy all files and directories from {@code source} to {@code destination} */
    public static void copyAll(File source, File destination) {
        copyAll(source, destination, ALL, true);
    }

    /** Copy all files from {@code source} to {@code destination} matching the given condition */
    public static void copyAll(File source, File destination, Predicate<File> condition, boolean recursive) {
        if (source.isFile()) {
            if (condition.test(source)) {
                if (!destination.getParentFile().exists())
                    destination.mkdirs();
                copyFile(source, destination);
            }
        } else if (source.isDirectory()) {
            if (destination.exists()) {
                if (!destination.isDirectory())
                    throw new IORuntimeException("Copy failed: can't overwrite " + destination);
            }
            else if (condition.test(source))
                destination.mkdirs(); // copy the directory structure

            File[] l = source.listFiles();
            if (l != null) {
                for (int i = 0; i < l.length; i++) {
                    File f = l[i];
                    if (recursive || f.isFile())
                        copyAll(f, new File(destination, l[i].getName()), condition, recursive);
                }
            }
        }
    }

    public static long getFolderSize(File path) {
        long res = 0;
        if (path.isDirectory()) {
            File[] l = path.listFiles();
            for (int i = 0; i < l.length; i++) {
                res += getFolderSize(l[i]);
            }
        } else if (path.isFile()) {
            res += path.length();
        }
        return res;
    }

    /** Checks if the given folder contains at least a file matching the given condition */
    public static boolean exists(File folder, Predicate<File> condition, boolean recursive) {
        File[] l = folder.listFiles();
        if (l != null) {
            for (int i = l.length; --i >= 0;) {
                File file = l[i];
                if (condition.test(file))
                    return true;
                if (recursive && file.isDirectory() && exists(file, condition, true))
                    return true;
            }
        }
        return false;
    }

    /** Returns the list ob matching files */
    public static List<File> lookup(File folder, Predicate<File> filter, boolean recursive) {
        File[] l = folder.listFiles();
        List<File> result = new ArrayList<File>();
        if (l != null) {
            for (int i = 0; i < l.length; i++) {
                if (recursive && l[i].isDirectory()) {
                    List<File> chr = lookup(l[i], filter, recursive);
                    if (chr.size() > 0) {
                        result.addAll(chr);
                    }
                }
                if (filter.test(l[i])) {
                    result.add(l[i]);
                }
            }
        }
        return result;
    }

    public static void deleteAllRecursive(File f) {
        if (f.exists()) {
            if (f.isDirectory()) {
                File[] ch = f.listFiles();
                if (ch != null) {
                    for (int i = 0; i < ch.length; i++) {
                        deleteAllRecursive(ch[i]);
                    }
                }
            }
            f.delete();
        }
    }

    /**
     * Creates a filter checking for a specific file pattern.
     * @param file if true we are looking for file
     * @param pattern the file name pattern
     * @return the created filter
     */
    public static Predicate<File> fileMatcher(boolean file, String pattern) {
        Pattern p = Pattern.compile(pattern);
        return f -> {
            if (file ? !f.isFile() : !f.isDirectory())
                return false;
            return p.matcher(f.getName()).matches();
        };
    }
}
