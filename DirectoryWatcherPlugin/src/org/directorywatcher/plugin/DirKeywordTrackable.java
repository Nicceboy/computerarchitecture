package org.directorywatcher.plugin;

import org.keyword.plugin.KeywordPlugin;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

class DirKeywordTrackable implements KeywordPlugin.KeywordTrackable {

    //Making class nested while still implementing it in another file
    private final DirectoryWatcherPlugin plugin;

    private final Map<WatchKey, DirKeywordTrackable.PathObservable> keys;
    private WatchService watcher;

    private boolean trace = true;
    private boolean recursive = false;
    private List<String> trackables = new ArrayList<String>();
    private String dir;
    private Observer observer;

    DirKeywordTrackable(List<String> keywords, String dir, Observer o, WatchService watcher, Map<WatchKey, DirKeywordTrackable.PathObservable> keys, DirectoryWatcherPlugin plugin) throws KeywordPlugin.FailedToDoPluginThing {

        this.plugin = plugin;
        this.watcher = watcher;
        this.keys = keys;

        this.trackables = keywords;
        this.dir = dir.split(",")[0];
        if ("recursive".equals(dir.split(",")[0].replaceAll("\\s+", ""))) {
            this.recursive = true;
        }
        this.observer = o;


        Path newPath = FileSystems.getDefault().getPath(dir);
        try {

            if (recursive) {
                System.out.format("Scanning %s ...\n", dir);
                registerAll(newPath, this.observer);
                System.out.println("Done.");
            } else {
                register(newPath, this.observer);
            }

        } catch (IOException e) {
            throw new KeywordPlugin.FailedToDoPluginThing("Incorrect path: " + dir);

        }
    }


    public List<String> getTrackables() {
        return trackables;
    }

    public String getExtraInfo() {
        return dir;
    }


    public boolean addTrackable(String newTrack) {

        if (!trackables.contains(newTrack)) {
            trackables.add(newTrack);
            return true;
        } else {
            System.out.println("Directory already exists: " + newTrack);
            return false;
        }
    }

    public boolean removeTrackable(String trackToRemove) {
        return (trackables.removeIf(trackToRemove::equals));
    }


    private void register(Path dir, Observer o) throws IOException {
        System.out.println("Registering a path " + dir.toString());

        WatchKey key = dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
        PathObservable pwo = null;
        if (this.trace) {
            pwo = keys.get(key);
            if (null != pwo) {
                pwo.addObserver(o);
                Path prev = keys.get(key).path;
                if (prev == null) {
                    System.out.format("register: %s\n", dir);
                } else {
                    if (!dir.equals(prev)) {
                        System.out.format("update: %s -> %s\n", prev, dir);
                    }
                }
            } else {
                pwo = new PathObservable(dir, false, o);
            }
        }
        keys.put(key, pwo);
    }

    private void registerAll(final Path start, Observer o) throws IOException {
        //  register directory and sub-directories
        System.out.println("Registering a path with subdirs " + start.toString());
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                register(dir, o);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    //Inner nested class,
    class PathObservable extends Observable {
        private Path path = null;
        private boolean recursive = false;

        PathObservable(Path p, boolean rec, Observer o) {
            super();
            path = p;
            recursive = rec;
            addObserver(o);
        }

        void setDirty() {
            setChanged();
        }

        Path getPath() {
            return path;
        }

         boolean isRecursive() {
            return recursive;
        }
    }
}