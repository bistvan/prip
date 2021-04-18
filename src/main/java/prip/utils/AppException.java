package prip.utils;

public class AppException extends RuntimeException {
    public AppException(String s) {
        super(s);
    }
    public AppException(String s, Exception e) {
        super(s, e);
    }
}
