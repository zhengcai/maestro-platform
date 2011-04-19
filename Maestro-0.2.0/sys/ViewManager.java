/*
  ViewManager.java

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

package sys;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Semaphore;

import drivers.Driver;
import events.Event;
import views.View;

/**
 * ViewManager: the view manager
 * @author Zheng Cai
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

    public void registerEventConcurrent(Event event, String viewName) {
	for (int i=0;i<Parameters.divide;i++) {
	    View view = global.getView(viewName+"_"+i);
	    Utilities.Assert(view != null, "View "+viewName+"_"+i+" does not exist!");
	    Utilities.Assert(view.whetherInterested(event), "View "+viewName+"_"+i+" is not interested in "+event.getClass().getSimpleName());
	}
	eventToView.put(event.getClass().getSimpleName(), viewName);
    }

    /** Load the driver class */
    public void loadDriver(String bundle) {
	try {
	    driver = (Driver)Class.forName("drivers."+bundle).newInstance();
	} catch (InstantiationException e) {
	    Utilities.Assert(false, "driver-"+bundle+" initialization fault");
	} catch (IllegalAccessException e) {
	    Utilities.Assert(false, "driver-"+bundle+" illegal access exception");
	} catch (ClassNotFoundException e) {
	    Utilities.Assert(false, "driver-"+bundle+" class not found");
	}
	
	driver.vm = this;
    }
    
    /** Start running the driver in the main thread */
    public void startDriver() {
	driver.start();
    }
    
    /**
     * Get the view instance based on its instance name, from the global environment
     */
    public View getViewInstance(String instName) {
    	return global.getView(instName);
    }

    public void printAllViews() {
	for (String s : global.getViewNames()) {
	    View v = global.getView(s);
	    System.out.println("Class "
			       + v.getClass().getSimpleName()
			       + ", Name "
			       + s
			       + ", Pointer"
			       + v);
	}
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
    	
    	View v = global.getView(viewName);
    	if (v == null) {
	    return;
    	}
    	
    	if (v.processEvent(e)) {
	    am.triggerDag(global, viewName);
    	}
    }

    public void postEventConcurrent(Event e, int which) {
	String viewName = eventToView.get(e.getClass().getSimpleName());
	if (viewName == null) {
	    return;
	}

	boolean whetherConcurrentEnabled = true;

	View v = global.getView(viewName+"_"+which);
	if (v == null) {
	    whetherConcurrentEnabled = false;
	    v = global.getView(viewName);
	    if (v == null) {
		return;
	    }
	}

	if (v.processEvent(e)) {
	    if (whetherConcurrentEnabled)
		am.triggerDag(global, viewName+"_"+which);
	    else
		am.triggerDag(global, viewName);
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
	    am.triggerDag(global, viewName);
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
