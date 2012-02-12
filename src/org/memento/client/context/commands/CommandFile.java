/*
    Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@ymail.com)
    Project       BackupSYNC
    Description   A backup system
    License       GPL version 2 (see GPL.txt for details)
 */

package org.memento.client.context.commands;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import org.memento.PathName;
import org.memento.json.FileAttrs;

/**
 *
 * @author enrico
 */
public class CommandFile {
    private HashMap<String, FileAttrs> json;

    public CommandFile(String aDirectory, Boolean acl) throws IOException {
        this.json = this.list(aDirectory, acl);
    }

    private HashMap<String, FileAttrs> list(String directory, Boolean acl) throws FileNotFoundException, IOException {
        FileAttrs data;
        HashMap<String, FileAttrs> result;

        result = new HashMap<>();
        for (PathName item : new PathName(Paths.get(directory)).walk()) {
            data = item.getAttrs();

            if (item.isDirectory()) {
                data.setType("directory");
            } else {
                data.setType("file");
                try {
                    data.setHash(item.hash());
                } catch (NoSuchAlgorithmException | IOException ex) {
                    data.setHash("");
                }
            }

            if (acl) {
                data.setAcl(item.getAcl());
            }
            result.put(item.getAbsolutePath(), data);
        }
        return result;
    }

    public HashMap<String, FileAttrs> get() {
        return this.json;
    }
}
