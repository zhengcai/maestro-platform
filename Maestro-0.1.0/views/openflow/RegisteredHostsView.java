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

public class RegisteredHostsView extends View {
    private static final long serialVersionUID = 3100268063129687196L;
	
    public static class Location {
	public long dpid;
	public int port;
		
	public Location(long dp, int p) {
	    dpid = dp;
	    port = p;
	}
    }

    public static final Location MAC_Broad_Cast = new Location(0, 0);
    public static final Location Location_Unknown = new Location(0, 0);
	
    /** Where the end hosts is bound, based on its MAC address */
    HashMap<Long, Location> hosts;
	
    public RegisteredHostsView() {
	hosts = new HashMap<Long, Location>();
    }
	
    public Location getHostLocation(short[] mac) {
	return hosts.get(Utilities.GetLongFromMAC(mac));
    }
	
    public Location addHostLocation(short[] mac, Location l) {
	return hosts.put(Utilities.GetLongFromMAC(mac), l);
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

}
