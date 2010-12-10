/*
  WorkQueue.java

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

import java.util.LinkedList;

/**
 * @author Zheng Cai
 */
public class WorkQueue {
    private final int nThreads;
    private final PoolWorker[] threads;
    private final Queue queue;

    public WorkQueue(int nThreads) {
	queue = new Queue();
	this.nThreads = nThreads;
	threads = new PoolWorker[nThreads];

	for (int i = 0; i < nThreads; i++) {
	    threads[i] = new PoolWorker();
	    threads[i].setName("PoolWorker #" + i);
	    threads[i].start();
	}
    }

    public void execute(Runnable r, int priority) {
	synchronized (queue) {
	    queue.putTask(r, priority);
	    queue.notify();
	}
    }

    private class Queue {
	LinkedList<Runnable> high;
	LinkedList<Runnable> medium;
	LinkedList<Runnable> low;

	public Queue() {
	    high = new LinkedList<Runnable>();
	    medium = new LinkedList<Runnable>();
	    low = new LinkedList<Runnable>();
	}

	public void putTask(Runnable r, int priority) {
	    if (priority == Constants.PRIORITY_HIGH) {
		high.addLast(r);
		// System.out.println("put high");
	    }
	    if (priority == Constants.PRIORITY_MEDIUM) {
		medium.addLast(r);
		// System.out.println("put medium");
	    }
	    if (priority == Constants.PRIORITY_LOW) {
		low.addLast(r);
		// System.out.println("put low");
	    }
	}

	public boolean isEmpty() {
	    return high.isEmpty() && medium.isEmpty() && low.isEmpty();
	}

	public Runnable popTask() {
	    Runnable ret = null;
	    if (!high.isEmpty()) {
		ret = high.removeFirst();
		// System.out.println("run high");
	    } else if (!medium.isEmpty()) {
		ret = medium.removeFirst();
		// System.out.println("run medium");
	    } else if (!low.isEmpty()) {
		ret = low.removeFirst();
		// System.out.println("run low");
	    }
	    return ret;
	}

	/*
	 * public int getSize() { return high.size()+medium.size()+low.size(); }
	 */
    }

    private class PoolWorker extends Thread {
	public void run() {
	    Runnable r;
	    long before = 0;

	    while (true) {
		synchronized (queue) {
		    while (queue.isEmpty()) {
			try {
			    before = System.nanoTime();
			    queue.wait();
			    synchronized (Parameters.blocked) {
				Parameters.blocked.value += System.nanoTime()
				    - before;
			    }
			} catch (InterruptedException ignored) {
			}
		    }
		    r = queue.popTask();
		}
		before = System.nanoTime();
		r.run();
		synchronized (Parameters.ran) {
		    Parameters.ran.value += System.nanoTime() - before;
		}
		/*
		 * // If we don't catch RuntimeException, // the pool could leak
		 * threads try { r.run(); } catch (RuntimeException e) { // You
		 * might want to log something here
		 * System.err.println("Thread running with an exception "+e); }
		 */
	    }
	}
    }
}
