/*
  RegisteredHostsView.java

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

import drivers.Driver;
import events.Event;
import sys.Utilities;
import views.View;

/**
 * Contains registered hosts' MAC addresses,
 * used by the LocationManagementApp.
 * @author Zheng Cai
 */
public class RegisteredHostsView extends View {
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

    public Location removeHostLocation(short[] mac) {
	return hosts.remove(Utilities.GetLongFromMAC(mac));
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
