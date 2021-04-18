package prip.model;

import prip.utils.Jsonable;

import java.util.Date;

public class Task implements Jsonable {
    private int id;
    private boolean pinned;
    private String name;
    private String descr;
    private String estimate;
    private Date lastActive;

    public Task() {

    }

    public Task(int id, boolean pinned, String name) {
        this.id = id;
        this.pinned = pinned;
        this.name = name;
    }

    public void update(Task t) {
        this.id = t.getId();
        this.name = t.getName();
        this.descr = t.getDescr();
        this.estimate = t.getEstimate();
        this.pinned = t.isPinned();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescr() {
        return descr;
    }

    public void setDescr(String descr) {
        this.descr = descr;
    }

    public String getEstimate() {
        return estimate;
    }

    public void setEstimate(String estimate) {
        this.estimate = estimate;
    }

    public Date getLastActive() {
        return lastActive;
    }

    public void setLastActive(Date lastActive) {
        this.lastActive = lastActive;
    }
}
