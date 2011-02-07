/*
  ActivationNode.java

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


/** The activation node in the DAG which remembers which 
 * view-events will trigger the execution of this DAG
 * @author Zheng Cai
 * 
 */
public class ActivationNode extends AppInstanceNode {
    /** views which will trigger the activation event of this DAG */
    Set<String> viewNames;

    /** Repeat timer for this activation node, if used */
    Timer timer;
	
    /** Create a new instance of ActivationNode */
    public ActivationNode(String n, DAG d) {
	super(n, null, d);
	viewNames = new HashSet<String>();
    }
}
