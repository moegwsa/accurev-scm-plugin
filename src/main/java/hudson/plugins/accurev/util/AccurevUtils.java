package hudson.plugins.accurev.util;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;

import java.io.Serializable;

public class AccurevUtils implements Serializable {


    public static Node workspaceToNode(FilePath workspace) {
        Jenkins j = Jenkins.getInstanceOrNull();
        if (workspace != null && workspace.isRemote()) {
            for (Computer c : j.getComputers()) {
                if (c.getChannel() == workspace.getChannel()) {
                    Node n = c.getNode();
                    if (n != null) {
                        return n;
                    }
                }
            }
        }
        return j;
    }
}
