/*
  LLDPHeader.java

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

  Each frame contains one Link Layer Discovery Protocol Data Unit.
  Each LLDPDU is a sequence of type-length-value (TLV) structures.
  
  LLDPDU format

  +------+-----+----+----+-----+-----+----+-------------------+
  | Chassis ID | Port ID | TTL | Optional | End of LLDPDU TLV |
  |    TLV     |  TLV    | TLV |   TLVs   |  type=0 length=0  |
  +------+-----+----+----+-----+-----+----+-------------------+

  TLV Format

  +----------+------------+------------------+
  | TLV type | TLV length |    TLV Value     |
  | (7 bits) |  (9 bits)  | (0 to 510 bytes) |
  +----------+------------+------------------+

  TLV Types:

  0   - end of LLDPDU
  1   - Chassis ID
  2   - Port ID
  3   - TTL
  4   - Port description (optional)
  5   - System name
  6   - System description
  7   - System capabilities
  8   - Management address
  127 - Organization specific TLVs                                                                                                               
  
  ====================================================================
*/

package headers;

import java.util.LinkedList;
import sys.Utilities;

/**
 * The representation of a LLDP Packet in Maestro
 *
 * @author Zheng Cai
 *
 */
public class LLDPHeader extends Header {
    public static class TLV {
	public int type;
	public int length;
	public byte[] value;

	public int parseTLV(byte[] buf, int pos) {
	    //. Read the first 8 bits from the buffer
	    short tmp = Utilities.getNetworkBytesUint8(buf, pos);
	    pos += 1;
	    //. Get the first 7 bits
	    type = (tmp & 0xFE) >> 1;
	    //. Get the last 1 bit
	    length = (tmp & 0x01) << 8;

	    //. Read the second 8 bits from the buffer
	    tmp = Utilities.getNetworkBytesUint8(buf, pos);
	    pos += 1;
	    length += tmp;

	    value = new byte[length];
	    Utilities.memcpy(value, 0, buf, pos, length);
	    pos += length;
	    
	    return pos;
	}

	public int convertToBytes(byte[] buf, int pos) {
	    int typeLength = length;
	    typeLength += (type << 9);
	    pos += Utilities.setNetworkBytesUint16(buf, pos, typeLength);
	    
	    for (int i = 0; i < length; i++)
		buf[pos++] = value[i];
	    
	    return pos;
	}
    }
    /*
      5   - System name
      6   - System description
      7   - System capabilities
      8   - Management address
      127 - Organization specific TLVs
    */
    
    /** Constants */
    public static final int TLV_TYPE_CHASSIS_ID = 1;
    public static final int TLV_TYPE_PORT_ID = 2;
    public static final int TLV_TYPE_TTL = 3;
    public static final int TLV_TYPE_PORT_DESC = 4;
    public static final int TLV_TYPE_SYSTEM_NAME = 5;
    public static final int TLV_TYPE_SYSTEM_DESC = 6;
    public static final int TLV_TYPE_SYSTEM_CAPA = 7;
    public static final int TLV_TYPE_MGNT_ADDR = 8;
    public static final int TLV_TYPE_ORG_SPECIFIC = 127;
    public static final int TLV_TYPE_END = 0;
    public static final int TLV_LENGTH_END = 0;

    /** Members */
    /** Chassis ID */
    public TLV chassisId;

    /** Port ID */
    public TLV portId;

    /** TTL */
    public TLV ttl;

    /** optional */
    public LinkedList<TLV> optional;

    
    /**
     * Constructor of LLDPHeader
     */
    public LLDPHeader() {
	chassisId = new TLV();
	portId = new TLV();
	ttl = new TLV();
	optional = new LinkedList<TLV>();
    }

    public void accept(HeaderVisitor visitor) {
	visitor.visit(this);
    }

    /**
     * Parse the LLDP header of this packet
     * @param buf Buffer which stores the whole packet data
     * @param pos Position of where the header starts in the buffer
     * @return The position in the buffer after parsing the header
     */
    public int parseHeader(byte[] buf, int pos) {
	pos = chassisId.parseTLV(buf, pos);
	Utilities.AssertWithoutExit(TLV_TYPE_CHASSIS_ID == chassisId.type, "LLDPDU Chassis ID type is wrong as "+chassisId.type);
	
	pos = portId.parseTLV(buf, pos);
	Utilities.AssertWithoutExit(TLV_TYPE_PORT_ID == portId.type, "LLDPDU Port ID type is wrong as "+portId.type);
	
	pos = ttl.parseTLV(buf, pos);
	Utilities.AssertWithoutExit(TLV_TYPE_TTL == ttl.type, "LLDPDU TTL type is wrong as "+ttl.type);

	int type_length = 0;
	while (0 != (type_length = Utilities.getNetworkBytesUint16(buf, pos))) {
	    TLV opt = new TLV();
	    pos = opt.parseTLV(buf, pos);
	    optional.addLast(opt);
	}
	//. Move the pos ahead by 2, because we read a 0-0 type_length
	pos += 2;

	return pos;
    }

    public String toString() {
	//. Currently print out only that it is a LLDPDU
	return String.format("|LLDP Header==|");
    }
}
