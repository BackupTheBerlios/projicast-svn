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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.DisplayMode;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;


/**
 * Class for displaying images in fullscreen mode using Java 2D API.
 * This class does pretty much everything in its drawing thread to not
 * fuck up xlib (async replies and so on).
 */
public class ImageDisplayer
{
    public enum TransitionEffect { NONE, FADE, BLUR, BLUR_FADE_COMBO };
    
    private TransitionEffect effect;
    private long delay;
    
    private GraphicsDevice dev;
    private JFrame win;
    private BufferStrategy strat;
    private BufferedImage image;
    
    private boolean isUp = false;
    
    private DrawingThread thread;
    
    private int width;
    private int height;
    
    /**
     * Creates a new ImageDisplayer using the specified transition effect and the specified
     * transition delay.
     * 
     * @param effect  the transition effect to use
     * @param delay   the delay between drawing frames in transition effects
     */
    public ImageDisplayer(TransitionEffect effect, long delay)
    {
        this.effect = effect;
        this.delay = delay;
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        dev = env.getDefaultScreenDevice();
        DisplayMode mode = dev.getDisplayMode();
        width = mode.getWidth();
        height = mode.getHeight();
    }
    
    /**
     * Goes into fullscreen mode ready to display pictures.
     */
    public synchronized void fireUp()
    {
        //We need to only create one thread once. If we use multiple threads, we'll
        //get Xlib errors.
        if(thread == null)
        {
            thread = new DrawingThread();
            thread.start();
        }
        else
        {
            thread.reawake();
        }
        isUp = true;
    }
    
    /**
     * Brings this ImageDisplayer down and out of full screen mode.
     */
    public synchronized void bringDown()
    {
        thread.cancel();
        isUp = false;
    }
    
    /**
     * Returns true if up.
     * 
     * @return true if up
     */
    public boolean isUp()
    {
        return isUp;
    }
    
    /**
     * Displays the specified image file.
     */
    public synchronized void displayImage(File file) throws IOException
    {
        if(isUp == false)
        {
            throw new IllegalStateException("Not up");
        }
        BufferedImage newImage = ImageIO.read(file);
        displayImage(newImage);
    }
    
    /**
     * Displays the specified image.
     */
    public synchronized void displayImage(BufferedImage newImage)
    {
        if(isUp == false)
        {
            throw new IllegalStateException("Not up");
        }
        image = copy(newImage, BufferedImage.TYPE_INT_ARGB);
    }
    
    /**
     * Fades the screen to black.
     */
    public synchronized void fadeToBlack()
    {
        if(!isUp)
        {
            throw new IllegalStateException("Display needs to be up.");
        }
        image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
    }
    
    /**
     * Copies the specified buffered image into a newly created buffered image of the specified type.
     * The new buffer will be of the same size as the screen of this ImageDisplayer.
     * 
     * @param src  the source to copy from
     * @param type  the type of image to copy to
     * 
     * @return a copy of src of the specified type
     */
    private BufferedImage copy(BufferedImage src, int type)
    {
        BufferedImage dst = new BufferedImage(width, height, type);
        Graphics2D g = dst.createGraphics();
        
        //Find the coordinates to draw the image in the center of the screen
        int x = (width / 2) - (src.getWidth() / 2);
        int y = (height / 2) - (src.getHeight() / 2);
        
        g.drawImage(src, x, y, Color.BLACK, null);
        g.dispose();
        return dst;
    }
    
    private class DrawingThread extends Thread
    {
        private boolean cancel = false;
        
        public void cancel()
        {
            cancel = true;
        }
        
        public synchronized void reawake()
        {
            cancel = false;
            notifyAll();
        }
        
        public void run()
        {
            while(true)
            {
                //Initialization of graphics
                //We need to do everything in the same thread as we'll
                //get dozens of lockups due to xlib errors otherwise.
                if(win == null)
                {
                    win = new JFrame();
                    win.setBackground(Color.BLACK);
                    win.setUndecorated(true);
                    win.setIgnoreRepaint(true);
                    
                    //We don't want an annoying cursor so create one that is not visible
                    Cursor cursor = Toolkit.getDefaultToolkit().createCustomCursor(
                            new BufferedImage(16,16, BufferedImage.TYPE_INT_ARGB),
                            new Point(0,0),
                            "");
                    win.setCursor(cursor);
                    
                    dev.setFullScreenWindow(win);
                    win.createBufferStrategy(2);
                    strat = win.getBufferStrategy();
                }
                else
                {
                    dev.setFullScreenWindow(win);
                }
                
                

                image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
                
                try
                {
                    BufferedImage curImg = image;
                    while(!cancel)
                    {
                        //While there's no new image, draw the current one
                        //again and again
                        while(!cancel && (curImg == image))
                        {
                            Graphics2D g = (Graphics2D)strat.getDrawGraphics();
                            g.drawImage(curImg, 0, 0, null);
                            g.dispose();
                            strat.show();
                            Thread.sleep(10);
                        }
                        BufferedImage old = curImg;
                        BufferedImage newI = image;
                        
                        transition(old, newI);
                        curImg = newI;
                    }
                }
                catch(InterruptedException e) {}
                
                dev.setFullScreenWindow(null);
                win.setVisible(false);
                
                try
                {
                    synchronized(this)
                    {
                        this.wait();
                    }
                }
                catch(InterruptedException e) { return; }
            }
        }
        
