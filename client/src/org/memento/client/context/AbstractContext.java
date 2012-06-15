/*
 Copyright (C) 2011 Enrico Bianchi (enrico.bianchi@gmail.com)
 Project       Memento
 Description   A backup system
 License       GPL version 2 (see GPL.txt for details)
*/

package org.memento.client.context;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;

/**
 *
 * @author enrico
 */

public abstract class AbstractContext {
    
    protected Socket connection;
    
    public abstract Boolean parse(HashMap command) throws IOException;
}