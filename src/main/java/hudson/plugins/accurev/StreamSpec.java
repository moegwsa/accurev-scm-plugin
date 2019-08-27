package hudson.plugins.accurev;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.Serializable;
import java.util.Objects;

@ExportedBean
public class StreamSpec extends AbstractDescribableImpl<StreamSpec> implements Serializable{
    private String name;
    private String depot;

    public StreamSpec(String name) {
        this.name = name;
    }

    @Exported
    public String getDepot() { return depot;}

    @Exported
    public String getName() { return name; }

    public void setName(String name){
        if(name == null)
            throw new IllegalArgumentException();
        else if(name.length() == 0)
            this.name = "**";
        else
            this.name = name.trim();
    }

    public void setDepot(String depot) { this.depot = depot;}

    @DataBoundConstructor
    public StreamSpec(String name, String depot){
        if(name != null) {
            setName(name);
        }else {
           setName(depot);
        }
        setDepot(depot);
    }

    public String toString() { return name; }

    @Override
    public int hashCode() {
        return Objects.hash(name, depot);
    }

    public boolean matches(String stream, String depot) {
        if(this.name.equals(stream) && this.depot.equals(depot)) {
            return true;
        }
        return false;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<StreamSpec> {
        @Override
        public String getDisplayName() { return "Stream Spec";}
    }
}
