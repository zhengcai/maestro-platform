/*
  SwitchJoinEvent.java

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

package events.openflow;

import drivers.OFPConstants;
import events.Event;

/**
 * @author Zheng Cai
 */
public class SwitchJoinEvent extends Event {
    public static class PhysicalPort {
	public int portNo;
	public short[] hwAddr;
	public byte[] name;
	public long config;
	public long state;
	public long curr;
	public long advertised;
	public long supported;
	public long peer;
		
	public PhysicalPort() {
	    hwAddr = new short[OFPConstants.OfpConstants.OFP_ETH_ALEN];
	    name = new byte[OFPConstants.OfpConstants.OFP_MAX_PORT_NAME_LEN];
	}
    }
    public long dpid;
    public long nBuffers;
    public short nTables;
    public long capabilities;
    public long actions;
    public int nPorts;
    public PhysicalPort[] ports;

    @Override
	public String toString() {
	return String.format("Switch %d: nBuffer=%d, nTables=%d, capabilities=%d, actions=%d, nPorts=%d",
			     dpid, nBuffers, nTables, capabilities, actions, nPorts);
    }
	
    public PhysicalPort findPort(int portNo) {
	for (PhysicalPort p : ports) {
	    if (p.portNo == portNo)
		return p;
	}
	return null;
    }

    @Override
	public int convertToBytes(byte[] buf, int index) {
	return 0;
    }
}
