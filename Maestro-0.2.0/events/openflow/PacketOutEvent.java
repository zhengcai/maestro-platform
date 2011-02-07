/*
  PacketOutEvent.java

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

package events.openflow;

import events.openflow.PacketInEvent.DataPayload;
import drivers.OFPConstants;
import sys.Utilities;

/**
 * @author Zheng Cai
 */
public class PacketOutEvent extends ToSpecificSwitchEvent implements Comparable<PacketOutEvent> {
	public PacketOutEvent() {
		super(-1);
	}
	
	public PacketOutEvent(int idx) {
		super(idx);
	}

	public static class Action {
		public int type;
		public int len;
		public int port;
		public int max_len;
	}

	public long xid;
	//public long dpid;
	public long bufferId;
	public int dataLen;	// Length of data
	public int inPort;
	public int actionsLen;
	public Action[] actions;
	public DataPayload data;
	
	@Override
	public int compareTo(PacketOutEvent o) {
		Long mine = dpid;
		return mine.compareTo(o.dpid);
	}
	
	public static Action makeOutputAction(int port) {
		Action ret = new Action();
		ret.type = OFPConstants.OfpActionType.OFPAT_OUTPUT;
		ret.len = OFPConstants.OfpConstants.OFPAT_OUTPUT_LENGTH;
		ret.port = port;
		return ret;
	}
	
	public static void setOutputAction(int port, Action act) {
		act.type = OFPConstants.OfpActionType.OFPAT_OUTPUT;
		act.len = OFPConstants.OfpConstants.OFPAT_OUTPUT_LENGTH;
		act.port = port;
	}

	@Override
	public int getLength() {
		int length = OFPConstants.OfpConstants.OFP_PACKET_OUT_LENGTH;
		for (PacketOutEvent.Action act : actions) {
			length += act.len;
		}
		if (OFPConstants.OP_UNBUFFERED_BUFFER_ID == bufferId) {
		    length += dataLen;
		}
		return length;
	}
	
	@Override
	public int convertToBytes(byte[] buf, int index) {
		int length = getLength();
		
		int pos = index;
		// ofp header
		pos += Utilities.setNetworkBytesUint8(buf, pos, OFPConstants.OfpConstants.OFP_VERSION);
		pos += Utilities.setNetworkBytesUint8(buf, pos, OFPConstants.PacketTypes.OFPT_PACKET_OUT);
		pos += Utilities.setNetworkBytesUint16(buf, pos, length);
		pos += Utilities.setNetworkBytesUint32(buf, pos, xid);
		
		
		pos += Utilities.setNetworkBytesUint32(buf, pos, bufferId);
		pos += Utilities.setNetworkBytesUint16(buf, pos, inPort);
		pos += Utilities.setNetworkBytesUint16(buf, pos, actionsLen);
		for (PacketOutEvent.Action act : actions) {
			// currently only support output action
			pos += Utilities.setNetworkBytesUint16(buf, pos, act.type);
			pos += Utilities.setNetworkBytesUint16(buf, pos, act.len);
			pos += Utilities.setNetworkBytesUint16(buf, pos, act.port);
			pos += Utilities.setNetworkBytesUint16(buf, pos, act.max_len);
		}

		if (OFPConstants.OP_UNBUFFERED_BUFFER_ID == bufferId) {
		    // TODO: potential room for optimization
		    for (int i=0;i<data.size;i++) {
			buf[pos+i] = data.data[i];
		    }
		    pos += data.size;
		}
		    
		Utilities.Assert((pos-index)==length, "pos-length does not match length!");
		
		return length;
	}
}
