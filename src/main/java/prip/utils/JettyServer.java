package prip.utils;

import org.eclipse.jetty.server.Server;
import prip.StaticContent;
import prip.WorkspaceActions;

import java.util.logging.Logger;

public class JettyServer {
    protected static Logger log = Logger.getLogger(JettyServer.class.getName());
    private final ActionHolder[] actions;

    private Server server;

    public JettyServer(int port, ActionHolder... actions) {
        this.server = new Server(port);
        this.actions = actions;
    }

    public void start() throws Exception {
        this.server = new Server(8181);
        this.server.setHandler(new ActionHandler(actions));
        this.server.start();
        this.server.join();
    }

}
