package sys;

/** The directed edges in the DAG, which contains the next node and the ViewIDs 
 * which tell what kinds of views need to be passed
 * @author Zheng
 * 
 */
public class AppInstanceEdge {
	/** Next node*/
	AppInstanceNode next;
	
	public AppInstanceEdge(AppInstanceNode n) {
		next = n;
	}
}
