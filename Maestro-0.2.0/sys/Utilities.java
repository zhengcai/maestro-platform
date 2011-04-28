/*
  Utilities.java

  Copyright (C) 2010  Rice University

  This software is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

  This software is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with this software; if not, write to the Free Software
  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/

package sys;

import java.io.*;
import java.util.*;

import drivers.OFPConstants;

/** All static utility functions are in this class
 * @author Zheng Cai
 * 
 */
public class Utilities {
    /** Whether it is small endian or big endian */
    static boolean bigEndian = true;

    static PrintWriter log = null;

    public static void openLogFile(String file) {
	try {
	    log = new PrintWriter(new File(file));
	} catch (FileNotFoundException e) {
	    Assert(false, "Error in opening log file "+file);
	} catch (SecurityException e) {
	    Assert(false, "Error in opening log file "+file);
	} catch (NullPointerException e) {
	    Assert(false, "Null path name");
	}
    }

    public static void closeLogFile() {
	log.close();
    }

    public static PrintWriter Log() {
	return log;
    }

    public static void ForceExit(int status) {
	closeLogFile();
	Exception e = new Exception();
	e.printStackTrace();
	System.exit(status);
    }
	
    public static void Assert(boolean b, String s) {
	if (!b) {
	    Utilities.printlnDebug("Fatal Error: "+s);
	    Utilities.ForceExit(0);
	}
    }

    public static void AssertWithoutExit(boolean b, String s) {
	if (!b) {
	    Utilities.printlnDebug("Critical Error: "+s);
	    Utilities.ForceExit(0);
	}
    }
	
    /** Maestro DEBUG printing system */
    public static void printDebug(String s) {
	if (Parameters.printDebug) {
	    System.err.print(s);
	}
    }
	
    public static void printDebug(int i) {
	if (Parameters.printDebug) {
	    System.err.print(i);
	}
    }
	
    public static void printDebug(float f) {
	if (Parameters.printDebug) {
	    System.err.print(f);
	}
    }
	
    public static void printDebug(boolean b) {
	if (Parameters.printDebug) {
	    System.err.print(b);
	}
    }
	
    public static void printlnDebug(String s) {
	if (Parameters.printDebug) {
	    System.err.println(s);
	}
    }
	
    public static void printlnDebug(int i) {
	if (Parameters.printDebug) {
	    System.err.println(i);
	}
    }
	
    public static void printlnDebug(float f) {
	if (Parameters.printDebug) {
	    System.err.println(f);
	}
    }
	
    public static void printlnDebug(boolean b) {
	if (Parameters.printDebug) {
	    System.err.println(b);
	}
    }
	
    public static void printlnDebug() {
	if (Parameters.printDebug) {
	    System.err.println();
	}
    }
	
    public static String TrimConfigString(String s) {
	return s.split("#")[0].trim();
    }
	
    public static void setBytesInt(byte[] target, int index, int number) {
	if ((target.length - index) < Integer.SIZE/8) {
	    Utilities.printlnDebug("Out of boundary when copying bytes, length="+target.length+", index="+index);
	    Utilities.ForceExit(0);
	}
	if (bigEndian) {
	    target[index] = (byte) (number & 0x00FF);
	    target[index + 1] = (byte) ((number >> 8) & 0x000000FF);
	    target[index + 2] = (byte) ((number >> 16) & 0x000000FF);
	    target[index + 3] = (byte) ((number >> 24) & 0x000000FF);
	}
	else {
	    target[index] = (byte) ((number >> 24) & 0x000000FF);
	    target[index + 1] = (byte) ((number >> 16) & 0x000000FF);
	    target[index + 2] = (byte) ((number >> 8) & 0x000000FF);
	    target[index + 3] = (byte) (number & 0x00FF);
	}
    }
	
    public static int GetIntFromBytesInt(byte[] target, int index) {
	int number = 0;
	if ((target.length - index) < Integer.SIZE/8) {
	    Utilities.printlnDebug("Out of boundary when copying bytes, length="+target.length+", index="+index);
	    Utilities.ForceExit(0);
	}
	if (bigEndian) {
	    number |= target[index + 3] & 0xFF;
	    number <<= 8;
	    number |= target[index + 2] & 0xFF;
	    number <<= 8;
	    number |= target[index + 1] & 0xFF;
	    number <<= 8;
	    number |= target[index + 0] & 0xFF;
	}
	else {
	    number |= target[index + 0] & 0xFF;
	    number <<= 8;
	    number |= target[index + 1] & 0xFF;
	    number <<= 8;
	    number |= target[index + 2] & 0xFF;
	    number <<= 8;
	    number |= target[index + 3] & 0xFF;
	}
	return number;
    }
	
