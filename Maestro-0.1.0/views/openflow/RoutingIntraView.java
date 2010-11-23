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

import java.util.HashMap;

import drivers.Driver;
import views.View;
import events.Event;

public class RoutingIntraView extends View {
    public static class Route {
	/** Port of this hop connected to the next hop */
	public int port;
		
	/** The next hop's datapath id */
	public long next;
		
	/** The next hop's port connected to this hop */
	public int nextPort;
		
	/** Total cost of this path */
	public int cost;
		
	public Route() {
	    port = -1;
	    next = -1;
	    nextPort = -1;
	    cost = -1;
	}
		
	public Route(int p, long n, int nP, int c) {
	    port = p;
	    next = n;
	    nextPort = nP;
	    cost = c;
	}
    }
	
    /** Two dimensional hash map for next hops */
    private HashMap<Long, HashMap<Long, Route>> routes;
	
    public RoutingIntraView() {
	routes = new HashMap<Long, HashMap<Long, Route>>();
    }
	
    /**
     * Get the next hop for route from A to B
     * @param A datapath id of A
     * @param B datapath id of B
     * @return null if route A-B does not exist
     */
    public Route getNextHop(long A, long B) {
	HashMap<Long, Route> cA = routes.get(A);
	if (cA == null) {
	    return null;
	}
	return cA.get(B);
    }
	
    /** 
     * Add(update) a next hop in the routing table
     * @param A datapath id of A
     * @param B datapath id of B
     * @param route the route info
     * @return the old route value of route A to B
     */
    public Route addNextHop(long A, long B, Route route) {
	HashMap<Long, Route> cA = routes.get(A);
	if (cA == null) {
	    cA = new HashMap<Long, Route>();
	    routes.put(A, cA);
	}
	Route ret = cA.put(B, route);
	return ret;
    }
	
    public void printAll() {
	for (long A : routes.keySet()) {
	    for (long B : routes.get(A).keySet()) {
		Route r = routes.get(A).get(B);
		System.out.println(String.format("Route %d->%d: next=%d, port=%d, nextPort=%d, cost=%d",
						 A, B, r.next, r.port, r.nextPort, r.cost));
	    }
	}
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
	// Not interested in any event
	return false;
    }

    @Override
	public void print() {

    }
}
