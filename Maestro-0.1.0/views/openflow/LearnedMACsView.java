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
import events.Event;
import sys.Utilities;
import views.View;

public class LearnedMACsView extends View {    
    /** All MAC addresses on each of all switches
	First index(Long) is switch's dpid
	Second index(Long) is the MAC
	The value(Integer) is the port on that switch where the MAC was learned
    */
    HashMap<Long, HashMap<Long, Integer>> learnedMACs;
	
    public LearnedMACsView() {
	learnedMACs = new HashMap<Long, HashMap<Long, Integer>>();
    }
	
    public Integer getMACLocation(long dpid, short[] mac) {
	HashMap<Long, Integer> sw = learnedMACs.get(dpid);
	if (null == sw) return null;
	return sw.get(Utilities.GetLongFromMAC(mac));
    }
	
    public void addMACLocation(long dpid, short[] mac, int port) {
	HashMap<Long, Integer> sw = learnedMACs.get(dpid);
	if (null == sw) {
	    sw = new HashMap<Long, Integer>();
	    learnedMACs.put(dpid, sw);
	}
	sw.put(Utilities.GetLongFromMAC(mac), port);
    }

    @Override
	public void commit(Driver driver) {
		
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
