package org.keyword.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Observer;

public interface KeywordPlugin extends Runnable {
    void startPlugin() throws FailedToDoPluginThing;

    String getPluginName();

    String getPluginDesc();

    String getPluginUsage();

    List<KeywordTrackable> getAllTrackables();

    void addTrackables(List<String> trackables, String extraInfo, Observer observer) throws FailedToDoPluginThing;

    void removeTrackables(List<String> trackables, String extraInfo, Observer observer) throws FailedToDoPluginThing;

    interface KeywordTrackable {

        List<String> getTrackables();

        String getExtraInfo();


        boolean addTrackable(String newTrack);
        boolean removeTrackable(String trackToRemove);
    }

    interface KeywordNotifyObject {
        String getModuleName();

        String getModuleExtraInfo();

        List<String> getTrackablesFound();


    }

    class FailedToDoPluginThing extends Exception {
        public FailedToDoPluginThing() {
            super();
        }

        public FailedToDoPluginThing(String message) {
            super(message);
        }

        public FailedToDoPluginThing(String message, Throwable cause) {
            super(message, cause);
        }

        public FailedToDoPluginThing(Throwable cause) {
            super(cause);
        }
    }
}
