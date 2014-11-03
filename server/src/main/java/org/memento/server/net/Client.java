/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@ymail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
 */
package org.memento.server.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.ini4j.Wini;

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

    public Client(String section, Wini cfg) {
        this.host = cfg.get(section, "host");
        this.port = Integer.parseInt(cfg.get(section, "port"));
        this.ssl = Boolean.parseBoolean(cfg.get(section, "ssl"));
        this.sslkey = cfg.get(section, "sslkey");
        this.sslpass = cfg.get(section, "sslpass");
    }

    private Socket open() throws UnknownHostException, IOException, SocketTimeoutException {
        if (this.isSSL()) {
            System.setProperty("javax.net.ssl.trustStore", this.sslkey);
            System.setProperty("javax.net.ssl.trustStorePassword", this.sslpass);

            SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            this.socket = (SSLSocket) sslsocketfactory.createSocket();
        } else {
            this.socket = new Socket();
        }
        this.socket.connect(new InetSocketAddress(this.host, this.port), 30000);

        return this.socket;
    }

    /**
     * @return the ssl
     */
    public boolean isSSL() {
        return ssl;
    }

    public Socket socket() throws UnknownHostException, IOException, SocketTimeoutException {
        if (this.socket instanceof Socket) {
            return this.socket;
        } else {
            return this.open();
        }
    }

    @Override
    public void close() throws IOException {
        if (this.socket instanceof Socket) {
            this.socket.close();
        }
    }
}
