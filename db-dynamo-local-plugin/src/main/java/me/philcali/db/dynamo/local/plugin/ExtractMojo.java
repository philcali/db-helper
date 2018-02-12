package me.philcali.db.dynamo.local.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.Enumeration;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "extract")
@Execute(goal = "download")
public class ExtractMojo extends AbstractLocalParametersMojo {
    @Parameter(property = "ddb.extract.force", defaultValue = "false")
    private boolean forceExtract;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final Optional<Path> localArchive = findExistingArchive().filter(this::isBinaryNewer);
        if (localArchive.isPresent()) {
            final Path parentDirectory = localArchive.get().getParent();
            try (final ZipFile zipFile = new ZipFile(localArchive.get().toFile())) {
                final Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    final ZipEntry entry = entries.nextElement();
                    final Path zipPath = parentDirectory.resolve(entry.getName());
                    getLog().info("Extracting " + entry.getName() + " into " + zipPath);
                    if (entry.isDirectory()) {
                        Files.createDirectories(zipPath);
                    } else {
                        final InputStream zipStream = zipFile.getInputStream(entry);
                        Files.copy(zipStream, zipPath);
                    }
                }
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage());
            }
        }
    }

    private boolean isBinaryNewer(final Path localArchive) {
        try {
            final Path binaryPath = localArchive.getParent().resolve(ARTIFACT_NAME + ".jar");
            final FileTime localTime = Files.getLastModifiedTime(localArchive);
            final FileTime binaryTime = Files.getLastModifiedTime(binaryPath);
            return forceExtract || new Date(localTime.toMillis()).after(new Date(binaryTime.toMillis()));
        } catch (IOException ie) {
            getLog().warn("Will attempt extraction.");
            return true;
        }

    }
}
