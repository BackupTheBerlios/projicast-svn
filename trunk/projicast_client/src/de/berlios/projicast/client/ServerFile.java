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

package de.berlios.projicast.client;

/**
 * Represents a file on a ProjiCast server
 */
public class ServerFile
{
    public enum Type { VIDEO, IMAGE, SLIDESHOW }
    
    private Type type;
    private String filename;
    private int id;
    
    /**
     * Constructs a new ServerFile.
     * 
     * @param filename  the name of the file
     * @param type      the type of the file
     * @param id        the file ID
     */
    public ServerFile(String filename, Type type, int id)
    {
        this.filename = filename;
        this.type = type;
        this.id = id;
    }

    /**
     * @return Returns the filename.
     */
    public String getFilename()
    {
        return filename;
    }
    

    /**
     * @return Returns the id.
     */
    public int getID()
    {
        return id;
    }
    

    /**
     * @return Returns the type.
     */
    public Type getType()
    {
        return type;
    }
    
    public String toString()
    {
        return filename;
    }
}
