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

// https://stackoverflow.com/questions/29827718/searching-and-counting-a-specific-word-in-a-text-file-java
// https://docs.oracle.com/javase/tutorial/essential/io/notification.html

public class DirectoryWatcherPlugin extends Thread implements KeywordPlugin {

    enum Event { ECreated, EModified }

    private WatchService watcher = null;
    private final Map<WatchKey,PathObservable> keys;
    private boolean trace = true;
    private boolean running = false;
    private final String name = "DirectoryWatcher Plugin";
    private final String desc = "You can watch for words you want in filepath selected by you.";
    private final String usage = "You must specify filepath as 'target' and keywords as 'trackables'. Filepath should be exactly in correct format. " +
            "Additionally if you want to track directories recursively, after target, add ',' and word 'recursive'. For example 'C:\\Mydir, recursive'";

    private List<KeywordPlugin.KeywordTrackable> trackables = new ArrayList<>();

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
    public void startPlugin() {


        try {
            super.run();
            running = true;
            System.out.println("DirWatcher starting to process.");
            while (running) {
                processEvents();
            }
        }
        catch (FailedToDoPluginThing failed){
            System.out.println(failed);
        }
    }

//    @Override
//    public void run(){
//        super.run();
//        running = true;
//        System.out.println("DirWatcher starting to process.");
//        while (running) {
//            processEvents();
//        }
//    }

    public void quit() {
        System.out.println("DirWatcher quitting...");
        running = false;
        this.interrupt();
        try {
            System.out.println("Closing the watcher...");
            watcher.close();
        } catch (IOException e) {
            System.out.println("Watcher closed.");
        }
    }


    public class PathObservable extends Observable {
        public Path path = null;
        public boolean recursive = false;
        public PathObservable(Path p, boolean rec, Observer o) {
            super();
            path = p;
            recursive = rec;
            addObserver(o);
        }
        public void setDirty() {
            setChanged();
        }
    }


    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>)event;
    }

    /**
     * Creates a WatchService and registers the given directory
     */
    public DirectoryWatcherPlugin() throws IOException {
        watcher = FileSystems.getDefault().newWatchService();
        keys = new HashMap<WatchKey,PathObservable>();
    }
    //Add watched directories
    public void addTrackables(List<String> keywords, String dir, Observer o)  {

        Path newPath = FileSystems.getDefault().getPath(dir);
        try {
            register(newPath, o);
        } catch (IOException e) {
            e.printStackTrace();
        }
      //  new KeywordTrackable()


//        if (recursive) {
//            System.out.format("Scanning %s ...\n", dir);
//            registerAll(dir, o);
//            System.out.println("Done.");
//        } else {
//            register(dir, o);
//        }
        // enable trace after initial registration
        this.trace = true;
    }
    public void removeTrackables(List<String> trackables, String extraInfo, Observer observer) throws FailedToDoPluginThing {

    }

    public void removeWatchedDirectory(Path dir, Observer o) {

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


    void register(Path dir, Observer o) throws IOException {
        System.out.println("Registering a path " + dir.toString());
        WatchKey key = dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
        PathObservable pwo = null;
        if (trace) {
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

    /**
     * Register the given directory, and all its sub-directories, with the
     * WatchService.
     */
  //  private void registerAll(final Path start, Observer o) throws IOException {
        // register directory and sub-directories
//        System.out.println("Registering a path with subdirs " + start.toString());
//        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
//            @Override
//            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
//                    throws IOException
//            {
//                register(dir, o);
//                return FileVisitResult.CONTINUE;
//            }
//        });
   // }

    /**
     * Process all events for keys queued to the watcher
     */
    void processEvents() throws FailedToDoPluginThing{
        // wait for key to be signaled
        WatchKey key = null;
        try {
            key = watcher.take();

            if (key != null) {
                Path dir = keys.get(key).path;
                if (dir == null) {
                    System.err.println("WatchKey not recognized!!");
                    return;
                }
                boolean recursive = keys.get(key).recursive;

                for (WatchEvent<?> event: key.pollEvents()) {
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
                    } else if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                        keys.get(key).setDirty();
                        keys.get(key).notifyObservers(new DirectoryEvent(Event.EModified, child.toString()));
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
    public class KeywordTrackable implements KeywordPlugin.KeywordTrackable{

        private List<String> trackables = new ArrayList<String>();
        private String extraInfo;

        public List<String> getTrackables(){
            return trackables;
        }

        public String getExtraInfo(){
            return extraInfo;
        }


        public boolean addTrackable(String newTrack){
            return true;
        }
        public boolean removeTrackable(String trackToRemove){
            return true;
        }
    }

    public class DirectoryEvent implements KeywordPlugin.KeywordNotifyObject{



        Event event;
        String fileName;
        DirectoryEvent(Event e, String file) {
            super();
            event = e;
            fileName = file;
        }
        public String getModuleName(){
            return DirectoryWatcherPlugin.this.getPluginName();
        }

        public String getModuleExtraInfo() {
            return "test";
        }

        public List<String> getTrackablesFound(){
            String [] array = {"Found This", "And this."};
            return Arrays.asList(array);
        }
    }


}
