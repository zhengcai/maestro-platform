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

import java.util.LinkedList;

import events.openflow.PacketInEvent;
import sys.Parameters;
import sys.Utilities;
import views.ViewsIOBucket;
import views.openflow.RegisteredHostsView;
import views.openflow.FlowsInView;
import views.openflow.JoinedSwitchesView;
import views.openflow.PacketsInView;
import apps.App;

public class LocationManagementApp extends App {
    @Override
	public ViewsIOBucket process(ViewsIOBucket input) {	    
	PacketsInView pis = (PacketsInView)input.getView(0);
	JoinedSwitchesView sws = (JoinedSwitchesView)input.getView(1);
	RegisteredHostsView hosts = (RegisteredHostsView)input.getView(2);

	//. For throughput measurement purpose
	if (Parameters.before == 0) {
	    Parameters.before = System.nanoTime();
	}
		
	FlowsInView fis = new FlowsInView();
		
	LinkedList<PacketInEvent> work = null;
	synchronized (pis.queues) {
	    work = pis.queues.removeFirst();
	}
		
	for (PacketInEvent pi : work) {
	    hosts.acquireWrite();
	    try {
		//. Warning: currently each end host can only register once with Maestro
		if (null == hosts.getHostLocation(pi.flow.dlSrc)) {
		    hosts.addHostLocation(pi.flow.dlSrc, new RegisteredHostsView.Location(pi.dpid, pi.inPort));
		    Utilities.printlnDebug("Registering "+String.format("MAC %d-%d-%d-%d-%d-%d",
									pi.flow.dlSrc[0], pi.flow.dlSrc[1], pi.flow.dlSrc[2],
									pi.flow.dlSrc[3], pi.flow.dlSrc[4], pi.flow.dlSrc[5])
					   +" at "+pi.dpid+" ("+pi.inPort+")");
				
		}
	    } catch (NullPointerException e) {
		continue;
	    }
	    hosts.releaseWrite();
	    RegisteredHostsView.Location dst = hosts.getHostLocation(pi.flow.dlDst);
	    if (null == dst) {
		if (Utilities.whetherMACBroadCast(pi.flow.dlDst)) {
		    fis.queue.addLast(new FlowsInView.FlowIn(pi, RegisteredHostsView.MAC_Broad_Cast));
		} else {
		    fis.queue.addLast(new FlowsInView.FlowIn(pi, RegisteredHostsView.Location_Unknown));
		}
	    } else {
		//. The registered switch has already left
		if (null == sws.getSwitch(dst.dpid)) {
		    hosts.removeHostLocation(pi.flow.dlDst);
		    fis.queue.addLast(new FlowsInView.FlowIn(pi, RegisteredHostsView.Location_Unknown));
		} else
		    fis.queue.addLast(new FlowsInView.FlowIn(pi, dst));
	    }
	}

	synchronized(Parameters.count) {
	    Parameters.count.value += work.size();
	}
		
	ViewsIOBucket output = new ViewsIOBucket();
	output.addView(0, fis);
	output.addView(1, hosts);
	return output;
    }
}
