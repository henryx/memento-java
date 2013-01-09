/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@gmail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
 */
package org.memento.client.context.commands;

import flexjson.JSONSerializer;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.memento.PathName;
import org.memento.json.FileAttrs;

/**
 *
 * @author enrico
 */
public class CommandFile {

    private boolean acl;
    private File aFile;
    private Socket connection;

    public CommandFile(Socket connection) {
        this.connection = connection;
    }

    private FileAttrs compute(PathName aPath) throws IllegalArgumentException, FileNotFoundException, IOException {
        FileAttrs result;

        result = aPath.getAttrs();

        result.setName(aPath.getAbsolutePath());
        result.setOs(System.getProperty("os.name").toLowerCase());

        if (aPath.isDirectory()) {
            result.setType("directory");
        } else if (aPath.isSymlink()) {
            result.setType("symlink");
        } else {
            result.setType("file");
            try {
                result.setHash(aPath.hash());
            } catch (NoSuchAlgorithmException | IOException ex) {
                result.setHash("");
            }
        }

        if (this.acl) {
            result.setAcl(aPath.getAcl());
        }

        return result;
    }

    /**
     * @return the acl
     */
    public boolean getAcl() {
        return this.acl;
    }

    /**
     * @param acl the acl to set
     */
    public void setAcl(boolean acl) {
        this.acl = acl;
    }

    /**
     * @return the aFile
     */
    public File getFile() {
        return this.aFile;
    }

    /**
     * @param aFile the object to set
     */
    public void setFile(File aFile) {
        this.aFile = aFile;
    }

    public void walk() throws IllegalArgumentException, IOException {
        File child;
        FileAttrs data;
        JSONSerializer serializer;
        PrintWriter writer;
        Stack<File> stack;

        stack = new Stack<>();
        serializer = new JSONSerializer();
        writer = new PrintWriter(this.connection.getOutputStream(), true);

        stack.push(this.aFile);
        while (!stack.isEmpty()) {
            child = stack.pop();

            try {
                data = this.compute(new PathName(child));
                writer.println(serializer.deepSerialize(data));
            } catch (FileNotFoundException ex) {
                Logger.getLogger(CommandFile.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(CommandFile.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (child.isDirectory()) {
                for (File f : child.listFiles()) {
                    stack.push(f);
                }
            }
        }
    }

    public void sendFile() throws FileNotFoundException, IOException {
        byte[] buffer;
        int read;

        buffer = new byte[8192];

        try (FileInputStream fis = new FileInputStream(this.aFile);
                BufferedInputStream buff = new BufferedInputStream(fis);
                BufferedOutputStream outStream = new BufferedOutputStream(this.connection.getOutputStream());) {

            while ((read = buff.read(buffer)) != -1) {
                outStream.write(buffer, 0, read);
                outStream.flush();
            }
        }
    }

    public void receiveFile() throws FileNotFoundException, IOException {
        byte[] buf = new byte[8192];
        int bytesRead = 0;

        try (InputStream in = this.connection.getInputStream();
                PrintWriter out = new PrintWriter(this.connection.getOutputStream(), true);
                FileOutputStream outFile = new FileOutputStream(this.aFile);) {

            while ((bytesRead = in.read(buf, 0, buf.length)) != -1) {
                outFile.write(buf, 0, bytesRead);
            }
            out.flush();
        }
    }
}