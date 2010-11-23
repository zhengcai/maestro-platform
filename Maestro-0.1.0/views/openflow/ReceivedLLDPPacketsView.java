package views.openflow;

import java.util.LinkedList;

import drivers.Driver;

import events.Event;
import events.openflow.LLDPPacketInEvent;
import views.View;

public class ReceivedLLDPPacketsView extends View {
	private static final long serialVersionUID = -5946439688732915769L;
	
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

}
