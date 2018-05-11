package org.directorywatcher.plugin;

import org.keyword.plugin.KeywordPlugin;

import java.util.Arrays;
import java.util.List;

public class DirectoryEvent implements KeywordPlugin.KeywordNotifyObject {

    private final DirectoryWatcherPlugin plugin;

    private DirectoryWatcherPlugin.Event event;
    private String fileName;

    DirectoryEvent(DirectoryWatcherPlugin.Event e, String file, DirectoryWatcherPlugin plugin) {
        super();
        this.plugin = plugin;
        this.event = e;
        this.fileName = file;
    }

    public String getModuleName() {
        return this.plugin.getPluginName();
    }

    public String getModuleExtraInfo() {
        return "test";
    }

    public List<String> getTrackablesFound() {
        String[] array = {"Found This", "And this."};
        return Arrays.asList(array);
    }
}