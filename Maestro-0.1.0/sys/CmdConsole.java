/*
  CmdConsole.java

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

import events.openflow.PacketInEvent;
import views.openflow.ConnectivityLocalView;
import views.View;


/**
 * CmdConsole, the command line console interface to interact
 * with Maestro
 * @author Zheng Cai
 */
public class CmdConsole extends Thread {
    ApplicationManager appManager;
    ViewManager viewManager;

    public CmdConsole(ApplicationManager cm, ViewManager vm) {
	appManager = cm;
	viewManager = vm;
	this.setName("CmdConsole");
	setDaemon(true);
    }
	
    public void run() {
	printOptions();
	while (true) {
	    System.out.print("->");
	    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
	    String s;
	    try {
		while ((s = in.readLine()).length() != 0) {
		    if (s.compareTo("print") == 0) {
			System.out.println("  Please input the name of the view instance:");
			System.out.print("=>");
			s = in.readLine();
			if (0 == s.length()) {
			    System.out.println("  Error reading the name of the view instance!");
			    System.out.print("->");
			    continue;
			}
			View v = viewManager.getViewInstance(s);
			if (null == v) {
			    System.out.println("  Cannot find the view instance with name "+s);
			    System.out.print("->");
			    continue;
			}
			v.print();
		    }
		    if (s.compareTo("help") == 0 || s.compareTo("h") == 0) {
			printOptions();
		    }
		    if (s.compareTo("1") == 0) {
			viewManager.printAllViews();
			System.out.println();
			for (DAG dag : appManager.dags.values()) {
			    dag.print();
			    System.out.println();
			}
			/*
			System.err.println("t1 = "+Parameters.t1/1000000);
			System.err.println("t2 = "+Parameters.t2/1000000);
			System.err.println("t3 = "+Parameters.t3/1000000);
			System.err.println("t4 = "+Parameters.t4/1000000);
			System.err.println("t5 = "+Parameters.t5/1000000);
			System.err.println("t6 = "+Parameters.t6/1000000);
			System.err.println("t7 = "+Parameters.t7/1000000);
			System.err.println("t8 = "+Parameters.t8/1000000);
			System.err.println("c1 = "+Parameters.c1);
			System.err.println("c2 = "+Parameters.c2);
			System.err.println("c3 = "+Parameters.c3);
			viewManager.driver.print();
			*/
		    }
		    if (s.compareTo("quit") == 0) {
			Utilities.closeLogFile();
			System.out.println("Bah-bye!");
			System.exit(0);
		    }
		    System.out.print("->");
		}
	    }
	    catch(IOException e) {
		Utilities.printlnDebug("IOException!");
	    }
	    catch(NullPointerException e) {
		System.exit(0);
	    }
        }
    }
	
    public void printOptions() {
    	System.out.println("=====================================================================");
    	System.out.println("| (print)             Call a particular view's print function       |");
    	System.out.println("| (help/h)            Print all options again                       |");
	System.out.println("| (1)                 Run function1()                               |");
    	System.out.println("| (quit)              Quit Maestro                                  |");
    	System.out.println("+-------------------------------------------------------------------+");
    	System.out.println("  Please input your option:");
    }
}
