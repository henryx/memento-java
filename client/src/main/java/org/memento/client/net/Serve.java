/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@ymail.com)
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
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import org.memento.client.context.Context;
import org.memento.json.commands.CommandFile;
import org.memento.json.commands.CommandSystem;

/**
 *
 * @author enrico
 */
public class Serve implements AutoCloseable {

    private Integer port;
    private ServerSocket socket;
    private String address;
    private String sslkey;
    private String sslpass;
    private boolean ssl;

    public Serve(Integer port) {
        this.port = port;
        this.sslpass = "";
    }

    public boolean listen() throws UnknownHostException, IOException {
        Context context;
        boolean exit;
        BufferedReader in;
        HashMap errMsg;
        org.memento.json.Context inJSON;
        Socket connection;

        connection = this.socket.accept();

        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        context = new Context(connection);
        try {
            inJSON = new JSONDeserializer<org.memento.json.Context>().deserialize(in.readLine(), org.memento.json.Context.class);
            switch (inJSON.getContext()) {
                case "file":
                    exit = context.parseFile((CommandFile) inJSON.getCommand());
                    break;
                case "system":
                    exit = context.parseSystem((CommandSystem) inJSON.getCommand());
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
        } catch (SSLException ex) {
            errMsg = new HashMap();
            errMsg.put("message", "Non SSL Connection: " + ex.getMessage());
            exit = context.parseError(errMsg);
        }
        connection.close();

        return exit;
    }

    public void open() throws IOException {
        ServerSocketFactory factory;
        SSLServerSocket socketSSL;

        if (this.ssl) {
            System.setProperty("javax.net.ssl.keyStore", this.sslkey);
            System.setProperty("javax.net.ssl.keyStorePassword", this.sslpass);

            factory = SSLServerSocketFactory.getDefault();
            socketSSL = (SSLServerSocket) factory.createServerSocket();

            //socketSSL.setNeedClientAuth(true);
            if (this.address == null) {
                socketSSL.bind(new InetSocketAddress(this.port));
            } else {

                socketSSL.bind(new InetSocketAddress(this.address, this.port));
            }

            this.socket = socketSSL;
        } else {
            if (this.address == null) {
                this.socket = new ServerSocket(this.port);
            } else {
                this.socket = new ServerSocket();
                this.socket.bind(new InetSocketAddress(this.address, this.port));
            }
        }
    }

    @Override
    public void close() throws IOException {
        this.socket.close();
    }

    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * @param sslkey the sslkey to set
     */
    public void setSSLkey(String sslkey) {
        this.sslkey = sslkey;
    }

    /**
     * @param sslpass the sslpass to set
     */
    public void setSSLpass(String sslpass) {
        this.sslpass = sslpass;
    }

    /**
     * @return the ssl
     */
    public boolean isSSL() {
        return this.ssl;
    }

    /**
     * @param ssl the ssl to set
     */
    public void setSSL(boolean ssl) {
        this.ssl = ssl;
    }
}
