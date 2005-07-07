/*
 * Copyright (c) 2005 Emil Eriksson <shadewind[at]gmail[dot]com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package de.berlios.projicast.server;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;


/**
 * Acceptor for files.
 */
public class FileAcceptor implements Acceptor
{
    private Selector selector;
    private Configuration config;
    
    /**
     * Constructs a new acceptor registering channels to the specified selector.
     * 
     * @param selector  the selector to register to
     * @param config    the config to use
     */
    public FileAcceptor(Selector selector, Configuration config)
    {
        this.selector = selector;
        this.config = config;
    }
    
    public void accept(SelectionKey key) throws IOException
    {
        ServerSocketChannel ssc = (ServerSocketChannel)key.channel();
        SocketChannel ch = ssc.accept();
        ch.configureBlocking(false);
        ch.register(selector, SelectionKey.OP_READ, new FileTransferSession(ch, config));
    }
}
