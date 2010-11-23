package sys;

import java.util.*;

/** Application Directed Acyclic Graph
 * @author Zheng
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
	
	/** Whether multiple DAG instances can wait in the queue */
	boolean redundantWaiting = false;
	
	//Set<Integer> localEnvironmentViews = Utilities.newViewNameSet();
	
	/** Create a new instance of DAG*/
	public DAG(int i, ViewManager vm) {
		id = i;
		nodes = new HashMap<String, AppInstanceNode>();
		toRead = new HashSet<String>();
		this.vm = vm;
	}
}
