package sys;

import java.util.HashMap;
import java.util.LinkedList;

public class TaskManager {
    private final PoolWorker[] threads;
    private final Queue queue;
    private final HashMap<Long, Integer> lid2id;

    public TaskManager(int nThreads) {
	queue = new Queue();
	threads = new PoolWorker[nThreads];
	lid2id = new HashMap<Long, Integer>();

	for (int i = 0; i < nThreads; i++) {
	    threads[i] = new PoolWorker(i);
	    threads[i].setName("PoolWorker #" + i);
	    threads[i].start();
	    lid2id.put(threads[i].getId(), i);
	}
    }

    public int getQueueSize() {
	return queue.getSize();
    }
	
    public int getCurrentWorkerID() {
	Integer ret = lid2id.get(Thread.currentThread().getId());
	if (null == ret)
	    return 0;
	else
	    return ret.intValue();
    }

    public void printCurrentQueue() {
	int id = -1;
	Integer workerID = lid2id.get(Thread.currentThread().getId());
	if (null != workerID)
	    id = lid2id.get(Thread.currentThread().getId()).intValue();

	if (id < 0 || id >= threads.length) {
	    System.err.println("Trying to bind to a wrong thread with id "+id);
	    return;
	}
	PoolWorker worker = threads[id];
	System.err.println("High="+worker.ownQ.high.size()+", medium="+worker.ownQ.medium.size()+", low="+worker.ownQ.low.size());
    }
	
    public void execute(Runnable r, int priority) {
	synchronized (queue) {
	    queue.putTask(r, priority);
	    queue.notify();
	}
    }
	
    /**
     * Will run the runnable in the same thread(core) of the current one
     * @param r
     * @param priority
     */
    public void bindingExecute(Runnable r, int priority) {
	int id = -1;
	Integer workerID = lid2id.get(Thread.currentThread().getId());
	if (null != workerID)
	    id = lid2id.get(Thread.currentThread().getId()).intValue();
	    
	if (id < 0 || id >= threads.length) {
	    System.err.println("Trying to bind to a wrong thread with id "+id);
	    return;
	}
	PoolWorker worker = threads[id];
	synchronized(worker.ownQ) {
	    worker.ownQ.putTask(r, priority);
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
	    }
	    if (priority == Constants.PRIORITY_MEDIUM) {
		medium.addLast(r);
	    }
	    if (priority == Constants.PRIORITY_LOW) {
		low.addLast(r);
	    }
	}

	public boolean isEmpty() {
	    return high.isEmpty() && medium.isEmpty() && low.isEmpty();
	}

	public Runnable popTask() {
	    Runnable ret = null;
	    if (!high.isEmpty()) {
		ret = high.removeFirst();
	    } else if (!medium.isEmpty()) {
		ret = medium.removeFirst();
	    } else if (!low.isEmpty()) {
		ret = low.removeFirst();
	    }
	    return ret;
	}

		
	public int getSize() { return high.size()+medium.size()+low.size(); }
    }

    private class PoolWorker extends Thread {
	Queue ownQ;
	int myID;
	public PoolWorker(int id) {
	    myID = id;
	    ownQ = new Queue();
	}
		
	public void run() {
	    Runnable r;
	    long before = 0;

	    while (true) {
		//before = System.nanoTime();
		synchronized (queue) {
		    //Parameters.waiting += System.nanoTime() - before;
		    while (queue.isEmpty()) {
			try {
			    /*
			      try {
			      Parameters.am.vm.driver.workerIdling();
			      //Parameters.am.vm.driver.print();
			      } catch (NullPointerException e) {

			      }
			    */
					
			    //before = System.nanoTime();
			    queue.wait();
			    /*
			      synchronized (Parameters.blocked) {
			      Parameters.blocked.value += System.nanoTime()
			      - before;
			      }
			    */
			} catch (InterruptedException ignored) {
			}
		    }
				
		    r = queue.popTask();
		}
		try {
		    //before = System.nanoTime();
		    r.run();
		    //Parameters.running += System.nanoTime() - before;
				
		    while (!ownQ.isEmpty()) {
			synchronized(ownQ) {
			    r = ownQ.popTask();
			}
				    
			//before = System.nanoTime();
			r.run();
			//Parameters.running += System.nanoTime() - before;
		    }
		} catch (Exception e) {
		    System.err.println("Thread running with an exception "+e);
		    e.printStackTrace();
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