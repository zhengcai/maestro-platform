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

/**
 *
 * @author Zheng
 * class that holds all constants used in the general Maestro system
 */
public class Constants {
    public class DAGStates {
	public static final int IDLE = 0;
	public static final int RUNNING = 1;
	public static final int WAITING = 2;
    }
	
    public class Mode {
	public static final int APPROVAL = 0;
	public static final int DICTATOR = 1;
	public static final int ITERATIVE = 2;
	public static final int CENTRALIZED = 3;
    }
	
    public final static int PRIORITY_HIGH = 2;
    public final static int PRIORITY_MEDIUM = 1;
    public final static int PRIORITY_LOW = 0;
    
    public static final long BEACONFRQCY = 20;
    public static final long LSATIMEOUT = 10;
    public static final int DEFAULTLSAMULTI = 50;
    
    public static final int REQUEST = 65535;
    public static final int TERMINAL = 65536;
    
    public static final int MAXIMUM_DIVIDE = 8;
}
