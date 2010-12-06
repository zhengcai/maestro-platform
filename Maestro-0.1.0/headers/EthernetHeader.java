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

  ===================================================================
                     Ethernet Header Format
   0                   1                   2                   3
   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
  |                    Destination MAC Address                    |
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
  |    Destination MAC Address    |      Source MAC Address       |
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
  |                       Source MAC Address                      |
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
  |           Type/Length         |             Data              |
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
  |                              Data                             |
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
  
  ===================================================================
*/

package headers;

import sys.Utilities;

/**
 * The representation of an Ethernet Packet in Maestro
 *
 * @author Zheng Cai
 *
 */
public class EthernetHeader extends Header {
    /** Constants */
    /** Length of a MAC address */
    public static final int OFP_ETH_ALEN = 6;
    public static final int ETH_TYPE_IPV4 = 0x0800;
    public static final int ETH_TYPE_ARP = 0x0806;
    public static final byte ETH_TYPE_LLDP_B0 = -52;
    public static final byte ETH_TYPE_LLDP_B1 = -120;
    public static final int ETH_TYPE_LLDP = 0x88cc;
    

    /** Members */
    /** Destination MAC address */
    public short[] dlDst;

    /** Source MAC address */
    public short[] dlSrc;

    /** Ethernet frame type */
    public int dlType;

    /** Input Vlan id */
    public int dlVlan;

    /** Input Vlan priority */
    public short dlVlanPcp;
    
    /**
     * Constructor of EthernetHeader
     */
    public EthernetHeader() {
	dlSrc = new short[OFP_ETH_ALEN];
	dlDst = new short[OFP_ETH_ALEN];
    }

    public void accept(HeaderVisitor visitor) {
	visitor.visit(this);
    }

    /**
     * Parse the ethernet header of this packet, also create the inner
     * header instance accordingly, but not parse it. Since an ethernet
     * frame does not have a outer layer, the outer is always null.
     * @param buf Buffer which stores the whole packet data
     * @param pos Position of where the header starts in the buffer
     * @return The position in the buffer after parsing the header
     */
    public int parseHeader(byte[] buf, int pos) {
	for (int j=0;j<OFP_ETH_ALEN;j++) {
	    dlDst[j] = Utilities.getNetworkBytesUint8(buf, pos++);
	}
	for (int j=0;j<OFP_ETH_ALEN;j++) {
	    dlSrc[j] = Utilities.getNetworkBytesUint8(buf, pos++);
	}
	dlType = Utilities.getNetworkBytesUint16(buf, pos);
	pos += 2;
	
	//. Warning: currently ignore Vlan information
	dlVlan = 0xffff;
	dlVlanPcp = 0;

	//. Creating the inner layer instance accordingly
	switch (dlType) {
	case ETH_TYPE_IPV4:
	    inner = new IPV4Header();
	    inner.outer = this;
	    break;
	case ETH_TYPE_ARP:
	    inner = new ARPHeader();
	    inner.outer = this;
	    break;
	case ETH_TYPE_LLDP:
	    inner = new LLDPHeader();
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
	return String.format("Ethernet Header||Dst MAC:%d-%d-%d-%d-%d-%d|Src MAC:%d-%d-%d-%d-%d-%d|dlType:%x|dlVlan:%x|dlVlanPcp:%x||",
			     dlDst[0], dlDst[1], dlDst[2], dlDst[3], dlDst[4], dlDst[5],
			     dlSrc[0], dlSrc[1], dlSrc[2], dlSrc[3], dlSrc[4], dlSrc[5],
			     dlType, dlVlan, dlVlanPcp);
    }
}
