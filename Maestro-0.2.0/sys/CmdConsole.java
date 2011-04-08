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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

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
    
    int mode;
    public static final int INTERACTIVE = 1;
    public static final int DAEMON = 0;

    public CmdConsole(ApplicationManager am, ViewManager vm, int m) {
	appManager = am;
	viewManager = vm;
	this.setName("CmdConsole");
	setDaemon(true);
	mode = m;
    }
	
    public void run() {
	if (mode == INTERACTIVE)
	    interactive();
	if (mode == DAEMON)
	    daemon();
    }

    public void daemon() {
	final int BUFFERSIZE = 4000;
	try {
	    int port = Parameters.daemonPort;
	    Selector s = Selector.open();
	    ServerSocketChannel acceptChannel = ServerSocketChannel.open();
	    acceptChannel.configureBlocking(false);
	    byte[] ip = {0, 0, 0, 0};
	    InetAddress lh = InetAddress.getByAddress(ip);
	    InetSocketAddress isa = new InetSocketAddress(lh, port);
	    acceptChannel.socket().bind(isa);
	    acceptChannel.socket().setReuseAddress(true);

	    SelectionKey acceptKey = acceptChannel.register(s, SelectionKey.OP_ACCEPT);
	    ByteBuffer buffer = ByteBuffer.allocate(BUFFERSIZE);
	    
	    while (s.select() > 0) {
		Set<SelectionKey> readyKeys = s.selectedKeys();
		for (SelectionKey k : readyKeys) {
		    try {
			if (k.isAcceptable()) {
			    SocketChannel channel = ((ServerSocketChannel)k.channel()).accept();
			    channel.configureBlocking(false);
			    SelectionKey clientKey = channel.register(s, SelectionKey.OP_READ);
			} else if (k.isReadable()) {
			    SocketChannel channel = (SocketChannel)k.channel();
			    buffer.clear();
			    int size = channel.read(buffer);
			    if (size == -1) {
				channel.close();
				continue;
			    } else if (size == 0) {
				continue;
			    }

			    String request = new String(buffer.array(), 0, size);
			    buffer.clear();
			    String[] fields = request.split(" ");
			    if (fields[0].compareToIgnoreCase("GET") == 0) {
				if (fields[1].compareToIgnoreCase("SYSTEM") == 0)
				    prepareSystemPage(buffer);
				if (fields[1].compareToIgnoreCase("DRIVER") == 0)
				    viewManager.driver.prepareDriverPage(buffer);
				
				size = channel.write(buffer);
			    }
			}
		    } catch (IOException e) {
			e.printStackTrace();
			k.channel().close();
		    }
		}
		readyKeys.clear();
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    public void prepareSystemPage(ByteBuffer buffer) {
	Runtime runtime = Runtime.getRuntime();
	buffer.put(String.format("SYSTEM\n").getBytes());
	buffer.put(String.format("SystemTime %d\n", System.nanoTime()).getBytes());
	buffer.put(String.format("NumThreads %d\n", Parameters.divide).getBytes());
	buffer.put(String.format("FreeMemory %d\n", runtime.freeMemory()).getBytes());
	buffer.put(String.format("TotalMemory %d\n", runtime.totalMemory()).getBytes());
	for (int i=0;i<Parameters.divide;i++) {
	    buffer.put(String.format("Worker %d Counter %d\n", i, appManager.workerMgr.getCounter(i)).getBytes());
	}
	buffer.flip();
    }

    public void interactive() {
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
			viewManager.driver.print();
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
