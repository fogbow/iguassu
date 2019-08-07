package org.fogbowcloud.app.jdfcompiler;

import org.apache.log4j.Logger;
import org.fogbowcloud.app.core.constants.ConfProperty;
import org.fogbowcloud.app.core.constants.DockerConstants;
import org.fogbowcloud.app.core.constants.JsonKey;
import org.fogbowcloud.app.core.models.command.Command;
import org.fogbowcloud.app.core.models.job.Job;
import org.fogbowcloud.app.core.models.job.JobSpecification;
import org.fogbowcloud.app.core.models.job.TaskSpecification;
import org.fogbowcloud.app.core.models.task.Specification;
import org.fogbowcloud.app.core.models.task.Task;
import org.fogbowcloud.app.jdfcompiler.semantic.IOCommand;
import org.fogbowcloud.app.jdfcompiler.semantic.JDLCommand;
import org.fogbowcloud.app.jdfcompiler.semantic.JDLCommand.JDLCommandType;
import org.fogbowcloud.app.jdfcompiler.semantic.RemoteCommand;
import org.springframework.http.HttpStatus;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class JobBuilder {

    private static final String USER_KEY = "user";
    private static final String REQUIREMENTS_IMAGE_KEY = "image";
    private static final String REQUIREMENTS_SEPARATOR = "and";
    private static final String REQUIREMENTS_CONCAT_STR = " && ";

    private static final Logger logger = Logger.getLogger(JobBuilder.class);

    private final Properties properties;

    public JobBuilder(Properties properties) {
        this.properties = properties;
    }

    /**
     * @param job         Job being created
     * @param jdfFilePath Path to the jdf file that describes the job
     * @throws IllegalArgumentException If path to jdf is empty
     * @throws IOException              If a problem during the reading of the file occurs
     */
    public void createJobFromJDFFile(Job job, String jdfFilePath, JobSpecification jobSpec,
                                     String userId, String externalOAuthToken, Long tokenVersion)
            throws IOException, InterruptedException {
        if (Objects.isNull(jdfFilePath) || jdfFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("JDF file path cannot be null.");
        }

        final File file = new File(jdfFilePath);

        if (file.exists()) {
            if (file.canRead()) {
                if (!jobSpec.getLabel().trim().isEmpty()) {
                    job.setLabel(jobSpec.getLabel());
                }

                String jobRequirements = jobSpec.getRequirements();
                jobRequirements = jobRequirements.replace("(", "").replace(")", "");

                String image = getImageFromJobRequirements(jobRequirements);
                Specification spec = new Specification(image, userId);

                addAllRequirements(jobRequirements, spec);

                Map<String, Task> tasks = new HashMap<>();
                for (TaskSpecification taskSpec : jobSpec.getTaskSpecs()) {
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                    Task task = new Task(spec);
                    task.getMetadata().put(JsonKey.JOB_ID.getKey(), String.valueOf(job.getId()));
                    task.getMetadata().put(USER_KEY, job.getUserId());

                    parseInitCommands(
                            taskSpec, task, userId, externalOAuthToken, tokenVersion);
                    parseTaskCommands(
                            taskSpec, task, userId, externalOAuthToken, tokenVersion);
                    parseFinalCommands(
                            taskSpec, task, userId, externalOAuthToken, tokenVersion);

                    tasks.put(task.getId(), task);
                    logger.debug("Task spec:\n" + task.getSpecification().toString());
                }
                job.setTasks(tasks);
            } else {
                throw new IllegalArgumentException(
                        "Unable to read file: "
                                + file.getAbsolutePath()
                                + " check your permissions.");
            }
        } else {
            throw new IOException("File: " + file.getAbsolutePath() + " does not exists.");
        }
    }

    private void addAllRequirements(String jobRequirements, Specification spec) {
        for (String req : jobRequirements.split(REQUIREMENTS_SEPARATOR)) {
            if (req != null && !req.isEmpty()) {
                if (req.startsWith(DockerConstants.PREFIX_DOCKER_REQUIREMENTS)) {
                    String requirements =
                            spec.getRequirementValue(DockerConstants.METADATA_DOCKER_REQUIREMENTS);
                    if (requirements == null) {
                        spec.addRequirement(DockerConstants.METADATA_DOCKER_REQUIREMENTS, req);
                    } else {
                        spec.addRequirement(
                                DockerConstants.METADATA_DOCKER_REQUIREMENTS,
                                requirements + REQUIREMENTS_CONCAT_STR + req);
                    }
                }
            }
        }
    }

    private String getImageFromJobRequirements(String requirements) {
        String image = null;

        for (String req : requirements.split(REQUIREMENTS_SEPARATOR)) {
            if (req.trim().startsWith(REQUIREMENTS_IMAGE_KEY)) {
                image = req.split("==")[1].trim();
            }
        }

        return image;
    }

    /**
     * This method translates the JDF remote executable command into the JDL format
     *
     * @param taskSpec The task specification {@link TaskSpecification}
     * @param task     The output expression containing the JDL job
     */
    private void parseTaskCommands(TaskSpecification taskSpec, Task task,
                                   String userId, String externalOAuthToken, Long tokenVersion) {
        List<JDLCommand> initBlocks = taskSpec.getTaskBlocks();
        addCommands(initBlocks, task, userId, externalOAuthToken, tokenVersion);
    }

    /**
     * It translates the input IOBlocks to JDL Input
     *
     * @param taskSpec The task specification {@link TaskSpecification}
     * @param task     The output expression containing the JDL job
     */
    private void parseInitCommands(TaskSpecification taskSpec, Task task,
                                   String userId,
                                   String externalOAuthToken, Long tokenVersion) {
        List<JDLCommand> initBlocks = taskSpec.getInitBlocks();
        addCommands(initBlocks, task, userId, externalOAuthToken, tokenVersion);
    }

    private void addCommands(List<JDLCommand> initBlocks, Task task, String userId,
                             String externalOAuthToken, Long tokenVersion) {
        if (initBlocks == null) {
            return;
        }
        for (JDLCommand jdlCommand : initBlocks) {
            if (jdlCommand.getBlockType().equals(JDLCommandType.IO)) {
                addIOCommands(task, (IOCommand) jdlCommand, userId, externalOAuthToken,
                        tokenVersion);
            } else {
                addRemoteCommand(task, (RemoteCommand) jdlCommand);
            }
        }
    }

    private void addIOCommands(Task task, IOCommand command, String userId,
                               String externalOAuthToken, Long tokenVersion) {
        String sourceFile = command.getEntry().getSourceFile();
        String destination = command.getEntry().getDestination();
        String IOType = command.getEntry().getCommand().toUpperCase();
        String rawCommand = IOType + " " + sourceFile + " " + destination;
        switch (IOType) {
            case "PUT":
            case "STORE":
                Command uploadFileCommand = uploadFileCommands(sourceFile, destination, userId,
                        externalOAuthToken, tokenVersion, rawCommand);
                logger.debug(rawCommand);

                task.addCommand(uploadFileCommand);
                break;
            case "GET":
                Command downloadFileCommand = downloadFileCommands(sourceFile, destination, userId,
                        externalOAuthToken, tokenVersion, rawCommand);
                task.addCommand(downloadFileCommand);
                break;
        }
    }

    private void addRemoteCommand(Task task, RemoteCommand remCommand) {
        String commandStr = remCommand.getContent();
        Command command = new Command(commandStr);
        task.addCommand(command);
    }

    /**
     * This method translates the Ourgrid output IOBlocks to JDL Input
     *
     * @param taskSpec The task specification {@link TaskSpecification}
     * @param task     The output expression containing the JDL job
     */
    private void parseFinalCommands(TaskSpecification taskSpec, Task task,
                                    String userId, String externalOAuthToken, Long tokenVersion) {
        List<JDLCommand> initBlocks = taskSpec.getFinalBlocks();
        addCommands(initBlocks, task, userId, externalOAuthToken, tokenVersion);
    }

    private Command uploadFileCommands(String localFilePath, String filePathToUpload, String userId,
                                       String token, Long tokenVersion, String rawCommand) {
        final String fileDriverHostIp =
                this.properties.getProperty(ConfProperty.STORAGE_SERVICE_HOST_URL.getProp());
        final String requestRefreshedTokenCommand =
                this.getRefreshTokenCommand(userId, tokenVersion);
        final String uploadCommand =
                " http_code=$(curl --write-out %{http_code} -X PUT --header \"Authorization:Bearer \"$token "
                        + " --data-binary @"
                        + localFilePath
                        + " --silent --output /dev/null "
                        + "$server/remote.php/webdav/"
                        + filePathToUpload
                        + "); ";
        final int sleepTime = 5;
        final String sleepCommand = "sleep " + sleepTime + ";";

        final String commandIO = "server=" + fileDriverHostIp + "; " + "token=" + token + "; "
                + uploadCommand + " while [ $http_code == " + HttpStatus.UNAUTHORIZED.toString()
                + " ] ; do " + "userId=" + userId + "; " + "tokenVersion=" + tokenVersion.toString()
                + "; " + "token=$(" + requestRefreshedTokenCommand + "); " + uploadCommand
                + sleepCommand + " done";

        return new Command(commandIO, rawCommand);
    }

    private Command downloadFileCommands(String localFilePath, String filePathToDownload,
                                         String userId, String token, Long tokenVersion, String rawCommand) {
        final String fileDriverHostIp =
                this.properties.getProperty(ConfProperty.STORAGE_SERVICE_HOST_URL.getProp());
        final String requestRefreshedTokenCommand =
                this.getRefreshTokenCommand(userId, tokenVersion);
        final String downloadCommand =
                " full_response=$(curl --write-out %{http_code} --header \"Authorization:Bearer \"$token"
                        + " $server/remote.php/webdav/"
                        + filePathToDownload
                        + " --silent --output "
                        + localFilePath
                        + " /dev/null); ";
        final String extractHttpStatusCode = "http_code=${full_response:0:3}; ";
        final int sleepTime = 5;
        final String sleepCommand = "sleep " + sleepTime + ";";

        final String commandIO = "server=" + fileDriverHostIp + "; " + "token=" + token + "; "
                + downloadCommand + extractHttpStatusCode + " while [ $http_code == "
                + HttpStatus.UNAUTHORIZED.toString() + " ] ; do "
                + "userId=" + userId + "; " + "tokenVersion=" + tokenVersion.toString() + "; "
                + "token=$(" + requestRefreshedTokenCommand + "); " + downloadCommand
                + extractHttpStatusCode + sleepCommand + " done";

        return new Command(commandIO, rawCommand);
    }

    private String getRefreshTokenCommand(String userId, Long tokenVersion) {
        final String iguassuUrl =
                this.properties.getProperty(ConfProperty.IGUASSU_SERVICE_HOST_URL.getProp());
        final String refreshTokenUrl =
                String.format(
                        "%s/auth/oauth2/refresh/${userId}/${tokenVersion}",
                        iguassuUrl, userId, tokenVersion);
        return String.format("curl -X POST %s", refreshTokenUrl);
    }
}