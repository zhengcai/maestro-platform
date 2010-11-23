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

package views;

import java.io.Serializable;
import java.util.concurrent.Semaphore;

import drivers.*;
import events.*;

/**
 * The abstract class for a View in Maestro
 * 
 * @author Zheng Cai
 *
 */
public abstract class View implements Serializable {
	/** Unique ID for Serializable */
	private static final long serialVersionUID = 5529001310172642894L;
	
	/**
	 * Semaphore used to synchronize critical section, such as modification
	 * to data structures
	 * TODO: Very primitive design, requires that apps use such semaphores
	 *       need to be redesigned later
	 */
	public Semaphore sem;

	public void acquireRead() {
		/*try {
			sem.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}*/
	}
	
	public void releaseRead() {
		//sem.release();
	}
	
	public void acquireWrite() {
		try {
			sem.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void releaseWrite() {
		sem.release();
	}
	
	public View() {
		sem = new Semaphore(1);
	}
	
	/**
	 * Check whether this view is interested(capable) for handling this event
	 * @param e the event to check against
	 * @return true if interested, false otherwise
	 */
	public abstract boolean whetherInterested(Event e);
	
	/**
	 * Process one event this view has registered for
	 * @param e The incoming event
	 * @return Whether this view should be considered as changed, to trigger DAGs to run
	 */
	public abstract boolean processEvent(Event e);
	
	/** 
	 * Commit this view, generate necessary configuration messages
	 * and send to the network
	 */
	public abstract void commit(Driver driver);
}