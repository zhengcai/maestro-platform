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

package drivers;

import java.util.LinkedList;

import events.Event;
import sys.ViewManager;

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
			//System.err.println("Wait");
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
		//System.err.println("Suspend");
	}
	
	/**
	 * Resume the previously suspended loop in the driver that calls whetherContinue()
	 */
	public boolean resume() {
		synchronized(lock) {
		    if (lock.locked) {
			lock.locked = false;
			lock.notify();
			//System.err.println("Resume");
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
	abstract public boolean commitEvent(LinkedList<Event> events);
	
	/**
	 * Print some infomation
	 */
	abstract public void print();

        /**
	 * Notify that one of the worker threads is idling
	 */
        //abstract public void workerIdling();
}
