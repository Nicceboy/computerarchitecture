package org.anttijuustila.wordwatch;

import org.anttijuustila.keywordclient.KeywordAPI;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;

/**
 * Created by anttijuustila on 15.2.2018.
 */

public class KeywordWatcher implements KeywordAPI.KeywordAPIListener {

    private Handler eventHandler = null;
    private Runnable hostingObject = null;
    private BlockingQueue<String> incomingMsgQueue = new LinkedBlockingQueue<String>();
    private Context context = null;

    KeywordWatcher(Runnable hostEventHandler, Context c) {
        eventHandler = new Handler(Looper.getMainLooper());
        hostingObject = hostEventHandler;
        context = c;
    }

    void initialize() throws KeywordAPI.KeywordAPIException {
        KeywordAPI.getInstance().prepareClient(this);
    }

    void connect(String server, String targetPath) throws KeywordAPI.KeywordAPIException {
        KeywordAPI.getInstance().attach(server, targetPath);
    }

    void disconnect() {
        KeywordAPI.getInstance().detach();
    }


    boolean isConnected() {
        return KeywordAPI.getInstance().myState() == KeywordAPI.ClientState.EConnected;
    }

    void watchKeyword(String word) throws IOException, InterruptedException {
        KeywordAPI.getInstance().addKeyword(word);
    }


    List<String> keywords() {
        return KeywordAPI.getInstance().keywords();
    }

    boolean hasAlerts() {
        return !incomingMsgQueue.isEmpty();
    }

    String getLatestAlert() throws InterruptedException {
        return incomingMsgQueue.take();
    }

    @Override
    public void changeEventHappened(String response, String text, String file) {
        String alert = "";
        if (response.equalsIgnoreCase("response")) {
            alert = context.getString(R.string.keyword_string) + " " + text + " " +
                    context.getString(R.string.found_in_string) + " " + file +"\n";
            incomingMsgQueue.add(alert);
            eventHandler.post(hostingObject);
            MediaPlayer mp = MediaPlayer.create(context, R.raw.scream);
            mp.start();
        }
    }

    @Override
    public void notify(String s) {
        incomingMsgQueue.add(s+"\n");
        eventHandler.post(hostingObject);
    }

}
