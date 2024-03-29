Memento backup system
----------------------

Link: https://github.com/henryx/memento-java

Description:
------------

Memento is a backup system for remote computers. It is similar of other systems
(e.g. rsnapshot) but it has some differences:

 - It uses an agent for checking and downloading data.
 - It manages four distinct datasets (hour, day, week, month).
 - It saves space with hard link creation.
 - It saves data attribute (owner, group, permission, ACL) into database.

Licence:
--------

Memento is released under GPLv2 (see GPL.txt for details)

Dependencies:
-------------

 - Java 1.7;
 - Firebird;

Installation:
-------------

 - Extract the archive into a directory.
 - Create an empty database in Firebird.
 - Configure the [database] and the [general] sections into configuration file.
 - Create machine section into configuration file (see the example below).
 - Run

Client usage:
-------------

Client usage is:

    $ java -jar memento-client.jar -p <port>

Where `<port>` is port number which service listen requests from server
(use -h option for help)

Server usage:
-------------

Server usage is:

    $ java -jar memento-server.jar --cfg=<cfgfile> -H # hour backup
    $ java -jar memento-server.jar --cfg=<cfgfile> -D # day backup
    $ java -jar memento-server.jar --cfg=<cfgfile> -W # week backup
    $ java -jar memento-server.jar --cfg=<cfgfile> -M # month backup

Where `<cfgfile>` is a file structured like the `backup.cfg` reported in the
archive. For other options, use `-h` switch. Some notes:

 - It is possible to have multiple configuration files, where each file has
   different parameters.
 - While [general] [dataset] and [database] sections are global, it is 
   possible to add any number of sections, one per server.

This is an example of configuration file:

    [general]
    repository = /full/path/to/store/backups
    log_file = memento.log
    log_level = INFO

    [database]
    host = localhost
    port = 3050
    user = sysdba
    password = masterkey
    dbname = memento.fdb

    [dataset]
    hour = 24
    day = 6
    week = 4
    month = 12

    [a_server]
    type = file
    host = localhost
    port = 4444
    path = /full/path/to/backup
    acl = true
    compress = true
    ssl = true
    sslkey = /path/for/java/keystore.jks
    sslpass = a_password
    pre_command =
    post_command =

SSL:
----

For SSL connection, it uses a java keystore. For its creation, use this command:

    keytool -genkey -keyalg RSA -keystore <keystore.jks> -storepass <optional_password> -keyalg RSA

In client app, use -S <cfgfile> options for enable it. The <cfgfile> has this syntax:

    [ssl]
    key = /path/to/keystore.jks
    password = optional_password #optional

In server app, use the configuration file for enable it

Caveats:
--------

Because Memento use hard links to store its dataset, its use is guaranteed on
linux or unix environments.
