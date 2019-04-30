package org.fogbowcloud.app.jes.arrebol;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.fogbowcloud.app.core.task.Task;
import org.fogbowcloud.app.core.task.TaskState;
import org.fogbowcloud.app.jdfcompiler.job.JDFJob;
import org.fogbowcloud.app.jdfcompiler.job.JDFJobState;
import org.fogbowcloud.app.jes.arrebol.models.*;
import org.fogbowcloud.app.jes.exceptions.GetJobException;
import java.util.*;

public class ArrebolJobSynchronizer implements JobSynchronizer {

	private static final Logger LOGGER = Logger.getLogger(ArrebolJobSynchronizer.class);

	private final ArrebolRequestsHelper requestsHelper;

	public ArrebolJobSynchronizer(Properties properties) {
		this.requestsHelper = new ArrebolRequestsHelper(properties);
	}

	@Override
	public JDFJob synchronizeJob(JDFJob job) {
		try {
			String arrebolJobId = job.getJobIdArrebol();
			if(arrebolJobId != null){
				String arrebolJobJson = this.requestsHelper.getJobJSON(arrebolJobId);
				LOGGER.debug("JSON Response [" + arrebolJobJson + "]");

				Gson gson = new Gson();
				ArrebolJob arrebolJob = gson.fromJson(arrebolJobJson, ArrebolJob.class);
				this.updateJob(job, arrebolJob);
			} else {
				LOGGER.info("ArrebolJobId from Job [" + job.getId() + "] is null.");
			}
		} catch (GetJobException e) {
			LOGGER.error(e.getMessage(), e);
		}
		return job;
	}

	private void updateJob(JDFJob job, ArrebolJob arrebolJob){
		updateTasksState(job.getTaskList(), arrebolJob.getTasks());
		LOGGER.info("Updated tasks state from job [" + job.getId() + "].");
		updateJobState(job, arrebolJob.getJobState());
	}

	private void updateTasksState(Map<String, Task> tasks, Map<String, ArrebolTask> arrebolTasks){

		for(ArrebolTask arrebolTask : arrebolTasks.values()){
			String taskId = arrebolTask.getTaskSpec().getId();
			Task task = tasks.get(taskId);

			ArrebolTaskState arrebolTaskState = arrebolTask.getState();
			TaskState taskState = getTaskState(arrebolTaskState);
			task.setState(taskState);
			LOGGER.debug("Updated task [" + task.getId() + "] to state " + taskState.toString());
		}
	}

	private void updateJobState(JDFJob job, ArrebolJobState arrebolJobState){
		JDFJobState jdfJobState = this.getJobState(arrebolJobState);
		job.setState(jdfJobState);
		LOGGER.info("Updated job [" + job.getId() + "] to state " + jdfJobState.toString());
	}

	private TaskState getTaskState(ArrebolTaskState arrebolTaskState){
		switch (arrebolTaskState){
			case FAILED:
				return TaskState.FAILED;
			case RUNNING:
				return TaskState.RUNNING;
			case FINISHED:
				return TaskState.FINISHED;
			case PENDING:
				return TaskState.READY;
			case CLOSED:
				return TaskState.COMPLETED;
			default:
				return null;
		}
	}

	private JDFJobState getJobState(ArrebolJobState arrebolJobState){
		switch(arrebolJobState){
			case SUBMITTED:
				return JDFJobState.SUBMITTED;
			case READY:
				return JDFJobState.CREATED;
			case RUNNING:
				return JDFJobState.SUBMITTED;
			case FINISHED:
				return JDFJobState.FINISHED;
			case FAILED:
				return JDFJobState.FAILED;
			default:
				return null;
		}
	}
}
