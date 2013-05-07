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
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import org.memento.client.context.Context;

/**
 *
 * @author enrico
 */
public class Serve implements AutoCloseable {

    private Integer port;
    private ServerSocket socket;
    private String address;

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

        connection = this.socket.accept();

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
        } catch (ClassCastException | FileNotFoundException | JSONException | IllegalArgumentException | NullPointerException ex) {
            errMsg = new HashMap();

            if (ex instanceof FileNotFoundException
                    || ex instanceof IllegalArgumentException) {
                errMsg.put("message", "Context not found: " + ex.getMessage());
            } else if (ex instanceof JSONException) {
                errMsg.put("message", "JSON value error: " + ex.getMessage());
            } else if (ex instanceof NullPointerException) {
                errMsg.put("message", "Buffer error: " + ex.getMessage());
            } else {
                errMsg.put("message", "Malformed command: " + ex.getMessage());
            }
            exit = context.parseError(errMsg);
        }
        connection.close();

        return exit;
    }

    public void open() throws IOException {
        if (this.address == null) {
            this.socket = new ServerSocket(this.port);
        } else {
            this.socket = new ServerSocket();
            this.socket.bind(new InetSocketAddress(this.address, this.port));
        }
    }
    
    public void open(boolean ssl) throws IOException {
        ServerSocketFactory factory;
        SSLServerSocket socketSSL;
        
        factory = SSLServerSocketFactory.getDefault();
        
        if (this.address == null) {
            socketSSL = (SSLServerSocket)factory.createServerSocket(this.port);
            socketSSL.setNeedClientAuth(true);
        } else {
            socketSSL = (SSLServerSocket)factory.createServerSocket();
            socketSSL.bind(new InetSocketAddress(this.address, this.port));
            socketSSL.setNeedClientAuth(true);
        }
        
        this.socket = socketSSL;
    }

    @Override
    public void close() throws IOException {
        this.socket.close();
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
