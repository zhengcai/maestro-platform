/*
  ApplicationManager.java

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
import java.io.*;
import java.util.concurrent.Semaphore;
import apps.App;
import views.View;
import views.ViewsIOBucket;
import events.Event;

/**
 * The ApplicationManager manages all applications in the system, all DAGs
 * defined by the administrator through configuration file. The
 * ApplicationManager serves as the central point of this program.
 * 
 * @author Zheng Cai
 */
public class ApplicationManager extends Thread {
    static int nextInstanceID = 0;

    CmdConsole console;
    public ViewManager vm;
    String configFile;

    public Semaphore synchro;
    // public Semaphore timing;

    /** All DAGs in this system */
    HashMap<Integer, DAG> dags;

    /**
     * Hash map for events triggering, in order to make indexing the triggered
     * DAG more quickly
     */
    HashMap<String, LinkedList<DAG>> triggerMap;

    /** Current running runtime DAGs */
    public HashMap<Integer, DAGRuntime> running;

    /** Triggered but wait to run runtime DAGs */
    public LinkedList<DAGRuntime> triggered;

    /** View instance names that are marked as concurrent */
    HashSet<String> concurrentNames;

    /** Worker thread manager */
    public WorkerManager workerMgr;

    /** Memory manager */
    public MemoryManager memMgr;

    /** Data log manager */
    public DataLogManager dataLogMgr;

    /**
     * Creates a new instance of ApplicationManager Initialize the whole system
     * read the configuration file and start applications contained in this file
     * 
     * @param conf
     *            the configuration file name
     */
    public ApplicationManager(String conf) {
	vm = new ViewManager(this);
	console = new CmdConsole(this, vm, Parameters.consoleMode);
	configFile = conf;
	dags = new HashMap<Integer, DAG>();
	running = new HashMap<Integer, DAGRuntime>();	
	triggered = new LinkedList<DAGRuntime>();
	synchro = new Semaphore(1);
	triggerMap = new HashMap<String, LinkedList<DAG>>();
	concurrentNames = new HashSet<String>();
	/*
	if (Parameters.divide > 0) {
	    taskMgr = new TaskManager(Parameters.divide);
	}
	*/
	loadSystem();
	vm.loadDriver(Parameters.bundle);

	console.start();
	
	workerMgr = new WorkerManager();
	dataLogMgr = new DataLogManager(workerMgr);
	dataLogMgr.addLogs(Parameters.divide);


	
	if (Parameters.useMemoryMgnt) {
	    memMgr = new MemoryManager();
	}
    }

    /**
     * Start the ApplicationManager, and it will start the whole system TODO So
     * it seems like you're loading all of your DAGs all at once. I believe the
     * final version was supposed to be able to load DAGs dynamically, so I
     * should probably think about adding this feature later.
     */
    public void run() {
	for (int i = 0; i < Parameters.divide; i++) {
	    workerMgr.addThread(vm.driver.newTask());
	}
	updateTriggerMap();
	vm.startDriver();
    }

    /**
     * Load all bundles, views, events, DAGs, etc, specified in the configFile
     */

