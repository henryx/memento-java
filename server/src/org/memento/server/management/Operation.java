/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@gmail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
 */
package org.memento.server.management;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.memento.server.storage.DbStorage;
import org.memento.server.storage.FileStorage;

/**
 *
 * @author enrico
 */
public abstract class Operation implements Properties {

    public abstract void setDbStore(DbStorage dbstore);

    public abstract DbStorage getDbStore();

    public abstract void setFsStore(FileStorage fsstore);

    public abstract FileStorage getFsStore();

    public void execCommand(String command) throws IOException, InterruptedException {
        BufferedReader bre;
        Process p;
        String line;
        String retString;
        int retCode;

        p = Runtime.getRuntime().exec(command);
        retCode = p.waitFor();

        if (retCode != 0) {
            retString = "";
            bre = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            while ((line = bre.readLine()) != null) {
                retString = retString + line + "\n";
            }

            throw new IOException(retString);
        }
    }

    public abstract void run();
}
