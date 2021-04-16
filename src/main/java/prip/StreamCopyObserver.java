package prip;

/**
 * Handles the stream copy process.
 */
public interface StreamCopyObserver {
    /** Sends a notification about a cperfomed copy */
    public void copyDone(long l);
}
