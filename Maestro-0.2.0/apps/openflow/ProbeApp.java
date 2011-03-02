/*
  ProbeApp.java

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

package apps.openflow;

import events.openflow.*;
import drivers.OFPConstants;
import sys.Utilities;
import sys.Parameters;
import views.ViewsIOBucket;
import views.openflow.*;
import apps.App;
import headers.LLDPHeader;

/**
 * ProbeApp periodically send out LLDP packets on ports of all switches
 * To help the DiscoveryApp discover links in the network
 * @author Zheng Cai
 */
public class ProbeApp extends App {
    public static final int DEFAULT_TTL = 255;
    
    @Override
	public ViewsIOBucket process(ViewsIOBucket input) {
	//. Assume view at pos 0 is JoinedSwitchesView, 
	JoinedSwitchesView sws = (JoinedSwitchesView)input.getView(0);
	PacketsOutView pkts = new PacketsOutView();
	
	//. Send out LLDP packets for all switches
	sws.acquireWrite();
	for (SwitchJoinEvent i : sws.all.values()) {
	    for (SwitchJoinEvent.PhysicalPort p : i.ports) {
		PacketOutEvent po = constructLLDPPacket(i, p);
		if (po != null) {
		    pkts.addPacketOutEvent(po);
		}
	    }
	}
	sws.releaseWrite();

	ViewsIOBucket output = new ViewsIOBucket();
	output.addView(0, pkts);
	return output;
    }

    /**
     * Construct a LLDP packet for a given port of a switch
     * @param sw the switch
     * @param port the physical port of the switch
     * @return the constructed LLDP packet (contained in a PacketOutEvent)
     */
    private PacketOutEvent constructLLDPPacket(SwitchJoinEvent sw, SwitchJoinEvent.PhysicalPort port) {
	//. TODO: Currently not compatible to standard LLDP, nor to NOX
	//. right now just a temporary simplified implementation for Maestro
	PacketOutEvent ret = new PacketOutEvent();
	ret.send = true;
	ret.dpid = sw.dpid;
	ret.bufferId = OFPConstants.OP_UNBUFFERED_BUFFER_ID;
	ret.inPort = OFPConstants.OfpPort.OFPP_CONTROLLER;
	ret.actions = new PacketOutEvent.Action[1];
	ret.actions[0] = PacketOutEvent.makeOutputAction(port.portNo);
	ret.actionsLen = ret.actions[0].len;
		
	//. The data section contains: [dstMAC(6) srcMAC(6) ethType(2) chassisId(2+long) portId(2+unsigned short) ttl(2+unsigned short) end(2)]
	ret.dataLen = OFPConstants.OfpConstants.OFP_ETH_ALEN*2 + 2 + 2 + Long.SIZE/8 + 2 + Short.SIZE/8 + 2 + Short.SIZE/8 + 2;
	//ret.data = new byte[ret.dataLen];
	ret.data = new PacketInEvent.DataPayload(ret.dataLen);
	int index = 0;
	for (int i=0;i<OFPConstants.OfpConstants.OFP_ETH_ALEN;i++) {
	    try {
		ret.data.data[OFPConstants.OfpConstants.OFP_ETH_ALEN+i] = (byte)port.hwAddr[i];
	    } catch (NullPointerException e) {
		Utilities.printlnDebug("in constructing LLDP: port "+port+" does not exist for switch "+sw.dpid);
		return null;
	    }
	    ret.data.data[i] = OFPConstants.OfpConstants.NDP_MULTICAST[i];
	}
	index += OFPConstants.OfpConstants.OFP_ETH_ALEN*2;
		
	//. Reverse the byte order to achieve htons effect, for ETH_TYPE_LLDP
	ret.data.data[index++] = OFPConstants.OfpConstants.ETH_TYPE_LLDP_B1;
	ret.data.data[index++] = OFPConstants.OfpConstants.ETH_TYPE_LLDP_B0;

	/*
	Utilities.setBytesLong(ret.data.data, index, sw.dpid);
	index += Long.SIZE/8;
	Utilities.setBytesInt(ret.data.data, index, port.portNo);
	*/
	LLDPHeader.TLV tlv = new LLDPHeader.TLV();
	tlv.type = LLDPHeader.TLV_TYPE_CHASSIS_ID;
	tlv.length = Long.SIZE/8;
	tlv.value = new byte[Long.SIZE/8];
	Utilities.setNetworkBytesUint64(tlv.value, 0, sw.dpid);
	index = tlv.convertToBytes(ret.data.data, index);

	tlv.type = LLDPHeader.TLV_TYPE_PORT_ID;
	tlv.length = Short.SIZE/8;
	tlv.value = new byte[Short.SIZE/8];
	Utilities.setNetworkBytesUint16(tlv.value, 0, port.portNo);
	index = tlv.convertToBytes(ret.data.data, index);

	tlv.type = LLDPHeader.TLV_TYPE_TTL;
	tlv.length = Short.SIZE/8;
	tlv.value = new byte[Short.SIZE/8];
	Utilities.setNetworkBytesUint16(tlv.value, 0, DEFAULT_TTL);
	index = tlv.convertToBytes(ret.data.data, index);

	tlv.type = LLDPHeader.TLV_TYPE_END;
	tlv.length = LLDPHeader.TLV_LENGTH_END;
	index = tlv.convertToBytes(ret.data.data, index);

	return ret;
    }
}
