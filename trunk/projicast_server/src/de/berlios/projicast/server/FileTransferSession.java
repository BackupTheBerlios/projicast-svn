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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;


/**
 * Class for handling file transfer sessions.
 */
public class FileTransferSession extends BasicInputHandler
{
    private enum State { NEW, AUTH_STRING_SENT, AWAITING_FILE_INFO, TRANSFER_IN_PROGRESS }
    
    private State state = State.NEW;
    private String authString;
    private Configuration configuration;
    
    private String filename;
    private long fileSize;
    private Configuration.FileType fileType;
    private File dest;
    private FileChannel fch;
    private long written = 0;
    
    /**
     * Constructs a new FileTransferSession with the specified SocketChannel and server configuration.
     * 
     * @param ch             the socket channel to use
     * @param configuration  the configuration to use
     */
    public FileTransferSession(SocketChannel ch, Configuration configuration)
    {
        super(ch, configuration.getCharset().newDecoder(), configuration.getCharset().newEncoder());
        this.configuration = configuration;
    }
    
    public void input(ByteBuffer ibuf)
    {
        try
        {
            if(state != State.TRANSFER_IN_PROGRESS)
            {
                cInput(ibuf);
                String command = getLine();
                if(command != null)
                {
                    auth(command);
                }
            }
            else
            {
                receive(ibuf);
            }
        }
        catch (CharacterCodingException e)
        {
            System.err.println("Unable to decode characters: " + e.getMessage());
            close();
        }
        catch (IOException e)
        {
            System.err.println("Error while writing to file: " + e.getMessage());
            close();
        }
    }
    
    /**
     * Writes data from buffer to file and keeps track of how much data has been received.
     */
    private void receive(ByteBuffer ibuf) throws IOException
    {
        try
        {
            //Make sure not too much is written
            if((written + ibuf.limit()) > fileSize)
            {
                ibuf.limit((int)(fileSize - written));
            }
            int wrote = fch.write(ibuf);
            written += wrote;
            if(written >= fileSize)
            {
                fch.close();
                close();
            }
        }
        catch (IOException e)
        {
            close();
            fch.close();
        }
    }
        
    /**
     * Authenticates with client.
     * 
     * @param command  the command received from the client
     */
    private void auth(String command)
    {
        switch(state)
        {
            case NEW:
                if(command.equals("PROJICAST TRANSFER"))
                {
                    authString = Security.generate(128);
                    writeCommand("PROJICAST TRANSFER AUTH " + authString);
                    state = State.AUTH_STRING_SENT;
                }
                else
                {
                    close();
                }
                break;
                
            case AUTH_STRING_SENT:
                String[] split = command.split(" ");
                if((split.length == 2) && split[0].equals("AUTH"))
                {
                    String expect = Security.digest(configuration.getPassword() + authString);
                    if(split[1].equals(expect))
                    {
                        writeCommand("AUTH OK");
                        state = State.AWAITING_FILE_INFO;
                    }
                    else
                    {
                        writeCommand("ERROR Invalid auth");
                        close();
                    }
                }
                break;
            case AWAITING_FILE_INFO:
                receiveFileInfo(command);
                break;
        }
    }
    
    /**
     * Receives file info from the specified command.
     * 
     * @param command  the command to receive the info from
     */
    private void receiveFileInfo(String command)
    {
        try
        {
            String[] split = command.split(" ");
            if((split.length == 4) && split[0].equals("FILE"))
            {
                if(split[1].equals("VIDEO"))
                {
                    fileType = Configuration.FileType.VIDEO;
                }
                else if(split[1].equals("IMAGE"))
                {
                    fileType = Configuration.FileType.IMAGE;
                }
                else if(split[1].equals("SLDSHW"))
                {
                    fileType = Configuration.FileType.SLIDESHOW;
                }
                else
                {
                    close();
                    return;
                }
                
                fileSize = Long.parseLong(split[2]);
                
                filename = URLDecoder.decode(split[3], configuration.getCharset().name());
                
                //Set everything up for receiving
                File path = null;
                switch(fileType)
                {
                    case VIDEO:
                        path = configuration.getVideoManager().getPath();
                        break;
                    case IMAGE:
                        path = configuration.getImageManager().getPath();
                        break;
                    case SLIDESHOW:
                        path = configuration.getSlideshowManager().getPath();
                        break;
                }
                
                dest = new File(path, filename);
                fch = new FileOutputStream(dest).getChannel();
                state = State.TRANSFER_IN_PROGRESS;
                writeCommand("FILE OK");
            }
        }
        catch (NumberFormatException e)
        {
            close();
        }
        catch (UnsupportedEncodingException e)
        {
            //Should nevere happen because we already got the encoding from the OS
            close();
        }
        catch (FileNotFoundException e)
        {
            System.err.println("Unable to create file: " + e.getMessage());
            close();
        }
    }
    
    public void close()
    {
        super.close();
        if(written != fileSize)
        {
            dest.delete();
        }
    }
}
