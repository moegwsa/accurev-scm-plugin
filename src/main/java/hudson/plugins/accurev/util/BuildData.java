package hudson.plugins.accurev.util;

import hudson.model.Action;
import hudson.model.Run;
import hudson.plugins.accurev.ServerRemoteConfig;
import hudson.plugins.accurev.StreamSpec;
import jenkins.model.Jenkins;
import jenkins.plugins.accurevclient.model.AccurevStream;
import jenkins.plugins.accurevclient.model.AccurevTransaction;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import javax.annotation.CheckForNull;
import java.io.Serializable;
import java.util.*;

@ExportedBean(defaultVisibility = 999)
public class BuildData implements Action, Serializable, Cloneable{
    private static final long serialVersionUID = 1L;

    public BuildData() {
    }

    public Build lastBuild;
    public Set<String> remoteStreams = new HashSet<>();
    private Integer index;
    public String scmName;

    public BuildData(String scmName, List<ServerRemoteConfig> serverRemoteConfigs) {
        this.scmName = scmName;
        for (ServerRemoteConfig src : serverRemoteConfigs){
            remoteStreams.add(src.getHost());
        }
    }

    public BuildData(String scmName) {
        this.scmName = scmName;
    }

    public void saveBuild(Build build){
        lastBuild = build;
        if(build.marked != null) {
            remoteStreams.add(build.marked.getName());
        }
    }

    @Exported
    public String getScmName()
    {
        if (scmName == null)
            scmName = "";
        return scmName;
    }

    public Integer getIndex() {
        return index;
    }

    public void setScmName(String scmName) {
        this.scmName = scmName;
    }

    public void setIndex(Integer index) {
        this.index = index == null || index <= 1 ? null : index;
    }

    public void setRemoteStreams(Set<String> remoteStreams) {
        this.remoteStreams = remoteStreams;
    }

    public Set<String> getRemoteStreams() {
        return remoteStreams;
    }

    public void addRemoteStream(String stream) {
        this.remoteStreams.add(stream);
    }


    public boolean hasBeenReferenced(String stream) {
        return remoteStreams.contains(stream);
    }

    @CheckForNull
    @Override
    public String getIconFileName() {
        return jenkins.model.Jenkins.RESOURCE_PATH + "/plugin/accurev/icons/accurev-48x48.png";
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        if (scmName != null && !scmName.isEmpty())
            return "Accurev Build Data: " + scmName;
        return "Accurev Build Data";
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return index == null ? "accurev" : "accurev-" + index;
    }

    public Build getLastBuild(AccurevTransaction at, AccurevStream as){
        if(lastBuild != null && ((lastBuild.transaction.getId() == at.getId()) || (lastBuild.marked.getName().equals(as.getName()))))  return lastBuild;
        return null;
    }

    public AccurevTransaction getLastBuiltTransaction() { return lastBuild == null ? null : lastBuild.transaction;}

    @Override
    public BuildData clone(){
        BuildData clone;
        try {
            clone = (BuildData) super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new RuntimeException("Error cloning BuildData", e);
        }

        IdentityHashMap<Build, Build> clonedBuilds = new IdentityHashMap<>();

        clone.remoteStreams = new HashSet<>();

        if (lastBuild != null){
            clone.lastBuild = clonedBuilds.get(lastBuild);
            if(clone.lastBuild == null){
                clone.lastBuild = lastBuild.clone();
            }
        }

        return clone;
    }


    @Restricted(NoExternalUse.class) // only used from stapler/jelly
    @edu.umd.cs.findbugs.annotations.CheckForNull
    public Run<?,?> getOwningRun() {
        StaplerRequest req = Stapler.getCurrentRequest();
        if (req == null) {
            return null;
        }
        return req.findAncestorObject(Run.class);
    }

    public boolean hasBeenBuilt(AccurevTransaction at, AccurevStream as) {
        return getLastBuild(at, as) != null;
    }

    @Override
    public String toString() {
        final String scmNameString = scmName == null ? "<null>" : scmName;
        return super.toString()+"[scmName="+scmNameString+
                ",remoteStreams="+remoteStreams+
                ",lastBuild="+lastBuild+"]";
    }
    @Override
    public int hashCode() {
        return Objects.hash(remoteStreams, lastBuild);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BuildData that = (BuildData) o;

        return Objects.equals(remoteStreams, that.remoteStreams)
                && Objects.equals(lastBuild, that.lastBuild);
    }

}
