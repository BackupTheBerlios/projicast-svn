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
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;


/**
 * The main server thread.
 */
public class ServerThread extends Thread
{
    private boolean running = true;
    private int port;
    private int ftPort;
    private Configuration configuration;
    
    /**
     * Constructs a new ServerThread running on the specified ports with the
     * specified configuration.
     * 
     * @param port           the port to listen for control sessions on
     * @param ftPort         the port to listen for file transfer sessions on
     * @param configuration  the server configuration to use
     */
    public ServerThread(Configuration configuration, int port, int ftPort)
    {
        this.port = port;
        this.ftPort = ftPort;
        this.configuration = configuration;
    }
    
    public void cancel()
    {
        running = false;
    }
    
    public void run()
    {
        try
        {
            Selector selector = Selector.open();
            //Control sessions
            ServerSocketChannel ssc = ServerSocketChannel.open();
            ServerSocket ss = ssc.socket();
            ssc.configureBlocking(false);
            ss.bind(new InetSocketAddress(port));
            ssc.register(selector, SelectionKey.OP_ACCEPT, new ControlAcceptor(selector, configuration));
            //File transfer sessions
            ssc = ServerSocketChannel.open();
            ss = ssc.socket();
            ssc.configureBlocking(false);
            ss.bind(new InetSocketAddress(ftPort));
            ssc.register(selector, SelectionKey.OP_ACCEPT, new FileAcceptor(selector, configuration));
            while(running)
            {
                ByteBuffer buf = ByteBuffer.allocateDirect(8 * 4096);
                int len;
                Iterator<SelectionKey> it;
                Set<SelectionKey> keys;
                selector.select();
                keys = selector.selectedKeys();
                it = keys.iterator();
                while(it.hasNext())
                {
                    SelectionKey key = it.next();
                    if(key.isAcceptable())
                    {
                        Acceptor acc = (Acceptor)key.attachment();
                        acc.accept(key);
                    }
                    if(key.isReadable())
                    {
                        InputHandler session = (InputHandler)key.attachment();
                        SocketChannel ch = (SocketChannel)key.channel();
                        len = ch.read(buf);
                        if(len != -1)
                        {
                            buf.flip();
                            session.input(buf);
                            buf.clear();
                        }
                        else
                        {
                            session.close();
                        }
                    }
                    it.remove();
                }
            }
        }
        catch (ClosedChannelException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
