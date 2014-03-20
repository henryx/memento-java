/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@ymail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
 */
package org.memento.server.net;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 *
 * @author ebianchi
 */
public class Client implements AutoCloseable {

    private Integer port;
    private Socket socket;
    private String host;
    private String sslkey;
    private String sslpass;
    private boolean ssl;

    /**
     * @param port the port to set
     */
    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     * @param host the host to set
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @param ssl the ssl to set
     */
    public void setSSL(boolean ssl) {
        this.ssl = ssl;
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

    public Socket open() throws UnknownHostException, IOException {
        if (this.ssl) {
            System.setProperty("javax.net.ssl.trustStore", this.sslkey);
            System.setProperty("javax.net.ssl.trustStorePassword", this.sslpass);

            SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            this.socket = (SSLSocket) sslsocketfactory.createSocket(this.host, this.port);
        } else {
            this.socket = new Socket(this.host, this.port);
        }

        return this.socket;
    }

    @Override
    public void close() throws IOException {
        if (this.socket instanceof Socket) {
            this.socket.close();
        }
    }
}
