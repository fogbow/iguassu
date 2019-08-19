package org.fogbowcloud.app.core.routines;

import org.apache.log4j.Logger;
import org.fogbowcloud.app.core.constants.ConfProperty;
import org.fogbowcloud.app.core.models.job.Job;
import org.fogbowcloud.app.jes.JobExecutionService;
import org.fogbowcloud.app.jes.arrebol.ArrebolJobExecutionService;
import org.fogbowcloud.app.jes.arrebol.JobSynchronizer;
import org.fogbowcloud.app.utils.ManagerTimer;

import java.util.Arrays;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.Executors;

/**
 * Default implementation of the RoutineManager
 */
public class DefaultRoutineManager implements RoutineManager {
    private static final long DEFAULT_INITIAL_DELAY_MS = 3000;
    private static final int DEFAULT_POOL_THREAD_NUMBER = 1;

    private static final Logger logger = Logger.getLogger(DefaultRoutineManager.class);

    private final JobExecutionService jobExecutionService;
    private final Properties properties;
    private final Queue<Job> jobsToSubmit;

    public DefaultRoutineManager(Properties properties, Queue<Job> jobsToSubmit) {
        this.properties = properties;
        this.jobExecutionService = new ArrebolJobExecutionService(this.properties);
        this.jobsToSubmit = jobsToSubmit;
    }

    @Override
    public void startAll() {
        try {
            startSyncJobRoutine();
            startSessionRoutine();
            startJobSubmissionRoutine();
        } catch (Throwable throwable) {
            logger.error(
                    "An error occurred while trying to init system monitors: "
                            + Arrays.toString(throwable.getStackTrace()));
        }
    }

    private void startSyncJobRoutine() {
        logger.info("----> Starting Sync Job State Routine...");
        final long SYNC_PERIOD =
                Long.parseLong(this.properties.getProperty(ConfProperty.JOB_STATE_MONITOR_PERIOD.getProp()));
        final String SYNC_JOB = "sync_job";
        final long SYNC_JOB_ROUTINE_ID = 1;

        final ManagerTimer syncJobTimer =
                new ManagerTimer(Executors.newScheduledThreadPool(DEFAULT_POOL_THREAD_NUMBER));
        SyncJobStateRoutine syncJobStateRoutine =
                new SyncJobStateRoutine(SYNC_JOB_ROUTINE_ID, SYNC_JOB, new JobSynchronizer(this.jobExecutionService));
        syncJobTimer.scheduleAtFixedRate(
                syncJobStateRoutine, DEFAULT_INITIAL_DELAY_MS, SYNC_PERIOD);
    }

    private void startSessionRoutine() {
        logger.info("----> Starting Session Verification Routine...");
        final long VERIFICATION_PERIOD =
                Long.parseLong(this.properties.getProperty(ConfProperty.SESSION_MONITOR_PERIOD.getProp()));
        final String CHECK_USER_SESSIONS = "check_user_sessions";
        final long CHECK_USER_SESSIONS_ROUTINE_ID = 2;

        final ManagerTimer verifySessionTimer =
                new ManagerTimer(Executors.newScheduledThreadPool(DEFAULT_POOL_THREAD_NUMBER));
        SessionVerificationRoutine sessionVerificationRoutine = new SessionVerificationRoutine(
                CHECK_USER_SESSIONS_ROUTINE_ID, CHECK_USER_SESSIONS);
        verifySessionTimer.scheduleAtFixedRate(
                sessionVerificationRoutine, DEFAULT_INITIAL_DELAY_MS, VERIFICATION_PERIOD);
    }

    private void startJobSubmissionRoutine() {
        logger.info("----> Starting Job Submission Routine...");
        final long SUBMISSION_PERIOD = Long.parseLong(
                this.properties.getProperty(ConfProperty.JOB_SUBMISSION_MONITOR_PERIOD.getProp()));
        final String JOB_SUBMISSION = "job_submission";
        final long JOB_SUBMISSION_ROUTINE_ID = 3;

        final ManagerTimer submissionJobTimer =
                new ManagerTimer(Executors.newScheduledThreadPool(DEFAULT_POOL_THREAD_NUMBER));
        JobSubmissionRoutine jobSubmissionRoutine = new JobSubmissionRoutine(JOB_SUBMISSION_ROUTINE_ID, JOB_SUBMISSION,
                this.jobExecutionService, this.jobsToSubmit);
        submissionJobTimer.scheduleAtFixedRate(
                jobSubmissionRoutine, DEFAULT_INITIAL_DELAY_MS, SUBMISSION_PERIOD);
    }
}
