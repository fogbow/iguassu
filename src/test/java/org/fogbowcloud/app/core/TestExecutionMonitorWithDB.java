package org.fogbowcloud.app.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.fogbowcloud.app.CurrentThreadExecutorService;
import org.fogbowcloud.app.core.datastore.JobDataStore;
import org.fogbowcloud.app.core.task.Specification;
import org.fogbowcloud.app.core.task.Task;
import org.fogbowcloud.app.core.task.TaskImpl;
import org.fogbowcloud.app.core.task.TaskState;
import org.fogbowcloud.app.jdfcompiler.job.JDFJob;
import org.junit.Before;
import org.junit.Test;

import org.mockito.Mockito;

import static org.mockito.Mockito.*;

public class TestExecutionMonitorWithDB {

	private static final String FAKE_UUID = "1234";
	private Task task;
	private IguassuController iguassuController;
	private JDFJob job;
	private String FAKE_TASK_ID = "FAKE_TASK_ID";
	private CurrentThreadExecutorService executorService;
	private JobDataStore db;
	private HashMap<String, JDFJob> jobDB;

	@Before
	public void setUp() {
		task = spy(new TaskImpl(FAKE_TASK_ID, null, FAKE_UUID));
		db = mock(JobDataStore.class);
		jobDB = mock(HashMap.class);
		job = mock(JDFJob.class);
		executorService = new CurrentThreadExecutorService();
		iguassuController = mock(IguassuController.class);
	}

	@Test
	public void testExecutionMonitor() {
		List<JDFJob> jdfJobs = new ArrayList<>();
		doReturn("jobId").when(job).getId();
		doReturn(job).when(jobDB).put("jobId", job);
		
		ArrayList<Task> tasks = new ArrayList<>();
		tasks.add(task);
		doReturn(tasks).when(job).getTasks();
		doReturn(TaskState.COMPLETED).when(iguassuController).getTaskState(FAKE_TASK_ID);
		doNothing().when(iguassuController).moveTaskToFinished(task);
		jdfJobs.add(job);
		doReturn(jdfJobs).when(db).getAll();
		ExecutionMonitorWithDB monitor = new ExecutionMonitorWithDB(iguassuController, executorService, db);
		monitor.run();
		verify(iguassuController).moveTaskToFinished(task);
	}

	@Test
	public void testExecutionMonitorTaskFails() {
		List<JDFJob> jdfJobs = new ArrayList<>();
		doReturn("jobId").when(job).getId();
		doReturn(job).when(jobDB).put("jobId", job);
		
		ArrayList<Task> tasks = new ArrayList<>();
		tasks.add(task);
		doReturn(tasks).when(job).getTasks();
		doReturn(TaskState.FAILED).when(iguassuController).getTaskState(FAKE_TASK_ID);
		jdfJobs.add(job);
		doReturn(jdfJobs).when(db).getAll();
		ExecutionMonitorWithDB monitor = new ExecutionMonitorWithDB(iguassuController,executorService, db);
		monitor.run();
		verify(iguassuController, never()).moveTaskToFinished(task);

	}

	@Test
	public void testExecutionIsNotOver() {
		List<JDFJob> jdfJobs = new ArrayList<>();
		doReturn("jobId").when(job).getId();
		doReturn(job).when(jobDB).put("jobId", job);
		
		ArrayList<Task> tasks = new ArrayList<>();
		tasks.add(task);
		doReturn(tasks).when(job).getTasks();
		doReturn(TaskState.RUNNING).when(iguassuController).getTaskState(FAKE_TASK_ID);
		jdfJobs.add(job);
		doReturn(jdfJobs).when(db).getAll();
		ExecutionMonitorWithDB monitor = new ExecutionMonitorWithDB(iguassuController,executorService, db);
		monitor.run();
		verify(iguassuController, never()).moveTaskToFinished(task);

	}

