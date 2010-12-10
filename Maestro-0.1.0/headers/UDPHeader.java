/*
  UDPHeader.java

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
                            UDP Header Format

                  0      7 8     15 16    23 24    31
                 +--------+--------+--------+--------+
                 |     Source      |   Destination   |
                 |      Port       |      Port       |
                 +--------+--------+--------+--------+
                 |     Length      |    Checksum     |
                 +--------+--------+--------+--------+
                 |           data bytes ...          |
                 +-----------------------------------+                 
                                                                                                                                                            
  =====================================================================
*/

package headers;

import sys.Utilities;

/**
 * The representation of an UDP Packet in Maestro
 *
 * @author Zheng Cai
 *
 */
public class UDPHeader extends Header {
    /** Constants */
    
    
    /** Members */
    /** Source port number */
    public int tpSrc;
    
    /** Destination port number */
    public int tpDst;

    /** Length */
    public int length;

    /** Checksum */
    public int checksum;

    public void accept(HeaderVisitor visitor) {
	visitor.visit(this);
    }
    
    public int parseHeader(byte[] buf, int pos) {
	tpSrc = Utilities.getNetworkBytesUint16(buf, pos);
	pos += 2;
	tpDst = Utilities.getNetworkBytesUint16(buf, pos);
	pos += 2;
	length = Utilities.getNetworkBytesUint16(buf, pos);
	pos += 2;
	checksum = Utilities.getNetworkBytesUint16(buf, pos);
	pos += 2;
	
	return pos;
    }
}
