package hudson.plugins.accurev;

import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.scm.EditType;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class AccurevChangeSet extends ChangeLogSet.Entry {
    private String transactionId;
    private String title;
    private String type;
    private String user;
    private Long timestamp;
    private String stream;
    private Collection<String> affectedPaths = Collections.EMPTY_LIST;

    public AccurevChangeSet(List<String> lines) {
        if (lines.size() > 0 ){
            parseTransaction(lines);
        }
    }

    private void parseTransaction(List<String> lines) {
        StringBuilder message = new StringBuilder();
        for (String line : lines){
            if ( line.length() < 1)
                continue;
            if (line.startsWith("transaction: ")) {
                String[] split = line.split(" ");
                if(split.length > 1) this.transactionId = split[1];
                else throw new IllegalArgumentException("Transaction has no ID" + lines);
            }
            else if (line.startsWith("stream: ")) {
                String[] split = line.split(" ");
                if(split.length > 1) this.stream = split[1];
            }
            else if (line.startsWith("    ")) message.append(line.substring(4)).append('\n');
            else if (line.startsWith("Type: ")){
                String[] split = line.split(" ");
                if(split.length > 1) this.type = split[1];
            }else if (line.startsWith("User: " )) {
                String[] split = line.split (" ");
                if(split.length > 1) this.user = split[1];
            }else if (line.startsWith("Time: ")) {
                String[] split = line.split(" ");
                String time = split[1] + " " + split[2];
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");
                try {
                    Date parsedDate = dateFormat.parse(time);
                    timestamp = parsedDate.getTime();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }else if (line.startsWith("File: ")){
                String[] split = line.split(" ");
                if(split.length > 1 ) {
                    affectedPaths = getAffectedPaths();
                    if (affectedPaths.isEmpty()){
                        affectedPaths = new ArrayList<String>();
                    }
                    affectedPaths.add(split[1]);
                    setAffectedPaths(affectedPaths);
                }
            }
        }
        this.title = message.toString();

    }

    @Override
    public void setParent(ChangeLogSet parent) {
        super.setParent(parent);
    }

    @Override
    @Exported
    public String getMsg() {
        return this.title.isEmpty() ? this.type : this.title;
    }

    public String getType() {
        return type;
    }

    public String getUser() {
        return user;
    }

    public String getStream() {
        return stream;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    public String getDate() {
        return timestamp.toString();
    }

    @Exported
    public String getId(){ return this.transactionId; }

    @Override
    public User getAuthor() {
        User user;
        user = User.getById(this.user, true);
        user.setFullName(this.user);
        return user;
    }

    @Override
    public Collection<String> getAffectedPaths() {
        return affectedPaths;
    }

    private void setAffectedPaths(Collection<String> value){
        affectedPaths = value;
    }


    @ExportedBean(defaultVisibility=999)
    public static class Path implements ChangeLogSet.AffectedFile {
        private String src;
        private String dst;
        private char action;
        private String path;
        private AccurevChangeSet changeSet;

        private Path(String source, String destination, char action, String filePath, AccurevChangeSet changeSet) {
            this.src = source;
            this.dst = destination;
            this.action = action;
            this.path = filePath;
            this.changeSet = changeSet;
        }

        public String getSrc() {
            return src;
        }

        public String getDst() {
            return dst;
        }

        @Exported(name="file")
        public String getPath() {
            return path;
        }

        public AccurevChangeSet getChangeSet() {
            return changeSet;
        }

        @Exported
        public EditType getEditType() {
            switch (action) {
                case 'A':
                    return EditType.ADD;
                case 'D':
                    return EditType.DELETE;
                default:
                    return EditType.EDIT;
            }
        }
    }

    public int hashCode() {
        return transactionId != null ? transactionId.hashCode() : super.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj instanceof AccurevChangeSet)
            return transactionId != null && transactionId.equals(((AccurevChangeSet) obj).transactionId);
        return false;
    }
}
