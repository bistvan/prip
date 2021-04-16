package prip;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Resource {
    private static Logger log = Logger.getLogger(PripServer.class.getName());

    private final String r;
    private File file;
    private String val;

    public Resource(String r) {
        this.r = r;
        try {
            File f = new File("./src/main/resources" + r).getCanonicalFile();
            if (f.isFile())
                this.file = f;
        } catch (IOException e) {
            log.log(Level.WARNING, "bad", e);
        }
    }

    public String asString() {
        if (this.file != null)
            return FileUtils.readFile(this.file);

        if (this.val == null)
            try {
                this.val = ResourceUtils.resourceToString(r, "utf-8");
            } catch (Exception e) {
                this.val = e.getMessage();
            }

        return this.val;
    }
}
