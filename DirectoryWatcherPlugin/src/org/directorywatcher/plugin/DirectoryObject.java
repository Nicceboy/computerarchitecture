package org.directorywatcher.plugin;

import org.keyword.plugin.KeywordPlugin;

import java.nio.file.*;
import java.util.*;

class DirectoryObject extends Observable implements KeywordPlugin.KeywordTrackable {

    //Making class nested while still implementing it in another file
    private final DirectoryWatcherPlugin plugin;
    private boolean trace = true;
    private boolean recursive = false;
    private boolean MasterPath =  false;
    private List<String> trackables = new ArrayList<String>();
    private String dir;
    private Observer observer;
    private Path path;

    DirectoryObject(Path dir, DirectoryObject directoryObject, DirectoryWatcherPlugin plugin){
        this.plugin = plugin;
        this.dir = dir.toString();
        this.observer = directoryObject.getObserver();
        this.path = dir;
        this.trackables = directoryObject.getTrackables();
    }
    DirectoryObject(List<String> keywords, String dir, Observer o, DirectoryWatcherPlugin plugin) throws KeywordPlugin.FailedToDoPluginThing {

        this.plugin = plugin;

        this.trackables = keywords;
        //Contains all data from client
        this.dir = dir;
        try{
        if ("recursive".equals(dir.split(",")[1].replaceAll("\\s+", ""))) {
            this.recursive = true;
        }}catch (ArrayIndexOutOfBoundsException e){
            //Not recursive path
            this.recursive =false;
        }
        this.observer = o;
        this.path = FileSystems.getDefault().getPath(dir.split(",")[0]);

    }


    public List<String> getTrackables() {
        return trackables;
    }

    public String getExtraInfo() {
        return this.dir;
    }

    Path getPath() {
        return this.path;
    }
    boolean isMasterPath(){
        return this.MasterPath;
    }
    void setMasterPath(){
        this.MasterPath = true;
    }

    boolean isRecursive() {
        return this.recursive;
    }

    Observer getObserver() {
        return this.observer;
    }

    public boolean addTrackable(String newTrack) {

        if (!trackables.contains(newTrack)) {
            trackables.add(newTrack);
            return true;
        } else {
            System.out.println("Keyword already exists: " + newTrack);
            return false;
        }
    }
    public boolean removeTrackable(String thingToRemove) {
        return (trackables.removeIf(thingToRemove::equals));
    }
    public boolean removeTrackable(List<String> thingsToRemove) {
        return (trackables.removeAll(thingsToRemove));
    }

    void setDirty() {
        setChanged();
    }
}
