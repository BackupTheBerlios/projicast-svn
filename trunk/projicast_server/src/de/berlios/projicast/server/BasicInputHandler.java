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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;


/**
 * Provides some useful methods for writing an InputHandler.
 */
public abstract class BasicInputHandler implements InputHandler
{
    protected SocketChannel ch;
    private CharsetDecoder decoder;
    private CharsetEncoder encoder;
    private ByteBuffer obuf;  
    protected StringBuffer buf;
    
    /**
     * Constructs a new BasicInputHandler for reading writing to the specified SocketChannel
     * using the specified decoders and encoders.
     */
    protected BasicInputHandler(SocketChannel ch, CharsetDecoder decoder, CharsetEncoder encoder)
    {
        this.ch = ch;
        this.decoder = decoder;
        this.encoder = encoder;
        decoder.onMalformedInput(CodingErrorAction.REPORT);
        decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
        decoder.replaceWith(" ");
        buf = new StringBuffer();
    }
    
    abstract public void input(ByteBuffer ibuf);
    
    /**
     * Inputs data in to the character buffer for command assembly.
     *
     * @param ibuf  the data to insert
     */
    protected void cInput(ByteBuffer ibuf) throws CharacterCodingException
    {
        buf.append(decoder.decode(ibuf));
    }
    
    /**
     * Writes a command to the socket channel.
     * 
     * @param command  the command to write
     */
    protected void writeCommand(String command)
    {
        //System.out.println("OUT: " + command);
        CharBuffer cbuf = CharBuffer.allocate(command.length() + 1);
        cbuf.put(command);
        cbuf.put("\n");
        cbuf.flip();
        try
        {
            obuf = encoder.encode(cbuf);
            int len = ch.write(obuf);
        }
        catch(CharacterCodingException e)
        {
            System.err.println("Error while encoding characters: " + e.getMessage());
            close();
        }
        catch (IOException e)
        {
            close();
        }
    }
    
    /**
     * Returns a complete line if there is one.
     * 
     * @return  a String containing a complete line, or <i>null</i> if none available
     */
    protected String getLine()
    {
        String line;
        //We need to be compatible with all types of linefeeds
        int pos = buf.indexOf("\r\n");
        if(pos == -1)
        {
            pos = buf.indexOf("\n");
        }
        if(pos == -1)
        {
            pos = buf.indexOf("\r");
        }
        
        if(pos != -1)
        {
            line = buf.substring(0, pos);
            buf.delete(0, pos + 1);
            if((buf.length() > 0) && (buf.charAt(0) == '\n'))
            {
                buf.deleteCharAt(0);
            }
            //System.out.println("IN: " + line);
            return line;
        }
        else
        {
            return null;
        }
    }
    
    /**
     * Closes connection to client.
     */
    public void close()
    {
        try
        {
            if(ch.isOpen())
            {
                ch.close();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
