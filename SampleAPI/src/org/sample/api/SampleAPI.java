package org.sample.api;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.sample.api.SampleAPI.sendPurpose.AddOrRemoveTracks;
import static org.sample.api.SampleAPI.sendPurpose.init;


//Example implementation of API in Java, which can follow contents from server regardless of modules/plugins, what server supports


public class SampleAPI extends Thread {
    //This is example implementation of SampleAPI. It can be used as API for java based applications

    //Module is one plugin from server. On handshake, supported plugins have been told to api
    //Each module has capability to look for certain targets. eg directory module can watch different directories or Twitter module can follow different Twitter accounts
    //Targets can be added and removed, and keycontents to follow for each target can be added or removed

    public boolean deBugginEnabled = false;
    //Use methods of implementing class
    private SampleAPIListener instance;
    private volatile AtomicBoolean isThreadReady = new AtomicBoolean(false);
    private Socket clientSocket = null;
    private String serverAddr = null;
    private ClientState state = ClientState.EDetached;
    private boolean running = false;
    private String targetToWatch = null;
    private List<String> keywords = null;
    private DataInputStream istream = null;
    private DataOutputStream ostream = null;
    private List<Module> availableModules = new ArrayList<>();
    private int moduleIds = 0;
    private BlockingQueue<String> sendQueue = new LinkedBlockingQueue<String>();

    public SampleAPI(SampleAPIListener sample) {
        this.instance = sample;

    }

    public List<Module> getModules() {
        return this.availableModules;
    }

    public void prepareClient() {
        running = true;
        state = ClientState.EDetached;

        start();
    }

    public void setThreadReady() {
        this.isThreadReady = new AtomicBoolean(true);
    }

    public void setThreadNotReady() {
        this.isThreadReady = new AtomicBoolean(false);
    }

    public AtomicBoolean stateOfThread() {
        return this.isThreadReady;
    }

    public ClientState myState() {
        return state;
    }

    public void attach(String url) {
        if (null != serverAddr && !url.equalsIgnoreCase(serverAddr)) {
            detach();
        }

        serverAddr = url;
        //targetToWatch = target;
        state = ClientState.EConnecting;
    }

    private void replaceTrackableDataToTargetInModule(String moduleName, String target, List<String> trackables) {

        for (Module module : this.availableModules) {
            if (module.getModuleName().equals(moduleName)) {
                for (Module.ModuleTarget ttarget : module.getModuleTargets()) {
                    //    this.instance.notify("Replacing target" + target + " with " + trackables);
                    if (ttarget.getName().equals(target)) {

                        ttarget.replaceTrackables(trackables);


                    }
                }
            }

        }
    }

