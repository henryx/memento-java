/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@gmail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
*/

package org.memento.client.net;

import flexjson.JSONDeserializer;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import org.memento.client.context.AbstractContext;
import org.memento.client.context.ContextError;
import org.memento.client.context.ContextFile;
import org.memento.client.context.ContextSystem;

/**
 *
 * @author enrico
 */
public class Serve implements AutoCloseable {

    private Integer port;
    private ServerSocket socket;

    public Serve(Integer port) {
        this.port = port;
    }

    public Boolean listen() throws UnknownHostException, IOException {
        AbstractContext context;
        Boolean exit;
        BufferedReader in;
        HashMap errMsg;
        HashMap inJSON;
        Socket connection;

        connection = socket.accept();

        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        try {
            inJSON = new JSONDeserializer<HashMap>().deserialize(in.readLine());
            switch (inJSON.get("context").toString()) {
                case "file":
                    context = new ContextFile(connection);
                    exit = context.parse((HashMap) inJSON.get("command"));
                    break;
                case "system":
                    context = new ContextSystem(connection);
                    exit = context.parse((HashMap) inJSON.get("command"));
                    break;
                default:
                    context = new ContextError(connection);
                    errMsg = new HashMap();
                    errMsg.put("message", "Command not found");
                    exit = context.parse(errMsg);
                    break;
            }
        } catch (FileNotFoundException | IllegalArgumentException | NullPointerException ex) {
            context = new ContextError(connection);
            errMsg = new HashMap();

            if (ex instanceof FileNotFoundException
                    || ex instanceof IllegalArgumentException) {
                errMsg.put("message", "Context not found");
            } else if (ex instanceof NullPointerException) {
                errMsg.put("message", "Buffer error");
            } else {
                errMsg.put("message", "Malformed command");
            }
            exit = context.parse(errMsg);
        }
        connection.close();

        return exit;
    }

    public void open() throws IOException {
        this.socket = new ServerSocket(port);
    }

    @Override
    public void close() throws IOException {
        this.socket.close();
    }
}
