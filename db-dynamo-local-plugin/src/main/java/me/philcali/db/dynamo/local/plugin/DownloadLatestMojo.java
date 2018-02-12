package me.philcali.db.dynamo.local.plugin;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import me.philcali.http.api.HttpMethod;
import me.philcali.http.api.IHttpClient;
import me.philcali.http.api.IRequest;
import me.philcali.http.api.IResponse;
import me.philcali.http.api.exception.HttpException;
import me.philcali.http.api.util.URLBuilder;
import me.philcali.http.java.NativeClientConfig;
import me.philcali.http.java.NativeHttpClient;

@Mojo(name = "download", requiresOnline = true)
public class DownloadLatestMojo extends AbstractLocalParametersMojo {
    private static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    private static final String S3_LOCATION_FORMAT = "s3.%s.amazonaws.com";
    private static final String PATH_FORMAT = "%s/%s.%s";

    @Parameter(property = "ddb.download.force", defaultValue = "false")
    private boolean forceDownload;

    @Parameter(property = "ddb.download.region", defaultValue = "us-west-2")
    private String region;

    @Parameter(property = "ddb.download.connectTimeout", defaultValue = "2000")
    private int connectTimeout;

    @Parameter(property = "ddb.download.readTimeout", defaultValue = "10000")
    private int readTimeout;

    private void downloadArchive(final IHttpClient client) throws MojoExecutionException {
        final Path localArchive = getLocalArchivePath();
        try {
            final IRequest request = client.createRequest(HttpMethod.GET, getArchiveURL().toString());
            final IResponse response = request.respond();
            try {
                Files.copy(response.body(), localArchive);
            } catch (IOException ie) {
                throw new MojoExecutionException("Failed to copy archive: " + localArchive);
            }
            getLog().info("Finished downloading " + localArchive);
        } catch (HttpException he) {
            throw new MojoExecutionException(he.getMessage());
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
       final IHttpClient client = new NativeHttpClient(new NativeClientConfig()
               .withConnectTimeout(connectTimeout)
               .withRequestTimeout(readTimeout));
       final Optional<Path> existingArchive = findExistingArchive()
               .filter(localArchive -> isLocalArchiveNewer(localArchive, client))
               .filter(localArchive -> {
                   if (forceDownload) {
                       getLog().info("Overwriting with the latest!");
                   }
                   return !forceDownload;
               });
       if (!existingArchive.isPresent()) {
           getLog().info("Downloading latest archive from " + getArchiveURL().toString());
           downloadArchive(client);
       }
    }

    private URL getArchiveURL() {
        return new URLBuilder()
                .withProtocol("https")
                .withHost(String.format(S3_LOCATION_FORMAT, region.toLowerCase()))
                .withPath(String.format(PATH_FORMAT, getPath(), FILE_NAME, extension))
                .build();
    }

    private String getPath() {
        switch(region) {
        case "ap-south-1":
            return "dynamodb-local-mumbai";
        case "ap-southeast-1":
            return "dynamodb-local-singapore";
        case "ap-northeast-1":
            return "dynamodb-local-tokyo";
        case "eu-central-1":
            return "dynamodb-local-frankfurt";
        case "sa-east-1":
            return "dynamodb-local-sao-paulo";
        default:
            return "dynamodb-local";
        }
    }

    private boolean isLocalArchiveNewer(final Path localArchive, final IHttpClient client) {
        final String archiveUrl = getArchiveURL().toString();
        getLog().info("Checking for newer archive at " + archiveUrl);
        try {
            final FileTime localTime = Files.getLastModifiedTime(localArchive);
            final IRequest request = client.createRequest(HttpMethod.HEAD, archiveUrl);
            final IResponse response = request.respond();
            if (response.status() == 200) {
                final String modifiedTime = response.header("last-modified");
                final SimpleDateFormat format = new SimpleDateFormat(HTTP_DATE_FORMAT);
                return format.parse(modifiedTime).before(new Date(localTime.toMillis()));
            } else {
                throw new RuntimeException("Response was " + response.status());
            }
        } catch (IOException | ParseException | RuntimeException e) {
            getLog().warn("Failed to check the remote " + archiveUrl, e);
            return true;
        }
    }

}
