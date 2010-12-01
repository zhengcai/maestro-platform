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
