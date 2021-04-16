package prip;

import org.eclipse.jetty.server.Server;

import java.util.logging.Level;
import java.util.logging.Logger;

public class PripServer {
    private static Logger log = Logger.getLogger(PripServer.class.getName());

    private Server server;

    private void run() throws Exception {
        this.server = new Server(8181);
        this.server.setHandler(new PripHandler(
            new StaticContent(),
            new WorkspaceActions()
        ));
        this.server.start();
        this.server.join();
    }

    public static void main(String[] args) {
        try {
            new PripServer().run();
        }
        catch (Exception e) {
            log.log(Level.SEVERE, "Fail", e);
        }
    }

}
