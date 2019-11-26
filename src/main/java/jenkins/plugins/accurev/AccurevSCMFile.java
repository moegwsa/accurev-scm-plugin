package jenkins.plugins.accurev;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.scm.api.SCMFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class AccurevSCMFile extends SCMFile {
    private final AccurevSCMFileSystem fs;
    private Type fileType;

    public AccurevSCMFile(AccurevSCMFileSystem fs) {
        this.fs = fs;
    }

    public AccurevSCMFile(AccurevSCMFileSystem fs, AccurevSCMFile parent, String name, Type fileType) {
        super(parent, name);
        this.fs = fs;
        this.fileType = fileType;
    }

    @NonNull
    @Override
    protected SCMFile newChild(@NonNull String name, boolean assumeIsDirectory) {
        return new AccurevSCMFile(fs, this, name, assumeIsDirectory ? Type.DIRECTORY : Type.REGULAR_FILE);
    }

    @NonNull
    @Override
    public Iterable<SCMFile> children() throws IOException, InterruptedException {
        List<SCMFile> result = new ArrayList<>();
        Path path = Paths.get(fs.getRoot().getPath());
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if(file.getFileName() != null) {
                        Path filename = file.getFileName();
                        if(filename!= null) result.add(new AccurevSCMFile(fs, AccurevSCMFile.this, filename.toString(), Type.REGULAR_FILE));
                    }
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dir,
                                                          IOException exc) {
                    if(dir.getFileName() != null) {
                        Path dirname = dir.getFileName();
                        if(dirname != null) result.add(new AccurevSCMFile(fs, AccurevSCMFile.this, dirname.toString(), Type.DIRECTORY));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

        return result;
    }

    @Override
    public long lastModified() throws IOException, InterruptedException {
        return 1;
    }

    @NonNull
    @Override
    protected Type type() throws IOException, InterruptedException {
        return fileType;
    }

    @Override
    public InputStream content() throws IOException, InterruptedException {
        if(fs.getAccurevClient() != null) {
            StandardUsernamePasswordCredentials cred = fs.getAccurevClient().getCredentials();

            if (cred != null) {
                fs.getAccurevClient().login().username(cred.getUsername()).password(cred.getPassword()).execute();
                String file = fs.getAccurevClient().getFile(fs.getHead(), getPath(), Long.toString(fs.lastModified()));
                return new ByteArrayInputStream(file.getBytes("UTF-8"));
            }
        }
        return new ByteArrayInputStream("".getBytes("UTF-8"));
    }
}
