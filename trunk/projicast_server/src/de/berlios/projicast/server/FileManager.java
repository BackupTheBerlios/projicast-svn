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
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Class for managing files and binding them to file ID:s.
 */
public class FileManager implements Serializable
{
    private File path;
    private HashMap<Integer, File> fileList;
    private int highestKey;
    
    /**
     * Constructs a new FileManager managing the specified path.
     * 
     * @param path  the path to manage
     */
    public FileManager(File path)
    {
        this.path = path;
        fileList = new HashMap<Integer, File>(10);
        rebuild();
    }
    
    /**
     * Refreshes the file list only adding files not already in the list and removing files
     * not available anymore.
     */
    public synchronized void refresh()
    {
        Iterator<File> it = fileList.values().iterator();
        while(it.hasNext())
        {
            File e = it.next();
            if(!e.exists())
            {
                it.remove();
            }
        }
        
        File[] files = path.listFiles();
        for(File file : files)
        {
            if(!fileList.containsValue(file))
            {
                highestKey++;
                fileList.put(highestKey, file);
            }
        }
    }
    
    /**
     * Rebuilds the file list completely. ID:s are not saved when
     * rebuilding so file ID:s may change completely.
     */
    public synchronized void rebuild()
    {
        File[] files = path.listFiles();
        fileList.clear();
        for(int i = 0; i < files.length; i++)
        {
            fileList.put(i, files[i]);
            highestKey = i;
        }
    }
    
    /**
     * Returns the file with the specified ID.
     *
     * @param id  the ID of the file to return
     * 
     * @return the file specified by the ID or null if no such file
     */
    public synchronized File getFile(int id)
    {
        return fileList.get(id);
    }
    
    /**
     * Deletes the file with the specified ID.
     * 
     * @param id  the ID of the file to delete
     */
    public synchronized void deleteFile(int id)
    {
        getFile(id).delete();
    }
    
    /**
     * Returns the ID for the specified file.
     * 
     * @param file  the file to get ID for
     * 
     * @return the ID of the file or -1 if non-existant
     */
    public synchronized int idForFile(File file)
    {
        for(Map.Entry<Integer, File> entry : fileList.entrySet())
        {
            if(entry.getValue().equals(file))
            {
                return entry.getKey();
            }
        }
        return -1;
    }
    
    /**
     * Returns a Map with files mapped to their respective ID:s.
     * 
     * @return a Map with files mapped to their respective ID:s.
     */
    public synchronized Map<Integer, File> getFileList()
    {
        return new HashMap<Integer, File>(fileList);
    }
    
    /**
     * Returns the path this FileManager manages.
     * 
     * @return the path this FileManager manages
     */
    public synchronized File getPath()
    {
        return path;
    }
}
