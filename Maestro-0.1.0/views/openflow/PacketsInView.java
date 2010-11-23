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

import java.util.LinkedList;

import drivers.Driver;
import sys.Parameters;
import views.View;
import events.Event;
import events.openflow.PacketInEvent;

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
	long before = 0;
	if (Parameters.measurePerf) {
	    before = System.nanoTime();
	}

	synchronized(incoming) {
	    incoming.addLast((PacketInEvent)e);
	    int queueSize = Parameters.am.taskMgr.getQueueSize();
		
	    if (incoming.size() >= Parameters.batchInputNum || queueSize <= 5) {
		synchronized (queues) {
		    LinkedList<PacketInEvent> toAdd = new LinkedList<PacketInEvent>();
		    toAdd.addAll(incoming);
		    queues.addLast(toAdd);
		}
		incoming.clear();
		return true;
	    } else {
		return false;
	    }
	}
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
