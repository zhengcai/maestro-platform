/*
  ICMPHeader.java

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
                           ICMP Header Format

   0                   1                   2                   3
   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
  |      Type     |      Code     |           Checksum            |
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
  |                             Data                              |
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                                                                                                                                                            
  =====================================================================
*/

package headers;

import sys.Utilities;

/**
 * The representation of an ICMP Packet in Maestro
 *
 * @author Zheng Cai
 *
 */
public class ICMPHeader extends Header {
    /** Constants */
    
    
    /** Members */
    /** ICMP type */
    public short type;
    
    /** ICMP code */
    public short code;

    /** Checksum */
    public int checksum;

    public void accept(HeaderVisitor visitor) {
	visitor.visit(this);
    }
    
    public int parseHeader(byte[] buf, int pos) {
	type = Utilities.getNetworkBytesUint8(buf, pos);
	pos += 1;
	code = Utilities.getNetworkBytesUint8(buf, pos);
	pos += 1;
	checksum = Utilities.getNetworkBytesUint16(buf, pos);
	pos += 2;
	
	return pos;
    }
}
