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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * This is the class responsible for playing the files, enqueueing them.
 * 
 * @author shadewind
 * 
 *  //TODO check that states are correct
 */
public class Player
{
    public enum State { PLAYING_VIDEO, PLAYING_SLIDESHOW, DISPLAYING_PICTURE, STOPPED }
    
    private String mplayerPath;
    private ImageDisplayer imageDisp;
    private SlideshowThread slideshowThread;
    private MPlayerThread mplayerThread;
    private State state = State.STOPPED;
    private Image defaultBackground;
    
    //This variable is for starting the slideshow automatically again after a video files
    //is played and the slideshow was running before
    private boolean autoSlideshow = false;
    
    private long slideshowDelay;
    private List<File> lastPlaylist;
    
    private final static String xDisplay = ":0.0";
    
    /**
     * Constructs a new player. Provide path for mplayer and gqview. Also
     * provide a path for the folder containing the pictures.
     * 
     * @param imageDisp      the image displayer to use for displaying images
     * @param mplayerPath  the path to the mplayer executable
     * @param slideshowPath  the path to the slideshow folder
     */
    public Player(ImageDisplayer imageDisp, Image defaultBackground, String mplayerPath, long slideshowDelay)
    {
        this.imageDisp = imageDisp;
        this.defaultBackground = defaultBackground;
        this.mplayerPath = mplayerPath;
        this.slideshowDelay = slideshowDelay;
        if(!imageDisp.isUp())
        {
            imageDisp.fireUp();
        }
    }
    
    /**
     * Starts the slideshow.
     */
    public synchronized void slideshow(List<File> playlist)
    {
        autoSlideshow = true;
        lastPlaylist = playlist;
        internalStop();
        slideshowThread = new SlideshowThread(playlist, slideshowDelay);
        slideshowThread.start();
        state = State.PLAYING_SLIDESHOW;
    }
    
    /**
     * Plays a video file.
     * 
     * @param file  the video file to be played
     */
    public synchronized void playVideo(final File file)
    {
        internalStop();
        mplayerThread = new MPlayerThread(file, mplayerPath, xDisplay);
        mplayerThread.start();
        try
        {
            Thread.sleep(2000);
        }
        catch(InterruptedException e) {}
        imageDisp.bringDown();
    }
    
    /**
     * Displays a picture.
     * 
     * @param file  the file to display
     */
    public synchronized void displayImage(final File file) throws IOException
    {
        internalStop();
        imageDisp.displayImage(file);
        state = State.DISPLAYING_PICTURE;
    }
    
    /**
     * Displays a text string against the Player default background.
     * 
     * @param text   the text to display
     * @param font   the font to use
     * @param color  the color to draw the font with
     * @param x      the X position of the text
     * @param y      the Y position of the text
     */
    public synchronized void displayText(String text, Font font, Color color, int x, int y)
    {
        internalStop();
        BufferedImage buffer = new BufferedImage(
                defaultBackground.getWidth(null),
                defaultBackground.getHeight(null),
                BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = (Graphics2D)buffer.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        g.setFont(font);
        g.setColor(color);
        
        g.drawImage(defaultBackground, 0, 0, Color.BLACK, null);
        String[] lines = text.split("\n");
        int pos = y;
        for(String line : lines)
        {
            g.drawString(line, x, pos);
            pos += font.getSize() * 1.2f;
        }
        
        imageDisp.displayImage(buffer);
        state = State.DISPLAYING_PICTURE;
    }
    
    /**
     * Stops all playing.
     */
    public synchronized void stop()
    {
        autoSlideshow = false;
        internalStop();
        imageDisp.fadeToBlack();
    }
    
    /**
     * Internal method for stopping all playing without touching autoSlideshow.
     */
    private void internalStop()
    {
        switch(state)
        {
            case PLAYING_VIDEO:
                mplayerThread.stopPlaying();
                mplayerThread = null;
                imageDisp.fireUp();
                break;
            case PLAYING_SLIDESHOW:
                slideshowThread.cancel();
                break;
            case DISPLAYING_PICTURE:
                break;
        }
        state = State.STOPPED;
    }
    
    /**
     * Thread for running mplayer.
     */
    private class MPlayerThread extends Thread
    {
        private File file;
        private String display;
        private boolean interrupted = false;
        private Process mplayer;
        private String mplayerPath;
        
        /**
         * Creates a new MPlayerThread for playing the specified file
         * with the specified parameters.
         * 
         * @param file         the file to be played
         * @param mplayerPath  the path to the mPlayer executable
         * @param display      the X display to play the file on
         */
        public MPlayerThread(File file, String mplayerPath, String display)
        {
            this.file = file;
            this.mplayerPath = mplayerPath;
            this.display = display;
        }
        
        /**
         * Stops the playing of the file.
         */
        public void stopPlaying()
        {
            interrupted = true;
            mplayer.destroy();
        }
        
        public void run()
        {
            state = Player.State.PLAYING_VIDEO;
            try
            {
                mplayer = Runtime.getRuntime().exec(
                        new String[] { mplayerPath,"-fs",file.getCanonicalPath() });
                
                mplayer.waitFor();
                
                //If we were interrupted, then let whatever interrupted us
                //handle it. Otherwise, start the slideshow if it's supposed to be started.
                if(!interrupted) 
                {
                    imageDisp.fireUp();
                    if(autoSlideshow) //As stated above, start slideshow if autoSlideshow is set
                    {
                        slideshow(lastPlaylist);
                    }
                }
                else
                {
                    state = Player.State.STOPPED;
                }
            }
            catch(IOException e)
            {
                state = Player.State.STOPPED;
                e.printStackTrace();
            }
            catch(InterruptedException e)
            {
                e.printStackTrace();
            }
            finally //TODO should this really be placed here?
            {
                interrupted = false;
            }
        }
    }
    
    /**
     * Thread for displaying slideshow.
     */
    private class SlideshowThread extends Thread
    {
        private boolean cancel = false;
        private List<File> playlist;
        private long delay;
        
        /**
         * Creates a new SlideshowThread.
         * 
         * @param delay  the delay between images in milliseconds
         */
        public SlideshowThread(List<File> playlist, long delay)
        {
            this.playlist = playlist;
            this.delay = delay;
        }
        
        /**
         * Stops the slideshow.
         */
        public void cancel()
        {
            cancel = true;
            interrupt();
        }
        
        public void run()
        {
            try
            {
                while(!cancel)
                {
                    for(File file : playlist)
                    {
                        imageDisp.displayImage(file);
                        Thread.sleep(delay);
                    }
                }
            }
            catch(InterruptedException e)
            {
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
