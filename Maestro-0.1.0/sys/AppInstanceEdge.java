/*
  AppInstanceEdge.java

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

/** The directed edges in the DAG, which contains the next node and the ViewIDs 
 * which tell what kinds of views need to be passed
 * @author Zheng Cai
 * 
 */
public class AppInstanceEdge {
    /** Next node*/
    AppInstanceNode next;
    
    public AppInstanceEdge(AppInstanceNode n) {
	next = n;
    }
}
