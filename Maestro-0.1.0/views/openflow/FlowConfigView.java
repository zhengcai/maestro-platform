package views.openflow;

import java.util.HashMap;
import java.util.LinkedList;

import drivers.Driver;
import events.Event;
import events.openflow.FlowModEvent;
import views.View;

public class FlowConfigView extends View {
	private static final long serialVersionUID = -35551105457536035L;
	
	public HashMap<Long, LinkedList<Event>> configs;
	
	public FlowConfigView() {
		configs = new HashMap<Long, LinkedList<Event>>();
	}
	
	public void addFlowModEvent(FlowModEvent fm) {
		LinkedList<Event> holder = configs.get(fm.dpid);
	    if (holder == null) {
	    	holder = new LinkedList<Event>();
	    	configs.put(fm.dpid, holder);
	    }
	    holder.addLast(fm);
	}

	@Override
	public void commit(Driver driver) {
	    for (LinkedList<Event> events : configs.values()) {
		driver.commitEvent(events);
	    }
	    configs.clear();
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
