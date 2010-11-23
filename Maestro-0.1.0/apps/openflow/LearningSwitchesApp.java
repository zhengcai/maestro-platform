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
import events.openflow.FlowModEvent;
import events.openflow.PacketOutEvent;
import sys.Parameters;
import sys.Utilities;
import views.ViewsIOBucket;
import views.openflow.LearnedMACsView;
import views.openflow.JoinedSwitchesView;
import views.openflow.PacketsInView;
import views.openflow.FlowConfigView;
import views.openflow.PacketsOutView;
import drivers.OFPConstants;

import apps.App;

public class LearningSwitchesApp extends App {
    private long currentCookie = 1;

    synchronized private long nextCookie() {
	return currentCookie ++;
    }
    
    @Override
	public ViewsIOBucket process(ViewsIOBucket input) {	    
	PacketsInView pis = (PacketsInView)input.getView(0);
	JoinedSwitchesView sws = (JoinedSwitchesView)input.getView(1);
	LearnedMACsView macs = (LearnedMACsView)input.getView(2);

	FlowConfigView config = new FlowConfigView();
	PacketsOutView pkts = new PacketsOutView();

	LinkedList<PacketInEvent> work = null;
	synchronized (pis.queues) {
	    work = pis.queues.removeFirst();
	}
		
	for (PacketInEvent pi : work) {
	    macs.acquireWrite();
	    try {
		macs.addMACLocation(pi.dpid, pi.flow.dlSrc, pi.inPort);
		Utilities.printlnDebug("Learning "+String.format("MAC %d-%d-%d-%d-%d-%d",
								 pi.flow.dlSrc[0], pi.flow.dlSrc[1], pi.flow.dlSrc[2],
								 pi.flow.dlSrc[3], pi.flow.dlSrc[4], pi.flow.dlSrc[5])
				       +" at "+pi.dpid+" ("+pi.inPort+")");
	    } catch (NullPointerException e) {
		continue;
	    }
	    macs.releaseWrite();
	    Integer outPort = macs.getMACLocation(pi.dpid, pi.flow.dlDst);
	    if (null != outPort) {
		FlowModEvent fm = null;
		if (Parameters.useMemoryMgnt) {
		    fm = Parameters.am.memMgr.allocFlowModEvent();
		} else {
		    fm = new FlowModEvent();
		}

		fm.xid = pi.xid;
		fm.flow = pi.flow;
		fm.dpid = pi.dpid;
		fm.inPort = pi.inPort;
		fm.cookie = nextCookie();
		fm.outPort = outPort.intValue();
		fm.bufferId = pi.bufferId;
		fm.command = OFPConstants.OfpFlowModCommand.OFPFC_ADD;
		fm.idleTimeout = 30;
		fm.hardTimeout = 180;
		fm.priority = 100;
		fm.flags = 0;
		fm.reserved = 0;

		if (Parameters.useMemoryMgnt) {
		    PacketOutEvent.setOutputAction(outPort.intValue(), fm.actions[0]);
		} else {
		    fm.actions = new PacketOutEvent.Action[1];
		    fm.actions[0] = PacketOutEvent.makeOutputAction(outPort.intValue());
		}

		fm.actionsLen = fm.actions[0].len;
		
		config.addFlowModEvent(fm);
	    }
	    
	    if (null == outPort || OFPConstants.OP_UNBUFFERED_BUFFER_ID == pi.bufferId) {
		PacketOutEvent po;
		if (Parameters.useMemoryMgnt) {
		    po = Parameters.am.memMgr.allocPacketOutEvent();
		} else {
		    po = new PacketOutEvent();
		}
		po.xid = pi.xid;
		po.dpid = pi.dpid;
		po.bufferId = pi.bufferId;
		po.inPort = pi.inPort;
		po.dataLen = pi.totalLen;
		po.data = pi.data;

		if (Parameters.useMemoryMgnt) {
		    PacketOutEvent.setOutputAction(null==outPort?OFPConstants.OfpPort.OFPP_FLOOD:outPort.intValue(),
						   po.actions[0]);
		} else {
		    po.actions = new PacketOutEvent.Action[1];
		    po.actions[0] = PacketOutEvent.makeOutputAction(null==outPort?OFPConstants.OfpPort.OFPP_FLOOD:outPort.intValue());
		}
		po.actionsLen = po.actions[0].len;
		pkts.addPacketOutEvent(po);
	    }
	}

	ViewsIOBucket output = new ViewsIOBucket();
	output.addView(0, config);
	output.addView(1, pkts);
	output.addView(2, macs);
	return output;
    }
}
