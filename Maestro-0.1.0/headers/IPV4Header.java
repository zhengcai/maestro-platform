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
  
                         IPv4 Header Format
  
  0                   1                   2                   3
  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
  |Version|  IHL  |Type of Service|          Total Length         |
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
  |         Identification        |Flags|      Fragment Offset    |
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
  |  Time to Live |    Protocol   |         Header Checksum       |
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
  |                       Source Address                          |
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
  |                    Destination Address                        |
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
  |                    Options                    |    Padding    |
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
  
  ====================================================================
*/

package headers;

import sys.Utilities;

/**
 * The representation of an IPV4 Packet in Maestro
 *
 * @author Zheng Cai
 *
 */
public class IPV4Header extends Header {
    /** Constants */
    public static final short IPV4_TYPE_ICMP = 1;
    public static final short IPV4_TYPE_TCP = 6;
    public static final short IPV4_TYPE_UDP = 17;
    

    /** Members */
    /** Source MAC address */
    public long nwSrc;

    /** Destination IP address */
    public long nwDst;

    /** IP Type of Service */
    public short nwTos;

    /** IP protocol */
    public short nwProto;

    //. Warning: currently not all fields of the IP header are included
    //. Need to expand this later on
    
    
    /**
     * Constructor of IPV4Header
     */
    public IPV4Header() {
	
    }

    public void accept(HeaderVisitor visitor) {
	visitor.visit(this);
    }

    /**
     * Parse the ipv4 header of this packet, also create the inner
     * header instance accordingly, but not parse it.
     * @param buf Buffer which stores the whole packet data
     * @param pos Position of where the header starts in the buffer
     * @return The position in the buffer after parsing the header
     */
    public int parseHeader(byte[] buf, int pos) {
	pos += 1;
	nwTos = Utilities.getNetworkBytesUint8(buf, pos);
	pos += 8;
	nwProto = Utilities.getNetworkBytesUint8(buf, pos);
	pos += 3;
	nwSrc = Utilities.getNetworkBytesUint32(buf, pos);
	pos += 4;
	nwDst = Utilities.getNetworkBytesUint32(buf, pos);
	pos += 4;

	//. Creating the inner layer instance accordingly
	switch (nwProto) {
	case IPV4_TYPE_ICMP:
	    inner = new ICMPHeader();
	    inner.outer = this;
	    break;
	case IPV4_TYPE_TCP:
	    inner = new TCPHeader();
	    inner.outer = this;
	    break;
	case IPV4_TYPE_UDP:
	    inner = new UDPHeader();
	    inner.outer = this;
	    break;
	default:
	    break;
	}

	if (null != inner)
	    return inner.parseHeader(buf, pos);
	else
	    return pos;
    }

    public String toString() {
	return String.format("|IPV4 Header==Src IP:%x|Dst IP:%x|nwTos:%x|nwProto:%x|",
			     nwSrc, nwDst, nwTos, nwProto);
    }
}
