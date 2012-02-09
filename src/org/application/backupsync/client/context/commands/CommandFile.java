/*
    Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@ymail.com)
    Project       BackupSYNC
    Description   A backup system
    License       GPL version 2 (see GPL.txt for details)
 */

package org.application.backupsync.client.context.commands;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import org.application.backupsync.PathName;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author enrico
 */
public class CommandFile {
    /*
     * TODO: write code for using FlexJSON library
     *       Example:
     * 
     *  FileAttrs f = new FileAttrs();
        FileAcl acl = new FileAcl();

        f.setPosixOwner("pippo");
        f.setType("file");
        f.setPosixAttrs("r--r--r--");
        
        acl.setName("pippo");
        acl.setAttrs("r--r--r--");
        acl.setAclType(FileAcl.OWNER);
        
        ArrayList<FileAcl> acls = new ArrayList<>();
        acls.add(acl);
        
        f.setAcl(acls);
        
        JSONSerializer serializer = new JSONSerializer();
        
        System.out.println(serializer.exclude("*.class").deepSerialize(f));
     * 
     */
    private JSONObject json;

    public CommandFile(String aDirectory, Boolean acl) throws JSONException, IOException {
        this.json = new JSONObject();
        this.json = this.list(aDirectory, acl);
    }

    private JSONObject list(String directory, Boolean acl) throws JSONException, FileNotFoundException, IOException {
        JSONObject result;
        JSONObject data;

        result = new JSONObject();
        for (PathName item : new PathName(Paths.get(directory)).walk()) {
            data = new JSONObject();

            if (item.isDirectory()) {
                data.append("type", "directory");
            } else {
                data.append("type", "file");
                try {
                    data.append("hash", item.hash());
                } catch (NoSuchAlgorithmException | IOException ex) {
                    data.append("hash", "");
                }
            }
            data.append("attrs", item.getAttrs());

            if (acl) {
                data.append("acl", item.getAcl());
            }
            result.append(item.getAbsolutePath(), data.toString());
        }
        return result;
    }

    public JSONObject get() {
        return this.json;
    }
}