        /**
         * Renders the correct transition effect using the specified images.
         * 
         * @param curImage  the current image
         * @param newImage  the new image
         */
        private void transition(BufferedImage curImage, BufferedImage newImage)
        {
            newImage = copy(newImage, BufferedImage.TYPE_INT_ARGB);
            switch(effect)
            {
                case FADE:
                    transitionFade(curImage, newImage);
                    break;
                case BLUR:
                    transitionBlur(curImage, newImage);
                    break;
                case BLUR_FADE_COMBO:
                    transitionBlurCombo(curImage, newImage);
                    break;
            }
        }
        
        /**
         * Draws the transition effect fading.
         * 
         * @param curImage  the current image
         * @param newImage  the new image
         */
        private void transitionFade(Image curImage, Image newImage)
        {
            try
            {
                for(float i = 0.0f; i < 1.0f; i += 0.05f)
                {
                    Graphics2D g = (Graphics2D)strat.getDrawGraphics();
                    g.drawImage(curImage, 0, 0, Color.BLACK, null);
                    AlphaComposite comp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, i);
                    g.setComposite(comp);
                    g.drawImage(newImage, 0, 0, Color.BLACK, null);
                    g.fillRect(0,0,100,100);
                    g.dispose();
                    strat.show();
                    Thread.sleep(delay);
                }
            }
            catch (InterruptedException e) {}
        }
        
        /**
        
        /**
         * Transition blur.
         * 
         * @param curImage  the current image
         * @param newImage  the new image
         */
        private void transitionBlur(BufferedImage curImage, BufferedImage newImage)
        {
            BufferedImage img = copy(curImage, BufferedImage.TYPE_BYTE_GRAY);
            for(int i = 0; i <  30; i++)
            {
                Graphics2D g = (Graphics2D)strat.getDrawGraphics();
                img = blur(img);
                img = copy(img, BufferedImage.TYPE_BYTE_GRAY);
                g.drawImage(img, 0, 0, null);
                g.dispose();
                strat.show();
            }
            transitionFade(img, newImage);
        }
        
        /**
         * Transition blur combo.
         * 
         * @param curImage  the current image
         * @param newImage  the new image
         */
        private void transitionBlurCombo(BufferedImage curImage, BufferedImage newImage)
        {
            try
            {
                BufferedImage img = copy(curImage, BufferedImage.TYPE_BYTE_GRAY);
                float alpha = 0.0f;
                for(int i = 0; i <  100; i++)
                {
                    alpha += 1.0f / 100.0f;
                    Graphics2D g = (Graphics2D)strat.getDrawGraphics();
                    img = blur(img);
                    img = copy(img, BufferedImage.TYPE_BYTE_GRAY);
                    g.drawImage(img, 0, 0, null);
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                    g.drawImage(newImage, 0, 0, null);
                    g.dispose();
                    strat.show();
                    Thread.sleep(delay);
                }
            }
            catch (InterruptedException e) {}
        }
        
        /**
         * Blurs image returning the blurred image.
         * 
         * @param src  the image to blur
         * 
         * @return a BufferedImage containing the result
         */
        private BufferedImage blur(BufferedImage src)
        {
            float part = 1.0f/9.0f;
            float[] krnl = new float[] {
                    part, part, part,
                    part, part, part,
                    part, part, part };
            ConvolveOp conop = new ConvolveOp(new Kernel(3, 3, krnl));
            try
            {
                BufferedImage b = conop.filter(src, null);
                return b;
            }
            catch (RuntimeException e)
            {
                e.printStackTrace();
            }
            return null;
        } 
    }
        
    public static void main(String[] args) throws Exception
    {
        ImageDisplayer displayer = new ImageDisplayer(ImageDisplayer.TransitionEffect.BLUR_FADE_COMBO, 0);
        while(true) {
        displayer.fireUp();
        displayer.displayImage(new File("/home/shadewind/workspace/projicast/bin/images/dc.jpg"));
        displayer.bringDown();
        Thread.sleep(4000);
        }
    }
}
