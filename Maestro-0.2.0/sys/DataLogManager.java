/*
  DataLogManager.java

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

import java.util.ArrayList;
import java.util.PriorityQueue;
import java.io.*;

/**
 * DataLogManager: the data log manager, each worker thread has its own log queue,
 * so the concurrency overhead is minimized
 * @author Zheng Cai
 */
public class DataLogManager extends Thread {
    public static final boolean enabled = false;

    public static final int MILLISEC_SLEEP = 100;
    
    public static abstract class Content {
	public abstract String toString();
    }
    
    private static class Entry {
	/** The time stamp of this entry, the nanoTime() */
	public long time;

	/** Body of this log entry */
	public Content content;

	public Entry(Content c) {
	    time = System.nanoTime();
	    content = c;
	}
    }
    
    private ArrayList<ArrayList<Entry>> logs;

    private WorkerManager mgr;

    private boolean dumpDone = false;

    public DataLogManager(WorkerManager m) {
	if (enabled) {
	    logs = new ArrayList<ArrayList<Entry>>();
	    mgr = m;
	}
	else
	    start();
    }

    public void run() {
	while (true) {
	    try {
		Thread.sleep(MILLISEC_SLEEP);
	    } catch (InterruptedException e) {

	    }
	    if (Parameters.warmuped) {
		long now = System.nanoTime();
		if (Parameters.whenWarmuped == 0) {
		    Parameters.whenWarmuped = now;
		}
		Utilities.log.println(now+Parameters.am.vm.driver.getCounters());
		if (Parameters.dynamicExp) {
		    if ((now-Parameters.whenWarmuped) > 50000000000L) {
			Parameters.am.vm.driver.print();
			Utilities.ForceExit(0);
		    }
		} else {
		    if ((now-Parameters.whenWarmuped) > 50000000000L) {
			Parameters.am.vm.driver.print();
			Utilities.ForceExit(0);
		    }
		}
	    }
	}
    }

    /**
     * Add more log queues
     * @param num the number of more log queues to add
     */
    public void addLogs(int num) {
	if (!enabled)
	    return;
	
	for (int i=0;i<num;i++) {
	    logs.add(new ArrayList<Entry>());
	}
    }

    public void addEntry(Content c) {
	if (!enabled)
	    return;

	if (!Parameters.warmuped)
	    return;
	
	int idx = mgr.getCurrentWorkerID();
	if (idx < 0)
	    return;
	ArrayList<Entry> log = logs.get(idx);
	Utilities.Assert(log != null, "Wrong worker ID in adding a data log entry!");

	log.add(new Entry(c));
    }

    public void dumpLogs() {
	if (!enabled)
	    return;
	
	class TmpNode implements Comparable {
	    public Entry e;
	    public int idx;
	    public int pos;
	    public ArrayList<Entry> log;
	    
	    public TmpNode(Entry entry, int index, int position, ArrayList<Entry> l) {
		e = entry;
		idx = index;
		pos = position;
		log = l;
	    }
	    
	    public int compareTo(Object o) {
		TmpNode other = (TmpNode)o;
		if (e.time > other.e.time)
		    return 1;
		else if (e.time < other.e.time)
		    return -1;
		else
		    return 0;
	    }	    
	}
	
	synchronized(this) {
	    
	    if (dumpDone)
		return;
	    
	    try {
		File file = new File(Parameters.dataLogFile);
		FileWriter writer = new FileWriter(file, false);
		
		PriorityQueue<TmpNode> current = new PriorityQueue<TmpNode>();
		for (int i=0;i<logs.size();i++) {
		    if (logs.get(i).size() > 0)
			current.add(new TmpNode(logs.get(i).get(0), i, 1, logs.get(i)));
		}
	    
		while (current.peek() != null) {
		    TmpNode tmp = current.poll();
		    if (tmp.e.content == null)
			System.err.println("FAINT, get a null in "+tmp.idx);
		    writer.write(tmp.e.time+" "+tmp.e.content.toString());
		
		    if (tmp.pos < tmp.log.size()) {
			tmp.e = tmp.log.get(tmp.pos++);
			if (tmp.e == null)
			    System.err.println("DAMN, get a null in "+tmp.idx);
			current.add(tmp);
		    }
		}
	    
		writer.close();
		dumpDone = true;
	    }
	    catch (FileNotFoundException e) {
		Utilities.Assert(false, "dataLogFile Not Found!");
	    }
	    catch (IOException e) {
		Utilities.Assert(false, "IO ERROR!");
	    }
	}
    }
}