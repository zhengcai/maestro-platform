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

package views.openflow;

import java.util.HashMap;
import java.util.LinkedList;

import drivers.Driver;
import events.Event;
import events.openflow.FlowModEvent;
import views.View;

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
	for (LinkedList<Event> events : configs.values()) {
	    driver.commitEvent(events);
	}
	configs.clear();
    }

    @Override
	public boolean processEvent(Event e) {
	return false;
    }

    @Override
	public boolean whetherInterested(Event e) {
	// TODO Auto-generated method stub
	return false;
    }

    @Override
	public void print() {

    }
}
