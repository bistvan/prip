package prip;

public class StaticContent {
    @HtAction(path = "/")
    public final Resource main = new Resource("/static/main.html");

    @HtAction(path = "/index.html")
    public final Resource index = main;

    @HtAction(path = "/main.js")
    public final Resource mainJs = new Resource("/static/main.js");

    @HtAction(path = "/main.css")
    public final Resource mainCss = new Resource("/static/main.css");
}
