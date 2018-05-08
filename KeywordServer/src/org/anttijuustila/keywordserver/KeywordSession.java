package org.anttijuustila.keywordserver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.NoSuchFileException;
import java.util.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;
import org.keskikettera.keywordplugin.KeywordPlugin;


public class KeywordSession extends Thread implements Observer {

    private Socket socket = null;
    private static Map<String, KeywordPlugin> plugins;
    private List<String> keywords = null;
    private DataOutputStream out = null;
    private SessionManager manager = null;
    private int sessionId = 0;

    KeywordSession(Socket s, SessionManager mgr, int session) {
        socket = s;
        keywords = new ArrayList();
        manager = mgr;
        sessionId = session;
    }

    static void setPlugins(Map<String, KeywordPlugin> plugins) {
        KeywordSession.plugins = plugins;
    }

    public void run() {
        String data = "";
        String dir = "";

        try {
            DataInputStream inStream = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            byte[] messageByte = new byte[4096];

            while (!interrupted() && socket != null && socket.isConnected()) {
                // Read data from socket
                System.out.println(sessionId + ": Start to receive data...");
                try {
                    dir = "";
                    messageByte[0] = inStream.readByte();
                    messageByte[1] = inStream.readByte();
                    ByteBuffer byteBuffer = ByteBuffer.wrap(messageByte, 0, 2);
                    int bytesToRead = byteBuffer.getShort();
                    System.out.println(sessionId + ": Read " + bytesToRead + " bytes");

                    if (bytesToRead > 0) {
                        int bytesRead = 0;
                        byteBuffer.clear();
                        while (bytesToRead > bytesRead) {
                            byteBuffer.put(inStream.readByte());
                            bytesRead++;
                        }
                        if (bytesRead == bytesToRead) {
                            data = new String(messageByte, 0, bytesRead, StandardCharsets.UTF_16);
                            System.out.println(sessionId + ": Data received: " + data);

                            JSONObject mainJsonObj = (JSONObject) new JSONParser().parse(data);

                            int command =  Integer.parseInt(mainJsonObj.get("Command").toString());

                            JSONArray wordsForModule = (JSONArray) mainJsonObj.get("WordsForModule");
                            if (command == 1) {
                                for (Object pairObj : wordsForModule) {

                                    List<String> trackablesToAdd = new ArrayList<>();
                                    List<String> trackablesToRemove = new ArrayList<>();
                                    List<Map<String, String>> modules = new ArrayList<>();

                                    JSONObject trackableAndModule = (JSONObject) pairObj;

                                    if (trackableAndModule.containsKey("TrackablesToAdd")) {
                                        for (Object trackableAdd : (JSONArray) trackableAndModule.get("TrackablesToAdd")) {
                                            JSONObject trackable = (JSONObject) trackableAdd;
                                            trackablesToAdd.add(trackable.toString());
                                        }
                                    }

                                    if (trackableAndModule.containsKey("TrackablesToRemove")) {
                                        for (Object trackableRemove : (JSONArray) trackableAndModule.get("TrackablesToRemove")) {
                                            JSONObject trackable = (JSONObject) trackableRemove;
                                            trackablesToRemove.add(trackable.toString());
                                        }
                                    }

                                    for (Object moduleObj : (JSONArray) trackableAndModule.get("Modules")) {
                                        JSONObject module = (JSONObject) moduleObj;
                                        Map<String, String> tempMap = new HashMap<>();

                                        tempMap.put("ModuleName", module.get("ModuleName").toString());
                                        tempMap.put("ExtraInfo", module.get("ExtraInfo").toString());
                                        modules.add(tempMap);
                                    }

                                    if (trackablesToAdd.size() > 0) {
                                        for (Map<String, String> module : modules) {
                                            KeywordPlugin kp = plugins.get(module.get("ModuleName"));
                                            kp.addTrackables(trackablesToAdd, module.get("ExtraInfo"), this);
                                        }
                                    }

                                    if (trackablesToRemove.size() > 0) {
                                        for (Map<String, String> module : modules) {
                                            KeywordPlugin kp = plugins.get(module.get("ModuleName"));
                                            kp.removeTrackables(trackablesToRemove, module.get("ExtraInfo"), this);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                } catch (KeywordPlugin.FailedToDoPluginThing e) {
//					JSONObject toSend = createResponse("response", "Path does not exist", dir);
//					sendResponse(toSend.toString());
                }

            } // while
        } catch (EOFException e1) {
            // Remove from Server, since connection was broken.
            System.out.println(sessionId + ": Session connection with client closed");
            manager.removeSession(this);
        } catch (IOException e1) {
            e1.printStackTrace();
            System.out.println(sessionId + ": Session: IOException in socket connection with client");
            // Remove from Server, since connection was broken.
            manager.removeSession(this);
        }

        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        socket = null;
        keywords.clear();
        keywords = null;
    }

    public void end() {
        System.out.println(sessionId + ": Session end called");
        if (null != socket) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }


    @Override
    public void update(Observable o, Object arg) {
        KeywordPlugin.KeywordNotifyObject event = (KeywordPlugin.KeywordNotifyObject) arg;

        System.out.println(sessionId + ": Change event happened in " + event.getModuleName());
        System.out.println(sessionId + ": " + event.getTrackablesFound() + " has been found in " + event.getModuleExtraInfo());

        this.createTrackableFoundResponse(event);
    }

    @SuppressWarnings("unchecked")
    private JSONArray createModuleList() {
        JSONArray finalModuleList = new JSONArray();

        for (KeywordPlugin plugin : plugins.values()) {
            JSONObject module = new JSONObject();
            JSONArray watchList = new JSONArray();

            List<KeywordPlugin.KeywordTrackable> pluginTrackables = plugin.getAllTrackables();
            for (KeywordPlugin.KeywordTrackable pluginTrackable : pluginTrackables) {
                boolean createNewEntry = true;
                if (watchList.size() > 0) {
                    for (Object watchObj : watchList) {
                        JSONObject watch = (JSONObject) watchObj;
                        if (pluginTrackable.getExtraInfo().equals(watch.get("ModuleTarget"))) {
                            JSONArray trackables = (JSONArray) watch.get("Trackables");
                            //trackables.add(pluginTrackable.getTrackable());
                            trackables.addAll(pluginTrackable.getTrackables());
                            createNewEntry = false;
                            break;
                        }
                    }
                }

                if (createNewEntry) {
                    JSONObject tempWatch = new JSONObject();
                    JSONArray tempTrackables = new JSONArray();

                   // tempTrackables.add(pluginTrackable.getTrackable());
                    tempTrackables.addAll(pluginTrackable.getTrackables());

                    tempWatch.put("ModuleTarget", pluginTrackable.getExtraInfo());
                    tempWatch.put("Trackables", tempTrackables);

                    watchList.add(tempWatch);
                }
            }

            module.put("ModuleName", plugin.getPluginName());
            module.put("ModuleDesc", plugin.getPluginDesc());
            module.put("ModuleUsage", plugin.getPluginUsage());
            module.put("WatchList", watchList);

            finalModuleList.add(module);
        }

        return finalModuleList;
    }

    @SuppressWarnings("unchecked")
    private JSONObject createChangeSucceedResponse() {
        JSONObject toSend = new JSONObject();
        toSend.put("ResponseType", 1);
        toSend.put("AdditionalInfo", "Change succeed");

        toSend.put("ModuleList", this.createModuleList());

        return toSend;
    }

    @SuppressWarnings("unchecked")
    private JSONObject createDetailedWatchListResponse() {
        JSONObject toSend = new JSONObject();
        toSend.put("ResponseType", 2);
        toSend.put("AdditionalInfo", "Detailed list of words in watch list");

        toSend.put("ModuleList", this.createModuleList());

        return toSend;
    }

    @SuppressWarnings("unchecked")
    private JSONObject createTrackableFoundResponse(final KeywordPlugin.KeywordNotifyObject notifyObject) {
        JSONObject toSend = new JSONObject();
        toSend.put("ResponseType", 3);
        toSend.put("AdditionalInfo", "Trackable found");

        toSend.put("ModuleList", this.createModuleList());

        JSONObject notificationContent = new JSONObject();
        notificationContent.put("ModuleName", notifyObject.getModuleName());
        notificationContent.put("ModuleTarget", notifyObject.getTrackablesFound());
        notificationContent.put("Trackables", notifyObject.getTrackablesFound());

        toSend.put("NotificationContent", notificationContent);

        return toSend;
    }

    private void sendResponse(String response) throws IOException {
        String data = response;
        byte[] buf = new byte[(data.length() * 3) + 2];
        ByteBuffer buffer = ByteBuffer.wrap(buf);
        byte[] msg = data.getBytes(StandardCharsets.UTF_16);
        short len = (short) msg.length;
        System.out.println(sessionId + ": Message length in bytes" + len);
        buffer.putShort(len);
        buffer.put(msg);
        out.write(buf, 0, len + 2);
        out.flush();
    }
}

