package me.philcali.db.dynamo.local.plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

public abstract class AbstractLocalParametersMojo extends AbstractMojo {
    protected static final String ARTIFACT_NAME = "DynamoDBLocal";
    protected static final int MAX_BUFFER_SIZE = 8192;
    protected static final String FILE_NAME = "dynamodb_local_latest";
    protected static final String PROCESS_LOOKUP = "ddb.started";

    @Parameter(property = "ddb.download.directory", defaultValue = "${project.build.directory}/dynamodb-local")
    protected String outputDirectory;

    @Parameter(property = "ddb.download.extension", defaultValue = "zip")
    protected String extension;

    protected Optional<Path> findExistingArchive() throws MojoExecutionException {
        return Optional.of(getLocalArchivePath()).filter(Files::exists);
    }

    protected Path getLocalArchivePath() throws MojoExecutionException {
        final Path localFile = Paths.get(outputDirectory, getLocalFileName());
        final Path parentDirectory = localFile.getParent();
        try {
            Files.createDirectories(parentDirectory);
            return localFile;
        } catch (IOException ie) {
            throw new MojoExecutionException("Failed to create outputDirectory: " + parentDirectory);
        }
    }

    protected String getLocalFileName() {
        return String.format("%s.%s", FILE_NAME, extension);
    }

}