    private void loadSystem() {

	try {
	    BufferedReader input = new BufferedReader(new FileReader(configFile));

	    //. 0 for nothing, 1 for bundle, 2 for views, 3 for event-view, 4 for
	    //. DAGs
	    int section = 0;
	    DAG currentDAG = null;

	    //. Start DAG ID from 1
	    int currentDAGid = 1;
	    String line = null;
	    while ((line = input.readLine()) != null) {
		line = Utilities.TrimConfigString(line);
		String[] words = line.split(" ");
		if (words.length == 0)
		    continue;

		switch (section) {
		case 2: // For Views
		    if (line.compareToIgnoreCase("End Views") == 0) {
			section = 0;
			break;
		    }
		    Class viewClass = Class.forName("views."
						    + Parameters.bundle + "." + words[0]);		    
		    if (3 == words.length &&
			words[2].compareToIgnoreCase("Concurrent") == 0 &&
			Parameters.divide > 0) {
			for (int i=0;i<Parameters.divide;i++) {
			    View v = (View) viewClass.newInstance();
			    vm.global.addView(words[1]+"_"+i, v);
			    concurrentNames.add(words[1]);
			}
		    } else {
			vm.global.addView(words[1], (View) viewClass.newInstance());
		    }
		    break;
		case 3: // For Events
		    if (line.compareToIgnoreCase("End Events") == 0) {
			section = 0;
			break;
		    }
		    // Verify that the event class does exist
		    Class eventClass = Class.forName("events."
						     + Parameters.bundle + "." + words[0]);
		    Utilities.Assert(words[1].compareToIgnoreCase("by") == 0,
				     "Second word in Events section needs to be by!");
		    if (concurrentNames.contains(words[2])) {
			vm.registerEventConcurrent((Event)eventClass.newInstance(), words[2]);
		    } else {
			vm.registerEvent((Event)eventClass.newInstance(), words[2]);
		    }
		    break;
		case 4: // For DAGs
		    if (words[0].compareToIgnoreCase("Begin") == 0) {
			Utilities.Assert((words.length == 2),
					 "Parsing error: Expected-- Begin DAG");
			//currentDAGid = Integer.parseInt(words[2]);
			currentDAG = new DAG(currentDAGid, vm);
			while ((line = input.readLine()) != null) {
			    line = Utilities.TrimConfigString(line);
			    words = line.split(" ");
			    if (words.length == 0)
				continue;

			    if (words[0].compareToIgnoreCase("Node") == 0) {
				Utilities
				    .Assert((words.length == 3),
					    "Parsing error: Expected -- Node name app");
				if (words[2].compareToIgnoreCase("Activation") == 0) {
				    currentDAG.activation = new ActivationNode(words[1], currentDAG);
				    currentDAG.nodes.put(words[1],
							 currentDAG.activation);
				    while ((line = input.readLine()) != null) {
					line = Utilities.TrimConfigString(line);
					words = line.split(" ");
					if (words.length == 0)
					    continue;
					if (words[0].compareToIgnoreCase("End") == 0)
					    break;
					if (words[0].compareToIgnoreCase("Input") == 0) {
					    currentDAG.activation.viewNames
						.add(words[2]);
					} else if (words[0].compareToIgnoreCase("Timer") == 0) {
					    currentDAG.activation.timer = new Timer();
					    long period = Long.parseLong(words[1]);
					    final DAG toRun = currentDAG;
					    /*
					    class DAGTimerEvent implements Runnable {
						DAG toRun;

						public DAGTimerEvent(DAG d) {
						    toRun = d;
						}

						public void run() {
						    vm.timerStartDag(toRun);
						}
					    }
					    */
					    currentDAG.activation.timer.scheduleAtFixedRate(new TimerTask() {
						    public void run() {
							/*
							DAGTimerEvent r = new DAGTimerEvent(toRun);
							enqueueTask(r, Constants.PRIORITY_HIGH);
							*/
							vm.timerStartDag(toRun);
						    }
						}, period, period);
					}
				    }
				} else if (words[2].compareToIgnoreCase("Terminal") == 0) {
				    currentDAG.terminal = new TerminalNode(words[1], currentDAG);
				    currentDAG.nodes.put(words[1],
							 currentDAG.terminal);
				    int inputPos = 0, outputPos = 0;
				    while ((line = input.readLine()) != null) {
					line = Utilities.TrimConfigString(line);
					words = line.split(" ");
					if (words.length == 0)
					    continue;
					if (words[0].compareToIgnoreCase("End") == 0)
					    break;
					// Verify that the view class does exist
					Class vClass = Class.forName("views."
								     + Parameters.bundle + "."
								     + words[1]);
					if (words[0]
					    .compareToIgnoreCase("Input") == 0) {
					    currentDAG.toRead.add(words[2]);
					    currentDAG.terminal.input
						.put(
						     words[2],
						     currentDAG.terminal
						     .newIOSpecification(
									 inputPos++,
									 words[1]));
					}
					if (words[0]
					    .compareToIgnoreCase("Output") == 0) {
					    currentDAG.terminal.output
						.put(
						     words[2],
						     currentDAG.terminal
						     .newIOSpecification(
									 outputPos++,
									 words[1]));
					}
				    }
				} else {
				    Class appClass = Class.forName("apps."
								   + Parameters.bundle + "."
								   + words[2]);
				    AppInstanceNode node = new AppInstanceNode(words[1],
									       (App) appClass.newInstance(),
									       currentDAG);
				    node.app.initiate(vm, this);
				    currentDAG.nodes.put(words[1], node);
				    int inputPos = 0, outputPos = 0;
				    while ((line = input.readLine()) != null) {
					line = Utilities.TrimConfigString(line);
					words = line.split(" ");
					if (words.length == 0)
					    continue;
					if (words[0].compareToIgnoreCase("End") == 0)
					    break;
					//. Verify that the view class does exist
					Class vClass = Class.forName("views."
								     + Parameters.bundle + "."
								     + words[1]);
					if (words[0]
					    .compareToIgnoreCase("Input") == 0) {
					    currentDAG.toRead.add(words[2]);
					    node.input.put(words[2], node
							   .newIOSpecification(
									       inputPos++,
									       words[1]));
					}
					if (words[0]
					    .compareToIgnoreCase("Output") == 0) {
					    node.output.put(words[2], node
							    .newIOSpecification(
										outputPos++,
										words[1]));
					}
				    }
				}
				continue;
			    }
			    if (words[0].compareToIgnoreCase("Edge") == 0) {
				Utilities.Assert((words.length == 2),
						 "Parsing error: Expected-- Edge link ");
				String[] links = words[1].split(",");
				for (int i = 0; i < links.length; i++) {
				    String[] ns = links[i].split("->");
				    Utilities
					.Assert((ns.length == 2),
						"Parsing error: Expected-- node->node");
				    AppInstanceNode from, to;
				    from = currentDAG.nodes.get(ns[0]);
				    to = currentDAG.nodes.get(ns[1]);
				    Utilities.Assert(
						     (from != null && to != null),
						     "Parsing error: Undefined node in line: "
						     + line);

				    AppInstanceEdge e = new AppInstanceEdge(to);
				    to.inDegree++;
				    from.edges.addLast(e);

				    /*
				     * if (to instanceof TerminalNode) {
				     * ((TerminalNode)to).setViewIDs(viewIDs); }
				     */
				}
				continue;
			    }
			    if (words[0].compareToIgnoreCase("Concurrent") == 0) {
				currentDAG.concurrent = true;
				continue;
			    }
			    if (words[0].compareToIgnoreCase("End") == 0) {
				if (currentDAG.concurrent && Parameters.divide > 0) {
				    for (int i=0;i<Parameters.divide;i++) {
					HashMap<String, String> replace = new HashMap<String, String>();
					for (String s : concurrentNames) {
					    replace.put(s, s+"_"+i);
					}
					DAG newDag = currentDAG.cloneWithViewNameReplacing(replace, currentDAGid);
					dags.put(new Integer(newDag.id), newDag);
					currentDAGid ++;
				    }
				} else {
				    dags.put(new Integer(currentDAG.id), currentDAG);
				    currentDAGid ++;
				}
				currentDAG = null;
				break;
			    }
			} //. End of <while more lines in input>
		    } //. End of <If first word of line is "DAG">
		    break;
		default:
		    break;
		} //. End of <switch (section)>

		if (0 == section) {
		    //. Enter the bundles section
		    if (words[0].compareToIgnoreCase("Package") == 0) {
			Utilities.Assert(words.length >= 2,
					 "Have to specify the package name!");
			Parameters.bundle = words[1];
			continue;
		    }

		    //. Enter the Views section
		    if (words[0].compareToIgnoreCase("Views") == 0) {
			section = 2;
			continue;
		    }

		    //. Enter the Events section
		    if (words[0].compareToIgnoreCase("Events") == 0) {
			section = 3;
			continue;
		    }

		    //. Enter the DAGS section
		    if (words[0].compareToIgnoreCase("DAGS") == 0) {
			section = 4;
			continue;
		    }
		}
	    } //. End of outer <while more lines in input>
	    input.close();
	} //. end of <Try to read some input>
	catch (FileNotFoundException e) {
	    Utilities.Assert(false, "Configuration File Not Found!");
	} catch (EOFException e) {
	    Utilities.Assert(false, "End of stream!");
	} catch (IOException e) {
	    Utilities.Assert(false, "IO ERROR!");
	} catch (ClassNotFoundException e) {
	    Utilities.Assert(false, "Class file(s) not found! "
			     + e.getMessage());
	} catch (InstantiationException e) {
	    Utilities.Assert(false, "Class instantiation error!");
	} catch (IllegalAccessException e) {
	    Utilities.Assert(false, "Class instantiation error!");
	}
    }

