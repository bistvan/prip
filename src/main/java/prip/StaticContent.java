package prip;

import prip.utils.ActionHolder;
import prip.utils.HtAction;
import prip.utils.Resource;

public class StaticContent implements ActionHolder {
    @HtAction(path = "/main.js")
    public final Resource mainJs = new Resource("/static/main.js");

    @HtAction(path = "/main.css")
    public final Resource mainCss = new Resource("/static/main.css");
}
