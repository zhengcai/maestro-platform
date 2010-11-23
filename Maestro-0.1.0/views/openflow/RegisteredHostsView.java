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
