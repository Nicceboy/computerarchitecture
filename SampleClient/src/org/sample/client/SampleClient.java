package org.sample.client;
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


//Example implementation of client, which can follow contents from server regardless of types, what server supports

//Server is supporting plugins, and so is the protocol to Client

public class SampleClient extends Thread {

    //Module is one plugin from server. On handshake, supported plugins have been told to client
    //Each module has capability to look for certain targets. eg directory module can watch different directories or Twitter module can follow different Twitter accounts
    //Targets can be added and removed, and keycontents to follow for each target can be added or removed

    private class Module {
        private String ModuleName;
        private String ModuleDesc;
        private String ModuleUsage;
        private List<ModuleTarget> moduleTargets = new ArrayList<>();

        public Module(String Name, String Desc, String Usage){
            this.ModuleName = Name;
            this.ModuleDesc = Desc;
            this.ModuleUsage = Usage;

        }

        public String getModuleName() {
            return this.ModuleName;
        }
        public String getModuleDesc(){
            return this.ModuleDesc;
        }
        public String getModuleUsage(){
            return this.ModuleUsage;
        }
        public List<ModuleTarget> getModuleTargets (){
            return this.moduleTargets;
        }
        public void addTarget (ModuleTarget target){
            moduleTargets.add(target);
                    }
        public void removeTarget (ModuleTarget target){
            this.moduleTargets.removeIf(a -> a.getName().equals(target.getName()));
                   }
        }


    class ModuleTarget {
        private String targetName;
        private List<String> trackables = new ArrayList<>();

        public ModuleTarget(String Name){
            this.targetName = Name;
        }

