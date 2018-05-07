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


public class KeywordSession extends Thread implements Observer {

	private Socket socket = null;
	private Set<String> keywords = null;
	private DirectoryWatcher watcher = null;
	private 	DataOutputStream out = null;
	private SessionManager manager = null;
	private int sessionId = 0;

	KeywordSession(Socket s, DirectoryWatcher w, SessionManager mgr, int session) {
		socket = s;
		keywords = new HashSet<String>();
		watcher = w;
		manager = mgr;
		sessionId = session;
	}


	public void run() {
		String data = "";
		String dir = "";
		
		try {
			DataInputStream inStream = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());

			byte [] messageByte = new byte[4096];

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

							int command = (int) mainJsonObj.get("Command");

							JSONArray wordsForModule = (JSONArray) mainJsonObj.get("WordsForModule");
                            List<String> trackablesToAdd = new ArrayList<>();
                            List<String> trackablesToRemove = new ArrayList<>();
                            List<Map<String, String>> modules = new ArrayList<>();

                            for (Object pairObj : wordsForModule) {
                                JSONObject trackableAndModule = (JSONObject) pairObj;

                                for (Object trackableAdd : (JSONArray) trackableAndModule.get("TrackablesToAdd")) {
                                    JSONObject trackable = (JSONObject) trackableAdd;
                                    trackablesToAdd.add(trackable.toString());
                                }

                                for (Object trackableRemove : (JSONArray) trackableAndModule.get("TrackablesToRemove")) {
                                    JSONObject trackable = (JSONObject) trackableRemove;
                                    trackablesToRemove.add(trackable.toString());
                                }

                                for (Object moduleObj : (JSONArray) trackableAndModule.get("Modules")) {
                                    JSONObject module = (JSONObject) moduleObj;
                                    Map<String, String> tempMap = new HashMap<>();

                                    tempMap.put("ModuleName", module.get("ModuleName").toString());
                                    tempMap.put("ExtraInfo", module.get("ExtraInfo").toString());
                                    modules.add(tempMap);
                                }
                            }

//							JSONObject root;
//
//							root = (JSONObject) new JSONParser().parse(data);
//
//							String command = root.get("command").toString(); // id of the operation, for async operations.
//							dir = (String) root.get("dir"); // request/response
//							Boolean recursive = (Boolean)root.get("recursive");
//							JSONArray words = (JSONArray)root.get("keywords");
//
//							if (command != null && command.equalsIgnoreCase("watch")) {
//								if (null != words) {
//									@SuppressWarnings("unchecked")
//									Iterator<String> iterator = words.iterator();
//									while (iterator.hasNext()) {
//										keywords.add(iterator.next());
//									}
//								}
//								if (dir != null) {
//									System.out.println(sessionId + ": Adding dir " + dir + " under watch");
//									watcher.addWatchedDirectory(FileSystems.getDefault().getPath(dir), recursive, this);
//								}
//							}
						}
					}

				} catch (ParseException e) {
					e.printStackTrace();
				} catch (NoSuchFileException nsf) {
					JSONObject toSend = createResponse("response", "Path does not exist", dir);
					sendResponse(toSend.toString());
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
			} catch (IOException e) {
			}
		}
		watcher.removeWatchedDirectories(this);
	}


	@Override
	public void update(Observable o, Object arg) {
		DirectoryEvent event = (DirectoryEvent)arg;

		System.out.println(sessionId + ": Change event happened in file system");
		Scanner s;
		try {
			s = new Scanner(new File(event.fileName));
			String words = "";
			boolean isFirst = true;
			while (s.hasNextLine()){
				String nextLine = s.nextLine();
				for (String word : keywords) {
					if (nextLine.toLowerCase().contains(word.toLowerCase())) {
						System.out.println(sessionId + ": Keyword " + word + " in file, adding to client notification msg.");
						if (!isFirst) {
							words += ",";
						}
						words += word;
						if (isFirst) isFirst = false;
					}
				}
			}
			s.close();
			// Send the line to the client.
			if (words.length() > 0) {
				System.out.println(sessionId + ": Creating response msg to client");
				JSONObject toSend = createResponse("response", words, event.fileName);						
				try {
					System.out.println(sessionId + ": Sending response msg to client");
					sendResponse(toSend.toString());
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println(sessionId + ": Could not send change event msg to client!");
				}
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	private JSONObject createResponse(String msgType, String msgText, String msgFile) {
		JSONObject toSend = new JSONObject();
		toSend.put("response", msgType);
		toSend.put("text", msgText);
		toSend.put("file", msgFile);
		return toSend;
	}
	
	private void sendResponse(String response) throws IOException {
		String data = response.toString();
		byte [] buf = new byte[(data.length()*3)+2];
		ByteBuffer buffer = ByteBuffer.wrap(buf);
		byte [] msg = data.getBytes(StandardCharsets.UTF_16);
		short len = (short)msg.length;
		System.out.println(sessionId + ": Message length in bytes" + len);
		buffer.putShort(len);
		buffer.put(msg);
		out.write(buf, 0, len+2);
		out.flush();	
	}
	
}

