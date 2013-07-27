/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@gmail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
 */
package org.memento.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import org.apache.commons.cli.*;
import org.apache.log4j.*;
import org.ini4j.Wini;
import org.memento.server.management.Manager;
import org.memento.server.storage.DBConnection;

/**
 *
 * @author enrico
 */
public class Main {

    private Options opts;
    private Wini cfg;
    public static final String VERSION = "1.0";
    public static Logger logger = Logger.getLogger("Memento");

    public Main() {
        this.opts = new Options();

        this.opts.addOption("h", "help", false, "Print this help");
        this.opts.addOption(OptionBuilder.withLongOpt("cfg")
                .withDescription("Use the specified configuration file")
                .hasArg()
                .withArgName("CFGFILE")
                .create("c"));
        this.opts.addOption(OptionBuilder.withLongOpt("restore")
                .withDescription("Restore a backup")
                .hasArg()
                .withArgName("RESTOREFILE")
                .create("R"));
        this.opts.addOption("H", false, "Hourly backup is executed");
        this.opts.addOption("D", false, "Daily backup is executed");
        this.opts.addOption("W", false, "Weekly backup is executed");
        this.opts.addOption("M", false, "Monthly backup is executed");
        this.opts.addOption(OptionBuilder.withLongOpt("reload-dataset")
                .withDescription("Reload last dataset")
                .create("r"));

    }

    /**
     * Create log via log4j
     */
    private void setLog() {
        Appender appender;

        try {
            appender = new FileAppender(new PatternLayout("%d %-5p %c - %m%n"),
                    this.cfg.get("general", "log_file"));
            Main.logger.addAppender(appender);
            Main.logger.setLevel(Level.toLevel(this.cfg.get("general", "log_level")));
        } catch (IOException | SecurityException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.FATAL, null, ex);
            System.exit(2);
        }
    }

    /**
     * Open the configuration file
     *
     * @param cfgFile a configuration file
     */
    private void setCfg(String cfgFile) {
        try {
            this.cfg = new Wini();
            this.cfg.load(new FileInputStream(cfgFile));
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(2);
        }
    }

    public void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Memento", this.opts);
        System.exit(0);
    }

    public void go(String[] args) throws ParseException, IOException, SQLException, ClassNotFoundException {
        CommandLine cmd;
        CommandLineParser parser;
        Manager manage;
        Boolean sync;

        parser = new PosixParser();
        cmd = parser.parse(this.opts, args);
        
        sync = null;

        if (cmd.hasOption("h") || cmd.hasOption("help")) {
            this.printHelp();
        }

        if (cmd.hasOption("cfg") && cmd.hasOption("restore")) {
            System.out.println("Cannot perform a backup or restore in the same session");
            System.exit(1);
        }
        
        if (cmd.hasOption("cfg")) {
            sync = Boolean.TRUE;
            this.setCfg(cmd.getOptionValue("cfg"));
        } else if (cmd.hasOption("restore")) {
            sync = Boolean.FALSE;
            this.setCfg(cmd.getOptionValue("restore"));
        } else {
            System.out.println("No configuration file defined (see help)");
            System.exit(2);
        }

        this.setLog();
        Main.logger.info("Started version " + VERSION);

        manage = new Manager(this.cfg);

        if (cmd.hasOption("H")) {
            Main.logger.debug("Hour mode selected");
            manage.setGrace("hour");
        } else if (cmd.hasOption("D")) {
            Main.logger.debug("Day mode selected");
            manage.setGrace("day");
        } else if (cmd.hasOption("W")) {
            Main.logger.debug("Week mode selected");
            manage.setGrace("week");
        } else if (cmd.hasOption("M")) {
            Main.logger.debug("Month mode selected");
            manage.setGrace("month");
        } else {
            Main.logger.fatal("No grace period selected");
            System.exit(3);
        }

        if (cmd.hasOption("r")) {
            Main.logger.debug("Reload dataset selected");
            manage.setReload(true);
        } else {
            manage.setReload(false);
        }

        if (sync) {
            manage.exec("sync");
        } else {
            manage.exec("restore");
        }

        Main.logger.info("Ended version " + VERSION);
    }

    public static void main(String[] args) {
        Main m;

        m = new Main();

        if (args.length == 0) {
            m.printHelp();
        }

        try {
            m.go(args);
        } catch (ParseException | IOException | SQLException | ClassNotFoundException ex) {
            Main.logger.fatal(ex);
            System.exit(2);
        }
    }
}