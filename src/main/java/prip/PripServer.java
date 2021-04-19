package prip;

import prip.utils.ActionHolder;
import prip.utils.JettyServer;

import java.io.File;
import java.util.logging.Level;

/** Yes bro, I agree with you! */
public class PripServer extends JettyServer {
    public static final File WORK_FOLDER = new File("./work");
    static {
        if (!WORK_FOLDER.exists())
            WORK_FOLDER.mkdirs();
    }


    public PripServer(int port, ActionHolder... actions) {
        super(port, actions);
    }

    public static void main(String[] args) {
        try {
            new PripServer(8181,
                new StaticContent(),
                new WorkspaceActions()
            ).start();
            log.info("Server Started --oOOo-(O|O)-oOOo--");
        }
        catch (Exception e) {
            log.log(Level.SEVERE, "Fail", e);
        }
    }

}
