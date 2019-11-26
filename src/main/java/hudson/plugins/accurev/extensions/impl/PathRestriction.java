package hudson.plugins.accurev.extensions.impl;

import hudson.Extension;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.accurev.AccurevSCM;
import hudson.plugins.accurev.extensions.AccurevSCMExtension;
import hudson.plugins.accurev.extensions.AccurevSCMExtensionDescriptor;
import jenkins.plugins.accurevclient.AccurevClient;
import jenkins.plugins.accurevclient.AccurevException;
import jenkins.plugins.accurevclient.commands.PopulateCommand;
import jenkins.plugins.accurevclient.model.AccurevTransaction;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class PathRestriction extends AccurevSCMExtension {

    private String includedRegions;
    private String excludedRegions;
    private transient volatile List<Pattern> includedPatterns,excludedPatterns;

    @DataBoundConstructor
    public PathRestriction(String includedRegions, String excludedRegions) {
        this.includedRegions = includedRegions;
        this.excludedRegions = excludedRegions;
    }

    public PathRestriction() {
        includedRegions = "";
        excludedRegions = "";
    }

    @Override
    public boolean requiresWorkspaceForPolling() {
        return true;
    }

    public String getIncludedRegions() {
        return includedRegions;
    }

    public void setIncludedRegions(String includedRegions) {
        this.includedRegions = includedRegions;
    }

    public String getExcludedRegions() {
        return excludedRegions;
    }

    public void setExcludedRegions(String excludedRegions) {
        this.excludedRegions = excludedRegions;
    }

    private String[] normalize(String s) {
        return StringUtils.isBlank(s) ? null : s.split("[\\r\\n]+");
    }

    public String[] getExcludedRegionsNormalized() {
        return normalize(excludedRegions);
    }

    public String[] getIncludedRegionsNormalized() {
        return normalize(includedRegions);
    }

    private List<Pattern> getIncludedPatterns() {
        if (includedPatterns==null)
            includedPatterns = getRegionsPatterns(getIncludedRegionsNormalized());
        return includedPatterns;
    }

    private List<Pattern> getExcludedPatterns() {
        if (excludedPatterns == null)
            excludedPatterns = getRegionsPatterns(getExcludedRegionsNormalized());
        return excludedPatterns;
    }



    private List<Pattern> getRegionsPatterns(String[] regions) {
        if (regions != null) {
            List<Pattern> patterns = new ArrayList<>(regions.length);
            for (String region : regions) {
                patterns.add(Pattern.compile(region));
            }
            return patterns;
        }
        return Collections.emptyList();
    }


    public Boolean isTransactionExcluded(AccurevTransaction transaction, TaskListener listener) throws IOException, InterruptedException, AccurevException {

        Collection<String> paths = transaction.affectedPaths();
        if(paths.isEmpty()) {
            return false;
        }

        List<Pattern> included = getIncludedPatterns();
        List<Pattern> excluded = getExcludedPatterns();

        // Assemble the list of included paths
        List<String> includedPaths = new ArrayList<>(paths.size());
        if (!included.isEmpty()) {
            for (String path : paths) {
                for (Pattern pattern : included) {
                    if (pattern.matcher(path).matches()) {
                        includedPaths.add(path);
                        break;
                    }
                }
            }
        } else {
            includedPaths.addAll(paths);
        }

        // Assemble the list of excluded paths
        List<String> excludedPaths = new ArrayList<>();
        if (!excluded.isEmpty()) {
            for (String path : includedPaths) {
                for (Pattern pattern : excluded) {
                    if (pattern.matcher(path).matches()) {
                        excludedPaths.add(path);
                        break;
                    }
                }
            }
        }

        if (excluded.isEmpty() && !included.isEmpty() && includedPaths.isEmpty()) {
            listener.getLogger().println("Ignored transaction " + transaction.getId()
                    + ": No paths matched included region whitelist");
            return true;
        } else if (includedPaths.size() == excludedPaths.size()) {
            // If every affected path is excluded, return true.
            listener.getLogger().println("Ignored transaction " + transaction.getId()
                    + ": Found only excluded paths: "
                    + Util.join(excludedPaths, ", "));
            return true;
        }

        return false;
    }

    @Override
    public void decoratePopulateCommand(AccurevSCM scm, Run<?, ?> build, AccurevClient accurev, TaskListener listener, PopulateCommand cmd) throws IOException, InterruptedException {

    }



    @Extension
    public static class DescriptorImpl extends AccurevSCMExtensionDescriptor {
        @Override
        public String getDisplayName() {
            return "Ignore keep/promotes from certain paths";
        }
    }

}
