/*
  Parameters.java

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

import java.util.concurrent.Semaphore;

/**
 * Parameters: the static class that contains all parameters in Maestro
 * @author Zheng Cai
 */
public class Parameters {
    /** Which bundle this Maestro is running with */
    public static String bundle = null;
	
    /** Whether to print debug information
     */
    public static boolean printDebug = true;
	
    public static int batchInputNum = 200;
    
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
	
    public static MyLong blocked = new MyLong((long)0);
    public static MyLong ran = new MyLong((long)0);
	
    public static boolean measurePerf = false;

    public static int mode = 1; //. 1 for round-robin, 2 for partition, 3 for selector

    public static long newCountfm = 0;
    public static long newCountpi = 0;
    public static long freeCountpi = 0;
    public static long newCountpo = 0;
    public static long newCountdata = 0;
    public static long newCountbuffer = 0;
    public static long newCountdr = 0;
    
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

    public static long totalProcessed = 0;

    public static long confSent = 0;
	
    public static ApplicationManager am = null;
    
    public static int maxWaitingDAGIns = 20;

    public static long maxDelay = 3000;
    
    public static boolean useMemoryMgnt = true;
    public static boolean dynamicExp = false;
    public static boolean useIBTAdaptation = true;
    
    public static boolean warmuped = false;
    public static boolean changePeriod = false;
    public static int numPorts = 0;

    public static long waiting = 0;
    public static long running = 0;

    public static int listenPort = 6633;
    public static int daemonPort = 8080;

    public static int consoleMode = 0;
    public static String dataLogFile = "datalog.txt";

    public static long bufferId = -1;
    public static long whenWarmuped = 0;
}
