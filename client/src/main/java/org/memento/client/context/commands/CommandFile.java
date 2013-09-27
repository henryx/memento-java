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
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;

/**
 *
 * @author enrico
 */
public class CommandFile {

    private boolean acl;
    private String fileName;
    private Socket connection;
    private boolean compressed;

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
     * @return the fileName
     */
    public String getFileName() {
        return this.fileName;
    }

    /**
     * @param fileName the object to set
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * @return the compressed
     */
    public boolean isCompressed() {
        return compressed;
    }

    /**
     * @param compressed the compressed to set
     */
    public void setCompressed(boolean compressed) {
        this.compressed = compressed;
    }

    public void walk() throws IllegalArgumentException, IOException {
        File child;
        File parent;
        FileAttrs data;
        JSONSerializer serializer;
        PrintWriter writer;
        Stack<File> stack;

        parent = new File(this.fileName);
        stack = new Stack<>();
        serializer = new JSONSerializer();
        writer = new PrintWriter(this.connection.getOutputStream(), true);

        stack.push(parent);
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
        File fileProcessed;
        byte[] buffer;
        int read;

        fileProcessed = null;
        buffer = new byte[8192];

        try {
            if (this.isCompressed()) {
                fileProcessed = File.createTempFile(new File(this.fileName).getName(), ".compressed");
                try (FileInputStream in = new FileInputStream(this.fileName);
                        XZOutputStream out = new XZOutputStream(new FileOutputStream(fileProcessed), new LZMA2Options())) {

                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }

                    out.finish();
                }

            } else {
                fileProcessed = new File(this.fileName);
            }

            try (FileInputStream fis = new FileInputStream(fileProcessed);
                    BufferedInputStream buff = new BufferedInputStream(fis);
                    BufferedOutputStream outStream = new BufferedOutputStream(this.connection.getOutputStream());) {

                while ((read = buff.read(buffer)) != -1) {
                    outStream.write(buffer, 0, read);
                    outStream.flush();
                }
            }
        } finally {
            if (this.isCompressed()) {
                if (fileProcessed instanceof File) {
                    fileProcessed.delete();
                }
            }
        }
    }

    public void receiveFile() throws FileNotFoundException, IOException {
        byte[] buf = new byte[8192];
        int bytesRead = 0;

        try (InputStream in = this.connection.getInputStream();
                FileOutputStream outFile = new FileOutputStream(new File(this.fileName));) {

            while ((bytesRead = in.read(buf, 0, buf.length)) != -1) {
                outFile.write(buf, 0, bytesRead);
            }
            outFile.flush();
        }
    }
}