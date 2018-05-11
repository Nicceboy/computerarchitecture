package org.directorywatcher.plugin;

import org.keyword.plugin.KeywordPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DirectoryEvent implements KeywordPlugin.KeywordNotifyObject {

    private final DirectoryWatcherPlugin plugin;

    private DirectoryWatcherPlugin.Event event;
    private String fileName;
    private ArrayList<String> keywords = new ArrayList<>();

    DirectoryEvent(DirectoryWatcherPlugin.Event e, String file, DirectoryWatcherPlugin plugin) {
        super();
        this.plugin = plugin;
        this.event = e;
        this.fileName = file;
    }
    public void addkeywords(ArrayList<String> keywords){
        this.keywords = keywords;
    }

    public String getModuleName() {
        return this.plugin.getPluginName();
    }

    public String getFileName() {
        return fileName;
    }

    public String getModuleExtraInfo() {
        return (this.event + " in: " +this.fileName);
    }

    public List<String> getTrackablesFound() {
        return this.keywords;
    }
}