/*
    Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@ymail.com)
    Project       BackupSYNC
    Description   A backup system
    License       GPL version 2 (see GPL.txt for details)
 */

package org.application.backupsync.client.context;

import flexjson.JSONSerializer;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;

/**
 *
 * @author enrico
 */

public class ContextError extends AbstractContext {
    public ContextError(Socket connection) {
        this.connection = connection;
    }

    @Override
    public Boolean parse(HashMap command) throws IOException {
        Boolean exit;
        HashMap result;
        JSONSerializer serializer;
        PrintWriter out;

        out = new PrintWriter(connection.getOutputStream(), true);
        result = command;
        exit = Boolean.FALSE;
        serializer = new JSONSerializer();

        result.put("result", "error");
        out.println(serializer.exclude("*.class").serialize(result));

        return exit;
    }
}
