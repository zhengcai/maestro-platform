package views.openflow;

import java.util.LinkedList;

import drivers.Driver;
import views.View;
import events.Event;
import events.openflow.PacketInEvent;

public class FlowsInView extends View {
	private static final long serialVersionUID = 8406602400958884846L;
	
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
}
