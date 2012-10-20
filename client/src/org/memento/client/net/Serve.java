/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@gmail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
*/

package org.memento.client.net;

import flexjson.JSONDeserializer;
import flexjson.JSONException;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import org.memento.client.context.Context;

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

    public boolean listen() throws UnknownHostException, IOException {
        Context context;
        boolean exit;
        BufferedReader in;
        HashMap errMsg;
        HashMap inJSON;
        Socket connection;

        connection = socket.accept();

        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        context = new Context(connection);
        try {
            inJSON = new JSONDeserializer<HashMap>().deserialize(in.readLine());
            switch (inJSON.get("context").toString()) {
                case "file":
                    exit = context.parseFile((HashMap) inJSON.get("command"));
                    break;
                case "system":
                    exit = context.parseSystem((HashMap) inJSON.get("command"));
                    break;
                default:
                    errMsg = new HashMap();
                    errMsg.put("message", "Command not found");
                    exit = context.parseError(errMsg);
                    break;
            }
        } catch (FileNotFoundException | JSONException | IllegalArgumentException | NullPointerException ex) {
            errMsg = new HashMap();

            if (ex instanceof FileNotFoundException
                    || ex instanceof IllegalArgumentException) {
                errMsg.put("message", "Context not found");
            } else if (ex instanceof JSONException) {
                errMsg.put("message", "JSON value error");
            } else if (ex instanceof NullPointerException) {
                errMsg.put("message", "Buffer error");
            } else {
                errMsg.put("message", "Malformed command");
            }
            exit = context.parseError(errMsg);
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
