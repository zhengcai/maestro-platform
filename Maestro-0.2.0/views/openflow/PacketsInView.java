/*
  PacketsInView.java

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
import sys.Parameters;
import views.View;
import events.Event;
import events.openflow.PacketInEvent;

/**
 * Contains a number of PacketInEvent.
 * The inputbatching behavior is currently realized in this view.
 * @author Zheng Cai
 */
public class PacketsInView extends View {
    public LinkedList<LinkedList<PacketInEvent>> queues;
    public LinkedList<PacketInEvent> incoming;
    
    public PacketsInView() {
	queues = new LinkedList<LinkedList<PacketInEvent>>();
	incoming = new LinkedList<PacketInEvent>();
    }

    @Override
	public void commit(Driver driver) {
	
    }

    @Override
	public boolean processEvent(Event e) {

	if (!(e instanceof PacketInEvent)) {
	    return false;
	}

	if (!((PacketInEvent)e).dummy)
	    incoming.addLast((PacketInEvent)e);

	if (((PacketInEvent)e).flush) {
	    return incoming.size() > 0;
	}

	return false;

	/*
	if (incoming.size() >= Parameters.batchInputNum) {
	    return true;
	} else {
	    return false;
	}
	*/
    }
    
    @Override
	public boolean whetherInterested(Event e) {
	if (e instanceof PacketInEvent)
	    return true;
	return false;
    }

    @Override
	public void print() {

    }
}
