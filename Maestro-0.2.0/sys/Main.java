/*
  Main.java

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

import java.io.*;
import views.openflow.*;

/**
 * The Main class, entry point of the program.
 * @author Zheng Cai
 */
public class Main {
    
    /** Creates a new instance of Main */
    public Main() {
    	
    }
    
    public static void readConfiguration(String configFile) {
	try {
	    BufferedReader input = new BufferedReader(new FileReader(configFile));
	    String line = null;
	    String outputLog = "log.txt";
	    while ((line = input.readLine()) != null) {
		line = Utilities.TrimConfigString(line);
		String[] words = line.split(" ");
		if (words.length == 0) continue;
	    
		if (words[0].compareToIgnoreCase("verbose") == 0) {
		    Parameters.printDebug = (1 == Integer.parseInt(words[1]))?true:false;
		} else if (words[0].compareToIgnoreCase("batchInputNum") == 0) {
		    Parameters.batchInputNum = Integer.parseInt(words[1]);
		} else if (words[0].compareToIgnoreCase("batchOutput") == 0) {
		    Parameters.batchOutput = (1 == Integer.parseInt(words[1]))?true:false;
		} else if (words[0].compareToIgnoreCase("numThreads") == 0) {
		    Parameters.divide = Integer.parseInt(words[1]);
		    Utilities.Assert(Parameters.divide <= Constants.MAXIMUM_DIVIDE, 
				     "numThreads exceeds the maximum value allowed which is "+Constants.MAXIMUM_DIVIDE);
		} /*else if (words[0].compareToIgnoreCase("threadBind") == 0) {
		    Parameters.threadBind = Integer.parseInt(words[1]);
		} else if (words[0].compareToIgnoreCase("countDone") == 0) {
		    Parameters.countDone = Integer.parseInt(words[1]);
		} else if (words[0].compareToIgnoreCase("queueUpperBound") == 0) {
		    Parameters.queueUpperBound = Integer.parseInt(words[1]);
		    } */
		else if (words[0].compareToIgnoreCase("outputLog") == 0) {
		    outputLog = words[1];
		} else if (words[0].compareToIgnoreCase("dataLogFile") == 0) {
		    Parameters.dataLogFile = words[1];
		} else if (words[0].compareToIgnoreCase("port") == 0) {
		    Parameters.listenPort = Integer.parseInt(words[1]);
		} else if (words[0].compareToIgnoreCase("daemonPort") == 0) {
		    Parameters.daemonPort = Integer.parseInt(words[1]);
		} else if (words[0].compareToIgnoreCase("mode") == 0) {
		    Parameters.mode = Integer.parseInt(words[1]);
		} else if (words[0].compareToIgnoreCase("maxDelay") == 0) {
		    Parameters.maxDelay = Integer.parseInt(words[1]);
		} else if (words[0].compareToIgnoreCase("disableIBTAdaptation") == 0) {
		    Parameters.useIBTAdaptation = false;
		} else if (words[0].compareToIgnoreCase("bufferSize") == 0) {
		    Parameters.bufferSize = Integer.parseInt(words[1]);
		} else {

		}
	    } //. End of while
	    Utilities.openLogFile(outputLog);
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
	/*
	MACTrie trie = new MACTrie();
	short[] mac1 = {1,2,3,4,5,6};
	short[] mac2 = {10,20,30,40,50,60};
	short[] mac3 = {100, 200, 300, 400, 500, 600};
	trie.addEntry(mac1, (short)1);
	trie.addEntry(mac2, (short)2);

	System.out.println(trie.getEntry(mac1));
	System.out.println(trie.getEntry(mac2));
	System.out.println(trie.getEntry(mac3));
	*/
	if (args.length != 3) {
	    System.err.println("Usage: parameter-configuration-file dag-file consoleMode");
	    System.exit(0);
	}
    	Thread.currentThread().setName("Main thread");
    	readConfiguration(args[0]);
	if (args[2].compareToIgnoreCase("interactive") == 0)
	    Parameters.consoleMode = CmdConsole.INTERACTIVE;
	else if (args[2].compareToIgnoreCase("daemon") == 0)
	    Parameters.consoleMode = CmdConsole.DAEMON;
	else
	    Parameters.consoleMode = Integer.parseInt(args[2]);
    	ApplicationManager am = new ApplicationManager(args[1]);
    	Parameters.am = am;
    	am.run();
    }
}
