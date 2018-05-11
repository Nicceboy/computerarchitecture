package org.directorywatcher.plugin;

import org.keyword.plugin.KeywordPlugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

// https://stackoverflow.com/questions/29827718/searching-and-counting-a-specific-word-in-a-text-file-java
// https://docs.oracle.com/javase/tutorial/essential/io/notification.html

public class DirectoryWatcherPlugin implements KeywordPlugin {

    //Plugin for KeywordServer to follow directories and find changes in files


    private final Map<WatchKey, DirectoryObject> keys;
    private final String name = "DirectoryWatcher Plugin";
    private final String desc = "You can watch for words you want in filepath selected by you.";
    private final String usage = "You must specify filepath as 'target' and keywords as 'trackables'. Filepath should be exactly in correct format. " +
            "Additionally if you want to track directories recursively, after target, add ',' and word 'recursive'. For example 'C:\\Mydir, recursive'";
    //Atomic needed to stop thread manually from inside
    public AtomicBoolean isStopped = new AtomicBoolean(false);
    private boolean trace = true;
    private WatchService watcher = null;
    private boolean running = false;


    //DirectoryWatcher plugin methods
    public DirectoryWatcherPlugin() throws IOException {
        watcher = FileSystems.getDefault().newWatchService();
        keys = new HashMap<WatchKey, DirectoryObject>();
    }


    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    @Override
    public String getPluginName() {
        return this.name;
    }

    @Override
    public String getPluginDesc() {
        return this.desc;
    }

    @Override
    public String getPluginUsage() {
        return this.usage;
    }

    //In this case, we get list of all directories, and what we are tracking on each directory
    public List<KeywordPlugin.KeywordTrackable> getAllTrackables() {
        List<KeywordPlugin.KeywordTrackable> trackables = new ArrayList<>();
        //Create proper list from map, which implements our API
        for (WatchKey trackKey : this.keys.keySet()) {
            //Let's not add objects which are mapped as recursive objects
            DirectoryObject temp = this.keys.get(trackKey);
            if(temp.isMasterPath()) {
                trackables.add(temp);

            }
        }
        return trackables;
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
    @Override
    public void addTrackables(List<String> keywords, String dir, Observer o) throws FailedToDoPluginThing {

        for (WatchKey dirKey : this.keys.keySet()) {
            DirectoryObject temp = this.keys.get(dirKey);
            //Masterpaths are wanted targets
            if(temp.isMasterPath()) {
                if (this.keys.get(dirKey).getExtraInfo().equals(dir)) {
                    for (String keyword : keywords) {
                        this.keys.get(dirKey).addTrackable(keyword);
                    }
                    return;
                }
            }

        }
        DirectoryObject newTargetDir = new DirectoryObject(keywords, dir, o, this);
        //Master path mark in case of recursive including
        newTargetDir.setMasterPath();
        try {

            if ( newTargetDir.isRecursive()) {
                System.out.format("Scanning %s ...\n", dir);
                registerAll(newTargetDir);
                System.out.println("Done.");
            } else {
                register(newTargetDir);
            }
        } catch (IOException e) {
            throw new FailedToDoPluginThing("Incorrect path: " + e);
        }

    }

    private void register(DirectoryObject directoryObject) throws IOException {
        System.out.println("Registering a path " + directoryObject.getPath().toString());
        WatchKey key = directoryObject.getPath().register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);

        if(this.keys.containsKey(key)) {
            System.out.format("Path registered already : %s\n", directoryObject.getPath().toString());
            return;}
        directoryObject.addObserver(directoryObject.getObserver());

        this.keys.put(key, directoryObject);
    }

    private void registerAll(final DirectoryObject directoryObject) throws IOException {
        //  register directory and sub-directories
        //Master path must be registered, new object already created in recursive
        register(directoryObject);
        DirectoryWatcherPlugin self = this;
        System.out.println("Registering a path with subdirs " + directoryObject.getPath().toString());
        Files.walkFileTree(directoryObject.getPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {

                try {
                    register(new DirectoryObject(dir, directoryObject, self));
                } catch (IOException e) {
                    throw  new IOException("Something went wrong when adding recursively in path; " + dir.toString());
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Override
    public void removeTrackables(List<String> trackables, String extraInfo, Observer observer) throws FailedToDoPluginThing {
        List<WatchKey> itemsToRemove = new ArrayList<WatchKey>();
        for (Map.Entry<WatchKey,DirectoryObject> entry : keys.entrySet())
        {
            if(keys.get(entry).getExtraInfo().equals(extraInfo)){
                keys.get(entry).removeTrackable(trackables);

            }  if (keys.get(entry).getTrackables().isEmpty()){
                    removeTarget(observer);
        }

        }


    }
    void removeTarget(Observer observer){
        List<WatchKey> itemsToRemove = new ArrayList<WatchKey>();
        for (Map.Entry<WatchKey,DirectoryObject> entry : keys.entrySet())
        {
            DirectoryObject current = entry.getValue();
            current.deleteObserver(observer);
            if (current.countObservers() == 0) {
                entry.getKey().cancel();
                itemsToRemove.add(entry.getKey());
            }
        }
        keys.entrySet().removeAll(itemsToRemove);
    }

    public void removeWatchedDirectory(Path dir, Observer o) {

    }

    /**
     * Process all events for keys queued to the watcher
     */
    void processEvents() throws FailedToDoPluginThing {
        // wait for key to be signaled
        System.out.println("DirectoryWatcher running...");
        DirectoryEvent matchFound;
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
                        matchFound = checkforKeywords(new DirectoryEvent(Event.ECreated, child.toString(), this), key);
                        if (matchFound != null) {
                            keys.get(key).notifyObservers(matchFound);
                        }
                    } else if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                        keys.get(key).setDirty();
                        matchFound = checkforKeywords(new DirectoryEvent(Event.EModified, child.toString(), this), key);
                        if (matchFound != null) {
                            keys.get(key).notifyObservers(matchFound);
                        }
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
    
    DirectoryEvent checkforKeywords(DirectoryEvent object, WatchKey key) throws FailedToDoPluginThing{


        Scanner s;
        try {
            s = new Scanner(new File(object.getModuleExtraInfo()));
            ArrayList <String> matches = new ArrayList<String>();
            while (s.hasNextLine()){
                String nextLine = s.nextLine();
                for (String word : this.keys.get(key).getTrackables()) {
                    if (nextLine.toLowerCase().contains(word.toLowerCase())) {
                        System.out.println("Keyword " + word + " in file, adding to client notification msg.");
                      matches.add(word);
                    }
                }
            }
            s.close();
            // Send the line to the client.
            if(matches.isEmpty()){
                return null;
            }
            object.addkeywords(matches);
            return object;

        } catch (FileNotFoundException e1) {
            throw new FailedToDoPluginThing("File disappeared during scanning or something else mysterious happened.");
        }

    }



}
