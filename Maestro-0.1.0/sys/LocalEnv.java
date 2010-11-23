package sys;

import views.*;

import java.util.*;
import java.util.concurrent.Semaphore;

/** Environment of view bindings in which DAGs are running*/
public class LocalEnv {
	public Environment local;
	
	Map<View,Semaphore> viewSemaphores;
	
	public LocalEnv() {
		viewSemaphores = new HashMap<View,Semaphore>();
	}
	
	public void addLocalENV(Environment env) {
	    /* Take a snapshot of the current environment */
	    synchronized(env) {
		local = env.clone();
	    }
		//for(View v : local.getAllViews()) viewSemaphores.put(v, new Semaphore(1));
	}
	
	public void bindView(String name, View v) {
		View current = local.getView(name);
		/* If we're binding a new view over an old view, I decided
		 * not to change whether the view was acquired.  It seemed as
		 * if this would be called while an application was running,
		 * meaning that while the new and old view occupy different
		 * spaces in memory, they represent the same idea, and so
		 * should be controlled by the same semaphore.  This can
		 * easily be changed if it's undesirable.
		 */
		if (current == null) {
			local.addView(name, v);
			//viewSemaphores.put(v,new Semaphore(1));
		} else {
			local.addView(name, v);
			//viewSemaphores.put(v, viewSemaphores.get(current));
			//viewSemaphores.remove(current);
		}
	}
	
	/** 
	 * This method gives all of the views that an AppInstanceNode
	 * reads.  Specifically, if, in the DAG configuration file,
	 * a view is listed as "input" under the app node, 
	 * then it is placed in the ViewsBucket returned by this method.
	 * 
	 * @param node The application node this method is examining
	 * @return All views read by node
	 */
	public ViewsIOBucket getAppNodeReadViews(AppInstanceNode node) {
		ViewsIOBucket result = new ViewsIOBucket();
		Iterator<String> it = node.input.keySet().iterator();
		while (it.hasNext()) {
			String name = it.next();
			View v = local.getView(name);
			/* Check whether the view instance exists */
			Utilities.Assert(null != v, "View instance "+name+" is null");
			AppInstanceNode.IOSpecification input = node.input.get(name);
			/* Check whether the class type of the view instance matches
			 * with what is declared for this application instance
			 */
			try {
				Utilities.Assert(Class.forName("views."+Parameters.bundle+"."+input.viewClassName).equals(v.getClass()),
						"View class type mismatch for view instance "+name);
			} catch (ClassNotFoundException e) {
				Utilities.Assert(false, "View class declared by name "
						+input.viewClassName+" not found");
			}
			result.addView(input.pos, v);
		}
		return result;
	}
	
	/** 
	 * This method gives all of the views that an AppInstanceNode
	 * writes.  It's similar to getAppNodeReadViews() except that
	 * this method returns views listed as "output" under the 
	 * app {@value node} in the DAG configuration file.
	 * 
	 * @param node The application node this method is examining
	 * @return All views written to by {@value node}
	 */
	private ViewsIOBucket getAppNodeWriteViews(AppInstanceNode node) {
		ViewsIOBucket result = new ViewsIOBucket();
		Iterator<String> it = node.output.keySet().iterator();
		while (it.hasNext()) {
			String name = it.next();
			View v = local.getView(name);
			// Check whether the view instance exists
			Utilities.Assert(null != v, "View instance "+name+" is null");
			AppInstanceNode.IOSpecification output = node.output.get(name);
			// Check whether the class type of the view instance matches
			// with what is declared for this application instance
			try {
				Utilities.Assert(Class.forName("views."+Parameters.bundle+"."+output.viewClassName).equals(v.getClass()),
						"View class type mismatch for view instance "+name);
			} catch (ClassNotFoundException e) {
				Utilities.Assert(false, "View class declared by name "
						+output.viewClassName+" not found");
			}
			result.addView(node.output.get(name).pos, v);
		}
		return result;
	}
	
	/** 
	 * This method releases all of the semaphores for every view
	 * this is read or written by node.  This it the logical
	 * complement to acquireViewForNode()
	 * 
	 * @param node The node whose views we're releasing
	 */
	public void releaseViewsForNode(AppInstanceNode node) {
		LinkedList<View> toRelease = new LinkedList<View>();
		// Both of these sets of reviews should've been reserved
		toRelease.addAll(getAppNodeReadViews(node).getAllViews());
		toRelease.addAll(getAppNodeWriteViews(node).getAllViews());
		
		for(View v : toRelease) {
			Semaphore s = viewSemaphores.get(v);
			s.release();
		}
	}
	
	/* FIXME: To really implement this mechanism correctly, we would have
	 * to implement read/write locks.  This is a simple stand-in
	 */
	public ViewsIOBucket acquireViewsForNode(AppInstanceNode node) {
		ViewsIOBucket ret;
		LinkedList<View> toAcquire = new LinkedList<View>();
		
		//toReturn = read views
		//toAcquire = read & write views
		ret = getAppNodeReadViews(node);
		toAcquire.addAll(getAppNodeWriteViews(node).getAllViews());
		toAcquire.addAll(ret.getAllViews());
		
		/* We need to acquire both the views that we'll read
		 * and the ones we'll write, to avoid stepping on other
		 * apps' toes, though processVV only expects to get the apps
		 * that its application reads, so we only return those.
		 * 
		 * It should always hold that toReturn is a subset of toAcquire
		 */
		acquireViews(toAcquire);
		
		return ret;
	}
	
