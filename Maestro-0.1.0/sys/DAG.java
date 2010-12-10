/*
  DAG.java

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

import apps.App;

/**
 * DAG: the Application Directed Acyclic Graph
 * @author Zheng Cai
 */
public class DAG {
    /** Unique ID of this DAG */
    int id;
	
    /** All the nodes in this graph
     * the activation and terminal node are also in this list*/
    HashMap<String, AppInstanceNode> nodes;
	
    /** The activation node */
    ActivationNode activation;
	
    /** The terminal node */
    TerminalNode terminal;
	
    ViewManager vm;
	
    /** All View instances that this DAG is going to read */
    HashSet<String> toRead;
	
    /** Whether multiple DAG instances can wait in the queue and run concurrently */
    boolean concurrent = false;
	
    /** Create a new instance of DAG*/
    public DAG(int i, ViewManager vm) {
	id = i;
	nodes = new HashMap<String, AppInstanceNode>();
	toRead = new HashSet<String>();
	this.vm = vm;
    }

    public DAG cloneWithViewNameReplacing(HashMap<String, String> replace, int newId) {
	DAG ret = new DAG(newId, vm);
	for (String s : nodes.keySet()) {
	    AppInstanceNode n = nodes.get(s);
	    AppInstanceNode newOne;

	    if (n == activation) {
		//. Update the activation node
		ret.activation = new ActivationNode(new String(s), ret);
		for (String ss : activation.viewNames) {
		    String to = replace.get(ss);
		    ret.activation.viewNames.add(null != to ? to : ss);
		}

		newOne = ret.activation;
	    } else if (n == terminal) {
		ret.terminal = new TerminalNode(new String(s), ret);
		newOne = ret.terminal;
	    } else {
		Class appClass = n.app.getClass();
		try {
		    newOne = new AppInstanceNode(new String(s), (App) appClass.newInstance(), ret);
		} catch (InstantiationException e) {
		    Utilities.printlnDebug("Cannot creating new instance for class "+appClass);
		    return null;
		} catch (IllegalAccessException e) {
		    Utilities.printlnDebug("Error accessing class "+appClass);
		    return null;
		}
	    }

	    for (String ss : n.input.keySet()) {
		AppInstanceNode.IOSpecification ios = n.input.get(ss);
		AppInstanceNode.IOSpecification newIos = newOne.newIOSpecification(ios.pos, new String(ios.viewClassName));
		String to = replace.get(ss);
		newOne.input.put(null != to ? to : ss, newIos);
	    }

	    for (String ss : n.output.keySet()) {
		AppInstanceNode.IOSpecification ios = n.output.get(ss);
		AppInstanceNode.IOSpecification newIos = newOne.newIOSpecification(ios.pos, new String(ios.viewClassName));
		String to = replace.get(ss);
		newOne.output.put(null != to ? to : ss, newIos);
	    }

	    newOne.inDegree = n.inDegree;
	    ret.nodes.put(s, newOne);
	}

	//. Update the edges information in the new DAG
	for (String s : nodes.keySet()) {
	    AppInstanceNode n = nodes.get(s);
	    AppInstanceNode newOne = ret.nodes.get(s);
	    for (AppInstanceEdge e : n.edges) {
		AppInstanceNode next = ret.nodes.get(e.next.name);
		Utilities.Assert(null != next, "Found a null to node in cloning DAG");
		newOne.edges.addLast(new AppInstanceEdge(next));
	    }
	}

	//. Update the toRead set
	ret.toRead = new HashSet<String>();
	for (String s : toRead) {
	    String to = replace.get(s);
	    ret.toRead.add(null != to ? to : s);
	}
	
	ret.concurrent = concurrent;
	
	return ret;
    }

    public void print() {
	System.out.println("DAG ID is "+id);
	
	for (String s : nodes.keySet()) {
	    AppInstanceNode n = nodes.get(s);
	    System.out.println("Node type: "
			       + n.getClass().getSimpleName()
			       + ", Node name: "
			       + s
			       + " "
			       + n);
	    if (null != n.app) {
		System.out.println("App type: "
				   + n.app.getClass().getSimpleName());
	    }

	    if (n == activation) {
		for (String ss : activation.viewNames) {
		    System.out.print(ss+" ");
		}
		System.out.println();
	    }

	    System.out.println("Input Specification:");
	    for (String ss : n.input.keySet()) {
		AppInstanceNode.IOSpecification ios = n.input.get(ss);
		System.out.println(ios.viewClassName+" "+ios.pos+" "+ss);
	    }

	    System.out.println("Output Specification:");
	    for (String ss : n.output.keySet()) {
		AppInstanceNode.IOSpecification ios = n.output.get(ss);
		System.out.println(ios.viewClassName+" "+ios.pos+" "+ss);

	    }
	    
	    System.out.println("inDegree "+n.inDegree);

	    System.out.println("Edges:");
	    for (AppInstanceEdge e : n.edges) {
		System.out.println("Next "+e.next.name+" "+e.next);
	    }
	}

	System.out.println("toRead:");
	for (String s : toRead) {
	    System.out.print(s+" ");
	}
	System.out.println();
	
	System.out.println("Concurrent: "+concurrent);
    }
}
