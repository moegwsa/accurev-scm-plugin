package hudson.plugins.accurev.util;

import java.io.IOException;

public class AccurevTestExtensions {
    public static Boolean checkCommandExist(String command) throws IOException, InterruptedException {
        String which =  (System.getProperty("os.name").toLowerCase().contains("windows")) ? "where" : "which";
        String cmd = which + " " + command;
        return Runtime.getRuntime().exec(cmd).waitFor() == 0;

    }
}