	/** 
	 * This method acquires all of the views in {@value toAcquire}.  It blocks
	 * until all of the views in it are acquired, and is guaranteed not to
	 * deadlock, as it never holds a view while attempting to acquire another.
	 * A proof that this method will not deadlock is in comments at the bottom.
	 * 
	 * @param toAcquire The views to acquire
	 */
	private void acquireViews(LinkedList<View> toAcquire) {
		LinkedList<Semaphore> viewSemaphoreList = new LinkedList<Semaphore>();
		
		for(View v : toAcquire) viewSemaphoreList.add(viewSemaphores.get(v));
		
		Semaphore waitingOn = null, oldWaitingOn;
		
		while(true) {
			// Invariant: waitingOn contains the first known unavailable view
			
			// Boundry test: reserveViews has been called at least once, so
			// that we know if any view is unavailable, and waitingOn is null,
			// meaning that we know no view is unavailable.
			
			/* It should be true that either waitingOn is null, meaning reserveViews
			 * hasn't been called, or it is a view that has been acquired and is not
			 * in resultViewList.  If that latter is true, then oldWaitingOn keeps
			 * track of that view so that if we still fail to acquire all necessary
			 * views, we don't forget to acquire it on our third iteration.
			 * 
			 * We don't want to try (and inevitably fail) to acquire it twice.
			 */
			oldWaitingOn = waitingOn;
			waitingOn = attemptReserveViews(viewSemaphoreList);
			
			if(waitingOn != null) {
				/* We want to add oldWaitingOn back to resultView list so that next time
				 * reserveViews gets called, it attempts to acquire oldWaitingOn, which will
				 * have been released.
				 */
				if(oldWaitingOn != null) {
					oldWaitingOn.release(); // To avoid deadlock
					viewSemaphoreList.add(oldWaitingOn); //was removed earlier so that we didn't
														 //try to acquire it twice.
				}
				try {
					waitingOn.acquire();
				} catch (InterruptedException e) {
					Utilities.printlnDebug("Error acquiring views from environment");
					e.printStackTrace();
				}
				
				/* Next statement ensures that when reserveViews is called
				 * in the next iteration of this loop, it doesn't try to
				 * acquire the same view twice.
				 */
				viewSemaphoreList.remove(waitingOn);
			} else {
				if(oldWaitingOn != null && !viewSemaphoreList.contains(oldWaitingOn)) viewSemaphoreList.add(oldWaitingOn);
				break;
			}
		}
		
		/* A brief proof that this method will not cause deadlock:
		 * 
		 * Deadlock will happen if two calls to this method block because
		 * they are waiting for each other. That can only happen if one of
		 * our desired views is acquired (meaning another call cannot acquire
		 * it) while this method is waiting.  So to show that this method will
		 * not cause deadlock, it is sufficient to show that either this method
		 * has not acquired any views and may wait, or it has acquired views
		 * but must inevitably finish.
		 * 
		 * At this point, oldWaitingOn is either null or a view that has
		 * already been acquired, and union(resultViewList,oldWaitingOn)
		 * is the set of all views we wish to acquire.
		 * 
		 * There are four cases:
		 * 
		 * 1) oldWaitingOn and waitingOn are null:
		 *     All views successfully acquired; this method will finish.
		 * 
		 * 2) oldWaitingOn is null, and waitingOn is not null:
		 *     (This is the first time we are progressing through this loop)
		 *     ReserveViews has released all of the views that it acquired in
		 *     resultViewList, and because oldWaitingOn is null, we know that
		 *     resultViewList = the set of all desired views.  So none of our
		 *     desired views will be acquired while this method blocks.
		 *     
		 * 3) oldWaitingOn is not null, and waitingOn is null:
		 *     Because waitingOn is null, all views in resultViewList have
		 *     been acquired.  Because oldWaitingOn is already acquired,
		 *     we know that all desired views are acquired, and this method
		 *     will return so the next application can start.
		 * 
		 * 4) neither oldWaitingOn nor waitingOn is null:
		 *     Then no view in resultViewList has been acquired, so the only
		 *     one of our desired views that remains acquired is oldWaitingOn.
		 *     Because of the conditions in the if statement below, we know that
		 *     oldWaitingOn will also inevitably be released, so none of our
		 *     desired views will be acquired while this method blocks.
		 */
	}
	
	/** 
	 * This method attempts to acquire all the views in {@value views}.
	 * If all views in {@value views} are free, than this returns null, otherwise
	 * it returns the view that's in use.  Because this method is synchronized,
	 * it cannot livelock other threads; if a view is available when the method
	 * starts it will stay available until this method acquires it or terminates.
	 * 
	 * Note that it is very important to actually acquire the views, rather
	 * than simply checking that they're available and acquiring them in
	 * getViews(), because if they are not acquired then that creates a race condition
	 * in which a view could be available when this method exits but acquired
	 * immediately afterwords, so that it's not available when it's needed.
	 * 
	 * @param views The Views to attempt to acquire.
	 * @return the view that could not be acquired, or null if they were all acquired.
	 */
	private synchronized Semaphore attemptReserveViews(LinkedList<Semaphore> toAcquire) {
		/* FIXME View IDs of 0 would break this method, so we must either
		 * perform a sanity check earlier to forbid people from using a
		 * viewID of 0, or use some other means to identify IDs (i.e. if
		 * views have string identifiers, then this could return null if
		 * all views were acquired).
		 */
		LinkedList<Semaphore> acquired = new LinkedList<Semaphore>();
		for (Semaphore s : toAcquire) {
			if(s.tryAcquire()) {
				acquired.add(s);
			} else {
				for(Semaphore toRelease : acquired) toRelease.release();
				return s;
			}
		}
		return null;
	}
	
	public void clearLocal() {
		local.clearViews();
		viewSemaphores.clear();
	}
}
