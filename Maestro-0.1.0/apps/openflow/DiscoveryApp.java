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

import events.openflow.LLDPPacketInEvent;
import events.openflow.PacketInEvent;
import events.openflow.PacketOutEvent;
import events.openflow.SwitchJoinEvent;
import sys.Utilities;
import sys.Parameters;
import views.ViewsIOBucket;
import views.openflow.ConnectivityLocalView;
import views.openflow.JoinedSwitchesView;
import views.openflow.PacketsOutView;
import views.openflow.ReceivedLLDPPacketsView;
import apps.App;

/**
 * DiscoveryApp maintains the topology of the network.
 * It first process switch join and switch leave event,
 * to manage all switches exist in the network.
 * It also construct the link map based on the LLDP packets
 * received on switches, sent out by ProbeApp
 */
public class DiscoveryApp extends App {
    public boolean cold = true;
    
    @Override
	public ViewsIOBucket process(ViewsIOBucket input) {
	JoinedSwitchesView sws = (JoinedSwitchesView)input.getView(0);
	ReceivedLLDPPacketsView lldps = (ReceivedLLDPPacketsView)input.getView(1);
	ConnectivityLocalView conn = (ConnectivityLocalView)input.getView(2);
	
	sws.acquireWrite();
	//. TODO: handle the switches specified in the sws.removed
	sws.releaseWrite();
		
	//. Process LLDP packets to construct the ConnectivityLocalView
	lldps.acquireWrite();
	conn.acquireWrite();
	for (LLDPPacketInEvent lldp : lldps.lldps) {
	    /*
	    Utilities.printlnDebug(String.format("Updating link %d(%d)-->%d(%d)", 
						 lldp.srcDpid, lldp.srcPort, lldp.dstDpid, lldp.dstPort));
	    */
	    conn.addLink(new ConnectivityLocalView.Link(lldp.srcDpid, lldp.srcPort, lldp.dstDpid, lldp.dstPort));
	}

	conn.releaseWrite();
	lldps.lldps.clear();
	lldps.releaseWrite();
		
	ViewsIOBucket output = new ViewsIOBucket();
	output.addView(0, sws);
	output.addView(1, conn);
	return output;
    }
}
