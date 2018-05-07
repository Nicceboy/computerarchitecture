package org.keskikettera.keywordplugin;

import java.util.List;
import java.util.Observer;

public interface KeywordPlugin {
    public void startPlung();
    public void addTrackables(List<String> trackables, String extraInfo, Observer observer);
    public void removeTrackables(List<String> trackables, String extraInfo, Observer observer);

    public interface KeywordNotifyObject {
        public String getModuleName();
        public String getModuleExtraInfo();
        public List<String> getTrackablesFound();
    }
}
