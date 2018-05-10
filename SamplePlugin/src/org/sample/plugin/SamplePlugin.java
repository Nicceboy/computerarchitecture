package org.sample.plugin;



import org.keyword.plugin.KeywordPlugin;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SamplePlugin implements KeywordPlugin {
    private final String name = "SamplePlugin";
    private final String desc = "This plugin is just for testing purposes.";
    private final String usage = "You can use this plugin to test program.";

    private List<KeywordTrackable> trackables = new ArrayList<>();

    @Override
    public void startPlugin() {
   // this.start();
//        currentThread().start();
        System.out.println("Sample Plugin started\n");
    }
    class observableThing extends Observable{
        private String happening;

        public observableThing(String string, Observer o) {
            super();
            this.happening = string;
            addObserver(o);
        }
        public void setDirty() {
            setChanged();
        }

    }

    @Override
    public void run() {
//        super.run();

        while (true) {
            try {
                Thread.sleep(60*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Hello world!\n");


        }
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

    public void addTrackables(List<String> trackables, String extraInfo, Observer observer) {
        if (!this.trackables.isEmpty()) {
            //Iterate through list to find existing object with same extra info
            for (KeywordTrackable sample : this.trackables){
                if(sample.getExtraInfo().equals(extraInfo)){
                    //Add missing trackables to object
                   for (String s : trackables){
                       sample.addTrackable(s);
                   }
                }
            }

        }else {
            this.trackables.add(new KeywordTrackSample(trackables, extraInfo, observer));
        }


    }

    public void removeTrackables(List<String> trackables, String extraInfo, Observer observer) {
        if (!this.trackables.isEmpty()) {
            //Iterate through list to find existing object with same extra info
            for (KeywordTrackable sample : this.trackables){
                if(sample.getExtraInfo().equals(extraInfo)){
                    //Add missing trackables to object
                    for (String s : trackables){
                        sample.removeTrackable(s);
                    }
                }
            }

        }else {
            this.trackables.add(new KeywordTrackSample(trackables, extraInfo, observer));
        }

    }

    //Have to create own class for each different type of extraInfo
    //For example different Twitter account, File path and so on.
    //Each type of info might have different content, what we are going to follow up
    class KeywordTrackSample implements KeywordPlugin.KeywordTrackable {



        private List<String> trackables = new ArrayList<String>();
        private String extraInfo;
        private Observer observer;

        public KeywordTrackSample (List<String> trackables, String extraInfo, Observer observer){
            this.trackables = new ArrayList<>(trackables);
            this.extraInfo = extraInfo;
            this.observer = observer;

        }

        public List<String> getTrackables() {
            return this.trackables;
        }

        public String getExtraInfo() {
            return this.extraInfo;
        }
        public boolean addTrackable(String newTrack){
            if (!trackables.contains(newTrack)) {
                trackables.add(newTrack);
                return true;
            }
            else {
                System.out.println("List contains already track: " + newTrack);
                return false;
            }

        }
        public boolean removeTrackable(String trackToRemove){
             return (trackables.removeIf(trackToRemove::equals));
        }
    }

     public class KeywordNotifySample implements KeywordPlugin.KeywordNotifyObject {

        public String getModuleName(){
            return SamplePlugin.this.getPluginName();
        }

        public String getModuleExtraInfo(){
            return "Sample module in secret place.";
        }

        public List<String> getTrackablesFound(){
            List<String> templist = new ArrayList<>();
            templist.add("Sample module found something from nowhere.");
            return templist;
        }
    }

//    public class FailedToDoPluginThing extends Exception {
//        public FailedToDoPluginThing() {
//            super();
//        }
//
//        public FailedToDoPluginThing(String message) {
//            super(message);
//        }
//
//        public FailedToDoPluginThing(String message, Throwable cause) {
//            super(message, cause);
//        }
//
//        public FailedToDoPluginThing(Throwable cause) {
//            super(cause);
//        }
//    }
}