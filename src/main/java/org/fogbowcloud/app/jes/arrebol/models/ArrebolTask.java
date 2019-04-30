package org.fogbowcloud.app.jes.arrebol.models;


public class ArrebolTask {
    private String id;

    private ArrebolTaskState state;

    private ArrebolTaskSpec taskSpec;

    public ArrebolTask(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public ArrebolTaskState getState() {
        return state;
    }

    public ArrebolTaskSpec getTaskSpec(){
        return this.taskSpec;
    }
}
