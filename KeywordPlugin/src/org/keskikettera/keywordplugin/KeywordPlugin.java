package org.keskikettera.keywordplugin;

import java.util.List;
import java.util.Observer;

public interface KeywordPlugin {
    public void startPlugin() throws FailedToDoPluginThing;

    public String getPluginName();

    public String getPluginDesc();

    public String getPluginUsage();

    public List<KeywordTrackable> getAllTrackables();

    public void addTrackables(List<String> trackables, String extraInfo, Observer observer) throws FailedToDoPluginThing;

    public void removeTrackables(List<String> trackables, String extraInfo, Observer observer) throws FailedToDoPluginThing;

    public interface KeywordTrackable {
        public String getTrackable();

        public String getExtraInfo();
    }

    public interface KeywordNotifyObject {
        public String getModuleName();

        public String getModuleExtraInfo();

        public List<String> getTrackablesFound();
    }

    public class FailedToDoPluginThing extends Exception {
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
