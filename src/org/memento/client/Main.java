/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@ymail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
*/

package org.memento.client;

import java.io.IOException;
import java.net.BindException;
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

    public Main() {
        this.opts = new Options();

        this.opts.addOption("h", "help", false, "Print this help");
        this.opts.addOption(OptionBuilder.withLongOpt("port").withDescription("Set port number").hasArg().withArgName("PORT").create("p"));
    }

    public void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Memento", this.opts);
        System.exit(0);
    }

    public void go(String[] args) throws ParseException, ClassCastException {
        Boolean exit;
        CommandLine cmd;
        CommandLineParser parser;

        parser = new PosixParser();

        cmd = parser.parse(this.opts, args);
        exit = Boolean.FALSE;

        if (cmd.hasOption("h") || cmd.hasOption("help")) {
            this.printHelp();
        }

        if (!cmd.hasOption("p")) {
            throw new ParseException("No port defined!");
        }

        try (Serve serve = new Serve(Integer.parseInt(cmd.getOptionValue("p")));) {
            serve.open();
            while (!exit) {
                exit = serve.listen();
            }
        } catch (BindException | UnknownHostException | NullPointerException ex) {
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
        } catch (ClassCastException ex) {
            System.err.println(ex.getMessage());
            System.exit(3);
        }
    }
}