    /**
     * Go through all DAGs to update the triggerMap Assume that one DAG can only
     * be triggered by only one kind of event TODO right now it is a list, so
     * the DAG in the head will always be preferred have to come up with either
     * priority scheme, or round-robin like scheme
     */
    public void updateTriggerMap() {
	for (DAG d : dags.values()) {
	    for (String i : d.activation.viewNames) {
		LinkedList<DAG> list = triggerMap.get(i);
		if (list == null) {
		    list = new LinkedList<DAG>();
		    triggerMap.put(i, list);
		}
		list.add(d);
	    }
	}
    }

    /**
     *  Start a particular DAG. Usually this is a DAG triggered by a timer event
     */
    public void startDag(Environment env, DAG dag) {
	if (dag.concurrent) {
	    DAGRuntime drun = new DAGRuntime(dag, env, vm,
					     getNextInstanceID());
	    
	    synchronized (running) {
		running.put(drun.instanceID ,drun);
	    }
	    drun.start(this);
	} else {
	    if (checkConflict(dag)) {
		synchronized (triggered) {
		    if (whetherAlreadyWaiting(dag)) {
			return;
		    } else {
			DAGRuntime drun = new DAGRuntime(dag, env, vm,
							 getNextInstanceID());
			drun.state = Constants.DAGStates.WAITING;
			triggered.addLast(drun);
		    }
		}
	    } else {
		DAGRuntime drun = new DAGRuntime(dag, env, vm,
						 getNextInstanceID());
		
		synchronized (running) {
		    running.put(drun.instanceID ,drun);
		}

		drun.start(this);
	    }
	}
    }

