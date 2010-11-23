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
                          TCP Header Format                                                                                                                
                                                                                                                                                             
   0                   1                   2                   3                                                                                            
   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1                                                                                          
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+                                                                                         
  |          Source Port          |       Destination Port        |                                                                                         
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+                                                                                         
  |                        Sequence Number                        |                                                                                         
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+                                                                                         
  |                    Acknowledgment Number                      |                                                                                         
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+                                                                                         
  |  Data | Res |  E  |U|A|P|R|S|F|                               |                                                                                         
  | Offset| erv |  C  |R|C|S|S|Y|I|            Window             |                                                                                         
  |       | ed  |  N  |G|K|H|T|N|N|                               |                                                                                         
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+                                                                                         
  |           Checksum            |         Urgent Pointer        |                                                                                         
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+                                                                                         
  |                    Options                    |    Padding    |                                                                                         
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+                                                                                         
  |                             data                              |                                                                                         
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+                                                                                         
                                                                                                                                                            
  =====================================================================
*/

package headers;

import sys.Utilities;

/**
 * The representation of an TCP Packet in Maestro
 *
 * @author Zheng Cai
 *
 */
public class TCPHeader extends Header {
    /** Constants */
    
    
    /** Members, not complete */
    /** Source port number */
    public int tpSrc;
    
    /** Destination port number */
    public int tpDst;

    /** Sequence number */
    public long seq;

    /** Acknowledgment number */
    public long ack;

    public void accept(HeaderVisitor visitor) {
	visitor.visit(this);
    }

    public int parseHeader(byte[] buf, int pos) {
	tpSrc = Utilities.getNetworkBytesUint16(buf, pos);
	pos += 2;
	tpDst = Utilities.getNetworkBytesUint16(buf, pos);
	pos += 2;

	seq = Utilities.getNetworkBytesUint32(buf, pos);
	pos += 4;
	ack = Utilities.getNetworkBytesUint32(buf, pos);
	pos += 4;
	
	return pos;
    }
    
}
