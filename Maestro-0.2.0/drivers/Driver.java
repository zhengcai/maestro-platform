/*
  Driver.java

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

package drivers;

import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.nio.ByteBuffer;

import events.Event;
import sys.ViewManager;

/**
 * @author Zheng Cai
 */
public abstract class Driver {
    public ViewManager vm;
	
    public class Lock {
	public boolean locked = false;
    }
	
    /**
     * The Boolean object used to control the execution of the driver, usually for resource
     * consumption reason some part of the execution should be suspended
     */
    public Lock lock = new Lock();
	
    /**
     * This function should be called within the start() function of the driver,
     * in the loop of handling high-resource(memory)-consumption incoming messages.
     * Usually the ApplicationManager will decide whether the specific loop should be
     * suspended based on the number of current waiting DAG instances enqueued
     */
    public void whetherContinue() {
	synchronized (lock) {
	    if (lock.locked) {
		try {
		    lock.wait();
		} catch (InterruptedException e) {
		    System.err.println("Driver:whetherContinue: InterruptedException");
		    e.printStackTrace();
		}
	    }
	}
    }
	
    /**
     * Set the shouldContinue to be false, such that the loop which calls whetherContinue()
     * will be temporarily suspended, until resume() is called
     */
    public void suspend() {
	synchronized(lock) {
	    lock.locked = true;
	}
    }
	
    /**
     * Resume the previously suspended loop in the driver that calls whetherContinue()
     */
    public boolean resume() {
	synchronized(lock) {
	    if (lock.locked) {
		lock.locked = false;
		lock.notify();
		return true;
	    }
	}
	return false;
    }

    /**
     * Start the driver execution
     */
    abstract public void start();
	
    /**
     * Commit events to the driver, let it generate related configuration messages and 
     * send to the underlying network
     * @param events the events to commit
     * @return true if successful, false otherwise
     */
    abstract public boolean commitEvent(ArrayList<Event> events);
	
    /**
     * Print some infomation
     */
    abstract public void print();

    /**
     * Let the driver prepare a page for the TUI monitor
     */
    abstract public void prepareDriverPage(ByteBuffer buffer);

    /**
     * Let the driver return a new Runnable task,
     * for each worker thread to work on
     */
    abstract public Runnable newTask();
}