	@Test
	public void testExecutionMonitorRunningWithUpdatedList() {
		doReturn(TaskState.READY).when(this.iguassuController).getTaskState(anyString());
		final String user = "testuser";
		final String username = "'this is a test user'";
		final String testImage = "testimage";
		Specification spec = new Specification(
				testImage,
				user
		);
		JobDataStore dataStore = Mockito.spy(new JobDataStore("jdbc:h2:/tmp/datastores/testfogbowresourcesdatastore"));

		ExecutorService service = Mockito.mock(ExecutorService.class);
		doReturn(null).when(service).submit(any(Runnable.class));

		ExecutionMonitorWithDB em = Mockito.spy(new ExecutionMonitorWithDB(iguassuController, service, dataStore));


		JDFJob job1 = new JDFJob(user, new ArrayList<Task>(), username);
		Task task1 = new TaskImpl("TaskNumber-" + 1 + "-" + UUID.randomUUID(), spec, "0000");
		job1.addTask(task1);

		JDFJob job2 = new JDFJob(user, new ArrayList<Task>(), username);
		Task task2 = new TaskImpl("TaskNumber-" + 2 + "-" + UUID.randomUUID(), spec, "0000");
		job2.addTask(task2);

		JDFJob job3 = new JDFJob(user, new ArrayList<Task>(), username);
		Task task3 = new TaskImpl("TaskNumber-" + 3 + "-" + UUID.randomUUID(), spec, "0000");
		job3.addTask(task3);

		JDFJob job4 = new JDFJob(user, new ArrayList<Task>(), username);
		Task task4 = new TaskImpl("TaskNumber-" + 4 + "-" + UUID.randomUUID(), spec, "0000");
		job4.addTask(task4);

		JDFJob job5 = new JDFJob(user, new ArrayList<Task>(), username);
		Task task5 = new TaskImpl("TaskNumber-" + 5 + "-" + UUID.randomUUID(), spec, "0000");
		job5.addTask(task5);

		JDFJob job6 = new JDFJob(user, new ArrayList<Task>(), username);
		Task task6 = new TaskImpl("TaskNumber-" + 6 + "-" + UUID.randomUUID(), spec, "0000");
		job6.addTask(task6);

		JDFJob job7 = new JDFJob(user, new ArrayList<Task>(), username);
		Task task7 = new TaskImpl("TaskNumber-" + 7 + "-" + UUID.randomUUID(), spec, "0000");
		job7.addTask(task7);

		JDFJob job8 = new JDFJob(user, new ArrayList<Task>(), username);
		Task task8 = new TaskImpl("TaskNumber-" + 8 + "-" + UUID.randomUUID(), spec, "0000");
		job8.addTask(task8);

		JDFJob job9 = new JDFJob(user, new ArrayList<Task>(), username);
		Task task9 = new TaskImpl("TaskNumber-" + 9 + "-" + UUID.randomUUID(), spec, "0000");
		job9.addTask(task9);

		JDFJob job10 = new JDFJob(user, new ArrayList<Task>(), username);
		Task task10 = new TaskImpl("TaskNumber-" + 10 + "-" + UUID.randomUUID(), spec, "0000");
		job10.addTask(task10);

		/* ////////////////////////////////////// */
		dataStore.insert(job1);
		dataStore.insert(job2);

		em.run();

		verify(this.iguassuController, times(1)).getTaskState(task1.getId());
		verify(this.iguassuController, times(1)).getTaskState(task2.getId());
		verify(this.iguassuController, never()).getTaskState(task3.getId());
		verify(this.iguassuController, never()).getTaskState(task4.getId());
		verify(this.iguassuController, never()).getTaskState(task5.getId());
		verify(this.iguassuController, never()).getTaskState(task6.getId());
		verify(this.iguassuController, never()).getTaskState(task7.getId());
		verify(this.iguassuController, never()).getTaskState(task8.getId());
		verify(this.iguassuController, never()).getTaskState(task9.getId());
		verify(this.iguassuController, never()).getTaskState(task10.getId());

		/* ////////////////////////////////////// */
		dataStore.insert(job3);
		dataStore.insert(job4);

		em.run();

		verify(this.iguassuController, times(2)).getTaskState(task1.getId());
		verify(this.iguassuController, times(2)).getTaskState(task2.getId());
		verify(this.iguassuController, times(1)).getTaskState(task3.getId());
		verify(this.iguassuController, times(1)).getTaskState(task4.getId());
		verify(this.iguassuController, never()).getTaskState(task5.getId());
		verify(this.iguassuController, never()).getTaskState(task6.getId());
		verify(this.iguassuController, never()).getTaskState(task7.getId());
		verify(this.iguassuController, never()).getTaskState(task8.getId());
		verify(this.iguassuController, never()).getTaskState(task9.getId());
		verify(this.iguassuController, never()).getTaskState(task10.getId());

		/* ////////////////////////////////////// */
		dataStore.insert(job5);
		dataStore.insert(job6);
		dataStore.deleteByJobId(job1.getId(), user);

		em.run();

		verify(this.iguassuController, times(2)).getTaskState(task1.getId());
		verify(this.iguassuController, times(3)).getTaskState(task2.getId());
		verify(this.iguassuController, times(2)).getTaskState(task3.getId());
		verify(this.iguassuController, times(2)).getTaskState(task4.getId());
		verify(this.iguassuController, times(1)).getTaskState(task5.getId());
		verify(this.iguassuController, times(1)).getTaskState(task6.getId());
		verify(this.iguassuController, never()).getTaskState(task7.getId());
		verify(this.iguassuController, never()).getTaskState(task8.getId());
		verify(this.iguassuController, never()).getTaskState(task9.getId());
		verify(this.iguassuController, never()).getTaskState(task10.getId());

		/* ////////////////////////////////////// */
		dataStore.insert(job7);
		dataStore.insert(job8);
		dataStore.insert(job9);
		dataStore.insert(job10);

		em.run();

		verify(this.iguassuController, times(2)).getTaskState(task1.getId());
		verify(this.iguassuController, times(4)).getTaskState(task2.getId());
		verify(this.iguassuController, times(3)).getTaskState(task3.getId());
		verify(this.iguassuController, times(3)).getTaskState(task4.getId());
		verify(this.iguassuController, times(2)).getTaskState(task5.getId());
		verify(this.iguassuController, times(2)).getTaskState(task6.getId());
		verify(this.iguassuController, times(1)).getTaskState(task7.getId());
		verify(this.iguassuController, times(1)).getTaskState(task8.getId());
		verify(this.iguassuController, times(1)).getTaskState(task9.getId());
		verify(this.iguassuController, times(1)).getTaskState(task10.getId());

		/* ////////////////////////////////////// */
		dataStore.deleteByJobId(job2.getId(), user);
		dataStore.deleteByJobId(job6.getId(), user);
		dataStore.deleteByJobId(job9.getId(), user);

		em.run();

		verify(this.iguassuController, times(2)).getTaskState(task1.getId());
		verify(this.iguassuController, times(4)).getTaskState(task2.getId());
		verify(this.iguassuController, times(4)).getTaskState(task3.getId());
		verify(this.iguassuController, times(4)).getTaskState(task4.getId());
		verify(this.iguassuController, times(3)).getTaskState(task5.getId());
		verify(this.iguassuController, times(2)).getTaskState(task6.getId());
		verify(this.iguassuController, times(2)).getTaskState(task7.getId());
		verify(this.iguassuController, times(2)).getTaskState(task8.getId());
		verify(this.iguassuController, times(1)).getTaskState(task9.getId());
		verify(this.iguassuController, times(2)).getTaskState(task10.getId());

		/* ////////////////////////////////////// */
		dataStore.deleteByJobId(job3.getId(), user);
		dataStore.deleteByJobId(job4.getId(), user);
		dataStore.deleteByJobId(job5.getId(), user);

		em.run();

		verify(this.iguassuController, times(2)).getTaskState(task1.getId());
		verify(this.iguassuController, times(4)).getTaskState(task2.getId());
		verify(this.iguassuController, times(4)).getTaskState(task3.getId());
		verify(this.iguassuController, times(4)).getTaskState(task4.getId());
		verify(this.iguassuController, times(3)).getTaskState(task5.getId());
		verify(this.iguassuController, times(2)).getTaskState(task6.getId());
		verify(this.iguassuController, times(3)).getTaskState(task7.getId());
		verify(this.iguassuController, times(3)).getTaskState(task8.getId());
		verify(this.iguassuController, times(1)).getTaskState(task9.getId());
		verify(this.iguassuController, times(3)).getTaskState(task10.getId());

		/* ////////////////////////////////////// */
		dataStore.deleteByJobId(job7.getId(), user);
		dataStore.deleteByJobId(job8.getId(), user);
		dataStore.deleteByJobId(job10.getId(), user);

		em.run();

		verify(this.iguassuController, times(2)).getTaskState(task1.getId());
		verify(this.iguassuController, times(4)).getTaskState(task2.getId());
		verify(this.iguassuController, times(4)).getTaskState(task3.getId());
		verify(this.iguassuController, times(4)).getTaskState(task4.getId());
		verify(this.iguassuController, times(3)).getTaskState(task5.getId());
		verify(this.iguassuController, times(2)).getTaskState(task6.getId());
		verify(this.iguassuController, times(3)).getTaskState(task7.getId());
		verify(this.iguassuController, times(3)).getTaskState(task8.getId());
		verify(this.iguassuController, times(1)).getTaskState(task9.getId());
		verify(this.iguassuController, times(3)).getTaskState(task10.getId());

		/* ////////////////////////////////////// */
		dataStore.deleteByJobId(job7.getId(), user);
		dataStore.deleteByJobId(job8.getId(), user);
		dataStore.deleteByJobId(job10.getId(), user);

		em.run();

		verify(this.iguassuController, times(2)).getTaskState(task1.getId());
		verify(this.iguassuController, times(4)).getTaskState(task2.getId());
		verify(this.iguassuController, times(4)).getTaskState(task3.getId());
		verify(this.iguassuController, times(4)).getTaskState(task4.getId());
		verify(this.iguassuController, times(3)).getTaskState(task5.getId());
		verify(this.iguassuController, times(2)).getTaskState(task6.getId());
		verify(this.iguassuController, times(3)).getTaskState(task7.getId());
		verify(this.iguassuController, times(3)).getTaskState(task8.getId());
		verify(this.iguassuController, times(1)).getTaskState(task9.getId());
		verify(this.iguassuController, times(3)).getTaskState(task10.getId());

		/* ////////////////////////////////////// */
		em.run();

		verify(this.iguassuController, times(2)).getTaskState(task1.getId());
		verify(this.iguassuController, times(4)).getTaskState(task2.getId());
		verify(this.iguassuController, times(4)).getTaskState(task3.getId());
		verify(this.iguassuController, times(4)).getTaskState(task4.getId());
		verify(this.iguassuController, times(3)).getTaskState(task5.getId());
		verify(this.iguassuController, times(2)).getTaskState(task6.getId());
		verify(this.iguassuController, times(3)).getTaskState(task7.getId());
		verify(this.iguassuController, times(3)).getTaskState(task8.getId());
		verify(this.iguassuController, times(1)).getTaskState(task9.getId());
		verify(this.iguassuController, times(3)).getTaskState(task10.getId());

		/* ////////////////////////////////////// */
		dataStore.insert(job1);

		em.run();

		verify(this.iguassuController, times(3)).getTaskState(task1.getId());
		verify(this.iguassuController, times(4)).getTaskState(task2.getId());
		verify(this.iguassuController, times(4)).getTaskState(task3.getId());
		verify(this.iguassuController, times(4)).getTaskState(task4.getId());
		verify(this.iguassuController, times(3)).getTaskState(task5.getId());
		verify(this.iguassuController, times(2)).getTaskState(task6.getId());
		verify(this.iguassuController, times(3)).getTaskState(task7.getId());
		verify(this.iguassuController, times(3)).getTaskState(task8.getId());
		verify(this.iguassuController, times(1)).getTaskState(task9.getId());
		verify(this.iguassuController, times(3)).getTaskState(task10.getId());

		/* ////////////////////////////////////// */
		dataStore.deleteByJobId(job1.getId(), user);

		em.run();

		verify(this.iguassuController, times(3)).getTaskState(task1.getId());
		verify(this.iguassuController, times(4)).getTaskState(task2.getId());
		verify(this.iguassuController, times(4)).getTaskState(task3.getId());
		verify(this.iguassuController, times(4)).getTaskState(task4.getId());
		verify(this.iguassuController, times(3)).getTaskState(task5.getId());
		verify(this.iguassuController, times(2)).getTaskState(task6.getId());
		verify(this.iguassuController, times(3)).getTaskState(task7.getId());
		verify(this.iguassuController, times(3)).getTaskState(task8.getId());
		verify(this.iguassuController, times(1)).getTaskState(task9.getId());
		verify(this.iguassuController, times(3)).getTaskState(task10.getId());
	}
}
