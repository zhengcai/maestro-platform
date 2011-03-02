/*
  WorkerManager.java

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
import java.util.ArrayList;

/**
 * WorkerManager: the worker threads manager
 * @author Zheng Cai
 */
public class WorkerManager {
    private final ArrayList<WorkerThread> threads;
    private final HashMap<Long, Integer> lid2id;

    private int nextID = 0;

    public WorkerManager() {
	threads = new ArrayList<WorkerThread>();
	lid2id = new HashMap<Long, Integer>();
    }

    public void addThread(Runnable r) {
	WorkerThread thread = new WorkerThread(nextID, r);
	thread.setName("WorkerThread #" + nextID);
	lid2id.put(thread.getId(), nextID);
	nextID ++;
	threads.add(thread);
	thread.start();	    
    }

    public int getCurrentWorkerID() {
	Integer ret = lid2id.get(Thread.currentThread().getId());
	if (null == ret)
	    return -1;
	else
	    return ret.intValue();
    }

    public long increaseCounter(long inc) {
	int idx = getCurrentWorkerID();
	if (idx == -1)
	    return -1;
	WorkerThread thread = threads.get(idx);
	if (thread != null) {
	    thread.counter += inc;
	    return thread.counter;
	} else {
	    return -1;
	}
    }

    public long getCounter(int which) {
	WorkerThread thread = threads.get(which);
	if (thread != null) {
	    return thread.counter;
	} else {
	    return -1;
	}
    }

    private class WorkerThread extends Thread {
	public int myID;
	public Runnable task;

	/**
	   A counter that can be used for any purpose,
	   currently for counting how many requests have been
	   processed by this worker thread
	*/
	public long counter;
	public WorkerThread(int id, Runnable r) {
	    myID = id;
	    task = r;
	    counter = 0;
	}
		
	public void run() {
	    try {
		task.run();
	    } catch (Exception e) {
		Utilities.printlnDebug("Worker thread #"+myID+" running with an exception "+e);
		e.printStackTrace();
	    }
	}
    }
}
