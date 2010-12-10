/*
  AppInstanceNode.java

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

import java.util.*;

import views.ViewsIOBucket;
import apps.*;

/** The node in the DAG, which holds one application instance
 * @author Zheng Cai
 * 
 */
public class AppInstanceNode {
    /** Name of this node */
    String name;
    
    /** Which DAG does this node belong to*/
    DAG dag;
	
    /** Edges from this node*/
    LinkedList<AppInstanceEdge> edges;
	
    /** The application instance*/
    App app;
	
    /** 
     * A holder for represent the IO specification for an application instance
     * pos is the position of this view instance in the application's input/output list
     * viewClassName is the name of the view instance's class
     */
    public static class IOSpecification {
	int pos;
	String viewClassName;
	public IOSpecification(int p, String name) {
	    pos = p;
	    viewClassName = name;
	}
    }
	
    public IOSpecification newIOSpecification(int p, String name) {
	return new IOSpecification(p, name);
    }
	
    /** Names of input view instances */
    HashMap<String, IOSpecification> input;
	
    /** Names of output view instances */
    HashMap<String, IOSpecification> output;
	
    /** The in-degree of this vertex in the DAG.  Alternatively, this is the the number of applications for which this node waits before running */
    int inDegree;
	
    /** Create a new instance of AppInstanceNode */
    public AppInstanceNode(String n, App a, DAG d) {
	name = n;
	app = a;
	dag = d;
	edges = new LinkedList<AppInstanceEdge>();
		
	inDegree = 0;
		
	input = new HashMap<String, IOSpecification>();
	output = new HashMap<String, IOSpecification>();
    }

    /**
     * Check whether the output views in the bucket conform with the specification
     * when creating this application instance
     * @return whether true if they conform
     */
    public boolean checkOutput(ViewsIOBucket vb) {
	if (output.size() != vb.getSize()) {
	    return false;
	}
		
	return true;
    }
}
