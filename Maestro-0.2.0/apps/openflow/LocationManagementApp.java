/*
  LocationManagementApp.java

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

import java.util.*;

import events.openflow.PacketInEvent;
import sys.Parameters;
import sys.Utilities;
import sys.DataLogManager;
import views.ViewsIOBucket;
import views.openflow.RegisteredHostsView;
import views.openflow.FlowsInView;
import views.openflow.JoinedSwitchesView;
import views.openflow.PacketsInView;
import apps.App;

/**
 * LocationManagementApp: the application to register the source location of
 * all MAC addresses that have been seen
 * @author Zheng Cai
 */
public class LocationManagementApp extends App {
    @Override
	public ViewsIOBucket process(ViewsIOBucket input) {	    
	PacketsInView pis = (PacketsInView)input.getView(0);
	JoinedSwitchesView sws = (JoinedSwitchesView)input.getView(1);
	RegisteredHostsView hosts = (RegisteredHostsView)input.getView(2);

	//FlowsInView fis = new FlowsInView();
		
	ArrayList<PacketInEvent> work = pis.incoming;
		
	for (PacketInEvent pi : work) {
	    class LogContent extends DataLogManager.Content {
		public long dpid;
		public int size;
		public LogContent(long d, int s) {
		    dpid = d;
		    size = s;
		}
		public String toString() {
		    return String.format("%d %d\n", dpid, size);
		}
	    }
	    if (Parameters.am.dataLogMgr.enabled && pi.reason == 1) {
		if (!Parameters.warmuped) {
		    Parameters.warmuped = true;
		    //Parameters.am.dataLogMgr.addEntry(new LogContent(pi.dpid, -1));
		}
	    }
	    /*
	    if (Parameters.am.dataLogMgr.enabled && pi.reason == 2) {
		if (!Parameters.changePeriod) {
		    Parameters.changePeriod = true;
		    Parameters.am.dataLogMgr.addEntry(new LogContent(pi.dpid, -2));
		}
	    }
	    */
	    //. Warning: Disable this buffer id thing in real release! This is only for experiment
	    if (Parameters.warmuped)
		pi.bufferId = 55;
	    
	    try {
		//. Warning: currently each end host can only register once with Maestro
		if (null == hosts.getHostLocation(pi.flow.dlSrc)) {
		    hosts.acquireWrite();
		    hosts.addHostLocation(pi.flow.dlSrc, new RegisteredHostsView.Location(pi.dpid, pi.inPort));
		    /*
		    Utilities.printlnDebug("Registering "+String.format("MAC %d-%d-%d-%d-%d-%d",
									pi.flow.dlSrc[0], pi.flow.dlSrc[1], pi.flow.dlSrc[2],
									pi.flow.dlSrc[3], pi.flow.dlSrc[4], pi.flow.dlSrc[5])
					   +" at "+pi.dpid+" ("+pi.inPort+")");
		    */
		    hosts.releaseWrite();
		}
	    } catch (NullPointerException e) {
		continue;
	    }
	    RegisteredHostsView.Location dst = hosts.getHostLocation(pi.flow.dlDst);
	    if (null == dst) {
		if (Utilities.whetherMACBroadCast(pi.flow.dlDst)) {
		    //fis.queue.add(new FlowsInView.FlowIn(pi, RegisteredHostsView.MAC_Broad_Cast));
		    pi.dst = RegisteredHostsView.MAC_Broad_Cast;
		} else {
		    //fis.queue.add(new FlowsInView.FlowIn(pi, RegisteredHostsView.Location_Unknown));
		    pi.dst = RegisteredHostsView.Location_Unknown;
		}
	    } else {
		//. The registered switch has already left
		if (null == sws.getSwitch(dst.dpid)) {
		    hosts.acquireWrite();
		    hosts.removeHostLocation(pi.flow.dlDst);
		    hosts.releaseWrite();
		    //fis.queue.add(new FlowsInView.FlowIn(pi, RegisteredHostsView.Location_Unknown));
		    pi.dst = RegisteredHostsView.Location_Unknown;
		} else {
		    //fis.queue.add(new FlowsInView.FlowIn(pi, dst));
		    pi.dst = dst;
		}
	    }
	}

	//work.clear();
		
	ViewsIOBucket output = new ViewsIOBucket();
	output.addView(0, pis);
	output.addView(1, hosts);
	return output;
    }
}
