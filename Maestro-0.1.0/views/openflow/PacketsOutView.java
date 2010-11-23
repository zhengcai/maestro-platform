package views.openflow;

import java.util.HashMap;
import java.util.LinkedList;

import drivers.Driver;

import events.Event;
import events.openflow.PacketOutEvent;
import views.View;

public class PacketsOutView extends View {
	private static final long serialVersionUID = -8034608737379288275L;
	
	public HashMap<Long, LinkedList<Event>> pkts;
	
	public PacketsOutView() {
		pkts = new HashMap<Long, LinkedList<Event>>();
	}
	
	public void addPacketOutEvent(PacketOutEvent po) {
		LinkedList<Event> pktHolder = pkts.get(po.dpid);
	    if (pktHolder == null) {
	    	pktHolder = new LinkedList<Event>();
	    	pkts.put(po.dpid, pktHolder);
	    }
	    pktHolder.addLast(po);
	}

	@Override
	public void commit(Driver driver) {
		for (LinkedList<Event> events : pkts.values()) {
			driver.commitEvent(events);
			//events.clear();
		}
		pkts.clear();
	}

	@Override
	public boolean processEvent(Event e) {
		return true;
	}

	@Override
	public boolean whetherInterested(Event e) {
		// interested in nothing
		return false;
	}

    public int getTotalSize() {
	int ret = 0;
	for (LinkedList<Event> events : pkts.values()) {
	    ret += events.size();
	}
	return ret;
    }
}
