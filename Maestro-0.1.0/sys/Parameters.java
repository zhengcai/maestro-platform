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

import java.util.concurrent.Semaphore;

public class Parameters {
    /** Which bundle this Maestro is running with */
    public static String bundle = null;
	
    /** Which mode the system is running in
     * APPROVAL, DICTATOR, ITERATIVE
     */
    public static int mode = 0;
	
    /** Simulator communication port */
    public static int simuPort = 0;
    /** Simulator host name */
    public static String simuHost = "";
	
    /** Whether maestro is run with the console
     * or with an actual network
     */
    public static boolean localDebug = false;
	
    /** Whether to print debug information
     */
    public static boolean printDebug = true;
	
    /** For time measurement usage
     * better not use in concurrency
     */
    public static long timeBefore = 0;
    public static long timeAfter = 0;
	
    /** For whether to allow ns simulator to continue */
    public static boolean nsContinue = true;
    public static Semaphore nsSem = new Semaphore(1);
	
    public static int tunnelFlowID = 1;
	
    public static String fwFile = "";
    public static String trafficFile = "";
	
    public static boolean DAshutup = false;
	
    public static int sensitive = 4;
    public static int multiplier = 20000;
	
    public static int retry = 4;
	
    public static boolean optimized = false;
	
    public static int batchInputNum = 4;
    
    public static boolean batchOutput = true;
    public static int divide = 1;

    /**
       Not using Long for be able to synchronize on this object
    */
    public static class MyLong {
	public long value;
		
	public MyLong(long l) {
	    value = l;
	}
    }
    public static MyLong count = new MyLong((long)0);
    public static long countDone = 2000000;
    public static long before = 0;
	
    public static MyLong blocked = new MyLong((long)0);
    public static MyLong ran = new MyLong((long)0);
	
    public static boolean measurePerf = false;
    public static long t1 = 0;
    public static long t2 = 0;
    public static long t3 = 0;
    public static long t4 = 0;
    public static long t5 = 0;
    public static long cc = 0;

    public static long t6 = 0;
    public static long t7 = 0;
    public static long t8 = 0;
    public static long c1 = 0;
    public static long c2 = 0;
    public static long c3 = 0;

    public static long confSent = 0;
	
    public static ApplicationManager am = null;
    
    public static int maxWaitingDAGIns = 10;
    
    public static boolean useMemoryMgnt = false;
    public static boolean warmuped = false;

    public static int threadBind = 1;

    public static int numPorts = 0;

    public static int queueUpperBound = 1000;
    public static int pipeDrained = 0;

    public static long waiting = 0;
    public static long running = 0;
}
