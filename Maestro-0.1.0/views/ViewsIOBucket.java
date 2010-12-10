/*
  ViewsIOBucket.java

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

package views;

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

/**
 * The data structure that holds arbitrary number of views for I/O
 * All positions start from 0
 * 
 * @author Zheng Cai
 */
public class ViewsIOBucket {
    private Semaphore sem;
    private HashMap<Integer, View> views;
	
    public ViewsIOBucket() {
	views = new HashMap<Integer, View>();
	sem = new Semaphore(1);
    }
	
    /**
     * Add a new view instance to the specified position in this bucket
     * This can also be used as update of one view instance
     * @param pos the position of the view instance
     * @param view the view instance
     * @return the previous view instance in this, null if not existed
     */
    public View addView(int pos, View view) {
	try {
	    sem.acquire();
	} catch (InterruptedException e) {
	    System.err.println("ViewsIOBucket_addViews: Inturrepted sem.acquire()");
	    e.printStackTrace();
	}
	View ret = views.put(pos, view);
	sem.release();
	return ret;
    }
	
    /**
     * Remove the view instance to the specified position in this bucket
     * @param pos the position of the view instance to remove
     * @return the removed view instance, null if not existed
     */
    public View removeView(int pos) {
	try {
	    sem.acquire();
	} catch (InterruptedException e) {
	    System.err.println("ViewsIOBucket_removeViews: Inturrepted sem.acquire()");
	    e.printStackTrace();
	}
	View ret = views.remove(pos);
	sem.release();
	return ret;
    }
	
    /** 
     * Get the specified view instance to the specified position in this bucket
     * @param pos the position of the view instance
     * @return the view instance, null if not existed
     */
    public View getView(int pos) {
	return views.get(pos);
    }
	
    /**
     * Get all the view instances in this view bucket
     * @return the collection of view instances
     */
    public Collection<View> getAllViews() {
    	return views.values();
    }
    
    /**
     * Get how many view instances are there in this bucket
     * @return the size of the bucket
     */
    public int getSize() {
    	return views.size();
    }
}
