package hudson.plugins.accurev;

import hudson.Util;
import hudson.model.Action;
import hudson.model.InvisibleAction;
import hudson.model.Queue;
import hudson.model.queue.FoldableAction;
import org.eclipse.jgit.transport.URIish;

import java.io.Serializable;
import java.util.List;

public class TransactionParameterAction extends InvisibleAction implements Serializable, Queue.QueueAction, FoldableAction {

    public final URIish server;
    public final String transaction;

    public TransactionParameterAction(String transaction, URIish server) {
        this.transaction = transaction;
        this.server = server;
    }

    @Override
    public boolean shouldSchedule(List<Action> actions) {
        List<TransactionParameterAction> otherActions = Util.filter(actions, TransactionParameterAction.class);
        for (TransactionParameterAction action : otherActions) {
            if(this.transaction.equals(action.transaction)) return false;
        }

        return true;
    }

    @Override
    public void foldIntoExisting(Queue.Item item, Queue.Task task, List<Action> list) {
        item.getActions().add(this);
    }
}
