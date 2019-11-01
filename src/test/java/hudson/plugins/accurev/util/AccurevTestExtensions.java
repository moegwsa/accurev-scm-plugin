package hudson.plugins.accurev.util;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.util.Secret;
import jenkins.plugins.accurevclient.Accurev;
import jenkins.plugins.accurevclient.AccurevClient;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class AccurevTestExtensions {
    public static Boolean checkCommandExist(String command) throws IOException, InterruptedException {
        String which =  (System.getProperty("os.name").toLowerCase().contains("windows")) ? "where" : "which";
        String cmd = which + " " + command;
        return Runtime.getRuntime().exec(cmd).waitFor() == 0;

    }

    public static String generateString(int count){
        String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder builder = new StringBuilder();
        while (count-- != 0) {
            int character = (int)(Math.random()*ALPHA_NUMERIC_STRING.length());
            builder.append(ALPHA_NUMERIC_STRING.charAt(character));
        }
        return builder.toString();
    }

    public static File createFile(String path, String fileName, String text) throws IOException {
        File file = new File(path, fileName);
        FileUtils.write(file, text);
        return file;
    }

    public static AccurevClient createClientAtDir(File path, String url, String username, String password) throws InterruptedException {
        Accurev accurev = Accurev.with(TaskListener.NULL, new EnvVars(),  new Launcher.LocalLauncher(TaskListener.NULL))
                .at(path).on(url);
        AccurevClient client = accurev.getClient();
        client.login().username(username).password(Secret.fromString(password)).execute();
        return client;
    }
}

