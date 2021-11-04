package ua.aharoo.clouds;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;

public class CloudRepliesControlSet {

    public Semaphore waitReplies = new Semaphore(0);
    public CopyOnWriteArrayList<CloudReply> replies;
    public int sequence;

    public CloudRepliesControlSet(int sequence){
        this.sequence = sequence;
        this.replies = new CopyOnWriteArrayList<>();
    }

}