    /**
     * Trigger the DAG which is responsible for the activation event Linear
     * search, not optimized Only trigger normal DAGs, not approval evaluation
     * DAGs
     * */
    public void triggerDag(Environment env, HashSet<String> trigger) {
	long before = 0;
	if (Parameters.measurePerf) {
	    before = System.nanoTime();
	}
	//. Generate the list of all DAGS triggered by any of the views in the VB
	LinkedList<DAG> dags = new LinkedList<DAG>(), toAdd;
	for (String s : trigger) {
	    if ((toAdd = triggerMap.get(s)) != null) {
		for (DAG j : toAdd) {
		    if (!dags.contains(j))
			dags.add(j);
		}
	    }
	}

	//. If no DAGs are triggered, then go home
	if (dags == null || dags.size() == 0) {
	    return;
	}

	//. For each DAG queued to be run, create the local environment and
	//. DAGRuntime, and put it in the queue.
	for (DAG dag : dags) {
	    //. TODO: Only work for non-cloning version
	    if (dag.concurrent) {
		DAGRuntime drun = new DAGRuntime(dag, env, vm,
						 getNextInstanceID());

		synchronized (running) {
		    running.put(drun.instanceID ,drun);
		}
		drun.start(this);
	    } else {
		if (checkConflict(dag)) {
		    synchronized (triggered) {
			if (whetherAlreadyWaiting(dag)) {
			    continue;
			} else {
			    DAGRuntime drun = new DAGRuntime(dag, env, vm,
							     getNextInstanceID());
			    drun.state = Constants.DAGStates.WAITING;
			    //Utilities.printlnDebug("("+workerMgr.getCurrentWorkerID()+") Add triggered "+drun.dag.id+" instance "+drun.instanceID);
			    triggered.addLast(drun);
			}
			/*
			  if (triggered.size() >= Parameters.maxWaitingDAGIns) {
			  vm.driver.suspend();
			  }
			*/
		    }
		} else {
		    DAGRuntime drun = new DAGRuntime(dag, env, vm,
						     getNextInstanceID());
		    synchronized (running) {
			running.put(drun.instanceID ,drun);
		    }
		    drun.start(this);
		}
	    }
	}
	if (Parameters.measurePerf) {
	    Parameters.t4 += System.nanoTime() - before;
	}
    }

