/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@ymail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
*/

package org.memento.client;

import java.io.IOException;
import java.net.BindException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.*;
import org.memento.client.net.Serve;

/**
 *
 * @author enrico
 */

public class Main {

    private Options opts;
    public static final String VERSION = "1.0";

    public Main() {
        this.opts = new Options();

        this.opts.addOption("h", "help", false, "Print this help");
        this.opts.addOption(OptionBuilder
                .withLongOpt("port")
                .withDescription("Set port number")
                .hasArg()
                .withArgName("PORT")
                .create("p"));
        this.opts.addOption(OptionBuilder
                .withLongOpt("listen")
                .withDescription("Set listen address")
                .hasArg()
                .withArgName("ADDRESS")
                .create("l"));
    }

    public void printHelp(Integer code) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Memento", this.opts);
        System.exit(code);
    }

    public void go(String[] args) throws ParseException, ClassCastException {
        boolean exit;
        CommandLine cmd;
        CommandLineParser parser;

        parser = new PosixParser();

        cmd = parser.parse(this.opts, args);
        exit = false;

        if (cmd.hasOption("h") || cmd.hasOption("help")) {
            this.printHelp(0);
        }

        if (!cmd.hasOption("p")) {
            System.out.println("No port defined!");
            this.printHelp(2);
        }

        try (Serve serve = new Serve(Integer.parseInt(cmd.getOptionValue("p")));) {
            if (cmd.hasOption("l")) {
                serve.listenTo(cmd.getOptionValue("l"));
            }

            serve.open();
            while (!exit) {
                exit = serve.listen();
            }
        } catch (BindException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, ex.getMessage());
        } catch (IllegalArgumentException | SocketException | UnknownHostException | NullPointerException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) {
        Main m;

        m = new Main();
        try {
            m.go(args);
        } catch (ParseException ex) {
            System.err.println("Error when passing command: " + ex.getMessage());
            System.exit(2);
        }
    }
}
