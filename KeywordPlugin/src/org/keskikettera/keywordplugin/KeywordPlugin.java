package org.keskikettera.keywordplugin;

import java.util.List;
import java.util.Observer;

public interface KeywordPlugin {
    public void startPlugin();
    public String getPluginName();
    public String getPluginDesc();
    public String getPluginUsage();
    public List<KeywordTrackable> getAllTrackables();
    public void addTrackables(List<String> trackables, String extraInfo, Observer observer);
    public void removeTrackables(List<String> trackables, String extraInfo, Observer observer);

    public interface KeywordTrackable {
        public String getTrackable();
        public String getExtraInfo();
    }

    public interface KeywordNotifyObject {
        public String getModuleName();
        public String getModuleExtraInfo();
        public List<String> getTrackablesFound();
    }
}
