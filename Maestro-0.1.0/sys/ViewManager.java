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

package sys;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Semaphore;

import drivers.Driver;
import events.Event;
import views.View;

/**
 * The ViewManager
 * @author Zheng
 */
public class ViewManager {
    public ApplicationManager am;
    
    /** Semaphore for shared variables */
    public Semaphore synchro;
    
    /** The global environment */
    Environment global;
    
    HashMap<String, String> eventToView;
    
    /** The driver for the network */
    public Driver driver;
    
    public ViewManager(ApplicationManager a) {
    	am = a;
    	synchro = new Semaphore(1);
    	global = new Environment();
    	eventToView = new HashMap<String, String>();
    }
    
    public void registerEvent(Event event, String viewName) {
    	View view = global.getView(viewName);
    	Utilities.Assert(view != null, "View "+viewName+" does not exist!");
    	Utilities.Assert(view.whetherInterested(event), "View "+viewName+" is not interested in "+event.getClass().getSimpleName());
    	eventToView.put(event.getClass().getSimpleName(), viewName);
    }
    
    /** Start running the driver in the main thread */
    public void startDriver(String name) {
    	try {
	    driver = (Driver)Class.forName("drivers."+name).newInstance();
	} catch (InstantiationException e) {
	    Utilities.Assert(false, "driver-"+name+" initialization fault");
	} catch (IllegalAccessException e) {
	    Utilities.Assert(false, "driver-"+name+" illegal access exception");
	} catch (ClassNotFoundException e) {
	    Utilities.Assert(false, "driver-"+name+" class not found");
	}
		
	driver.vm = this;
	driver.start();
    }
    
    /**
     * Get the view instance based on its instance name, from the global environment
     */
    public View getViewInstance(String instName) {
    	return global.getView(instName);
    }
    
    /**
     * Post an event to the view manager, to be processed by a view, and finally
     * trigger the relative DAG to handle it.
     * Usually called by the driver.
     * @param e The event to post
     */
    public void postEvent(Event e) {
    	String viewName = eventToView.get(e.getClass().getSimpleName());
    	if (viewName == null) {
	    return;
    	}
    	
    	//. TODO: currently trigger the DAG for each event, no batching yet
    	View v = global.getView(viewName);
    	if (v == null) {
	    return;
    	}
    	
    	if (v.processEvent(e)) {
	    HashSet<String> trigger = new HashSet<String>();
	    trigger.add(viewName);
	    am.triggerDag(global, trigger);
    	}
    }
    
    /**
     * Post an event to the view manager, to be processed by the specific view, and finally
     * trigger the relative DAG to handle it.
     * Usually called by the driver.
     * @param e The event to post
     * @param viewName The name of the specific view
     */
    public void postEventToSpecificView(Event e, String viewName) {
    	if (viewName == null) {
	    return;
    	}
    	
    	//. TODO: currently trigger the DAG for each event, no batching yet
    	View v = global.getView(viewName);
    	if (v == null) {
	    return;
    	}
    	
    	if (v.processEvent(e)) {
	    HashSet<String> trigger = new HashSet<String>();
	    trigger.add(viewName);
	    am.triggerDag(global, trigger);
    	}
    }
    
    public void postEventWithoutTrigger(Event e) {
    	String viewName = eventToView.get(e.getClass().getSimpleName());
    	if (viewName == null) {
	    return;
    	}
    	
    	//. TODO: currently trigger the DAG for each event, no batching yet
    	View v = global.getView(viewName);
    	if (v == null) {
	    return;
    	}
    	
    	v.processEvent(e);
    }

    public void timerStartDag(DAG dag) {
	am.startDag(global, dag);
    }
}
