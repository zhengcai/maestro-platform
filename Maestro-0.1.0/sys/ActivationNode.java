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

package sys;

import java.util.*;


/** The activation node in the DAG which remembers which 
 * view-events will trigger the execution of this DAG
 * @author Zheng
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
