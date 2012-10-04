/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@gmail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
 */
package org.memento.client.context.commands;

import flexjson.JSONSerializer;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
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

    private String directory;
    private boolean acl;
    private PrintWriter writer;

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
     * @return the directory
     */
    public String getDirectory() {
        return this.directory;
    }

    /**
     * @param directory the directory to set
     */
    public void setDirectory(String directory) {
        this.directory = directory;
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
     * @return the writer
     */
    public PrintWriter getWriter() {
        return this.writer;
    }

    /**
     * @param writer the writer to set
     */
    public void setWriter(PrintWriter writer) {
        this.writer = writer;
    }

    public void walk(File path) throws IllegalArgumentException {
        File child;
        FileAttrs data;
        JSONSerializer serializer;
        Stack<File> stack;

        stack = new Stack<>();
        serializer = new JSONSerializer();

        stack.push(path);
        while (!stack.isEmpty()) {
            child = stack.pop();

            try {
                data = this.compute(new PathName(child));
                this.writer.println(serializer.deepSerialize(data));
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
}