    public static void setBytesLong(byte[] target, int index, long number) {
	if ((target.length - index) < Long.SIZE/8) {
	    Utilities.printlnDebug("Out of boundary when copying bytes, length="+target.length+", index="+index);
	    Utilities.ForceExit(0);
	}
	if (bigEndian) {
	    target[index] = (byte) (number & 0x00000000000000FF);
	    target[index + 1] = (byte) ((number >> 8) & 0x00000000000000FF);
	    target[index + 2] = (byte) ((number >> 16) & 0x00000000000000FF);
	    target[index + 3] = (byte) ((number >> 24) & 0x00000000000000FF);
	    target[index + 4] = (byte) ((number >> 32) & 0x00000000000000FF);
	    target[index + 5] = (byte) ((number >> 40) & 0x00000000000000FF);
	    target[index + 6] = (byte) ((number >> 48) & 0x00000000000000FF);
	    target[index + 7] = (byte) ((number >> 56) & 0x00000000000000FF);
	}
	else {
	    target[index] = (byte) ((number >> 56) & 0x00000000000000FF);
	    target[index + 1] = (byte) ((number >> 48) & 0x00000000000000FF);
	    target[index + 2] = (byte) ((number >> 40) & 0x00000000000000FF);
	    target[index + 3] = (byte) ((number >> 32) & 0x00000000000000FF);
	    target[index + 4] = (byte) ((number >> 24) & 0x00000000000000FF);
	    target[index + 5] = (byte) ((number >> 16) & 0x00000000000000FF);
	    target[index + 6] = (byte) ((number >> 8) & 0x00000000000000FF);
	    target[index + 7] = (byte) (number & 0x00000000000000FF);
	}
    }
	
    public static long GetLongFromBytesInt(byte[] target, int index) {
	long number = 0;
	if ((target.length - index) < Long.SIZE/8) {
	    Utilities.printlnDebug("Out of boundary when copying bytes, length="+target.length+", index="+index);
	    Utilities.ForceExit(0);
	}
	if (bigEndian) {
	    number |= target[index + 7] & 0xFF;
	    number <<= 8;
	    number |= target[index + 6] & 0xFF;
	    number <<= 8;
	    number |= target[index + 5] & 0xFF;
	    number <<= 8;
	    number |= target[index + 4] & 0xFF;
	    number <<= 8;
	    number |= target[index + 3] & 0xFF;
	    number <<= 8;
	    number |= target[index + 2] & 0xFF;
	    number <<= 8;
	    number |= target[index + 1] & 0xFF;
	    number <<= 8;
	    number |= target[index + 0] & 0xFF;
	}
	else {
	    number |= target[index + 0] & 0xFF;
	    number <<= 8;
	    number |= target[index + 1] & 0xFF;
	    number <<= 8;
	    number |= target[index + 2] & 0xFF;
	    number <<= 8;
	    number |= target[index + 3] & 0xFF;
	    number <<= 8;
	    number |= target[index + 4] & 0xFF;
	    number <<= 8;
	    number |= target[index + 5] & 0xFF;
	    number <<= 8;
	    number |= target[index + 6] & 0xFF;
	    number <<= 8;
	    number |= target[index + 7] & 0xFF;
	}
	return number;
    }
	
    public static long GetLongFromMAC(short[] mac) {
	Assert(mac.length == OFPConstants.OfpConstants.OFP_ETH_ALEN, "mac needs to be 6 bytes long");
	long number = 0;
	if (bigEndian) {
	    number |= mac[5] & 0xFF;
	    number <<= 8;
	    number |= mac[4] & 0xFF;
	    number <<= 8;
	    number |= mac[3] & 0xFF;
	    number <<= 8;
	    number |= mac[2] & 0xFF;
	    number <<= 8;
	    number |= mac[1] & 0xFF;
	    number <<= 8;
	    number |= mac[0] & 0xFF;
	}
	else {
	    number |= mac[0] & 0xFF;
	    number <<= 8;
	    number |= mac[1] & 0xFF;
	    number <<= 8;
	    number |= mac[2] & 0xFF;
	    number <<= 8;
	    number |= mac[3] & 0xFF;
	    number <<= 8;
	    number |= mac[4] & 0xFF;
	    number <<= 8;
	    number |= mac[5] & 0xFF;
	}
	return number;
    }

    public static boolean whetherMACBroadCast(short[] mac) {
	Assert(mac.length == OFPConstants.OfpConstants.OFP_ETH_ALEN, "mac needs to be 6 bytes long");
	return (mac[0]==0xFF)&&(mac[1]==0xFF)&&(mac[2]==0xFF)&&(mac[3]==0xFF)&&(mac[4]==0xFF)&&(mac[5]==0xFF);
    }
	
    public static int setNetworkBytesUint8(byte[] target, int index, short number) {
	if ((target.length - index) < 1) {
	    Utilities.printlnDebug("Out of boundary when copying bytes, length="+target.length+", index="+index);
	    Utilities.ForceExit(0);
	}
	target[index] = (byte) (number & 0x000000FF);
	return 1;
    }
	
    public static int setNetworkBytesUint16(byte[] target, int index, int number) {
	if ((target.length - index) < 2) {
	    Utilities.printlnDebug("Out of boundary when copying bytes, length="+target.length+", index="+index);
	    Utilities.ForceExit(0);
	}
	target[index] = (byte) ((number >> 8) & 0x000000FF);
	target[index + 1] = (byte) (number & 0x000000FF);
	return 2;
    }
	
