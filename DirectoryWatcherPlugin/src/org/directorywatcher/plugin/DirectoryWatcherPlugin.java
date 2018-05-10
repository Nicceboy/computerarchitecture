package org.directorywatcher.plugin;

import org.keyword.plugin.KeywordPlugin;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

// https://stackoverflow.com/questions/29827718/searching-and-counting-a-specific-word-in-a-text-file-java
// https://docs.oracle.com/javase/tutorial/essential/io/notification.html

public class DirectoryWatcherPlugin implements KeywordPlugin {

    //Plugin for KeywordServer to follow directories and find changes in files

    //It must implement two classes of KeywordPlugin interface
    private final Map<WatchKey, DirKeywordTrackable.PathObservable> keys;


    private final String name = "DirectoryWatcher Plugin";
    private final String desc = "You can watch for words you want in filepath selected by you.";
    private final String usage = "You must specify filepath as 'target' and keywords as 'trackables'. Filepath should be exactly in correct format. " +
            "Additionally if you want to track directories recursively, after target, add ',' and word 'recursive'. For example 'C:\\Mydir, recursive'";
    public AtomicBoolean isStopped = new AtomicBoolean(false);
    private WatchService watcher = null;
    private boolean running = false;
    private List<KeywordPlugin.KeywordTrackable> trackables = new ArrayList<>();

    //DirectoryWatcher plugin methods
    public DirectoryWatcherPlugin() throws IOException {
        watcher = FileSystems.getDefault().newWatchService();
        keys = new HashMap<WatchKey, DirKeywordTrackable.PathObservable>();
    }


    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    public String getPluginName() {
        return this.name;
    }

    public String getPluginDesc() {
        return this.desc;
    }

    public String getPluginUsage() {
        return this.usage;
    }

    public List<KeywordPlugin.KeywordTrackable> getAllTrackables() {

        return this.trackables;
    }

    @Override
    public void startPlugin() throws FailedToDoPluginThing {
        System.out.println("DirWatcher starting to process.");
    }

    @Override
    public void run() {
//        super.run();
        running = true;
        while (running) {
            try {
                processEvents();
            } catch (FailedToDoPluginThing failed) {
                failed.printStackTrace();
            }
        }
    }

    public void quit() {
        System.out.println("DirWatcher quitting...");
        running = false;
        //Way to stop thread, when it's implementing Runnable
        this.isStopped.set(false);
        try {
            System.out.println("Closing the watcher...");
            watcher.close();
        } catch (IOException e) {
            System.out.println("Watcher closed.");
        }
    }

    //Add watched directories
    public void addTrackables(List<String> keywords, String dir, Observer o) throws FailedToDoPluginThing {

        KeywordTrackable newTargetDir = new DirKeywordTrackable(keywords, dir, o, watcher, keys, this);


    }

    public void removeTrackables(List<String> trackables, String extraInfo, Observer observer) throws FailedToDoPluginThing {

    }

    public void removeWatchedDirectory(Path dir, Observer o) {

    }

    /**
     * Process all events for keys queued to the watcher
     */
    void processEvents() throws FailedToDoPluginThing {
        // wait for key to be signaled
        System.out.println("DirectoryWatcher running...");
        WatchKey key = null;
        try {
            key = watcher.take();

            if (key != null) {
                Path dir = keys.get(key).getPath();
                if (dir == null) {
                    System.err.println("WatchKey not recognized!!");
                    return;
                }
                boolean recursive = keys.get(key).isRecursive();

                for (WatchEvent<?> event : key.pollEvents()) {
                    Kind<?> kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    // Context for directory entry event is the file name of entry
                    WatchEvent<Path> ev = cast(event);
                    Path name = ev.context();
                    Path child = dir.resolve(name);

                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE
                            && !Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                        keys.get(key).setDirty();
                        keys.get(key).notifyObservers(new DirectoryEvent(Event.ECreated, child.toString()));
                        System.out.println("Ha, entry found : child.toString()");
                    } else if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                        keys.get(key).setDirty();
                        keys.get(key).notifyObservers(new DirectoryEvent(Event.EModified, child.toString()));
                        System.out.println("Ha, entry found : child.toString()");
                    }

                    // if directory is created, and watching recursively, then
                    // register it and its sub-directories
                    // TODO: register all observers to observe this new directory also.
//					if (recursive && (kind == StandardWatchEventKinds.ENTRY_CREATE)) {
//						try {
//							if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
//								registerAll(child, keys.get(key));
//							}
//						} catch (IOException x) {
//							// ignore to keep sample readable
//						}
//					}
                }

                // reset key and remove from set if directory no longer accessible
                boolean valid = key.reset();
                if (!valid) {
                    keys.remove(key);

                    // all directories are inaccessible
                    if (keys.isEmpty()) {
                        return;
                    }
                } // if !valid
            } // if key != null

        } catch (ClosedWatchServiceException | InterruptedException e) {
            running = false;
            throw new FailedToDoPluginThing(String.format("Error occured in plugin %s. Service closing...\n", getPluginName()));
        }
    }

    //DirectoryWatcherPlugin variables
    enum Event {
        ECreated, EModified
    }



    //   public void removeWatchedDirectories(Observer o) {
//        List<WatchKey> itemsToRemove = new ArrayList<WatchKey>();
//        for (Map.Entry<WatchKey,PathObservable> entry : keys.entrySet())
//        {
//            PathObservable current = entry.getValue();
//            current.deleteObserver(o);
//            if (current.countObservers() == 0) {
//                entry.getKey().cancel();
//                itemsToRemove.add(entry.getKey());
//            }
//        }
//        keys.entrySet().removeAll(itemsToRemove);
    //  }


    /**
     * Register the given directory, and all its sub-directories, with the
     * WatchService.
     */

    public class DirectoryEvent implements KeywordPlugin.KeywordNotifyObject {


        Event event;
        String fileName;

        DirectoryEvent(Event e, String file) {
            super();
            event = e;
            fileName = file;
        }

        public String getModuleName() {
            return DirectoryWatcherPlugin.this.getPluginName();
        }

        public String getModuleExtraInfo() {
            return "test";
        }

        public List<String> getTrackablesFound() {
            String[] array = {"Found This", "And this."};
            return Arrays.asList(array);
        }
    }


}
