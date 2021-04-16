package prip;

import java.io.*;

/**
 * Stream manipulation utils.
 */
public class StreamUtils {
    public static void print(CharSequence text, Appendable into) {
        try {
            into.append(text);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    public static void print(char c, Appendable into) {
        try {
            into.append(c);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    public static void print(Object c, Appendable into) {
        print(c == null ? "null" : c.toString(), into);
    }

    public static StringBuilder toString(InputStream input, String encoding) {
        return toString(input, encoding, -1);
    }

    public static StringBuilder toString(InputStream input, String encoding, int size) {
        try (
            InputStreamReader reader = encoding != null ? new InputStreamReader(input, encoding) : new InputStreamReader(input);
            Reader in = new BufferedReader(reader)
        ) {
            StringBuilder result = size > -1 ? new StringBuilder(size) : new StringBuilder();
            int ch;
            while ((ch = in.read()) > -1)
                result.append((char) ch);
            return result;
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    public static long copy(InputStream input, OutputStream output, StreamCopyObserver sco) throws IOException {
        byte[] buffer = new byte[1024];
        long count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
            if (sco != null) {
                sco.copyDone(count);
            }
        }
        return count;
    }

    public static long copy(InputStream input, OutputStream output) {
        byte[] buffer = new byte[2048];
        long count = 0;
        int n = 0;
        try {
            while (-1 != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
                count += n;
            }
            return count;
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

}
