package org.anttijuustila.keywordserver;

import org.keskikettera.keywordplugin.KeywordPlugin;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.lang.reflect.*;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.stream.Collectors;


public class KeywordServer extends Thread implements SessionManager {

    private static boolean running = true;
    private static ServerSocket socket = null;

    private static Map<String, KeywordPlugin> plugins = new HashMap<>();
    private static Vector<KeywordSession> sessions = null;

    public static void main(String[] args) {
        System.out.println("\n\n ****** Starting keyword server... ****** \n\n ");
        KeywordServer server = new KeywordServer();
        server.start();
        System.out.println("Keyword server started. ");
        Scanner keyboard = new Scanner(System.in);

        String command = "";
        do {
            System.out.println("To quit the server, write \"quit\" and hit enter > ");
            command = keyboard.nextLine();
        } while (!command.equalsIgnoreCase("quit"));
        System.out.println("Preparing to quit server... ");

        server.quit();
        keyboard.close();
        System.out.println("<<<< Exiting the server <<<<< \n\n");
    }

    private void quit() {
        System.out.println("Server's quit called");
        running = false;
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Server's quit finished");
    }

    public void run() {
        try {
            // register directory and process its events
            System.out.println("Getting all plugins...");
            this.getPlugins();
            KeywordSession.setPlugins(plugins);
            System.out.println("Creating server socket...");
            socket = new ServerSocket(10000);
            sessions = new Vector<KeywordSession>();
            int sessionCount = 0;
            System.out.println("Entering server accept connections loop...");
            while (running) {
                System.out.println("Accepting socket connections...");
                Socket s = socket.accept();
                System.out.println(" ** New connection created, added to sessions...");
                KeywordSession session = new KeywordSession(s, this, ++sessionCount);
                session.start();
                sessions.add(session);
                System.out.println("Session count: " + sessions.size());
            }
        } catch (IOException e) {
            System.out.println("Server accept socket closed.");
        }
        System.out.println("Cleaning server sessions while closing down...");
        sessions.forEach(session -> session.end());
    }


    @Override
    public void removeSession(KeywordSession toRemove) {
        sessions.remove(toRemove);
        toRemove.end();
        toRemove = null;
        System.out.println("Session count after remove session: " + sessions.size());
    }

    public void getPlugins() {
        File workingDir = new File(System.getProperty("user.dir"));
        File plugDir = new File(workingDir + File.separator + "plugins");
        if (plugDir.exists() && plugDir.isDirectory()) {
            String[] files = plugDir.list();
            assert files != null;
            for (String file : files) {
                try {
                    if (!file.endsWith(".jar")) {
                        continue;
                    }
                    File pathToJar = new File(plugDir + File.separator + file);
                    System.out.printf("Trying to load plugin from path: %s\n", pathToJar);
                    JarFile jarFile = new JarFile(pathToJar);
                    Enumeration<JarEntry> e = jarFile.entries();

                    URL[] urls = {new URL("jar:file:" + pathToJar + "!/"), new URL("jar:file:" + workingDir + File.separator + "KeywordPlugin.jar" + "!/")};
                    ClassLoader cl = URLClassLoader.newInstance(urls,
                            getClass().getClassLoader());

                    while (e.hasMoreElements()) {
                        JarEntry je = e.nextElement();
                        if (je.isDirectory() || !je.getName().endsWith(".class")) {
                            continue;
                        }
                        // -6 because of .class
                        String className = je.getName().substring(0, je.getName().length() - 6);
                        className = className.replace('/', '.');
                        Class c = cl.loadClass(className);
                        Class[] intf = c.getInterfaces();
                        for (Class anIntf : intf) {

                            if (anIntf.getSimpleName().equalsIgnoreCase("KeywordPlugin")) {
                                KeywordPlugin kp = (KeywordPlugin) c.newInstance();
                                System.out.println(kp.getPluginName() + " loaded successfully.");
                                plugins.put(kp.getPluginName(), kp);
                            }
                        }

                    }


                } catch (IOException | ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
