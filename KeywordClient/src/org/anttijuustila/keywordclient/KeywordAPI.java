package org.anttijuustila.keywordclient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;



public class KeywordAPI extends Thread {

	public interface KeywordAPIListener {
		void changeEventHappened(String response, String text, String file);
		void notify(String reason);
	}
	public enum ClientState { EDetached, EConnecting, EConnected };
	
	public class KeywordAPIException extends Exception {
		public KeywordAPIException(String string) {
			super(string);
		}
		private static final long serialVersionUID = 1L;
		
	}
	private KeywordAPIListener listener = null;
	private Socket clientSocket = null;
	private String serverAddr = null;
	
	private ClientState state = ClientState.EDetached;
	private boolean running = false;
	private String targetToWatch = null;
	private List<String> keywords = null;
	private DataInputStream istream = null;
	private DataOutputStream ostream = null;

	private static KeywordAPI instance = null;
	
	private BlockingQueue<String> sendQueue = new LinkedBlockingQueue<String>();
	
	public static KeywordAPI getInstance() {
		if (null == instance) {
			instance = new KeywordAPI();
		}
		return instance;
	} 
	
	private KeywordAPI() {
	}
	
	public void prepareClient(KeywordAPIListener o) throws KeywordAPIException {
		running = true;
		state = ClientState.EDetached;
		listener = o;
		if (null == listener) throw new KeywordAPIException("Keyword client must have a listener");
		keywords = new Vector<String>();
		start();		
	}

	public ClientState myState() {
		return state;
	}
	
	public void attach(String url, String target) throws KeywordAPIException {
		if (null != serverAddr && !url.equalsIgnoreCase(serverAddr)) {
			detach();
		}
		if (null == url || null == target)
			throw new KeywordAPIException("Must specify the server address and target path to watch.");
		serverAddr = url;
		targetToWatch = target;
		state = ClientState.EConnecting;
	}

	public void addKeyword(String word) throws IOException, InterruptedException {
		if (keywords.add(word)) {
			List<String> tmp = new Vector<String>();
			tmp.add(word);
			sendListeningMsg(tmp);			
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

	public void removeKeyword(String word) throws IOException, InterruptedException  {

	}
	
	public void removeKeywords(Set<String> words) throws IOException, InterruptedException  {
		
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

		byte [] messageByte = new byte[4096];
		
		while (running) {
			try {
				switch (state) {
				case EDetached:
				{
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
									// Get the required elements, response, text and file.
									String response = (String) root.get("response"); // request/response
									String notification = (String)root.get("text");
									String file = (String)root.get("file");
									// Notify the listener.
									listener.changeEventHappened(response, notification, file);
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
							byte [] buf = new byte[(message.length()*3)+2];
							ByteBuffer buffer = ByteBuffer.wrap(buf);
							byte [] msg = message.getBytes(StandardCharsets.UTF_16);
							short len = (short)msg.length;
							System.out.println("Message length in bytes" + len);
							buffer.putShort(len);
							buffer.put(msg);
							ostream.write(buf, 0, len+2);
							ostream.flush();
						}
					}
					break;
				case EConnecting:
					// Server listens in port 10000.
					clientSocket = new Socket(serverAddr, 10000);
					// Important to use timeouts when reading, this gives the thread a chance to send once in a while.
					clientSocket.setSoTimeout(200);
					istream = new DataInputStream(clientSocket.getInputStream());
					ostream = new DataOutputStream(clientSocket.getOutputStream());
					state = ClientState.EConnected;
					listener.notify("Connected");
					sendListeningMsg(null);
					break;
				default:
					break;
				}
			} catch (EOFException | SocketException e) {
				detach();
				listener.notify("Connection closed");
			} catch (IOException | InterruptedException ioe) {
				ioe.printStackTrace();
				detach();
				listener.notify("Connection closed");
			} catch (ParseException e) {
				e.printStackTrace();
				listener.notify("Invalid message from remote Watcher");
			}
		} // while (running)
	}


	
	@SuppressWarnings("unchecked")
	private void sendListeningMsg(List<String> words) throws IOException, InterruptedException {
		if (state == ClientState.EConnected) {
			JSONObject root = new JSONObject();
			
			root.put("command", "watch");
			root.put("dir", targetToWatch);
			root.put("recursive", false);
			if (null == words)
			{
				words = keywords;
			}
			if (null != words && words.size() > 0) {
				JSONArray array = new JSONArray();
				for (String keyword : words) {
					array.add(keyword);
				}
				root.put("keywords", array);
			}
			sendQueue.put(root.toJSONString());
		}
		
	}
	

}
