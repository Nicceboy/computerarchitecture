package org.anttijuustila.keywordserver;

import java.io.*;

public class PluginClassLoader extends ClassLoader {
    File directory;

    public PluginClassLoader (File dir) {
        directory = dir;
    }

    public Class loadClass (String name) throws ClassNotFoundException {
        return loadClass(name, true);
    }

    public Class loadClass (String classname, boolean resolve) throws ClassNotFoundException {
        try {
            Class c = findLoadedClass(classname);

            if (c == null) {
                try {
                    c = findSystemClass(classname);
                } catch (Exception ignored) {

                }
            }

            if (c == null) {
                String filename = classname.replace('.', File.separatorChar) + ".class";
                File f = new File(directory, filename);

                int length = (int) f.length();
                byte[] classBytes = new byte[length];
                DataInputStream in = new DataInputStream(new FileInputStream(f));
                in.readFully(classBytes);
                in.close();
            }

            if (resolve) {
                resolveClass(c);
            }

            return c;
        } catch (Exception e) {
            throw new ClassNotFoundException(e.toString());
        }
    }
}
