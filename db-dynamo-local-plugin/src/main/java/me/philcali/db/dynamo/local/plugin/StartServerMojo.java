package me.philcali.db.dynamo.local.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "start")
@Execute(goal = "extract")
public class StartServerMojo extends AbstractLocalParametersMojo {
    @Parameter(property = "ddb.start.parameters")
    private String[] ddbArgs;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final String javaHome = Optional.ofNullable(System.getenv("JAVA_HOME"))
                .orElseThrow(() -> new MojoExecutionException("Please set the JAVA_HOME env!"));
        final Path parentDirectory = getLocalArchivePath().getParent();
        final Path libraryFolder = parentDirectory.resolve(ARTIFACT_NAME + "_lib");
        final Path jarFile = parentDirectory.resolve(ARTIFACT_NAME + ".jar");
        final List<String> commands = new ArrayList<>();
        commands.add("java");
        commands.add("-Djava.library.path=" + libraryFolder);
        commands.add("-D" + PROCESS_LOOKUP);
        commands.add("-jar");
        commands.add(jarFile.toString());
        Arrays.stream(ddbArgs).forEach(commands::add);
        try {
            getLog().info("Starting dynamodb local server with: " + commands);
            final Process server = new ProcessBuilder(commands)
                    .directory(new File(javaHome, "bin"))
                    .start();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(server.getInputStream(), StandardCharsets.UTF_8));
            String line = reader.readLine();
            while (Optional.ofNullable(line).map(String::trim).filter(l -> !l.isEmpty()).isPresent()) {
                getLog().info(line);
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to start dynamodb local server: " + e.getMessage());
        }
    }
}
