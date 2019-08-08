package org.fogbowcloud.app.core;

import org.apache.log4j.Logger;
import org.fogbowcloud.app.core.auth.AuthManager;
import org.fogbowcloud.app.core.auth.DefaultAuthManager;
import org.fogbowcloud.app.core.datastore.managers.JobDBManager;
import org.fogbowcloud.app.core.datastore.managers.UserDBManager;
import org.fogbowcloud.app.core.exceptions.UserNotExistException;
import org.fogbowcloud.app.core.models.job.Job;
import org.fogbowcloud.app.core.models.job.JobSpecification;
import org.fogbowcloud.app.core.models.user.Credential;
import org.fogbowcloud.app.core.models.user.OAuth2Identifiers;
import org.fogbowcloud.app.core.models.user.OAuthToken;
import org.fogbowcloud.app.core.models.user.User;
import org.fogbowcloud.app.core.routines.DefaultRoutineManager;
import org.fogbowcloud.app.core.routines.RoutineManager;
import org.fogbowcloud.app.jdfcompiler.JobBuilder;
import org.fogbowcloud.app.jdfcompiler.main.CommonCompiler;
import org.fogbowcloud.app.jdfcompiler.main.CompilerException;
import org.fogbowcloud.app.utils.JDFUtil;

import java.security.GeneralSecurityException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ApplicationFacade {
    private static final Logger logger = Logger.getLogger(ApplicationFacade.class);

    private static ApplicationFacade instance;
    private final List<Integer> nonceList;
    private final Queue<Job> jobsToSubmit;
    private AuthManager authManager;
    private JobBuilder jobBuilder;
    private JobDBManager jobDBManager;
    private UserDBManager userDBManager;

    private ApplicationFacade() {
        this.jobsToSubmit = new ConcurrentLinkedQueue<>();
        this.nonceList = new ArrayList<>();
        this.jobDBManager = JobDBManager.getInstance();
        this.userDBManager = UserDBManager.getInstance();
    }

    public static ApplicationFacade getInstance() {
        synchronized (ApplicationFacade.class) {
            if (instance == null) {
                instance = new ApplicationFacade();
            }
            return instance;
        }
    }

    public void init(Properties properties) {
        this.authManager = new DefaultAuthManager(properties);
        this.jobBuilder = new JobBuilder(properties);
        final RoutineManager routineManager = new DefaultRoutineManager(properties, this.jobsToSubmit);
        routineManager.startAll();
    }


//    public Job getJobById(String jobId, String userId) {
//        return this.iguassuController.getJobById(jobId, userId);
//    }

    public long submitJob(String jdfFilePath, User user) throws CompilerException {
        logger.debug("Adding job of user " + user.getAlias() + " to buffer.");

        final Job job = buildJob(jdfFilePath, user);
        this.jobsToSubmit.offer(job);
//        this.jobDataStore.insert(job);

        return job.getId();
    }

//    public ArrayList<Job> getAllJobs(String userId) {
//        return this.iguassuController.getAllJobs(userId);
//    }

    private Job buildJob(String jdfFilePath, User user) throws CompilerException {

        final String userIdentification = user.getAlias();
        Job job = new Job(new HashMap<>(), userIdentification, "");

        logger.debug("Building job " + job.getId() + " of user " + user.getAlias() + " of jdf " + jdfFilePath);
        JobSpecification jobSpec = compile(jdfFilePath);
        JDFUtil.removeEmptySpaceFromVariables(jobSpec);
        OAuthToken oAuthToken = null;
        try {
//            oAuthToken = getCurrentTokenByUserId(userIdentification);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return buildJobFromJDFFile(
                job,
                jdfFilePath,
                jobSpec,
                userIdentification,
                Objects.requireNonNull(oAuthToken).getAccessToken(),
                oAuthToken.getVersion());
    }

//    ArrayList<Job> getAllJobs(String userId) {
//        return (ArrayList<Job>) this.jobDataStore.getAllByUserId(userId);
//    }

//    void updateJob(Job job) {
//        this.jobDataStore.update(job);
//    }

//    String stopJob(String jobId, String userId) {
//        final int updateCount = this.jobDataStore.deleteByJobId(jobId, userId);
//
//        if (updateCount >= 1) {
//            return jobId;
//        } else {
//            logger.error("Job with id [" + jobId + "] not found in the Datastore.");
//            return null;
//        }
//    }

//    Task getTaskById(String taskId, String userId) {
////        for (Job job : getAllJobs(userId)) {
////            Task task = job.getTaskById(taskId);
//            if (task != null) {
//                return task;
//            }
//        }
//        return null;
//    }

    public User authorizeUser(String credentials) throws GeneralSecurityException, UserNotExistException {
        if (Objects.isNull(credentials)) {
            logger.error("Invalid credentials. The fields are null.");
            return null;
        }

        Credential credential = null;
        // retrieve the user credentials

        User authenticatedUser = null;
        logger.debug("Checking nonce.");
        if (this.nonceList.contains(credential.getNonce())) {
            this.nonceList.remove(credential.getNonce());
            authenticatedUser = this.authManager.authorize(credential);
        }
        return authenticatedUser;
    }

    private JobSpecification compile(String jdfFilePath) throws CompilerException {
        CommonCompiler commonCompiler = new CommonCompiler();
        logger.debug(
                "Job " + jdfFilePath + " compilation started at time: " + System.currentTimeMillis());
        commonCompiler.compile(jdfFilePath, CommonCompiler.FileType.JDF);
        logger.debug("Job " + jdfFilePath + " compilation ended at time: " + System.currentTimeMillis());
        return (JobSpecification) commonCompiler.getResult().get(0);
    }

    private Job buildJobFromJDFFile(Job job, String jdfFilePath, JobSpecification jobSpec, String userId,
                                    String externalOAuthToken, Long tokenVersion) {
        try {
            this.jobBuilder.createJobFromJDFFile(
                    job, jdfFilePath, jobSpec, userId, externalOAuthToken, tokenVersion);

            logger.info(
                    "Job ["
                            + job.getId()
                            + "] was built with success at time: "
                            + System.currentTimeMillis());

//            job.finishCreation();

        } catch (Exception e) {

            logger.error(
                    "Failed to build ["
                            + job.getId()
                            + "] : at time: "
                            + System.currentTimeMillis(),
                    e);
//            job.failCreation();
        }

        return job;
    }

    public User authenticateUser(OAuth2Identifiers oAuth2Identifiers, String authorizationCode)
            throws GeneralSecurityException {
        try {
            return this.authManager.authenticate(oAuth2Identifiers, authorizationCode, this.getNonce());
        } catch (Exception gse) {
            throw new GeneralSecurityException(gse.getMessage());
        }
    }

    private int getNonce() {
        final int nonce = UUID.randomUUID().hashCode();
        this.nonceList.add(nonce);
        return nonce;
    }

    public OAuthToken refreshToken(OAuthToken oAuthToken) throws GeneralSecurityException {
        try {
            return this.authManager.refreshOAuth2Token(oAuthToken);
        } catch (Exception e) {
            throw new GeneralSecurityException(e.getMessage());
        }
    }

    public OAuthToken findUserOAuthTokenByAlias(String userAlias) {
        return null;
    }

    public Collection<Job> findJobsByUserAlias(String alias) {
        Collection<Job> jobs = new ArrayList<>();
        this.jobDBManager.findAll().forEach(job -> {
            if (job.getUserAlias().equalsIgnoreCase(alias)) jobs.add(job);
        });
        return jobs;
    }
}