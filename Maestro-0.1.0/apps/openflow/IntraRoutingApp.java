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

package apps.openflow;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import views.ViewsIOBucket;
import views.openflow.ConnectivityLocalView;
import views.openflow.RoutingIntraView;
import apps.App;
import sys.Utilities;

/**
 * The IntraRoutingApp computes all-pair shortest-path routing tables
 * for all destinations in the network, based on the connectivity map.
 * It uses the Floyd-Warshall algorithm. In the future, we plan to
 * introduce the incremental shortest-path routing algorithm
 */
public class IntraRoutingApp extends App {
    @Override
	public ViewsIOBucket process(ViewsIOBucket input) {
	ConnectivityLocalView conn = (ConnectivityLocalView)input.getView(0);
	RoutingIntraView rt = (RoutingIntraView)input.getView(1);
	
	ViewsIOBucket result = new ViewsIOBucket();
		
    	Collection<ConnectivityLocalView.Link> links = conn.getAllLinks();
        int n = conn.getNodesNum();

        //. Nothing in the connectivity yet
        if (n == 0) {
	    result.addView(0, rt);
            return result;
        }
	
        int [][]adjacency = new int[n][n];
        long [][]nextHop = new long[n][n];
        
        //. Initialize adjacency matrix
        for (int i=0;i<n;i++) {
            for (int j=0;j<n;j++) {
                if (i==j) {
                    adjacency[i][j] = 0;
                }
                else {
                    adjacency[i][j] = ConnectivityLocalView.linkCostMax;
                }
                //. Warning: if the dpid of the switch is unluckily -1, then we are screwed
                nextHop[i][j] = -1;
            }
        }
        
        HashMap<Long, Integer> indexMap = new HashMap<Long, Integer>();
        int index = 0;
        long []idMap = new long[n];
        
        //. Construct adjacency matrix from Connectivity
        conn.acquireRead();
        HashSet<Long> seenIDs = new HashSet<Long>();
        for (ConnectivityLocalView.Link l : links) {
            if (!seenIDs.contains(l.A)) {
            	seenIDs.add(l.A);
                if (index >= n) {
		    conn.releaseRead();
		    result.addView(0, rt);
		    //Utilities.printlnDebug("Quit the intro-domain routing computation because index is out of bound");
                    return result;
                }
                idMap[index] = l.A;
                index ++;
            }
            if (!seenIDs.contains(l.B)) {
            	seenIDs.add(l.B);
                if (index >= n) {
		    conn.releaseRead();
		    result.addView(0, rt);
		    //Utilities.printlnDebug("Quit the intro-domain routing computation because index is out of bound");
                    return result;
                }
                idMap[index] = l.B;
                index ++;
            }
        }
        Arrays.sort(idMap);
        for(int i=0;i<idMap.length;i++) {
	    indexMap.put(idMap[i], i);
        }
        for (ConnectivityLocalView.Link l : links) {
            int nodeIndex = indexMap.get(l.A);
            int neighborIndex = indexMap.get(l.B);
            adjacency[nodeIndex][neighborIndex] = l.cost;
            nextHop[nodeIndex][neighborIndex] = l.B;
        }
        conn.releaseRead();
        
        //. Compute the routing table: using the Floyd-Warshall algorithm
        //. If two routes have the same cost, use the next hop with a smaller dpid
        for (int k=0;k<n;k++) {
            for (int i=0;i<n;i++) {
                for (int j=0;j<n;j++) {
                    if (adjacency[i][k] + adjacency[k][j] < adjacency[i][j]) {
                        adjacency[i][j] = adjacency[i][k] + adjacency[k][j];
                        nextHop[i][j] = nextHop[i][k];
                    }
                    else if (adjacency[i][k] + adjacency[k][j] == adjacency[i][j] 
			     && adjacency[i][j] < ConnectivityLocalView.linkCostMax) {
                    	if (nextHop[i][j] > nextHop[i][k]
			    && nextHop[i][k] != -1) {
			    nextHop[i][j] = nextHop[i][k];
                    	}
                    }
                }
            }
        }
        
        rt.acquireWrite();
        for (int i=0;i<n;i++) {
            for (int j=0;j<n;j++) {
                if (i==j) {
		    
                }
                else {
                    long from = idMap[i];
                    long to = idMap[j];
                    ConnectivityLocalView.Link l = conn.getLink(from, nextHop[i][j]);
                    if (l == null ) {
                    	rt.releaseWrite();
                    	result.addView(0, rt);
			//Utilities.printlnDebug("Quit the intro-domain routing computation because we find a null link");
                    	return result;
                    }
                    RoutingIntraView.Route route = new RoutingIntraView.Route(
									      l.portA, nextHop[i][j], l.portB, adjacency[i][j]);
                    rt.addNextHop(from, to, route);
                }
            }
        }
        rt.releaseWrite();
        
        result.addView(0, rt);
        return result;
    }
}
