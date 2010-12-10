/*
  ConnectivityLocalView.java

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

package views.openflow;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import drivers.Driver;
import sys.Utilities;
import views.View;
import events.Event;

/**
 * Topology data structure for the entire network, contains all the links
 * that have been discovered.
 * @author Zheng Cai
 */
public class ConnectivityLocalView extends View {
    public static final int linkCostMax = Integer.MAX_VALUE / 3;
	
    /** 
     * Directional link representation 
     * For a link A-B, where A and B are switches
     */
    public static class Link { // For a link A-B, where A and B are switches
	/** datapath id of A */
	public long A;
		
	/** datapath id of B */
	public long B;
		
	/** Port number of A for this link */
	public int portA;
		
	/** Port number of B for this link */
	public int portB;
		
	/** Cost of this link */
	public int cost;
		
	public Link(long a, int pA, long b, int pB) {
	    A = a;
	    B = b;
	    portA = pA;
	    portB = pB;
			
	    //. Default cost is 1
	    cost = 1;
	}

	@Override
	    public String toString() {
	    return String.format("Link dpid(port)--dpid(port): %d(%d)--%d(%d) cost %d", A, portA, B, portB, cost);
	}
    }

    /** Two dimensional hash map for links */
    private HashMap<Long, HashMap<Long, Link>> links;
	
    /** 
     * Temporary log for updated links
     * TODO Consistency has not been thought through 
     */
    public LinkedList<Link> updated;
	
    /** Temporary log for removed links
     * TODO Consistency has not been thought through 
     */
    public LinkedList<Link> removed;
	
    public ConnectivityLocalView() {
	links = new HashMap<Long, HashMap<Long, Link>>();
	updated = new LinkedList<Link>();
	removed = new LinkedList<Link>();
    }
	
    /**
     * Get the link A-B
     * @param A datapath id of A
     * @param B datapath id of B
     * @return null if link A-B does not exist
     */
    public Link getLink(long A, long B) {
	HashMap<Long, Link> cA = links.get(A);
	if (cA == null) {
	    return null;
	}
	return cA.get(B);
    }
	
    public Collection<Link> getAllLinks() {
	LinkedList<Link> result = new LinkedList<Link>();
	for (long A : links.keySet()) {
	    HashMap<Long, Link> col = links.get(A);
	    if (col != null) {
		for (long B : col.keySet()) {
		    result.add(col.get(B));
		}
	    }
	}
	return result;
    }
	
    public int getNodesNum() {
	int result = 0;
	HashSet<Long> seen = new HashSet<Long>();
	for (long A : links.keySet()) {
	    HashMap<Long, Link> col = links.get(A);
	    if (col != null) {
		for (long B : col.keySet()) {
		    Link l = col.get(B);
		    if (!seen.contains(l.A)) {
			seen.add(l.A);
			result ++;
		    }
		    if (!seen.contains(l.B)) {
			seen.add(l.B);
			result ++;
		    }
		}
	    }
	}
	return result;
    }
	
    /** 
     * Add(update) a link in the view
     * @param l the link to add
     * @return the old link value of link A-B
     */
    public Link addLink(Link l) {
	//acquireWrite();
	HashMap<Long, Link> cA = links.get(l.A);
	if (cA == null) {
	    cA = new HashMap<Long, Link>();
	    links.put(l.A, cA);
	}
	updated.add(l);
	Link ret =  cA.put(l.B, l);
	//releaseWrite();
	return ret;
    }
	
    public void removeLink(Link l) {
	//acquireWrite();
	HashMap<Long, Link> cA = links.get(l.A);
	if (cA == null) {
	    Utilities.printlnDebug(l+" does not exist in the ConnectivityLocalView");
	} else {
	    if (cA.remove(l.B) == null) {
		Utilities.printlnDebug(l+" does not exist in the ConnectivityLocalView");
	    } else {
		removed.add(l);
	    }
	}
	//releaseWrite();
    }
	
    @Override
	public void commit(Driver driver) {

    }

    @Override
	public boolean processEvent(Event e) {
	return false;
    }

    @Override
	public boolean whetherInterested(Event e) {
	return false;
    }

    @Override
	public void print() {
	for (Link l : getAllLinks()) {
	    System.out.println(l);
	}
    }
}
