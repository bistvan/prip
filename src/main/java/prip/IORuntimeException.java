package prip;

import java.io.IOException;

public class IORuntimeException extends RuntimeException {
    public IORuntimeException(IOException e) {
        super(e);
    }

    public IORuntimeException(String s) {
        super(s);
    }
}
