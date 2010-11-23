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
import views.View;
import events.Event;
import events.openflow.PacketInEvent;

public class FlowsInView extends View {
    public static class FlowIn {
	public PacketInEvent pi;
	public RegisteredHostsView.Location dst;
		
	public FlowIn(PacketInEvent p, RegisteredHostsView.Location d) {
	    pi =  p;
	    dst = d;
	}
    }
	
    public LinkedList<FlowIn> queue;
	
    public FlowsInView() {
	queue = new LinkedList<FlowIn>();
    }

    @Override
	public void commit(Driver driver) {
	// TODO Auto-generated method stub

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