        public String getName() {
            return this.targetName;
        }
        public void replaceTrackables(List<String> newTargets){
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

    public enum ClientState {EDetached, EConnecting, EConnected}

    ;

    private Socket clientSocket = null;
    private String serverAddr = null;

    private ClientState state = ClientState.EDetached;
    private boolean running = false;
    private String targetToWatch = null;
    private List<String> keywords = null;
    private DataInputStream istream = null;
    private DataOutputStream ostream = null;

    private List<Module> availableModules = new ArrayList<>();

    private static SampleClient instance = null;

    private BlockingQueue<String> sendQueue = new LinkedBlockingQueue<String>();

    public static SampleClient getInstance() {
        if (null == instance) {
            instance = new SampleClient();
        }
        return instance;
    }

    SampleClient() {

    }

    public void prepareClient() {
        running = true;
        state = ClientState.EDetached;
       // listener = o;

        keywords = new Vector<String>();
        start();
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

    public void addKeyword(String word) throws IOException, InterruptedException {
        if (keywords.add(word)) {
            List<String> tmp = new Vector<String>();
            tmp.add(word);
            sendListeningMsg(tmp);
        }
    }
    private void replaceTrackableDataToTargetInModule(String moduleName, String target, List<String> trackables){

        for (Module module : this.availableModules){
            if (module.getModuleName().equals(moduleName)){
                for (ModuleTarget ttarget :module.getModuleTargets()){
                    if (ttarget.getName().equals(target)){
                        ttarget.replaceTrackables(trackables);


                    }
                }
            }

        }
    }


    public void addKeywords(Set<String> words) throws IOException, InterruptedException {
        List<String> tmp = new Vector<String>();
        for (String w : words) {
            if (keywords.add(w)) {
                tmp.add(w);
            }
        }
        if (tmp.size() > 0) {
            sendListeningMsg(tmp);
        }

    }

    public void removeKeyword(String word) throws IOException, InterruptedException {

    }

    public void removeKeywords(Set<String> words) throws IOException, InterruptedException {

    }

    public List<String> keywords() {
        return keywords;
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
                                System.out.println("Read " + bytesToRead + " bytes");
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
                                        System.out.println("Data received: " + data);
                                        JSONObject root;
                                        // Parse the string to JSON object.


                                        root = (JSONObject) new JSONParser().parse(data);

                                        int response = (int) root.get("ResponseType"); // request/response
                                        String addinfo = (String) root.get("AdditionalInfo");
                                        JSONArray moduleList = (JSONArray) root.get("ModuleList");

                                        //Loop through modules, what server gave us. Enable new ones, if there are any
                                        //Get list of all trackables to this client in all modules and all targets in those modules
                                        if (response == 2) {
                                        for (Object module : moduleList) {

                                            JSONObject mod = (JSONObject) module;
                                            String moduleName = (String) mod.get("ModuleName");
                                            String moduleDesc = (String) mod.get("ModuleDesc");
                                            String moduleUsage = (String) mod.get("ModuleUsage");
                                            boolean addnew = true;
                                            for (Module OneModule : this.availableModules){
                                                if (OneModule.getModuleName().equals(moduleName)){
                                                    System.out.printf("Module %s already exists.\n", moduleName);
                                                    addnew = false;
                                                    break;
                                                }
                                            }

                                            if (addnew) {
                                                //Create new module if it does not exist
                                                this.availableModules.add(new Module(moduleName, moduleDesc, moduleUsage));
                                            }

                                            //Next let's see trackables per target from module

                                            JSONArray watchList = (JSONArray)mod.get("WatchList");

                                            for (Object watch : watchList){
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

                                            //TODO Trackable found



                                        }
                                        }

                                        // Get the required elements, response, text and file.
                                        JSONArray WordsForModule = (JSONArray) root.get("WordsForModule"); // request/response
                                        String notification = (String) root.get("text");
                                        String file = (String) root.get("file");
                                        // Notify the listener.

                                    }
                                }
                            } catch (SocketTimeoutException e) {
                                // Not an error, read times out and this gives us a change to send every now and then.
                            }
                            // Check if there is something to send in the send queue and send it.
                            while (!sendQueue.isEmpty()) {
                                System.out.println("Something to send...");
                                String message = sendQueue.take();
                                System.out.println("Message length in chars: " + message.length());
                                System.out.println("Message: " + message);
                                byte[] buf = new byte[(message.length() * 3) + 2];
                                ByteBuffer buffer = ByteBuffer.wrap(buf);
                                byte[] msg = message.getBytes(StandardCharsets.UTF_16);
                                short len = (short) msg.length;
                                System.out.println("Message length in bytes" + len);
                                buffer.putShort(len);
                                buffer.put(msg);
                                ostream.write(buf, 0, len + 2);
                                ostream.flush();
                            }
                        }
                        break;
                    case EConnecting:
                        // Server listens in port 10000.
                        System.out.println("Starting connection to the server..");
                        clientSocket = new Socket(serverAddr, 10000);
                        // Important to use timeouts when reading, this gives the thread a chance to send once in a while.
                        clientSocket.setSoTimeout(200);
                        istream = new DataInputStream(clientSocket.getInputStream());
                        ostream = new DataOutputStream(clientSocket.getOutputStream());
                        state = ClientState.EConnected;
                        System.out.print("Connected.\n");
                        sendListeningMsg(null);
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


    @SuppressWarnings("unchecked")
    private void sendListeningMsg(List<String> words) throws IOException, InterruptedException {
        // According to api
        //Command can be 1 or 2
        //
        //  1  Change trackables to be followed
        //  2  Get trackables in watch list

        if (state == ClientState.EConnected) {
            JSONObject root = new JSONObject();

            root.put("Command", "2");
//            JSONArray moduleWords = new JSONArray();
//            if (null == words) {
//                words = keywords;
//            }
//            if (null != words && words.size() > 0) {
//                JSONArray array = new JSONArray();
//                for (String keyword : words) {
//                    array.add(keyword);
//                }
//                root.put("keywords", array);
//            }
            sendQueue.put(root.toJSONString());
        }

    }


}

class runSample {

    public static void main(String[] args){

        Scanner reader = new Scanner(System.in);

        System.out.println("Initializing client...");
        SampleClient client = new SampleClient();
        client.prepareClient();
        System.out.println("Give address, where we should connect: \n");
        String address = reader.nextLine();
        System.out.println("Thank you! \n");

        client.attach(address);



    }
}