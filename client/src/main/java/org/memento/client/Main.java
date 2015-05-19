/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@ymail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
 */
package org.memento.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.ini4j.Profile.Section;
import org.ini4j.Wini;
import org.memento.client.net.Serve;

/**
 *
 * @author enrico
 */
public class Main {

    private Options opts;
    public static final String VERSION = "2.0";
    public final static Logger LOGGER = Logger.getLogger("Memento");

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
                .withLongOpt("ssl")
                .withDescription("Enable SSL connection")
                .hasArg()
                .withArgName("CFG")
                .create("S"));
        this.opts.addOption(OptionBuilder
                .withLongOpt("listen")
                .withDescription("Set listen address")
                .hasArg()
                .withArgName("ADDRESS")
                .create("l"));
        this.opts.addOption(OptionBuilder
                .withLongOpt("debug")
                .withDescription("Set debug messages")
                .create());
    }

    private Section getOptions(String section, String cfgFile) throws IOException {
        Wini cfg = new Wini();
        cfg.load(new FileInputStream(cfgFile));
        return cfg.get(section);
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

         if (cmd.hasOption("debug")) {
            Handler console = new ConsoleHandler();
            console.setLevel(Level.FINEST);

            Main.LOGGER.addHandler(console);
            Main.LOGGER.setLevel(Level.FINEST);
        } else {
            Main.LOGGER.setLevel(Level.OFF);
        }

        if (!cmd.hasOption("p")) {
            System.out.println("No port defined!");
            this.printHelp(2);
        }

        Main.LOGGER.fine("Main - Listen port " + cmd.getOptionValue("p"));
        try (Serve serve = new Serve(Integer.parseInt(cmd.getOptionValue("p")));) {
            if (cmd.hasOption("l")) {
                Main.LOGGER.fine("Listen address " + cmd.getOptionValue("l"));
                serve.setAddress(cmd.getOptionValue("l"));
            }

            if (cmd.hasOption("S")) {
                Main.LOGGER.fine("Main - SSL enabled");
                Section section = this.getOptions("ssl", cmd.getOptionValue("S"));
                serve.setSSL(true);
                
                serve.setSSLkey(section.get("key"));
                if (section.containsKey("password")) {
                    Main.LOGGER.fine("Main - SSL key password exists");
                    serve.setSSLpass(section.get("password"));
                }
            }

            serve.open();
            
            while (!exit) {
                try {
                    exit = serve.listen();
                } catch (SocketException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, ex.getMessage());
                }
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
