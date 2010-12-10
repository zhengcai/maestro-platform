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

import drivers.Driver;

import events.Event;
import events.openflow.PacketOutEvent;
import views.View;

/**
 * Contains a number of PacketOutEvent
 * @author Zheng Cai
 */
public class PacketsOutView extends View {
    public HashMap<Long, LinkedList<Event>> pkts;
	
    public PacketsOutView() {
	pkts = new HashMap<Long, LinkedList<Event>>();
    }
	
    public void addPacketOutEvent(PacketOutEvent po) {
	LinkedList<Event> pktHolder = pkts.get(po.dpid);
	if (pktHolder == null) {
	    pktHolder = new LinkedList<Event>();
	    pkts.put(po.dpid, pktHolder);
	}
	pktHolder.addLast(po);
    }

    @Override
	public void commit(Driver driver) {
	for (LinkedList<Event> events : pkts.values()) {
	    driver.commitEvent(events);
	    //events.clear();
	}
	pkts.clear();
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
	for (LinkedList<Event> events : pkts.values()) {
	    ret += events.size();
	}
	return ret;
    }

    @Override
	public void print() {

    }
}
