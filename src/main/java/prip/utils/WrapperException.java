package prip.utils;

import java.lang.reflect.InvocationTargetException;

public class WrapperException extends RuntimeException {
    public WrapperException(Exception e) {
        super(e);
    }

    public Throwable unwrap() {
        Throwable t = this.getCause();
        if (t instanceof InvocationTargetException)
            t = t.getCause();
        return t;
    }
}
