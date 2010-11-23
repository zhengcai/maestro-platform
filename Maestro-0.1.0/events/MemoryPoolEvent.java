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
