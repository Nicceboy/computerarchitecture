package org.sample.api;

import java.net.UnknownHostException;
import java.util.Scanner;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import static org.sample.api.SampleAPI.sendPurpose.*;



//Example implementation of api, which can follow contents from server regardless of types, what server supports

//Server is supporting plugins, and so is the protocol to Client







public class SampleAPI extends Thread {
    //This is example implementation of SampleAPI. It can be used as API for java based applications

    //Module is one plugin from server. On handshake, supported plugins have been told to api
    //Each module has capability to look for certain targets. eg directory module can watch different directories or Twitter module can follow different Twitter accounts
    //Targets can be added and removed, and keycontents to follow for each target can be added or removed

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

        public void removeTarget(ModuleTarget target) {
            this.moduleTargets.removeIf(a -> a.getName().equals(target.getName()));
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
            public void storeTempAddables(ArrayList<String> addables){
                this.temp_trackablesToAdd = addables;
            }
            public List<String> getTempAddables(){
                return this.temp_trackablesToAdd;
            }
            public List<String> getTempRemovables(){
                return this.temp_trackablesToRemove;
            }
            public void storeTempRemovables(ArrayList<String> removables){
                this.temp_trackablesToAdd = removables;
            }


            public String getName() {
                return this.targetName;
            }
            public boolean isThereChanges(){
                return this.isThereChanges;
            }
            public void resetStatus(){
                this.isThereChanges = false;
                this.temp_trackablesToAdd = new ArrayList<>();
                this.temp_trackablesToRemove = new ArrayList<>();
            }

            public void replaceTrackables(List<String> newTargets) {
                this.trackables = newTargets;
            }

            public List<String> getTrackables() {
                return this.trackables;
            }

            private boolean addTrackable(String trackableToAdd) {
                //TODO
                this.trackables.add(trackableToAdd);
                return true;
            }

            public void addTrackables(List<String> trackables) {

                for (String toAdd : trackables) {
                    if (addTrackable(toAdd)) {
                        System.out.printf("Word %s added. \n", toAdd);
                    }
                }


            }

            public void removeTrackables(List<String> trackables) {
                if (!this.trackables.isEmpty()) {
                    for (String toRemove : trackables) {
                        if (removeTrackable(toRemove)) {
                            System.out.printf("Word %s removed. \n", toRemove);
                        }
                    }

                } else {
                    System.out.println("Nothing left in trackables.");
                }

            }

            private boolean removeTrackable(String trackableToRemove) {
                return this.trackables.removeIf(trackableToRemove::equals);
            }
        }
    }





    public interface SampleAPIListener {
        void changeEventHappened(String response, String text, String file);


        void notify(String reason);
    }

    public enum ClientState {EDetached, EConnecting, EConnected}

    //Way to access main function methods.
    private SampleAPIListener instance;
    private volatile boolean isThreadReady = false;

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
        this.isThreadReady = true;
    }

    public void setThreadNotReady() {
        this.isThreadReady = false;
    }

    public boolean stateOfThread() {
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
                            try {
                                // Read message length from the first two bytes.
                                messageByte[0] = istream.readByte();
                                messageByte[1] = istream.readByte();
                                ByteBuffer byteBuffer = ByteBuffer.wrap(messageByte, 0, 2);
                                int bytesToRead = byteBuffer.getShort();
                                this.instance.notify("Read " + bytesToRead + " bytes");
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
                                        this.instance.notify("\n\n---- Data received ---");
                                        JSONObject root;
                                        // Parse the string to JSON object.

                                        root = (JSONObject) new JSONParser().parse(data);
                                        int response = Integer.parseInt(root.get("ResponseType").toString()); // request/response
                                        String addinfo = (String) root.get("AdditionalInfo");
                                        JSONArray moduleList = (JSONArray) root.get("ModuleList");
                                        this.instance.notify("ResponseType: " + response);
                                        this.instance.notify("Additional info: " + addinfo);


                                        //Loop through modules, what server gave us. Enable new ones, if there are any
                                        //Get list of all trackables to this api in all modules and all targets in those modules
                                        if (response == 2) {
                                            System.out.println("\n---List of modules, what server is supporting---\n");
                                            for (Object module : moduleList) {


                                                JSONObject mod = (JSONObject) module;
                                                String moduleName = (String) mod.get("ModuleName");
                                                String moduleDesc = (String) mod.get("ModuleDesc");
                                                String moduleUsage = (String) mod.get("ModuleUsage");


                                                boolean addnew = true;
                                                for (Module OneModule : this.availableModules) {
                                                    if (OneModule.getModuleName().equals(moduleName)) {
                                                        System.out.printf("Module %s already exists.\n", moduleName);
                                                        addnew = false;
                                                        break;
                                                    }
                                                }

                                                if (addnew) {
                                                    //Create new module if it does not exist
                                                    this.moduleIds += 1;
                                                    this.availableModules.add(new Module(moduleName, moduleDesc, moduleUsage, this.moduleIds));
                                                }

                                                this.instance.notify("Module name: " + moduleName);
                                                this.instance.notify("Module id: " + this.moduleIds);
                                                this.instance.notify("Module description: " + moduleDesc + "\n");
                                                //Next let's see trackables per target from module


                                                JSONArray watchList = (JSONArray) mod.get("WatchList");

                                                for (Object watch : watchList) {

                                                    List<String> currentTrackables = new ArrayList<>();

                                                    JSONObject watchItem = (JSONObject) watch;

                                                    String moduleTarget = (String) watchItem.get("ModuleTarget");
                                                    for (Object trackable_o : (JSONArray) watchItem.get("Trackables")) {
                                                        JSONObject trackable = (JSONObject) trackable_o;
                                                        currentTrackables.add(trackable.toString());
                                                    }
                                                    //Import trackables to module
                                                    //Let's suppose, that server is always correct. We replace lists based on what it gives.
                                                    replaceTrackableDataToTargetInModule(moduleName, moduleTarget, currentTrackables);

                                                }
                                                setThreadReady();


                                            }
                                        }

                                        if (response == 3) {
                                            //TODO Trackables found
                                            this.instance.notify("We got a hit!");
                                        }


                                    }
                                }
                            } catch (SocketTimeoutException e) {
                                // Not an error, read times out and this gives us a change to send every now and then.
                            }
                            // Check if there is something to send in the send queue and send it.
                            while (!sendQueue.isEmpty()) {
                                this.instance.notify("Something to send...");
                                String message = sendQueue.take();
                                this.instance.notify("Message length in chars: " + message.length());
                                this.instance.notify("Message: " + message);
                                byte[] buf = new byte[(message.length() * 3) + 2];
                                ByteBuffer buffer = ByteBuffer.wrap(buf);
                                byte[] msg = message.getBytes(StandardCharsets.UTF_16);
                                short len = (short) msg.length;
                                this.instance.notify("Message length in bytes" + len);
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
                        try{
                            clientSocket = new Socket(serverAddr, 10000);
                        }
                        catch (UnknownHostException e) {
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
                        sendListeningMsg(getStatus);
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
    public enum sendPurpose {ChangeTracks, getStatus }

    @SuppressWarnings("unchecked")
    private void sendListeningMsg(sendPurpose purpose) throws IOException, InterruptedException {
        //Method for asking server to add/remove data
        // According to api
        //Command can be 1 or 2
        //
        //  1  Change trackables to be followed
        //  2  Get trackables in watch list

        if (state == ClientState.EConnected) {
            JSONObject root = new JSONObject();
            if (purpose == getStatus){


                root.put("Command", "2");
            }
            if (purpose == ChangeTracks){
                //Add or removes trackable things on specific module and in specific target of module
                root.put("Command", "1");
                List<String> toAdd;
                List<String> toRemove;
                JSONArray finalModuleArray = new JSONArray();

                boolean anyChanges = false;

                for (Module module : this.availableModules){
                    JSONArray temp_list_addables = new JSONArray();
                    JSONArray temp_list_removables = new JSONArray();
                    JSONArray temp_TrackTargetPairs = new JSONArray();
                    JSONObject temp_ModuleObj = new JSONObject();
                    JSONObject temp_TargetObj = new JSONObject();
                    // Looping through modules
                    for (Module.ModuleTarget target : module.getModuleTargets()){
                        //Looping through targets

                        if (target.isThereChanges){
                            toAdd = target.getTempAddables();
                            toRemove =target.getTempRemovables();
                            temp_list_addables.addAll(toAdd);
                            temp_list_removables.addAll(toRemove);
                            temp_TargetObj.put("TrackablesToAdd", temp_list_addables);
                            temp_TargetObj.put("TrackablesToRemove", temp_list_removables);
                            temp_TargetObj.put("ExtraInfo", target.getName());
                            temp_TrackTargetPairs.add(temp_TargetObj);
                            target.resetStatus();
                            anyChanges = true;
                        }

                        //End of targets loop
                    }
                    if (anyChanges){
                    temp_ModuleObj.put("ModuleName", module.getModuleName());
                    temp_ModuleObj.put("TrackableAndTargetPair", temp_TrackTargetPairs);
                    finalModuleArray.add(temp_ModuleObj);
                    anyChanges = false;
                    }
                    //End of modules loop
                }
                //Goes null if no changes
                root.put("WordsForModule", finalModuleArray);

            }

            sendQueue.put(root.toJSONString());

        }

    }


}
