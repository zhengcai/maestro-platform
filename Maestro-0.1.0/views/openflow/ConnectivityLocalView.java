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

package views.openflow;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import drivers.Driver;
import sys.Utilities;
import views.View;
import events.Event;

public class ConnectivityLocalView extends View {
    private static final long serialVersionUID = 4124422538006966377L;
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
			
	    // Default cost 1
	    cost = 1;
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

}
