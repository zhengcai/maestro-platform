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

package apps.openflow;

import events.openflow.*;
import drivers.OFPConstants;
import sys.Utilities;
import sys.Parameters;
import views.ViewsIOBucket;
import views.openflow.*;
import apps.App;

/**
 * ProbeApp periodically send out LLDP packets on ports of all switches
 * To help the DiscoveryApp discover links in the network
 */
public class ProbeApp extends App {
    @Override
	public ViewsIOBucket process(ViewsIOBucket input) {
	//. Assume view at pos 0 is JoinedSwitchesView, 
	JoinedSwitchesView sws = (JoinedSwitchesView)input.getView(0);
	PacketsOutView pkts = new PacketsOutView();
	
	// Send out LLDP packets for all switches
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
	// TODO: Currently not compatible to standard LLDP, nor to NOX
	// right now just a temporary simplified implementation for Maestro
	PacketOutEvent ret = new PacketOutEvent();
	ret.dpid = sw.dpid;
	ret.bufferId = OFPConstants.OP_UNBUFFERED_BUFFER_ID;
	ret.inPort = OFPConstants.OfpPort.OFPP_CONTROLLER;
	ret.actions = new PacketOutEvent.Action[1];
	ret.actions[0] = PacketOutEvent.makeOutputAction(port.portNo);
	ret.actionsLen = ret.actions[0].len;
		
	//. The data section contains: [dstMAC(6) srcMAC(6) ethType(2) dpid(8) portNo(4)]
	ret.dataLen = OFPConstants.OfpConstants.OFP_ETH_ALEN*2 + 2 + Long.SIZE/8 + Integer.SIZE/8;
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
		
	Utilities.setBytesLong(ret.data.data, index, sw.dpid);
	index += Long.SIZE/8;
	Utilities.setBytesInt(ret.data.data, index, port.portNo);
	return ret;
    }
}