    public boolean whetherAlreadyWaiting(DAG dag) {
	for (DAGRuntime dr : triggered) {
	    if (dr.dag.id == dag.id) {
		return true;
	    }
	}
	return false;
    }

    public void produce(DAGRuntimeThread thread, ViewsIOBucket output) {
	AppInstanceNode node = thread.current;
	DAGRuntime d = thread.dr;

	// This is the last time we might refer to views acquired to
	// run our last application, so we can free them after this
	// (Unless the next application is a terminal node, and this
	// is the last app to finish, in which case no other app will be
	// modifying these views anyway. Besides, we're probably about
	// to start up some new applications, and for efficiency's sake
	// it would make sense to free these views before the new applications
	// ask for them).
	Set<String> names = node.output.keySet();
	Utilities.Assert(node.output.size() == output.getSize(),
			 "produce encounters mismatching number of views: "
			 + node.output.size() + " vs " + output.getSize());
	for (String name : names) {
	    AppInstanceNode.IOSpecification iospec = node.output.get(name);
	    Utilities.Assert(iospec != null, "produce encounters null iospec!");
	    View v = output.getView(iospec.pos);
	    Utilities.Assert(v != null, "produce encounters null view in pos: "
			     + iospec.pos);
	    String vClassName = v.getClass().getSimpleName();
	    Utilities.Assert(vClassName.compareTo(iospec.viewClassName) == 0,
			     "produce encounters mismatching view class type: "
			     + vClassName + " vs " + iospec.viewClassName);
	    d.env.bindView(name, v);
	}

	Iterator<AppInstanceEdge> it = node.edges.iterator();
	while (it.hasNext()) {
	    AppInstanceEdge e = it.next();
	    // If the next application isn't ready to be run, simply leave it
	    // until all
	    // preceding applications have finished.
	    if (d.signifyThreadArrived(e.next) == false)
		continue;

	    if (e.next instanceof TerminalNode) {
		//System.err.println("Worker "+taskMgr.getCurrentWorkerID()+"Committing DAG "+d.dag.id+" Instance "+d.instanceID);

		// To summit the DAG
		// Note that acquireViewsForNode should get all its views on the
		// first try, as all other strands of this DAG should
		// lead to the terminal node, and because all threads have
		// arrived at the terminal node, this should be the last
		// thread running in the DAG.
		// ViewsIOBucket output = d.env.acquireViewsForNode(e.next);
		// TODO: currently each view is committed separately. Advanced
		// feature is needed to
		// ensure that all views are either all successfully committed,
		// or none of them
		TerminalNode t = (TerminalNode) e.next;
		
		// Commit views in the order specified in the .dag file
		View[] outputs = new View[t.output.keySet().size()];
		String[] theNames = new String[t.output.keySet().size()];
		for (String name : t.output.keySet()) {
		    AppInstanceNode.IOSpecification iospec = t.output.get(name);
		    View view = d.env.local.getView(name);
		    outputs[iospec.pos] = view;
		    theNames[iospec.pos] = name;
		}
		for (int i = 0; i < outputs.length; i++) {
		    View view = outputs[i];
		    view.commit(vm.driver);
		    String name = theNames[i];
		    synchronized(vm.global) {
			vm.global.addView(name, view);
		    }
		}

		DAGFinish(d);

		break;
	    }

	    // We run all apps from outgoing edges in new threads,
	    // until we get to the last one, when we re-use this thread.
	    // if(it.hasNext()) {
	    // We have not reached the last edge in the list
	    // Currently doing this prevent the stack grows too large
	    DAGRuntimeThread tr = new DAGRuntimeThread(e.next, this, d);
	    d.addNodeThread(tr);
	    /*
	     * } else { // Last edge reached, reuse this thread
	     * thread.executeApp(e.next); }
	     */
	}

	// This is necessary because if an app calls produce() in processVV(),
	// and then DAGRuntime calls produce() again in run(), then produce is
	// called twice by the same app, possibly way after that app has
	// finished
	// and its data is no longer relevant. This could lead to unpredictable
	// behavior.
	// NOTE: Zheng: Now we assume that application does not call produce

	// thread.stop();
	// d.delNodeThread(thread);
    }