    public void detach() {
        if (null != clientSocket) {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                clientSocket = null;
                istream = null;
                ostream = null;
                serverAddr = null;
                targetToWatch = null;
            }
        }
        state = ClientState.EDetached;
    }

    @Override
    public void run() {
        super.run();

        byte[] messageByte = new byte[4096];

        while (running) {
            try {
                switch (state) {
                    case EDetached: {
                        Thread.sleep(200);
                        break;
                    }
                    case EConnected:
                        if (null != clientSocket && clientSocket.isConnected()) {
                            Thread.sleep(1);
                            try {
                                // Read message length from the first two bytes.
                                messageByte[0] = istream.readByte();
                                messageByte[1] = istream.readByte();
                                ByteBuffer byteBuffer = ByteBuffer.wrap(messageByte, 0, 2);
                                int bytesToRead = byteBuffer.getShort();

                                //With stringbuilder we can collect all things to notify for one call
                                StringBuilder notifyMessage = new StringBuilder();
                                if (deBugginEnabled) {
                                    notifyMessage.append(String.format("Read %s bytes\n", bytesToRead));
                                }
                                // If there are bytes to read, start reading.
                                if (bytesToRead > 0) {
                                    int bytesRead = 0;
                                    byteBuffer.clear();
                                    // Read bytes until required amount has been read.
                                    while (bytesToRead > bytesRead) {
                                        byteBuffer.put(istream.readByte());
                                        bytesRead++;
                                    }
                                    if (bytesRead == bytesToRead) {

                                        // Convert bytes to String, remembering that data is sent & received as UTF-16.
                                        String data = new String(messageByte, 0, bytesRead, StandardCharsets.UTF_16);
                                        notifyMessage.append("\n---- Data received ---\n");
                                        // System.out.println("Data received: " + data);
                                        JSONObject root;

                                        // Parse the string to JSON object.
                                        root = (JSONObject) new JSONParser().parse(data);


                                        int response = Integer.parseInt(root.get("ResponseType").toString()); // request/response
                                        String addinfo = (String) root.get("AdditionalInfo");
                                        JSONArray moduleList = (JSONArray) root.get("ModuleList");
                                        if (deBugginEnabled) {
                                            notifyMessage.append(String.format("ResponseType: %s\n", response));
                                            notifyMessage.append(String.format("Additional info: %s\n", addinfo));
                                        }
                                        switch (response) {

                                            case 1: {
                                                parseModules(moduleList, notifyMessage, false);
                                                this.instance.notify(notifyMessage.toString());
                                                setThreadReady();
                                                break;
                                            }


                                            //Loop through modules, what server gave us. Enable new ones, if there are any
                                            //Get list of all trackables to this api in all modules and all targets in those modules
                                            case 2: {

                                                if (moduleList.isEmpty()) {
                                                    notifyMessage.append("Server is not currently supporting any modules...\n");
                                                    this.instance.notify(notifyMessage.toString());
                                                    setThreadReady();
                                                } else {
                                                    notifyMessage.append(String.format("%-45s\n", " ").replace(' ', '-'));
                                                    parseModules(moduleList, notifyMessage, true);
                                                    this.instance.notify(notifyMessage.toString());
                                                    setThreadReady();
                                                }
                                                break;


                                            }

                                            case 3: {
                                                List<String> foundThings = new ArrayList<>();
                                                String moduleName;
                                                String moduleTarget = null;
                                                JSONObject notifyContent = (JSONObject) root.get("NotificationContent");
                                                if (notifyContent.containsKey("ModuleName")) {
                                                    moduleName = (String) notifyContent.get("ModuleName");
                                                    if (notifyContent.containsKey("ModuleTarget")) {
                                                        moduleTarget = (String) notifyContent.get("ModuleTarget");
                                                    }

                                                    if (notifyContent.containsKey("Trackables")) {
                                                        for (Object trackable : (JSONArray) notifyContent.get("Trackables")) {
                                                            String hit = (String) trackable;
                                                            foundThings.add(hit);
                                                        }
                                                    }
                                                    notifyMessage.append("\nWe got a hit! Following trackables have been found in following contex: \n");
                                                    notifyMessage.append(String.format("Module name: %s\n", moduleName));
                                                    notifyMessage.append(String.format("Target name: %s\n", moduleTarget));
                                                    for (String trackable : foundThings) {
                                                        notifyMessage.append(String.format(trackable + ", "));
                                                    }
                                                    this.instance.notify(notifyMessage.toString());


                                                } else {
                                                    notifyMessage.append("\nWe got a notification, but data was invalid... \n");
                                                }
                                                setThreadReady();
                                                break;


                                            }
                                            case 4: {
                                                notifyMessage.append("\nThere was an error on server side.. \n");
                                                notifyMessage.append(String.format("Error message: %s \n", root.get("AdditionalInfo").toString()));
                                                initToServerState();
                                                this.instance.notify(notifyMessage.toString());
                                                setThreadReady();
                                                break;
                                            }
                                            default: {
                                                setThreadReady();
                                                break;
                                            }
                                        }


                                    }
                                }
                            } catch (SocketTimeoutException e) {
                                // Not an error, read times out and this gives us a change to send every now and then.
                            }
                            // Check if there is something to send in the send queue and send it.
                            while (!sendQueue.isEmpty()) {

                                String message = sendQueue.take();
                                byte[] buf = new byte[(message.length() * 3) + 2];
                                ByteBuffer buffer = ByteBuffer.wrap(buf);
                                byte[] msg = message.getBytes(StandardCharsets.UTF_16);
                                short len = (short) msg.length;
                                if (deBugginEnabled) {
                                    this.instance.notify("Something to send...");
                                    this.instance.notify("Message length in chars: " + message.length());
                                    this.instance.notify("Message: " + message);
                                    this.instance.notify("Message length in bytes" + len);
                                }
                                buffer.putShort(len);
                                buffer.put(msg);
                                ostream.write(buf, 0, len + 2);
                                ostream.flush();
                            }
                        }
                        break;
                    case EConnecting:
                        // Server listens in port 10000.
                        this.instance.notify("Starting connection to the server..");
                        try {
                            clientSocket = new Socket(serverAddr, 10000);
                        } catch (UnknownHostException e) {
                            this.instance.notify("Requested address not found.");
                            this.state = ClientState.EDetached;
                            break;
                        }
                        // Important to use timeouts when reading, this gives the thread a chance to send once in a while.
                        clientSocket.setSoTimeout(200);
                        istream = new DataInputStream(clientSocket.getInputStream());
                        ostream = new DataOutputStream(clientSocket.getOutputStream());
                        state = ClientState.EConnected;
                        this.instance.notify("Connected.\n");
                        sendListeningMsg(init);
                        break;
                    default:
                        break;
                }
            } catch (EOFException | SocketException e) {
                detach();

            } catch (IOException | InterruptedException ioe) {
                ioe.printStackTrace();
                detach();

            } catch (ParseException e) {
                e.printStackTrace();

            }
        } // while (running)
    }

    private void parseModules(JSONArray moduleList, StringBuilder notifyMessage, Boolean enableNotify) {

        if (enableNotify) {
            notifyMessage.append("\nList of modules, what server is supporting\n");
        }

        for (Object module : moduleList) {


            JSONObject mod = (JSONObject) module;
            String moduleName = (String) mod.get("ModuleName");
            String moduleDesc = (String) mod.get("ModuleDesc");
            String moduleUsage = (String) mod.get("ModuleUsage");


            boolean addnew = true;
            for (Module OneModule : this.availableModules) {
                if (OneModule.getModuleName().equals(moduleName)) {
                    addnew = false;
                    break;
                }
            }
            int moduleId = this.moduleIds;
            if (addnew) {
                //Create new module if it does not exist
                this.moduleIds += 1;
                moduleId = this.moduleIds;
                this.availableModules.add(new Module(moduleName, moduleDesc, moduleUsage, moduleId));
            } else {
                for (Module moduleWithId : this.availableModules) {
                    if (moduleWithId.getModuleName().equals(moduleName))
                        moduleId = moduleWithId.getId();
                }
            }
            if (enableNotify) {

                notifyMessage.append(String.format("%-45s\n", " ").replace(' ', '-'));
                notifyMessage.append(String.format("Module name: %s\n", moduleName));
                notifyMessage.append(String.format("Module id: %d\n", moduleId));
                notifyMessage.append(String.format("Module description: %s\n", moduleDesc));
                notifyMessage.append(String.format("%-45s\n", " ").replace(' ', '-'));
            }
            //Next let's see trackables per target from module


            JSONArray watchList = (JSONArray) mod.get("WatchList");
            if (watchList.isEmpty()) {
                for (Module moduleWhereTargetsRemoved : this.availableModules) {
                    moduleWhereTargetsRemoved.removeAllTargets();
                }

            } else {
                boolean RemoveTarget = true;
                Iterator<Module> avaiModIter = this.availableModules.iterator();


                //remove all targets which not in new list -> keeps only targets what we get from server
                //have to use iterators because of concurrency problems
                while (avaiModIter.hasNext()) {
                    if (avaiModIter.next().getModuleName().equals(moduleName)) {
                        Iterator<Module.ModuleTarget> avaiTarget = avaiModIter.next().getModuleTargets().iterator();
                        while (avaiTarget.hasNext()) {
                            for (Object watch : watchList) {

                                JSONObject watchItem = (JSONObject) watch;
                                String moduleTarget = (String) watchItem.get("ModuleTarget");
                                if (avaiTarget.next().getName().equals(moduleTarget)) {
                                    RemoveTarget = false;
                                }
                            }
                            if (RemoveTarget) {
                                avaiTarget.remove();
                            }


                        }
                    }


                }
            }

            for (Object watch : watchList) {

                List<String> currentTrackables = new ArrayList<>();

                JSONObject watchItem = (JSONObject) watch;

                String moduleTarget = (String) watchItem.get("ModuleTarget");
                for (Object trackable_o : (JSONArray) watchItem.get("Trackables")) {
                    String trackable = (String) trackable_o;
                    currentTrackables.add(trackable);
                }
                boolean targetNotExist = false;
                for (Module tmod : this.availableModules) {
                    if (tmod.getModuleName().equals(moduleName)) {
                        for (Module.ModuleTarget ttarget : tmod.getModuleTargets()) {
                            if (ttarget.getName().equals(moduleTarget)) {
                                replaceTrackableDataToTargetInModule(moduleName, moduleTarget, currentTrackables);
                                setThreadReady();
                                return;
                            }
                        }
                        tmod.addTarget(moduleTarget, currentTrackables);
                    }
                }
                //Import trackables to module
                //Let's suppose, that server is always correct. We replace lists based on what it gives.


            }


        }
    }

    //Starts process of adding/removing selected trackables on selected targets on selected modules on Server
    public void commitChanges() throws InterruptedException {
        setThreadNotReady();
        sendListeningMsg(AddOrRemoveTracks);
    }

    //Reset client to state of server
    public void initToServerState() throws InterruptedException {
        setThreadNotReady();
        sendListeningMsg(init);
    }

    @SuppressWarnings("unchecked")
    private JSONObject createResponse(sendPurpose purpose) {
        JSONObject root = new JSONObject();


        //Add or removes trackable things on specific module and in specific target of module
        root.put("Command", purpose.getValue());
        if (purpose == init) {
            root.put("WordsForModule", null);
            return root;
        }
        List<String> toAdd;
        List<String> toRemove;
        JSONArray finalModuleArray = new JSONArray();

        boolean anyChanges = false;
        //Check all modules for changes
        for (Module module : this.availableModules) {

            JSONArray temp_list_addables = new JSONArray();
            JSONArray temp_list_removables = new JSONArray();
            JSONArray temp_TrackTargetPairs = new JSONArray();
            JSONObject temp_ModuleObj = new JSONObject();
            JSONObject temp_TargetObj = new JSONObject();

            for (Module.ModuleTarget target : module.getModuleTargets()) {
                assert false;
                //Looping through targets

                if (target.isThereChanges()) {
                    toAdd = target.getTempAddables();
                    toRemove = target.getTempRemovables();
                    temp_list_addables.addAll(toAdd);
                    temp_list_removables.addAll(toRemove);
                    temp_TargetObj.put("TrackablesToAdd", temp_list_addables);
                    temp_TargetObj.put("TrackablesToRemove", temp_list_removables);
                    temp_TargetObj.put("ExtraInfo", target.getName());
                    temp_TrackTargetPairs.add(temp_TargetObj);
                    //resetStatus applies changes to target
                    target.resetStatus();
                    anyChanges = true;
                }

                //End of targets loop
            }
            if (anyChanges) {
                temp_ModuleObj.put("ModuleName", module.getModuleName());
                temp_ModuleObj.put("TrackableAndTargetPair", temp_TrackTargetPairs);
                finalModuleArray.add(temp_ModuleObj);
                anyChanges = false;
            }
            //End of modules loop
        }
        //Goes null if no changes
        root.put("WordsForModule", finalModuleArray);


        return root;

    }

    private void sendListeningMsg(sendPurpose purpose) throws InterruptedException {
        //Method for asking server to add/remove data
        // According to api
        //Command can be 1 or 2
        //
        //  1  or init, Change trackables to be followed
        //  2  or AddOrRemoveTracks Get trackables in watch list

        if (state == ClientState.EConnected) {
            sendQueue.put(createResponse(purpose).toJSONString());
        } else {
            this.instance.notify("Unable to send data. Connection lost.\n");
            detach();
            System.exit(0);
        }

    }

    public enum ClientState {EDetached, EConnecting, EConnected}

    public enum sendPurpose {
        //Typing command numbers to Server for something easier to understand
        AddOrRemoveTracks(1), init(2);
        private final int id;

        sendPurpose(int id) {
            this.id = id;
        }

        public int getValue() {
            return id;
        }
    }

    public interface SampleAPIListener {
        void changeEventHappened(String response, String text, String file);


        void notify(String reason);
    }

    public class Module {
        private int id;
        private String ModuleName;
        private String ModuleDesc;
        private String ModuleUsage;
        private List<ModuleTarget> moduleTargets = new ArrayList<>();

        public Module(String Name, String Desc, String Usage, int id) {
            this.ModuleName = Name;
            this.ModuleDesc = Desc;
            this.ModuleUsage = Usage;
            this.id = id;

        }

        public String getModuleName() {
            return this.ModuleName;
        }

        public int getId() {
            return this.id;
        }

        public String getModuleDesc() {
            return this.ModuleDesc;
        }

        public String getModuleUsage() {
            return this.ModuleUsage;
        }

        public List<ModuleTarget> getModuleTargets() {
            return this.moduleTargets;
        }

        public void addTarget(ModuleTarget target) {
            moduleTargets.add(target);
        }
        public void addTarget(String targetName){moduleTargets.add(new ModuleTarget(targetName));}

        public void addTarget(String target, List<String> trackables) {

            ModuleTarget tmp = new ModuleTarget(target);
            tmp.addTrackables(trackables);
            moduleTargets.add(tmp);
        }

        public void removeTarget(ModuleTarget target) {
            this.moduleTargets.removeIf(a -> a.getName().equals(target.getName()));
        }

        void removeAllTargets() {
            this.moduleTargets = new ArrayList<>();
        }


        public class ModuleTarget {


            private String targetName;
            private List<String> trackables = new ArrayList<>();

            private boolean isThereChanges = false;
            private List<String> temp_trackablesToAdd = new ArrayList<>();
            private List<String> temp_trackablesToRemove = new ArrayList<>();


            public ModuleTarget(String Name) {
                this.targetName = Name;
            }

            public void storeTempAddables(ArrayList<String> addables) {
                this.temp_trackablesToAdd = addables;
            }

            public void storeTempRemovables(ArrayList<String> removables) {
                this.temp_trackablesToRemove = removables;
            }

            List<String> getTempAddables() {
                return this.temp_trackablesToAdd;
            }

            List<String> getTempRemovables() {
                return this.temp_trackablesToRemove;
            }


            public String getName() {
                return this.targetName;
            }

            boolean isThereChanges() {
                return this.isThereChanges;
            }

            public void newChanges() {
                this.isThereChanges = true;
            }

            void resetStatus() {

                this.addTrackables(temp_trackablesToAdd);
                this.removeTrackables(temp_trackablesToRemove);

                this.isThereChanges = false;
                this.temp_trackablesToAdd = new ArrayList<>();
                this.temp_trackablesToRemove = new ArrayList<>();
            }

            void replaceTrackables(List<String> newTargets) {
                this.trackables = newTargets;
            }


            public List<String> getTrackables() {
                return this.trackables;
            }

            private boolean addTrackable(String trackableToAdd) {
                //return true if added, false if already exists
                return !this.trackables.contains(trackableToAdd) && this.trackables.add(trackableToAdd);
                //  return true;
            }

            private void addTrackables(List<String> trackables) {

                for (String toAdd : trackables) {
                    if (addTrackable(toAdd)) {
                        SampleAPI.this.instance.notify(String.format("Word %s added in target %s.", toAdd, this.targetName));
                    }
                }


            }

            private boolean removeTrackable(String trackableToRemove) {
                return this.trackables.removeIf(trackableToRemove::equals);
            }

            void removeTrackables(List<String> trackables) {
                if (!this.trackables.isEmpty()) {
                    for (String toRemove : trackables) {
                        if (removeTrackable(toRemove)) {
                            SampleAPI.this.instance.notify(String.format("Word %s removed in target %s.", toRemove, this.targetName));
                        }
                    }

                } else {
                    SampleAPI.this.instance.notify("Nothing left in trackables.");
                }

            }


        }
    }


}
