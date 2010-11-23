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

import java.io.*;

/**
 * The Main class, entry point of the program.
 * @author Zheng
 */
public class Main {
    
    /** Creates a new instance of Main */
    public Main() {
    	
    }
    
    public static void readConfiguration(String configFile) {
	try {
	    BufferedReader input = new BufferedReader(new FileReader(configFile));
	    String line = null;
	    while ((line = input.readLine()) != null) {
		line = Utilities.TrimConfigString(line);
		String[] words = line.split(" ");
		if (words.length == 0) continue;
	    
		if (words[0].compareToIgnoreCase("verbose") == 0) {
		    Parameters.localDebug = (1 == Integer.parseInt(words[1]))?true:false;
		} else if (words[0].compareToIgnoreCase("batchInputNum") == 0) {
		    Parameters.batchInputNum = Integer.parseInt(words[1]);
		} else if (words[0].compareToIgnoreCase("batchOutput") == 0) {
		    Parameters.batchOutput = (1 == Integer.parseInt(words[1]))?true:false;
		} else if (words[0].compareToIgnoreCase("divide") == 0) {
		    Parameters.divide = Integer.parseInt(words[1]);
		    Utilities.Assert(Parameters.divide <= Constants.MAXIMUM_DIVIDE, 
				     "Divide exceeds MAXIMUM_DIVIDE which is "+Constants.MAXIMUM_DIVIDE);
		} else if (words[0].compareToIgnoreCase("maxWaitingDAGIns") == 0) {
		    Parameters.maxWaitingDAGIns = Integer.parseInt(words[1]);
		} else if (words[0].compareToIgnoreCase("threadBind") == 0) {
		    Parameters.threadBind = Integer.parseInt(words[1]);
		} else if (words[0].compareToIgnoreCase("countDone") == 0) {
		    Parameters.countDone = Integer.parseInt(words[1]);
		} else if (words[0].compareToIgnoreCase("queueUpperBound") == 0) {
		    Parameters.queueUpperBound = Integer.parseInt(words[1]);
		} else if (words[0].compareToIgnoreCase("outputLog") == 0) {
		    Utilities.openLogFile(words[1]);
		} else if (words[0].compareToIgnoreCase("port") == 0) {
		    Parameters.listenPort = Integer.parseInt(words[1]);
		} else {
		    
		}
	    } //. End of while
	} //. End of try
	catch (FileNotFoundException e) {
	    Utilities.Assert(false, "Configuration File Not Found!");
	} catch (EOFException e) {
	    Utilities.Assert(false, "End of stream!");
	} catch (IOException e) {
	    Utilities.Assert(false, "IO ERROR!");
	}
    }
	
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
	long test = (((long)1) << 32) - 1;
	System.err.println(test);

	
	if (args.length != 2) {
	    System.err.println("Usage: parameter-configuration-file dag-file");
	    System.exit(0);
	}
    	Thread.currentThread().setName("Main thread");
    	readConfiguration(args[0]);
    	ApplicationManager am = new ApplicationManager(args[1]);
    	Parameters.am = am;
    	am.run();
    }
}
