package prip.utils;

import prip.utils.Jsonable;

public class JsonResult implements Jsonable {
    private boolean ok;
    private String error;

    public JsonResult() {
        ok = true;
    }

    public JsonResult(String error) {
        this.error = error;
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
