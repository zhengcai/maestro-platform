/*
  Copyright (C) 2010 Zheng Cai
  
  Maestro is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.
  
  Maestro is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.
  
  You should have received a copy of the GNU General Public License
  along with Maestro.  If not, see <http://www.gnu.org/licenses/>.

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
