/*
  PacketsOutView.java

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

package views.openflow;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.ArrayList;

import drivers.Driver;

import events.Event;
import events.openflow.PacketOutEvent;
import views.View;
import sys.Parameters;
import sys.Utilities;

/**
 * Contains a number of PacketOutEvent
 * @author Zheng Cai
 */
public class PacketsOutView extends View {
    public HashMap<Long, ArrayList<Event>> pkts;
    private ArrayList<ArrayList<Event>> remaining;
    private boolean[] who = new boolean[512];
	
    public PacketsOutView() {
	pkts = new HashMap<Long, ArrayList<Event>>();
	remaining = new ArrayList<ArrayList<Event>>();
    }
	
    public void addPacketOutEvent(PacketOutEvent po) {
	ArrayList<Event> pktHolder = pkts.get(po.dpid);
	if (pktHolder == null) {
	    pktHolder = new ArrayList<Event>();
	    pkts.put(po.dpid, pktHolder);
	}
	pktHolder.add(po);
    }

    @Override
	public void commit(Driver driver) {
	for (ArrayList<Event> events : pkts.values()) {
	    if (events.size() > 0)
		if (!driver.commitEvent(events))
		    remaining.add(events);
	}
	//pkts.clear();

	if (remaining.size() > 512) {
	    System.err.println("BAD!! LARGER THAN 512");
	    Utilities.ForceExit(0);
	}
	for (int i=0;i<remaining.size();i++) {
	    who[i] = true;
	}
	int rem = remaining.size();
	int size = rem;

	int i = 0;
	while (rem > 0) {
	    if (who[i]) {
		ArrayList<Event> events = remaining.get(i);
		if (driver.commitEvent(events)) {
		    who[i] = false;
		    rem --;
		}
	    }
	    i = (++i)%size;
	}

	/*
	for (ArrayList<Event> events : remaining) {
	    while (!driver.commitEvent(events));
	}
	*/
	
	remaining.clear();
    }

    @Override
	public boolean processEvent(Event e) {
	return true;
    }

    @Override
	public boolean whetherInterested(Event e) {
	return false;
    }

    public int getTotalSize() {
	int ret = 0;
	for (ArrayList<Event> events : pkts.values()) {
	    ret += events.size();
	}
	return ret;
    }

    @Override
	public void print() {

    }
}
