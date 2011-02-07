/*
  FlowConfigView.java

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
import events.openflow.FlowModEvent;
import views.View;

/**
 * Contains all flow configuration events, a hash map indexed by
 * the target's DPID
 * @author Zheng Cai
 */
public class FlowConfigView extends View {
    public HashMap<Long, LinkedList<Event>> configs;
	
    public FlowConfigView() {
	configs = new HashMap<Long, LinkedList<Event>>();
    }
	
    public void addFlowModEvent(FlowModEvent fm) {
	LinkedList<Event> holder = configs.get(fm.dpid);
	if (holder == null) {
	    holder = new LinkedList<Event>();
	    configs.put(fm.dpid, holder);
	}
	holder.addLast(fm);
    }

    @Override
	public void commit(Driver driver) {
	ArrayList<LinkedList<Event>> remaining = new ArrayList<LinkedList<Event>>();
	for (LinkedList<Event> events : configs.values()) {
	    if (!driver.commitEvent(events))
		remaining.add(events);
	}
	configs.clear();
	for (LinkedList<Event> events : remaining) {
	    while (!driver.commitEvent(events));
	}
	remaining.clear();
    }

    @Override
	public boolean processEvent(Event e) {
	return false;
    }

    @Override
	public boolean whetherInterested(Event e) {
	return false;
    }

    @Override
	public void print() {

    }
}
