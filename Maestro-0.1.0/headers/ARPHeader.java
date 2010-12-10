/*
  ARPHeader.java

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

  ====================================================================
  
                          ARP Header Format

  (This is for a typical MAC-IPV4 ARP packet. Other kinds of ARP
  packet will look differently, depending on the hardware type and
  protocol type)
	  
  
    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |          Hardware type        |         Protocol type         |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |    HW Size    | Protocol Size |           Operation           |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                       Source MAC address                      |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |       Source MAC address      |        Source IP address      |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |        Source IP address      |      Destination MAC address  |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                     Destination MAC address                   |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                      Destination IP address                   |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

  =====================================================================
*/

package headers;

import sys.Utilities;

/**
 * The representation of an Arp Packet in Maestro
 *
 * @author Zheng Cai
 *
 */
public class ARPHeader extends Header {
    /** Constants */
    public static final int ARP_OPERATION_REQUEST = 1;
    public static final int ARP_OPERATION_REPLY = 2;
    public static final int ARP_OPERATION_REQUEST_REVERSE = 3;
    public static final int ARP_OPERATION_REPLY_REVERSE = 4;
    //. Not all operation codes are included here
	

    /** Members */
    /** Hardware type */
    public int hwType;

    /** Protocol type */
    public int protoType;

    /** Hardware size */
    public short hwSize;

    /** Protocol size */
    public short protoSize;

    /** Operation */
    public int operation;
    
    /** Source hardware address */
    public short[] hwSrc;
    
    /** Source protocol address */
    public short[] protoSrc;

    /** Destination hardware address */
    public short[] hwDst;

    /** Destination IP address */
    public short[] protoDst;

    
    /**
     * Constructor of ARPHeader
     */
    public ARPHeader() {
	
    }

    public void accept(HeaderVisitor visitor) {
	visitor.visit(this);
    }

    /**
     * Parse the arp header of this packet
     * @param buf Buffer which stores the whole packet data
     * @param pos Position of where the header starts in the buffer
     * @return The position in the buffer after parsing the header
     */
    public int parseHeader(byte[] buf, int pos) {
	hwType = Utilities.getNetworkBytesUint16(buf, pos);
	pos += 2;
	protoType = Utilities.getNetworkBytesUint16(buf, pos);
	pos += 2;
	hwSize = Utilities.getNetworkBytesUint8(buf, pos);
	pos += 1;
	protoSize = Utilities.getNetworkBytesUint8(buf, pos);
	pos += 1;
	operation = Utilities.getNetworkBytesUint16(buf, pos);
	pos += 2;

	hwSrc = new short[hwSize];
	for (int i=0;i<hwSize;i++) {
	    hwSrc[i] = Utilities.getNetworkBytesUint8(buf, pos);
	    pos += 1;
	}

	protoSrc = new short[protoSize];
	for (int i=0;i<protoSize;i++) {
	    protoSrc[i] = Utilities.getNetworkBytesUint8(buf, pos);
	    pos += 1;
	}

	hwDst = new short[hwSize];
	for (int i=0;i<hwSize;i++) {
	    hwDst[i] = Utilities.getNetworkBytesUint8(buf, pos);
	    pos += 1;
	}

	protoDst = new short[protoSize];
	for (int i=0;i<protoSize;i++) {
	    protoDst[i] = Utilities.getNetworkBytesUint8(buf, pos);
	    pos += 1;
	}

	//. For different operation types
	//. Currently no addition action needed
	switch (operation) {
	case ARP_OPERATION_REQUEST:

	    break;
	case ARP_OPERATION_REPLY:

	    break;
	case ARP_OPERATION_REQUEST_REVERSE:

	    break;
	case ARP_OPERATION_REPLY_REVERSE:

	    break;
	default:
	    break;
	}

	return pos;
    }

    public String toString() {
	//. Currently not printing all information
	return String.format("|ARP Header==HW type:%x|P type:%x|HW size:%x|P size:%x|Operation:%x|",
			     hwType, protoType, hwSize, protoSize, operation);
    }
}
