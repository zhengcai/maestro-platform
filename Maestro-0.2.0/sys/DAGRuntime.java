/*
  DAGRuntime.java

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

import java.util.*;
import java.util.concurrent.Semaphore;

import views.*;

/**
 * DAGRuntime: the runtime state data structure for a DAG
 * @author Zheng Cai
 */
public class DAGRuntime {
    /** The DAG source code */
    DAG dag;
	
    /** Instance ID of this DAGRuntime */
    public int instanceID;
	
    /** The environment for this DAG to run in*/
    LocalEnv env;
	
    /** Semaphore which make DAG thread safe*/
    Semaphore sem;
	
    /** Whether this DAG is currently running*/
    int state;
	
    /** Actively running threads in this DAG*/
    //ArrayList<DAGRuntimeThread> active;
	
    /** Violations generated during this run of the DAG*/
    //LinkedList<Violation> violations;
	
    /** Currently suspended application instances
     * By calling ProduceAndYield()*/
    //LinkedList<SuspendedThreadNode> suspended;
	
    /** 
     * A map from applications to the number of threads that
     * have tried to run them. This ensures that an application
     * node with an in degree greater than one will not be run repeatedly.
     * For example, if an application is designed to wait for two
     * preceding applications to finish, this will be used to track that
     * they both finish before it is executed.
     */
    Map<AppInstanceNode,Integer> threadArrivalMap;
	
    /** This is tracked so that when the DAGRuntime finishes, it
     * may remove itself from the applicationManager's list of
     * currently executing DAGs
     */
    ApplicationManager am;
	
    private long perfBefore;

    public ViewsIOBucket bucket;
    private DAGRuntimeThread tr;


    public DAGRuntime(DAG d, Environment theEnv, int instance, ApplicationManager a) {
	am = a;
	dag = d;
	sem = new Semaphore(1);
	env = new LocalEnv();
	env.addLocalENV(theEnv);
	//violations = new LinkedList<Violation>();
	//suspended = new LinkedList<SuspendedThreadNode>();
	state = Constants.DAGStates.IDLE;
	//active = new ArrayList<DAGRuntimeThread>();
	instanceID = instance;
	bucket = new ViewsIOBucket();
	tr = new DAGRuntimeThread(null, am, this);
		
	threadArrivalMap = new HashMap<AppInstanceNode,Integer>();
	for(AppInstanceNode n : d.nodes.values()) {
	    threadArrivalMap.put(n, 0);
	}
    }

    public void init(DAG d, Environment theEnv, int instance, ApplicationManager a) {
	am = a;
	dag = d;
	env.addLocalENV(theEnv);
	state = Constants.DAGStates.IDLE;
	instanceID = instance;
	threadArrivalMap.clear();
	for(AppInstanceNode n : d.nodes.values()) {
	    threadArrivalMap.put(n, 0);
	}
    }
	
    public void acquire() {
    	try {
	    sem.acquire();
    	}
    	catch (InterruptedException e) {
    		
    	}
    }
    
    public void release() {
    	sem.release();
    }
    
    /**
     * TODO Think about how DAGs are scheduled, with each node getting
     * its own thread and then terminating.  Also think about how local
     * views are created.  Views that are added to the local environment
     * later might present a picture of the network that is inconsistent
     * with earlier views.
     */
    public void start() {
	/*
	if (Parameters.am.workerMgr.getCurrentWorkerID() == 1) {
	    System.err.println("Starting DAG");
	}
	*/
	/*
	if (finished)
	    return;
	*/
    	if (Parameters.measurePerf)
	    perfBefore = System.nanoTime();
    	
    	//Utilities.printlnDebug("("+am.workerMgr.getCurrentWorkerID()+") "+System.currentTimeMillis()+" Starting DAG "+dag.id+" Instance "+instanceID);
    	state = Constants.DAGStates.RUNNING;
    	
    	//. FIXME: This code is copied from Produce().  We should consolidate them.
    	
    	Iterator<AppInstanceEdge> it = dag.activation.edges.iterator();

    	while (it.hasNext()) {
	    AppInstanceEdge e = it.next();
	    //DAGRuntimeThread tr = new DAGRuntimeThread(e.next, am, this);
	    tr.current = e.next;
	    addNodeThread(tr);
    	}
    }
    
    public void finish() {
    	//suspended.clear();
    	//active.clear();
    	env.clearLocal();
    	//violations.clear();
    	state = Constants.DAGStates.IDLE;
    	
    	if (Parameters.measurePerf) {
	    Parameters.t8 += System.nanoTime() - perfBefore;
    	}
    	//Utilities.printlnDebug("("+am.workerMgr.getCurrentWorkerID()+") "+System.currentTimeMillis()+" Finishing DAG "+dag.id+" Instance "+instanceID);
	//finished = true;
    }

    /*
    public void abort() {
    	suspended.clear();
    	
    	env.clearLocal();
    	violations.clear();
    	state = Constants.DAGStates.IDLE;
    }
    */
    
    /** Add a new NodeThread to the DAG and start the thread */
    public void addNodeThread(DAGRuntimeThread n) {
    	//active.add(n);
    	if (Parameters.divide == 0) {
	    n.run();
    	} else {
	    //am.enqueueBindingTask(n, Constants.PRIORITY_MEDIUM);
	    n.run();
    	}
    }
    
    public void delNodeThread(DAGRuntimeThread n) {
    	//active.remove(n);
    }
    
    /**
     * This method signifies that an application preceding n
     * in the currently executing DAG has terminated.  If multiple
     * branches of the DAG converge on n but not all of them
     * have terminated, then then this method returns false.  Otherwise
     * it returns true, signifying that n may be executed.
     * 
     * @param n The next application in the DAG's execution order.
     * @return Whether or not n is ready to be executed.
     */
    public synchronized boolean signifyThreadArrived(AppInstanceNode n) {
    	int threadsArrived = threadArrivalMap.get(n);
    	
    	/* 
    	 * All of the threads besides this one that precede n have finished
    	 * This thread completes the set.
    	 * 
    	 * Note that this method is synchronized to avoid the race condition
    	 * where each of the final two threads checking threadsArrived
    	 * simultaneously, before the other has chance to update. Neither
    	 * thread would run the next node, even though all threads would've arrived.
    	 */
    	if (threadsArrived == (n.inDegree-1)) return true;
    	
    	/*
    	 * Not all threads have arrived, and n is not ready to be executed.
    	 */
    	else threadArrivalMap.put(n, threadsArrived+1);
    	return false;
    }
}
