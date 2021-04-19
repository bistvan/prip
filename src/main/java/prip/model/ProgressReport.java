package prip.model;

import prip.utils.Jsonable;

import java.util.ArrayList;

public class ProgressReport implements Jsonable {
    private ArrayList<ReportChunk> chunks = new ArrayList<>();

    public ArrayList<ReportChunk> getChunks() {
        return chunks;
    }

    public void setChunks(ArrayList<ReportChunk> chunks) {
        this.chunks = chunks;
    }
}
