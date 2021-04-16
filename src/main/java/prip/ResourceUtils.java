package prip;

import java.io.*;

public class ResourceUtils {
    public static String resourceToString(String resource) {
        return resourceToString(resource, "UTF-8");
    }
    public static String resourceToString(String resource, String encoding) {
        try (InputStream in = ResourceUtils.class.getResourceAsStream(resource)) {
            if (in == null)
                throw new IllegalArgumentException("Resource not found: " + resource);
            return toString(in, encoding).toString();
        } catch (IOException e) {
            throw new IllegalArgumentException("Filed to load: " + resource, e);
        }
    }

    public static StringBuilder toString(InputStream input, String encoding) throws IOException {
        return toString(input, encoding, -1);
    }

    public static StringBuilder toString(InputStream input, String encoding, int size) throws IOException {
        try (
                InputStreamReader reader = encoding != null ? new InputStreamReader(input, encoding) : new InputStreamReader(input);
                Reader in = new BufferedReader(reader)
        ) {
            StringBuilder result = size > -1 ? new StringBuilder(size) : new StringBuilder();
            int ch;
            while ((ch = in.read()) > -1)
                result.append((char)ch);
            return result;
        }
    }
}
