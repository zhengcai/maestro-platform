/*
  MemoryPoolEvent.java

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

package events;

/**
 * The kind of events that will be explicitly managed in a memory pool,
 * instead of in Java object heap
 * @author Zheng Cai
 */
abstract public class MemoryPoolEvent extends Event {
    public int poolIdx;
    public boolean valid;
    
    public MemoryPoolEvent(int idx) {
	poolIdx = idx;
	valid = false;
    }
}
