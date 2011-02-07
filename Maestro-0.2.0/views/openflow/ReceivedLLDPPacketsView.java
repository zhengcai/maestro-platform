/*
  ReceivedLLDPPacketsView.java

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

import java.util.LinkedList;

import drivers.Driver;

import events.Event;
import events.openflow.LLDPPacketInEvent;
import views.View;

/**
 * Contains pending LLDPPacketInEvent
 * @author Zheng Cai
 */
public class ReceivedLLDPPacketsView extends View {
    public LinkedList<LLDPPacketInEvent> lldps;
	
    public ReceivedLLDPPacketsView() {
	lldps = new LinkedList<LLDPPacketInEvent>();
    }

    @Override
	public void commit(Driver driver) {

    }

    @Override
	public boolean processEvent(Event e) {
	if (e instanceof LLDPPacketInEvent) {
	    acquireWrite();
	    lldps.addLast((LLDPPacketInEvent)e);
	    releaseWrite();
	    return true;
	}
	return false;
    }

    @Override
	public boolean whetherInterested(Event e) {
	if (e instanceof LLDPPacketInEvent)
	    return true;
	return false;
    }

    @Override
	public void print() {

    }
}