    /**
     * Finish this DAG, and check whether any DAGs in the triggered queue can
     * run after the current finishing release some conflict condition
     * 
     * @param d
     *            the finishing DAG
     */
    public void DAGFinish(DAGRuntime d) {
	d.finish();
	synchronized (running) {
	    running.remove(d.instanceID);
	}
	    
	LinkedList<DAGRuntime> toRemove = new LinkedList<DAGRuntime>();

	synchronized (triggered) {
	    Iterator<DAGRuntime> it = triggered.iterator();
	    while (it.hasNext()) {
		d = it.next();
		if (!checkConflict(d.dag)) {
		    toRemove.addLast(d);
		    //triggered.remove(d);
		}
	    }

	    for (DAGRuntime dd: toRemove) {
		triggered.remove(dd);
	    }
	}

	//Utilities.printlnDebug("("+workerMgr.getCurrentWorkerID()+") torun size is "+toRemove.size());
	for (DAGRuntime dd: toRemove) {
	    synchronized (running) {
		running.put(dd.instanceID, dd);
	    }
	    //Utilities.printDebug("("+workerMgr.getCurrentWorkerID()+") Trigger to run ");
	    d.start(this);
	}

	/*
	Iterator<DAGRuntime> it = toRemove.iterator();
	synchronized (triggered) {
	    while (it.hasNext()) {
		triggered.remove(it.next());
	    }
	}
	*/
    }

    /**
     * Abort this DAG, and check whether any DAGs in the triggered queue can run
     * after the current aborting release some conflict condition
     * 
     * @param d
     *            the finishing DAG
     */
    /*
    public void DAGAbort(DAGRuntime d) {
	d.abort();
	synchronized (running) {
	    running.remove(d.instanceID);
	}
	if (running.size() > 20)
	    System.err.println("size "+running.size());

	LinkedList<DAGRuntime> toRemove = new LinkedList<DAGRuntime>();

	synchronized (running) {
	    synchronized (triggered) {
		Iterator<DAGRuntime> it = triggered.iterator();
		while (it.hasNext()) {
		    d = it.next();
		    if (!checkConflict(d.dag)) {
			toRemove.add(d);
			running.put(d.instanceID, d);
			d.start(this);
		    }
		}
	    }
	}
		
	Iterator<DAGRuntime> it = toRemove.iterator();
	synchronized (triggered) {
	    while (it.hasNext()) {
		triggered.remove(it.next());
	    }
	}
    }
    */

    /**
     * Check whether this DAG are conflicting with any current running DAGs
     * 
     * @param dag the DAG to check which against the running DAGs
     * @return true if there is potential conflict, otherwise false
     */
    public boolean checkConflict(DAG dag) {
	if (dag.concurrent)
	    return false;
	long before = 0;
	if (Parameters.measurePerf) {
	    before = System.nanoTime();
	}
	HashSet<String> output = new HashSet<String>();
	synchronized (running) {
	    for (DAGRuntime dr : running.values()) {
		output.addAll(dr.dag.terminal.output.keySet());
	    }
	}
	Set<String> myInput = dag.toRead;
	Set<String> myOutput = dag.terminal.output.keySet();
	if (Utilities.intersect(myInput, output)
	    || Utilities.intersect(myOutput, output)) {
	    if (Parameters.measurePerf) {
		Parameters.t6 += System.nanoTime() - before;
	    }
	    return true;
	} else {
	    if (Parameters.measurePerf) {
		Parameters.t6 += System.nanoTime() - before;
	    }
	    return false;
	}
    }

    private synchronized int getNextInstanceID() {
	return nextInstanceID++;
    }
}
