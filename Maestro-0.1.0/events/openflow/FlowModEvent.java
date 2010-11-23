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
*/

package events.openflow;

import drivers.OFPConstants;
import sys.Utilities;

/**
   Currently this is for OpenFlow Version 1.0.0
*/
public class FlowModEvent extends ToSpecificSwitchEvent implements Comparable<FlowModEvent> {
    public FlowModEvent() {
	super(-1);
    }
	
    public FlowModEvent(int idx) {
	super(idx);
    }

    public long xid;
    public long wildCards;
    public int inPort;
    public PacketInEvent.FlowInfo flow;
    public long cookie;
	
    public int command;
    public int idleTimeout;
    public int hardTimeout;
    public int priority;
    public long bufferId;
    public int outPort;
    public int flags;
    public long reserved;
    public int actionsLen;
    public PacketOutEvent.Action[] actions;
	
    @Override
	public int compareTo(FlowModEvent o) {
	Long mine = dpid;
	return mine.compareTo(o.dpid);
    }
	
    public int getLength() {
	int length = OFPConstants.OfpConstants.OFP_FLOW_MOD_LENGTH;
	for (PacketOutEvent.Action act : actions) {
	    length += act.len;
	}
	return length;
    }
	
    public int convertToBytes(byte[] buf, int index) {
	int length = OFPConstants.OfpConstants.OFP_FLOW_MOD_LENGTH;
	for (PacketOutEvent.Action act : actions) {
	    length += act.len;
	}
	int pos = index;
	//. ofp header
	pos += Utilities.setNetworkBytesUint8(buf, pos, OFPConstants.OfpConstants.OFP_VERSION);
	pos += Utilities.setNetworkBytesUint8(buf, pos, OFPConstants.PacketTypes.OFPT_FLOW_MOD);
	pos += Utilities.setNetworkBytesUint16(buf, pos, length);
	pos += Utilities.setNetworkBytesUint32(buf, pos, xid);
		
	//. match
	pos += Utilities.setNetworkBytesUint32(buf, pos, wildCards);
	pos += Utilities.setNetworkBytesUint16(buf, pos, inPort);
	for (int i=0;i<OFPConstants.OfpConstants.OFP_ETH_ALEN;i++) {
	    pos += Utilities.setNetworkBytesUint8(buf, pos, flow.dlSrc[i]);
	}
	for (int i=0;i<OFPConstants.OfpConstants.OFP_ETH_ALEN;i++) {
	    pos += Utilities.setNetworkBytesUint8(buf, pos, flow.dlDst[i]);
	}
	pos += Utilities.setNetworkBytesUint16(buf, pos, flow.dlVlan);
	pos += Utilities.setNetworkBytesUint8(buf, pos, flow.dlVlanPcp);
	pos += 1; //. one byte pad, for openflow 1.0.0
	pos += Utilities.setNetworkBytesUint16(buf, pos, flow.dlType);
	pos += Utilities.setNetworkBytesUint8(buf, pos, flow.nwTos);
	pos += Utilities.setNetworkBytesUint8(buf, pos, flow.nwProto);
	pos += 2; //. two bytes pad, for openflow 1.0.0
	//pos += 1; //. one byte pad, for openflow 0.8.9
	pos += Utilities.setNetworkBytesUint32(buf, pos, flow.nwSrc);
	pos += Utilities.setNetworkBytesUint32(buf, pos, flow.nwDst);
	if (flow.nwProto == OFPConstants.OfpConstants.IP_TYPE_ICMP) {
	    pos += Utilities.setNetworkBytesUint16(buf, pos, flow.icmpType);
	    pos += Utilities.setNetworkBytesUint16(buf, pos, flow.icmpCode);
	} else {
	    pos += Utilities.setNetworkBytesUint16(buf, pos, flow.tpSrc);
	    pos += Utilities.setNetworkBytesUint16(buf, pos, flow.tpDst);
	}

	pos += Utilities.setNetworkBytesUint64(buf, pos, cookie);
		
	//. flow actions
	pos += Utilities.setNetworkBytesUint16(buf, pos, command);
	pos += Utilities.setNetworkBytesUint16(buf, pos, idleTimeout);
	pos += Utilities.setNetworkBytesUint16(buf, pos, hardTimeout);
	pos += Utilities.setNetworkBytesUint16(buf, pos, priority);
	pos += Utilities.setNetworkBytesUint32(buf, pos, bufferId);
	pos += Utilities.setNetworkBytesUint16(buf, pos, outPort);
	pos += Utilities.setNetworkBytesUint16(buf, pos, flags);

	//. This is only for OpenFlow 0.8.9
	//pos += Utilities.setNetworkBytesUint32(buf, pos, reserved);
	for (PacketOutEvent.Action act : actions) {
	    //. Currently only support output action
	    pos += Utilities.setNetworkBytesUint16(buf, pos, act.type);
	    pos += Utilities.setNetworkBytesUint16(buf, pos, act.len);
	    pos += Utilities.setNetworkBytesUint16(buf, pos, act.port);
	    pos += Utilities.setNetworkBytesUint16(buf, pos, act.max_len);
	}
	Utilities.Assert((pos-index)==length, "pos-length does not match length!");
		
	return length;
    }
}
