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

import java.security.MessageDigest;
import java.util.Random;

/**
 * Class for various security methods.
 */
public class Security
{
	/**
	 * Calculates digest.
	 *
	 * @param data  the string to calculate the digest of
	 * @return the MD5 hex digest of <code>data</code>
	 */
	public static String digest(String data)
	{
		StringBuffer sb = new StringBuffer();
		
		try
		{
			MessageDigest messageDigest = MessageDigest.getInstance("MD5");
			messageDigest.update(data.getBytes());
			byte[] digestBytes = messageDigest.digest();
			
			/* convert to hexstring */
			String hex = null;
			
			for (int i = 0; i < digestBytes.length; i++) 
			{
				hex = Integer.toHexString(0xFF & digestBytes[i]);
				
				if (hex.length() < 2)
				{
					sb.append("0");
				}
				sb.append(hex);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return sb.toString();
	}
    
    /**
     * Generates a random string.
     *
     * @param length  the lenght of the string to generate
     * 
     * @return a random string
     */
    public static String generate(int length)
    {
        char[] chars = 
          "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
        Random rand = new Random();
        StringBuffer string = new StringBuffer();
        for(int i = 0; i < length; i++)
        {
            string.append(chars[rand.nextInt(chars.length)]);
        }
        return string.toString();
    }
}
