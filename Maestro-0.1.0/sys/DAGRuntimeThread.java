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

import views.*;

/**
 * 
 * @author Zheng
 *
 */
public class DAGRuntimeThread implements Runnable {
    AppInstanceNode current;
    private ViewsIOBucket currentReservedViews;
    ApplicationManager am;
    DAGRuntime dr;
	
    /** Create a new instance of NodeThread*/
    public DAGRuntimeThread(AppInstanceNode start, ApplicationManager a, DAGRuntime drtime) {
	current = start;
	am = a;
	dr = drtime;
	//this.setPriority(Thread.NORM_PRIORITY);
        //this.setDaemon(true);
    }
	
    public void run() {
	executeApp();
    }
	
    /* TODO Viewsbucket should be replaced by environment wherever possible, to avoid
     * the silly redundancy of having to store a one-use viewsbucket in an instance
     * variable.  Also, "node" shouldn't be an instance variable, but in the spirit of
     * KISS, I'm leaving this as is for now.
     */
    public void executeApp(AppInstanceNode next) {
	current = next;
	executeApp();
    }
	
    private void executeApp() {
	/* TODO Uncomment this after ApplicationManager is done
	   currentReservedViews = dr.env.acquireViewsForNode(current);
	   ViewsIOBucket result = current.app.process(currentReservedViews);
	   if (result == null) {
	   am.DAGAbort(dr);
	   return;
	   }
	   am.produce(this, result);
	*/
		
	ViewsIOBucket input = dr.env.getAppNodeReadViews(current);
	ViewsIOBucket result = current.app.process(input);
	am.produce(this, result);
    }
	
    public ViewsIOBucket getCurrentReservedViews() {
	return currentReservedViews;
    }
}