    public static int setNetworkBytesUint32(byte[] target, int index, long number) {
	if ((target.length - index) < 4) {
	    Utilities.printlnDebug("Out of boundary when copying bytes, length="+target.length+", index="+index);
	    Utilities.ForceExit(0);
	}
	target[index] = (byte) ((number >> 24) & 0x000000FF);
	target[index + 1] = (byte) ((number >> 16) & 0x000000FF);
	target[index + 2] = (byte) ((number >> 8) & 0x000000FF);
	target[index + 3] = (byte) (number & 0x000000FF);
	return 4;
    }
	
    public static int setNetworkBytesUint64(byte[] target, int index, long number) {
	if ((target.length - index) < 8) {
	    Utilities.printlnDebug("Out of boundary when copying bytes, length="+target.length+", index="+index);
	    Utilities.ForceExit(0);
	}
	target[index] = (byte) ((number >> 56) & 0x000000FF);
	target[index + 1] = (byte) ((number >> 48) & 0x000000FF);
	target[index + 2] = (byte) ((number >> 40) & 0x000000FF);
	target[index + 3] = (byte) ((number >> 32) & 0x000000FF);
	target[index + 4] = (byte) ((number >> 24) & 0x000000FF);
	target[index + 5] = (byte) ((number >> 16) & 0x000000FF);
	target[index + 6] = (byte) ((number >> 8) & 0x000000FF);
	target[index + 7] = (byte) (number & 0x000000FF);
	return 8;
    }
	
    public static short getNetworkBytesUint8(byte[] target, int index) {
	short number = 0;
	if ((target.length - index) < 1) {
	    Utilities.printlnDebug("Out of boundary when copying bytes, length="+target.length+", index="+index);
	    Utilities.ForceExit(0);
	}
	number |= target[index++] & 0xFF;
	return number;
    }
	
    public static int getNetworkBytesUint16(byte[] target, int index) {
	int number = 0;
	if ((target.length - index) < 2) {
	    Utilities.printlnDebug("Out of boundary when copying bytes, length="+target.length+", index="+index);
	    Utilities.ForceExit(0);
	}
	number |= target[index++] & 0xFF;
	number <<= 8;
	number |= target[index++] & 0xFF;
	return number;
    }
	
    public static long getNetworkBytesUint32(byte[] target, int index) {
	long number = 0;
	if ((target.length - index) < 4) {
	    Utilities.printlnDebug("Out of boundary when copying bytes, length="+target.length+", index="+index);
	    Utilities.ForceExit(0);
	}
	number |= target[index++] & 0xFF;
	number <<= 8;
	number |= target[index++] & 0xFF;
	number <<= 8;
	number |= target[index++] & 0xFF;
	number <<= 8;
	number |= target[index++] & 0xFF;
	return number;
    }
	
    public static long getNetworkBytesUint64(byte[] target, int index) {
	long number = 0;
	if ((target.length - index) < 8) {
	    Utilities.printlnDebug("Out of boundary when copying bytes, length="+target.length+", index="+index);
	    Utilities.ForceExit(0);
	}
	number |= target[index++] & 0xFF;
	number <<= 8;
	number |= target[index++] & 0xFF;
	number <<= 8;
	number |= target[index++] & 0xFF;
	number <<= 8;
	number |= target[index++] & 0xFF;
	number <<= 8;
	number |= target[index++] & 0xFF;
	number <<= 8;
	number |= target[index++] & 0xFF;
	number <<= 8;
	number |= target[index++] & 0xFF;
	number <<= 8;
	number |= target[index++] & 0xFF;
	return number;
    }
	
    public static <T> Set<T> newViewNameSet() {
	return new HashSet<T>();
    }
	
    public static <T> Set<T> newViewNameSet(int initialCapacity) {
	return new HashSet<T>(initialCapacity);
    }
	
    public static <T> boolean intersect(Set<T> a, Set<T> b) {
	int as = a.size(), bs = b.size();
	Set<T> c = (as<bs?a:b);
	Set<T> d = (as<bs?b:a);
	for(T i : c) {
	    if(d.contains(i)) return true;
	}
	return false;
    }
	
    @SuppressWarnings("unchecked")
	public static <T> T deepCopy(T obj) throws java.io.IOException, ClassNotFoundException {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	ObjectOutputStream oout = new ObjectOutputStream(out);
	oout.writeObject(obj);
	ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
	ObjectInputStream oin = new ObjectInputStream(in);
		
	return (T) oin.readObject();
    }
	
    public static void memcpy(byte[] dst, int dpos, byte[] src, int spos, int length) {
	if ((src.length - spos) < length) {
	    Utilities.printlnDebug("Src out of boundary when copying bytes");
	    Utilities.ForceExit(0);
	}
	if ((dst.length - dpos) < length) {
	    Utilities.printlnDebug("Dst out of boundary when copying bytes, length is "+length+" while dst is "+dst.length+" pos "+dpos);
	    Utilities.ForceExit(0);
	}
	for (int i=0;i<length;i++)
	    dst[dpos+i] = src[spos+i];
    }
}
