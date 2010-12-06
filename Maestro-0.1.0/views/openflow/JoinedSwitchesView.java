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

import drivers.Driver;
import views.View;
import events.*;
import events.openflow.SwitchJoinEvent;
import events.openflow.SwitchLeaveEvent;
import sys.Utilities;

public class JoinedSwitchesView extends View {
    /** All switches that exist in the network */
    public HashMap<Long, SwitchJoinEvent> all;

    /**
     * Switches that have just left the network
     * After usage, clear this data structure
     */
    public HashMap<Long, SwitchJoinEvent> removed;

    public JoinedSwitchesView() {
	all = new HashMap<Long, SwitchJoinEvent>();
	removed = new HashMap<Long, SwitchJoinEvent>();
    }
	
    @Override
	public boolean whetherInterested(Event e) {
	if (e instanceof SwitchJoinEvent)
	    return true;
	if (e instanceof SwitchLeaveEvent)
	    return true;
	return false;
    }

    @Override
	public boolean processEvent(Event e) {
	if (e instanceof SwitchJoinEvent) {
	    return handleSwitchJoinEvent((SwitchJoinEvent)e);
	}
	if (e instanceof SwitchLeaveEvent) {
	    return handleSwitchLeaveEvent((SwitchLeaveEvent)e);
	}
		
	return false;
    }
	
    @Override
	public void commit(Driver driver) {

    }
	
    private boolean handleSwitchJoinEvent(SwitchJoinEvent sj) {
	acquireWrite();
	all.put(sj.dpid, sj);
	releaseWrite();
		
	return true;
    }
	
    private boolean handleSwitchLeaveEvent(SwitchLeaveEvent sl) {
	acquireWrite();
	SwitchJoinEvent sj = all.get(sl.dpid);
	if (null == sj) {
	    Utilities.printlnDebug("Cannot find a switch with dpid "+sl.dpid);
	    return false;
	}
	removed.put(sj.dpid, sj);
	all.remove(sl.dpid);
	releaseWrite();
	return true;
    }

    @Override
	public void print() {
	for (SwitchJoinEvent e : all.values()) {
	    System.out.println(e);
	}
    }

    public SwitchJoinEvent getSwitch(long dpid) {
	return all.get(dpid);
    }
}
