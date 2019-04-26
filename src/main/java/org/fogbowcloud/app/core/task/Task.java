package org.fogbowcloud.app.core.task;

import org.fogbowcloud.app.core.command.Command;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public interface Task {

    Specification getSpecification();

    Task clone();

    String getId();

    void finish();

    void fail();

    boolean isFinished();

    boolean isFailed();

    boolean checkTimeOuted();

    void addCommand(Command command);

    List<Command> getAllCommands();

    List<String> getAllCommandsInStr();

    void startedRunning();

    void putMetadata(String attributeName, String value);

    String getMetadata(String attributeName);

    Map<String, String> getAllMetadata();

    boolean mayRetry();

    int getRetries();

    void setRetries(int retries);

    int getNumberOfCommands();

    void addProcessId(String procId);

    List<String> getProcessId();

    JSONObject toJSON();

    TaskState getState();

    void setState(TaskState state);

    String getUUID();
}
