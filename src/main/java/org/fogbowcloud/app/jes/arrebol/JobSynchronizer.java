package org.fogbowcloud.app.jes.arrebol;

import org.apache.log4j.Logger;
import org.fogbowcloud.app.core.models.command.Command;
import org.fogbowcloud.app.core.models.command.CommandState;
import org.fogbowcloud.app.core.models.job.Job;
import org.fogbowcloud.app.core.models.job.JobState;
import org.fogbowcloud.app.core.models.task.Task;
import org.fogbowcloud.app.core.models.task.TaskState;
import org.fogbowcloud.app.jes.JobExecutionService;
import org.fogbowcloud.app.jes.arrebol.models.*;
import org.fogbowcloud.app.jes.exceptions.ArrebolConnectException;
import org.fogbowcloud.app.jes.exceptions.JobExecStatusException;

import java.util.List;
import java.util.Objects;
import org.fogbowcloud.app.utils.Pair;

/**
 * Sync local job state with it execution.
 */
public class JobSynchronizer implements Synchronizer<Pair<String, Job>> {

    private static final Logger logger = Logger.getLogger(JobSynchronizer.class);

    private final JobExecutionService jobExecutionService;

    public JobSynchronizer(JobExecutionService jobExecutionService) {
        this.jobExecutionService = jobExecutionService;
    }

    @Override
    public Pair<String, Job> sync(Pair<String, Job> pair) {

        final String queueId = pair.getKey();
        final Job job = pair.getValue();
        final String executionId = job.getExecutionId();
        if (Objects.nonNull(executionId) && !executionId.trim().isEmpty()) {
            JobExecArrebol jobExecArrebol = null;
            try {
                jobExecArrebol = this.jobExecutionService.status(queueId, executionId);
            } catch (ArrebolConnectException ace) {
                logger.error("Error to get status for execution [" + executionId + "] with message + " + ace.getMessage());
            } catch (RuntimeException e) {
                throw new JobExecStatusException(e.getMessage());
            }
            if (Objects.nonNull(jobExecArrebol)) {
                this.updateJob(job, jobExecArrebol);
            } else {
                logger.error(
                        "Could not get job execution status with job id [" + job.getId() + "] and execution id " +
                                "[" + job.getExecutionId() + "]");
            }
        } else {
            logger.error("Execution identifier from Job [" + job.getId() + "] is null.");
        }
        return new Pair<>(queueId, job);
    }

    private void updateJob(Job job, JobExecArrebol jobExecArrebol) {
        updateTasks(job, jobExecArrebol.getTasks());
        logger.info("Updated tasks state from job [" + job.getId() + "].");
        updateJobState(job, jobExecArrebol.getState());
    }

    private void updateTasks(Job job, List<ArrebolTask> arrebolTasks) {
        for (ArrebolTask arrebolTask : arrebolTasks) {
            final Long iguassuTaskId = arrebolTask.getTaskSpec().getId();
            final Task iguassuTask = job.getTaskById(iguassuTaskId);

            if (iguassuTask != null) {
                updateTaskCommands(arrebolTask, iguassuTask);

                final ArrebolTaskState arrebolTaskState = arrebolTask.getState();
                final TaskState taskState = getTaskState(arrebolTaskState);
                if (Objects.nonNull(taskState)) {
                    iguassuTask.setState(taskState);
                    logger.info("Updated task [" + iguassuTask.getId() + "] to state " + taskState.toString());
                }
            }
        }
    }

    private void updateTaskCommands(ArrebolTask arrebolTask, Task iguassuTask) {
        List<ArrebolCommand> arrebolCommands = arrebolTask.getTaskSpec().getCommands();
        for (int i = 0; i < arrebolCommands.size(); i++) {
            ArrebolCommand arrebolCmd = arrebolCommands.get(i);
            Command command = iguassuTask.getCommands().get(i);
            CommandState commandState = getCommandState(arrebolCmd.getState());
            command.setState(commandState);
            command.setExitCode(arrebolCmd.getExitcode());
        }
    }

    private void updateJobState(Job job, ExecutionState executionState) {
        JobState jobState = this.getJobState(executionState);

        if (jobState != null) {
            job.setState(jobState);
            logger.info("Resuming job [" + job.getId() + "] to state " + jobState.toString());
        }
    }

    private CommandState getCommandState(ArrebolCommandState arrebolCommandState) {
        switch (arrebolCommandState) {
            case RUNNING:
            case UNSTARTED:
                return CommandState.RUNNING;
            case FINISHED:
                return CommandState.FINISHED;
            case FAILED:
                return CommandState.FAILED;
            default:
                return null;
        }
    }

    private TaskState getTaskState(ArrebolTaskState arrebolTaskState) {
        switch (arrebolTaskState) {
            case RUNNING:
                return TaskState.RUNNING;
            case FINISHED:
                return TaskState.FINISHED;
            case PENDING:
                return TaskState.PENDING;
            case FAILED:
                return TaskState.FAILED;
            default:
                return null;
        }
    }

    private JobState getJobState(ExecutionState executionState) {
        switch (executionState) {
            case SUBMITTED:
            case QUEUED:
                return JobState.QUEUED;
            case RUNNING:
                return JobState.RUNNING;
            case FAILED:
                return JobState.FAILED;
            case FINISHED:
                return JobState.FINISHED;
            default:
                return null;
        }
    }
}
