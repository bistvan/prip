package prip.model;

import prip.utils.StringUtils;

import java.util.ArrayList;

public class ReportTask {
    private String title;
    private String estimate;
    private ArrayList<String> activities;
    private String commits;
    private Integer spentTime;

    public ReportTask() {
    }

    public ReportTask(String title, String estimate) {
        this.title = title;
        this.estimate = estimate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getEstimate() {
        return estimate;
    }

    public void setEstimate(String estimate) {
        this.estimate = estimate;
    }

    public ArrayList<String> getActivities() {
        return activities;
    }

    public void setActivities(ArrayList<String> activities) {
        this.activities = activities;
    }

    public void addActivity(String act) {
        act = act.trim();
        if (activities == null)
            activities = new ArrayList<>();
        if (!activities.contains(act))
            activities.add(act);
    }

    public String getCommits() {
        return commits;
    }

    public void setCommits(String commits) {
        this.commits = commits;
    }

    public void addCommit(String commit) {
        this.commits = StringUtils.isEmpty(this.commits) ? commit : this.commits + ", " + commit;
    }

    public Integer getSpentTime() {
        return spentTime;
    }

    public void setSpentTime(Integer spentTime) {
        this.spentTime = spentTime;
    }

    public void addSpentTime(int secs) {
        if (spentTime == null)
            spentTime = secs;
        else
            spentTime += secs;
    }
}
