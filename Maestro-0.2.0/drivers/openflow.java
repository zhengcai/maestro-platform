/*
  openflow.java

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

package drivers;

import java.io.IOException;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.*;

import events.Event;
import events.openflow.FlowModEvent;
import events.openflow.LLDPPacketInEvent;
import events.openflow.PacketInEvent;
import events.openflow.PacketOutEvent;
import events.openflow.SwitchJoinEvent;
import events.openflow.ToSpecificSwitchEvent;
import sys.Constants;
import sys.Parameters;
import sys.Utilities;
import sys.DataLogManager;
import drivers.OFPConstants;
import headers.EthernetHeader;
import headers.LLDPHeader;

/**
 * The driver for OpenFlow switches
 * @author Zheng Cai
 */
public class openflow extends Driver {
    Random random;
    public boolean toprint = true;
	
    private static class Switch {
	public long dpid = 0;
	public SocketChannel channel = null;
	public int bufferSize = 0;
	//public byte[] buffer = new byte[10*BUFFERSIZE];
	public byte[] buffer = new byte[128000];

	public boolean chopping = false;
	public boolean sending = false;
	public boolean active = true;
	public boolean marked = false;
	public int chances = 0;
	public int skipped = 0;
	public int totalSize = 0;
	public int zeroes = 0;

	public long totalProcessed = 0;
	public long poPushed = 0;
	public long lastProcessed = 0;

	public Selector wSelector;

	public long c0 = 0; //.0
	public long c1 = 0; //.1-10
	public long c10 = 0; //.10-100
	public long c100 = 0; //.100-1024
		
	/** For those lldps received before the dpid of this switch is known */
	private LinkedList<LLDPPacketInEvent> lldpQueue;
		
	public Switch() {
	    lldpQueue = new LinkedList<LLDPPacketInEvent>();
	}

	public boolean send(ByteBuffer pkt) {
	    int ret = 0;
	    sending = true;
	    synchronized(channel) {
		try {
		    int count = 0;
		    while(pkt.hasRemaining()) {
			int wrt = channel.write(pkt);
			//if (Parameters.mode != 1) {
			if (true) {
			    if (wrt == 0) {			    
				boolean flag = true;
				while (flag) {
				    wSelector.select();
				    for (SelectionKey key : wSelector.selectedKeys()) {
					if (key.isValid() && key.isWritable()) {
					    flag = false;
					    break;
					}
				    }
				}
			    }
			}
			else {
			    count++;
			    if (count > 30000) {
				System.err.println("Too many tries for "+dpid);
				count = 0;
				//sending = false;
				//return ret;
			    }
			}
		    }
		} catch (Exception e) {
		    //e.printStackTrace();
		}
	    }
	    sending = false;
	    return true;
	}
    }

    /**
     * Switch socket round-robin pool implementation
     */
    private static class SwitchRRPool {
	public openflow of;
	public ArrayList<Switch> pool = null;
	private int currentPos = 0;

	public SwitchRRPool(openflow o) {
	    pool = new ArrayList<Switch>();
	    of = o;
	}

	public void addSwitch(Switch sw) {
	    synchronized(pool) {
		pool.add(sw);
	    }
	}

	public void removeSwitch(Switch sw) {
	    synchronized(pool) {
		int idx = pool.indexOf(sw);
		if (-1 != idx)
		    pool.remove(idx);
	    }
	}

	public void clearAndAdd(ArrayList<Switch> sws) {
	    synchronized(pool) {
		pool.clear();
		pool.addAll(sws);
		currentPos = 0;
	    }
	}

	public Switch getSwitchAt(int idx) {
	    if (idx >= pool.size())
		return null;
	    return pool.get(idx);
	}

	public Switch nextSwitch() {
	    synchronized(pool) {
		int size = pool.size();
		if (0 == size) {
		    return null;
		}
		for (int i = 0; i < size; i++) {
		    Switch sw = null;
		    try {
			sw = pool.get(currentPos);
		    } catch (IndexOutOfBoundsException e) {
			//. TODO: write logs in memory to disk here
			//Parameters.am.dataLogMgr.dumpLogs();
			//of.print();
			//System.err.println("System existing...");
			//Utilities.ForceExit(0);
			return null;
		    }
		    currentPos = (currentPos+1)%size;
		    if (!sw.chopping) {
			return sw;
		    }
		}

		//. All busy chopping
		return null;
	    }
	}

	public int getSize() {
	    return pool.size();
	}
    }
	
    private final static int BUFFERSIZE = 1024;
    private final static int PENDING = 500;
	
    private HashMap<Long, Switch> dpid2switch;
    private static HashMap<SocketChannel, Switch> chnl2switch;
    private Selector s;
    
    public Selector sel;
    //public boolean selecting = false;
    public int whoSel = 0;
    public ArrayList<Switch> sws = new ArrayList<Switch>();

    private static ArrayList<Selector> readSelectors = new ArrayList<Selector>();
    private SwitchRRPool swRRPool;
    private ArrayList<OpenFlowTask> workers;

    public LinkedList<ArrayList<RawMessage>> msgsQueue;

    private static class MsgsQueues {
	private ArrayList<LinkedList<ArrayList<RawMessage>>> qs;

	public MsgsQueues(int n) {
	    qs = new ArrayList<LinkedList<ArrayList<RawMessage>>>();
	    for (int i=0;i<n;i++) {
		qs.add(new LinkedList<ArrayList<RawMessage>>());
	    }
	}

	public LinkedList<ArrayList<RawMessage>> getQAt(int which) {
	    return qs.get(which);
	}

	public LinkedList<ArrayList<RawMessage>> getShortest() {
	    LinkedList<ArrayList<RawMessage>> ret = null;
	    int shortest = Integer.MAX_VALUE;
	    for (LinkedList<ArrayList<RawMessage>> q : qs) {
		int size = q.size();
		if (size < shortest) {
		    shortest = size;
		    ret = q;
		}
	    }
	    return ret;
	}

	public int getTotal() {
	    int total = 0;
	    for (LinkedList<ArrayList<RawMessage>> q : qs) {
		total += q.size();
	    }
	    return total;
	}
    }

    public MsgsQueues msgsQs;
	
    public openflow() {
    	random = new Random();
    	dpid2switch = new HashMap<Long, Switch>();
    	chnl2switch = new HashMap<SocketChannel, Switch>();
	swRRPool = new SwitchRRPool(this);
	workers = new ArrayList<OpenFlowTask>();
	if(Parameters.mode == 3) {
	    msgsQueue = new LinkedList<ArrayList<RawMessage>>();
	    //msgsQs = new MsgsQueues(Parameters.divide);
	}
	try {
	    for (int i=0;i<Parameters.divide;i++) {
		readSelectors.add(Selector.open());
	    }
	} catch (IOException e) {

	}
    }
    
    public boolean SendPktOut(long dpid, ByteBuffer pkt) {
	Switch target;
	target = dpid2switch.get(dpid);
	Utilities.Assert(target != null, "Cannot find target switch for dpid "+dpid);
	return target.send(pkt);
    }

    public SwitchRRPool getRRPool() {
	return swRRPool;
    }
    
    public void start() {
    	try {
	    int port = Parameters.listenPort;
	    s = Selector.open();
	    sel = Selector.open();
	    ServerSocketChannel acceptChannel = ServerSocketChannel.open();
	    acceptChannel.configureBlocking(false);
	    byte[] ip = {0, 0, 0, 0};
	    InetAddress lh = InetAddress.getByAddress(ip);
	    InetSocketAddress isa = new InetSocketAddress(lh, port);
	    acceptChannel.socket().bind(isa);
	    acceptChannel.socket().setReuseAddress(true);
			
	    SelectionKey acceptKey = acceptChannel.register(s, SelectionKey.OP_ACCEPT);
	    int which = 0;

	    int who = 0;

	    long thetime = 0;
	    long before = 0;
	    long num = 0;

	    while (s.select() > 0) {
		/*
		long now = System.nanoTime();
		if (Parameters.warmuped) {
		    thetime += now - before;
		    num ++;

		    if (num % 1000 == 0) {
			System.err.println(thetime / (num*1000));
		    }
		}
		before = now;
		*/

		
		//if (Parameters.warmuped) {
		//Thread.sleep(1);
		//}
		
		Set<SelectionKey> readyKeys = s.selectedKeys();
		for (SelectionKey k : readyKeys) {
		    try {
			if (k.isAcceptable()) {
			    SocketChannel channel = ((ServerSocketChannel)k.channel()).accept();
			    channel.configureBlocking(false);
			    Switch sw = new Switch();
			    sw.channel = channel;
			    chnl2switch.put(channel, sw);
			    swRRPool.addSwitch(sw);
			    synchronized (sws) {
				sws.add(sw);
			    }
			    //sendHelloMessage(sw);

			    if (Parameters.mode == 3) {
				SelectionKey clientKey = channel.register(s, SelectionKey.OP_READ);
			    } else if (Parameters.mode == 1) {
				//SelectionKey clientKey = channel.register(s, SelectionKey.OP_READ);
				SelectionKey clientKey = channel.register(sel, SelectionKey.OP_READ);
				/*
				for (Selector rs : readSelectors) {
				    SelectionKey clientKey = channel.register(rs, SelectionKey.OP_READ);
				}
				*/
			    } else if (Parameters.mode == 4) {
				workers.get(who).partition.addSwitch(sw);
				who = (who+1)%Parameters.divide;

				//workers.get(channel.hashCode()%Parameters.divide).partition.addSwitch(sw);
			    }

			    sw.wSelector = Selector.open();
			    channel.register(sw.wSelector, SelectionKey.OP_WRITE);
			} else if (k.isReadable()) { //. Only reachable when Parameters.mode == 3
			    if (Parameters.mode == 3) {
				Switch sw = chnl2switch.get((SocketChannel)k.channel());
				Utilities.Assert(sw.channel == k.channel(), "Channels do not match!");
				ByteBuffer buffer = ByteBuffer.allocate(Parameters.bufferSize);
				int size = sw.channel.read(buffer);
				if (size == -1) {
				    sw.channel.close();
				    continue;
				} else if (size == 0) {
				    System.err.println("DAMN, 0");
				    continue;
				}
				
				ArrayList<RawMessage> msgs = chopMessages(sw, buffer, size);			    
				if (msgs != null && msgs.size()>0) {
				    
				    synchronized (msgsQueue) {
					msgsQueue.add(msgs);
					msgsQueue.notify();
				    }
				    
				    
				    /*
				      LinkedList<ArrayList<RawMessage>> q = msgsQs.getQAt(which);
				      which = (which+1) % Parameters.divide;
				      synchronized (q) {
				      q.add(msgs);
				      q.notify();
				      }
				    */
				    
				    sw.totalProcessed += msgs.size();
				    Parameters.totalProcessed += msgs.size();
				}
				
				while (msgsQueue.size() > PENDING) {
				    //while (msgsQs.getTotal() > PENDING) {
				    Thread.sleep(1);
				}
			    }
			}
		    } catch (IOException e) {
			e.printStackTrace();
			k.channel().close();
			System.exit(0);
		    }
		}
		readyKeys.clear();
	    }
	} catch (IOException e) {
	    System.err.println("IOException in Selector.open()");
	    e.printStackTrace();
	} catch (InterruptedException e) {
	    e.printStackTrace();
	}
    }

    private static class RawMessage {
	public byte[] buf;
	public int start;
	public int length;
	public Switch sw;

	public RawMessage(byte[] b, int st, int len, Switch s) {
	    buf = b;
	    start = st;
	    length = len;
	    sw = s;
	}
    }

    /** Chop raw messages from a buffer, leaving half-done bytes in the switch's buffer */
    public ArrayList<RawMessage> chopMessages(Switch sw, ByteBuffer buffer, int size) {
	byte[] buf = buffer.array();
	ArrayList<RawMessage> ret = new ArrayList<RawMessage>();
	int bufPos = 0;
	if (sw.bufferSize >= OFPConstants.OfpConstants.OFP_HEADER_LEN) {
	    int length = Utilities.getNetworkBytesUint16(sw.buffer, 2);
	    
	    if ((length - sw.bufferSize) <= size) {
		byte[] b = new byte[length];
		Utilities.memcpy(b, 0, sw.buffer, 0, sw.bufferSize);
		Utilities.memcpy(b, sw.bufferSize, buf, bufPos, length-sw.bufferSize);                 
		size -= (length-sw.bufferSize);                                              
		bufPos += (length-sw.bufferSize);                                               
		ret.add(new RawMessage(b, 0, length, sw));
		sw.bufferSize = 0;                                                           
	    } else {                                                                        
		Utilities.memcpy(sw.buffer, sw.bufferSize, buf, bufPos, size);                                  
		sw.bufferSize += size;
		return ret;
	    }                                                                               
	} else if (sw.bufferSize > 0) {                                                  
	    if ((sw.bufferSize + size) >= OFPConstants.OfpConstants.OFP_HEADER_LEN) {                     
		Utilities.memcpy(sw.buffer, sw.bufferSize, buf, bufPos,
				 OFPConstants.OfpConstants.OFP_HEADER_LEN-sw.bufferSize);                             
		size -= (OFPConstants.OfpConstants.OFP_HEADER_LEN-sw.bufferSize);                           
		bufPos += (OFPConstants.OfpConstants.OFP_HEADER_LEN-sw.bufferSize);                            
		sw.bufferSize = OFPConstants.OfpConstants.OFP_HEADER_LEN;                                   
    		
		int length = Utilities.getNetworkBytesUint16(sw.buffer, 2);
    		
		if ((length - sw.bufferSize) <= size) {
		    byte[] b = new byte[length];
		    Utilities.memcpy(b, 0, sw.buffer, 0, sw.bufferSize);
		    Utilities.memcpy(b, sw.bufferSize, buf, bufPos, length-sw.bufferSize);                 
		    size -= (length-sw.bufferSize);                                              
		    bufPos += (length-sw.bufferSize);                                               
		    ret.add(new RawMessage(b, 0, length, sw));
		    sw.bufferSize = 0;
		} else {                                                                      
		    Utilities.memcpy(sw.buffer, sw.bufferSize, buf, bufPos, size);                                
		    sw.bufferSize += size;
		    return ret;                                                                     
		}                                                                             
	    } else {                                                                        
		//. Still not enough for holding ofp_header                                    
		Utilities.memcpy(sw.buffer, sw.bufferSize, buf, bufPos, size);                                  
		sw.bufferSize += size;
		return ret;
	    }                                                                               
	}
    	
	while (size > 0) {    		                                                                                    
	    //. Not enough for holding ofp_header                                            
	    if (size < OFPConstants.OfpConstants.OFP_HEADER_LEN) {                                         
		Utilities.memcpy(sw.buffer, 0, buf, bufPos, size);                                                 
		sw.bufferSize = size;                                                        
		break;                                                                        
	    } 
	    int length = Utilities.getNetworkBytesUint16(buf, bufPos+2);
	    if (length > size) {                                                     
		Utilities.memcpy(sw.buffer, 0, buf, bufPos, size);                                                 
		sw.bufferSize = size;                                                        
		break;                                                                        
	    }

	    if (0 == length) {
		Utilities.printlnDebug("ERROR: length in OFP header is 0!");
		return ret;
	    }
		
	    //. buffer is not going to be shared among worker threads, so no need to copy
	    ret.add(new RawMessage(buf, bufPos, length, sw));
	    size -= length;                                                                 
	    bufPos += length;                                                                  
	}
	return ret;
    }

    /**
      @return whether this packet is a PacketIn
    */
    public boolean dispatchPacket(Switch sw, byte[] buffer, int pos, int size, boolean flush) {
    	
    	short type = Utilities.getNetworkBytesUint8(buffer, pos+1);
	int length = Utilities.getNetworkBytesUint16(buffer, pos+2);
	switch(type) {
	case OFPConstants.PacketTypes.OFPT_HELLO:
	    //Utilities.printlnDebug("Got hello");
	    sendFeatureRequest(sw);
	    break;
	case OFPConstants.PacketTypes.OFPT_ECHO_REQUEST:
	    //Utilities.printlnDebug("Got echo");
	    Utilities.setNetworkBytesUint8(buffer, pos+1, OFPConstants.PacketTypes.OFPT_ECHO_REPLY);	    
	    ByteBuffer buf = ByteBuffer.allocate(length);
	    if (size != length) {
		Utilities.printlnDebug("BAD! In handling echo_request: size != length");
	    } else {
		buf.put(buffer, pos, length);
	    }
	    sw.send(buf);
	    break;
	case OFPConstants.PacketTypes.OFPT_FEATURES_REPLY:
	    //Utilities.printlnDebug("Got features_reply");
	    handleFeaturesReply(sw, buffer, pos, length);
	    break;
	case OFPConstants.PacketTypes.OFPT_PACKET_IN:
	    return handlePacketIn(sw, buffer, pos, length, flush);
	    //break;
	default:
	    break;
	}
	return false;
    }
    
    public void handleFeaturesReply(Switch sw, byte[] buffer, int pos, int length) {
    	pos += OFPConstants.OfpConstants.OFP_HEADER_LEN;
    	SwitchJoinEvent sj = new SwitchJoinEvent();
    	sj.dpid = Utilities.getNetworkBytesUint64(buffer, pos);
    	pos += 8;
    	sw.dpid = sj.dpid;
    	synchronized(dpid2switch) {
	    dpid2switch.put(sw.dpid, sw);
	}
    	
    	sj.nBuffers = Utilities.getNetworkBytesUint32(buffer, pos);
    	pos += 4;
    	sj.nTables = Utilities.getNetworkBytesUint8(buffer, pos);
    	pos += 4;
    	sj.capabilities = Utilities.getNetworkBytesUint32(buffer, pos);
    	pos += 4;
    	sj.actions = Utilities.getNetworkBytesUint32(buffer, pos);
    	pos += 4;
    	sj.nPorts = (length-OFPConstants.OfpConstants.OFP_SWITCH_FEATURES_LEN)
	    /OFPConstants.OfpConstants.OFP_PHY_PORT_LEN;
    	sj.ports = new SwitchJoinEvent.PhysicalPort[sj.nPorts];
    	for (int i=0;i<sj.nPorts;i++) {
	    sj.ports[i] = new SwitchJoinEvent.PhysicalPort();
	    SwitchJoinEvent.PhysicalPort p = sj.ports[i];
	    p.portNo = Utilities.getNetworkBytesUint16(buffer, pos);
	    pos += 2;
	    for (int j=0;j<OFPConstants.OfpConstants.OFP_ETH_ALEN;j++) {
		p.hwAddr[j] = Utilities.getNetworkBytesUint8(buffer, pos++);
	    }
	    for (int j=0;j<OFPConstants.OfpConstants.OFP_MAX_PORT_NAME_LEN;j++) {
		p.name[j] = buffer[pos++];
	    }
	    p.config = Utilities.getNetworkBytesUint32(buffer, pos);
	    pos += 4;
	    p.state = Utilities.getNetworkBytesUint32(buffer, pos);
	    pos += 4;
	    p.curr = Utilities.getNetworkBytesUint32(buffer, pos);
	    pos += 4;
	    p.advertised = Utilities.getNetworkBytesUint32(buffer, pos);
	    pos += 4;
	    p.supported = Utilities.getNetworkBytesUint32(buffer, pos);
	    pos += 4;
	    p.peer = Utilities.getNetworkBytesUint32(buffer, pos);
	    pos += 4;
    	}
    	vm.postEvent(sj);
    	synchronized(sw.lldpQueue) {
	    int size = sw.lldpQueue.size();
	    //LLDPPacketInEvent last = null;
	    for (LLDPPacketInEvent lldp : sw.lldpQueue) {
		lldp.dstDpid = sw.dpid;
		if (size > 1)
		    vm.postEventWithoutTrigger(lldp);
		else
		    vm.postEvent(lldp);
		size --;
	    }
	    sw.lldpQueue.clear();
    	}
    }

    public boolean handlePacketIn(Switch sw, byte[] buffer, int pos, int length, boolean flush) {
	PacketInEvent pi;
	if (Parameters.useMemoryMgnt) {
	    pi = Parameters.am.memMgr.allocPacketInEvent();
	} else {
	    pi = new PacketInEvent();
	}
	pi.flush = flush;
    	
    	pi.xid = Utilities.getNetworkBytesUint32(buffer, pos+4);
    	pos += OFPConstants.OfpConstants.OFP_HEADER_LEN;
    	pi.dpid = sw.dpid;
    	pi.bufferId = Utilities.getNetworkBytesUint32(buffer, pos);
    	pos += 4;
    	pi.totalLen = Utilities.getNetworkBytesUint16(buffer, pos);
    	pos += 2;
    	pi.inPort = Utilities.getNetworkBytesUint16(buffer, pos);
    	pos += 2;
    	pi.reason = Utilities.getNetworkBytesUint8(buffer, pos);
    	pos += 2; //. including 1 byte pad
    	
	/*
    	Utilities.Assert(pi.totalLen == (length-OFPConstants.OfpConstants.OFP_PACKET_IN_LEN), 
			 String.format("unmatched PacketIn data length: totalLen=%d bufLen=%d", 
				       pi.totalLen, (length-OFPConstants.OfpConstants.OFP_PACKET_IN_LEN)));
	
	
    	if (Parameters.useMemoryMgnt) {
	    pi.data = Parameters.am.memMgr.allocPacketInEventDataPayload(pi.totalLen);
    	}
    	else {
	    pi.data = new PacketInEvent.DataPayload(pi.totalLen);
    	}
	
    	Utilities.memcpy(pi.data.data, 0, buffer, pos, pi.totalLen);
	*/

	///////////////////// WARNING: CURRENT A HACK HERE, IGNORING pi.totalLen
	pi.totalLen = length-OFPConstants.OfpConstants.OFP_PACKET_IN_LEN;
	if (Parameters.useMemoryMgnt) {
	    pi.data = Parameters.am.memMgr.allocPacketInEventDataPayload(pi.totalLen);
	}
	else {
	    pi.data = new PacketInEvent.DataPayload(pi.totalLen);
	}

	Utilities.memcpy(pi.data.data, 0, buffer, pos, pi.totalLen);
	////////////////////////////////

	//. Currently assume that all packets are ethernet frames
	EthernetHeader eth;
	if (Parameters.useMemoryMgnt) {
	    eth = Parameters.am.memMgr.allocEthernetHeader();
	} else {
	    eth = new EthernetHeader();
	}
	pos = eth.parseHeader(buffer, pos);
	pi.extractFlowInfo(eth);

	if (OFPConstants.OfpConstants.ETH_TYPE_LLDP == pi.flow.dlType) {
	    LLDPPacketInEvent lldp = new LLDPPacketInEvent();
	    if(!(eth.inner instanceof LLDPHeader)) {
		Utilities.printlnDebug("The LLDP packet is not correctly formated");
		return false;
	    }
	    LLDPHeader lldpHeader = (LLDPHeader)eth.inner;
	    lldp.srcDpid = Utilities.getNetworkBytesUint64(lldpHeader.chassisId.value, 0);
	    lldp.srcPort = Utilities.getNetworkBytesUint16(lldpHeader.portId.value, 0);
	    lldp.ttl = Utilities.getNetworkBytesUint16(lldpHeader.ttl.value, 0);
	    
	    lldp.dstDpid =pi.dpid;
	    lldp.dstPort = pi.inPort;
	    if (Parameters.useMemoryMgnt) {
		Parameters.am.memMgr.freePacketInEvent(pi);
		eth.free();
	    }
	    if (sw.dpid == 0) {
		synchronized(sw.lldpQueue) {
		    sw.lldpQueue.addLast(lldp);
		}
	    } else {
		vm.postEvent(lldp);
	    }
	    return false;
	} else {
	    eth.free();
	    if (Parameters.divide > 0) {
		int toWhich = Parameters.am.workerMgr.getCurrentWorkerID();
		vm.postEventConcurrent(pi, toWhich);
	    } else {
		vm.postEvent(pi);
	    }
	    return true;
	}
    }

    /**
     * The worker thread is free, flush all the batched PacketsInEvent to be 
     * processed immediately
     */
    public void flush() {
	PacketInEvent pi = PacketInEvent.flushDummy;
	pi.dummy = true;
	if (Parameters.divide > 0) {
	    int toWhich = Parameters.am.workerMgr.getCurrentWorkerID();
	    vm.postEventConcurrent(pi, toWhich);
	} else {
	    vm.postEvent(pi);
	}
    }
    
    public void sendHelloMessage(Switch sw) {
    	ByteBuffer buffer = ByteBuffer.allocate(OFPConstants.OfpConstants.OFP_HEADER_LEN);
    	byte[] packet = buffer.array();
    	int pos = 0;
    	Utilities.setNetworkBytesUint8(packet, pos++, OFPConstants.OfpConstants.OFP_VERSION);
    	Utilities.setNetworkBytesUint8(packet, pos++, OFPConstants.PacketTypes.OFPT_HELLO);
    	Utilities.setNetworkBytesUint16(packet, pos, OFPConstants.OfpConstants.OFP_HEADER_LEN);
    	pos += 2;
    	Utilities.setNetworkBytesUint32(packet, pos, 0);
    	
	sw.send(buffer);
    }
    
    public void sendFeatureRequest(Switch sw) {
    	ByteBuffer buffer = ByteBuffer.allocate(OFPConstants.OfpConstants.OFP_HEADER_LEN);
    	byte[] packet = buffer.array();
    	int pos = 0;
    	Utilities.setNetworkBytesUint8(packet, pos++, OFPConstants.OfpConstants.OFP_VERSION);
    	Utilities.setNetworkBytesUint8(packet, pos++, OFPConstants.PacketTypes.OFPT_FEATURES_REQUEST);
    	Utilities.setNetworkBytesUint16(packet, pos, OFPConstants.OfpConstants.OFP_HEADER_LEN);
    	pos += 2;
    	Utilities.setNetworkBytesUint32(packet, pos, 0);
    	
	sw.send(buffer);
    }

    class LogContent extends DataLogManager.Content {
	public long dpid;
	public int size;
	public LogContent(long d, int s) {
	    dpid = d;
	    size = s;
	}
	public String toString() {
	    return String.format("%d %d\n", dpid, size);
	}
    }
    
    @Override
    public boolean commitEvent(ArrayList<Event> events) {
	if (events.size() == 0) {
	    return true;
	}
	Event e = events.get(0);
	if (e == null) {
	    return false;
	}
	if (e instanceof PacketOutEvent) {
	    int size = events.size();
	    boolean ret = true;
	    //if (((PacketOutEvent)e).send)
	    ret = processToSpecificSwitchEvent(events);

	    if (ret) {
		Parameters.am.workerMgr.increaseCounter(size);
		
		if (Parameters.am.dataLogMgr.enabled) {
		    Parameters.am.dataLogMgr.addEntry(new LogContent(((PacketOutEvent)e).dpid, size));
		}
	    }
	    
	    return ret;
	}
	if (e instanceof FlowModEvent) {
	    return processToSpecificSwitchEvent(events);
	}
	return true;
    }

    class Partition {
	public ArrayList<Event> es;// = new ArrayList<Event>();
	public int totalLength = 0;
	public long dpid;
	public Switch sw;
		
	public ByteBuffer toPacket() {
	    ByteBuffer pkt;
	    if (Parameters.useMemoryMgnt) {
		pkt = Parameters.am.memMgr.allocByteBuffer(totalLength);
	    }
	    else {
		pkt = ByteBuffer.allocate(totalLength);
	    }
	    
	    int pos = 0;
	    for (Event e : es) {
		ToSpecificSwitchEvent tsse = (ToSpecificSwitchEvent)e;
		Utilities.Assert(dpid == tsse.dpid, "dpid does not match!");
		pos += tsse.convertToBytes(pkt.array(), pos);
		if (Parameters.useMemoryMgnt) {
		    if (tsse instanceof FlowModEvent) {
			Parameters.am.memMgr.freeFlowModEvent((FlowModEvent)tsse);
		    }
		    if (tsse instanceof PacketOutEvent) {
			Parameters.am.memMgr.freePacketInEventDataPayload(((PacketOutEvent)tsse).data);
			Parameters.am.memMgr.freePacketOutEvent((PacketOutEvent)tsse);
		    }
		}
	    }
	    pkt.limit(totalLength);

	    return pkt;
    	}
    }
	
    private boolean processToSpecificSwitchEvent(ArrayList<Event> events) {
	// Assume that the dpids in "events" are the same
	long dpid = ((ToSpecificSwitchEvent)events.get(0)).dpid;
	Switch target = dpid2switch.get(dpid);
	if (null == target)
	    return false;
	if (target.sending)
	    return false;

	
	Partition pt = new Partition();
	pt.dpid = dpid;
	pt.sw = target;
	for (Event e : events) {
	    ToSpecificSwitchEvent tsse = (ToSpecificSwitchEvent)e;
	    pt.totalLength += tsse.getLength();
	}
	pt.es = events;

	/*
	if (Parameters.divide == 0) {
	    if (Parameters.batchOutput) {
		ByteBuffer pkt = pt.toPacket();
		SendPktOut(pt.dpid, pkt, pkt.array().length);
	    } else {
		for (Event e : pt.es) {
		    ToSpecificSwitchEvent tsse = (ToSpecificSwitchEvent)e;
		    ByteBuffer pkt = ByteBuffer.allocate(tsse.getLength());
		    tsse.convertToBytes(pkt.array(), 0);
		    SendPktOut(tsse.dpid, pkt, pkt.array().length);
		}
	    }
	    return true;
	}
	*/
		
	boolean toRun = true;
	
	/* For better paralizability of memory management,
	 * we do not do partition consolidation
	 */
	/*
	synchronized(pendingPts) {
	    Partition pp = pendingPts.get(pt.dpid);
	    if (pp != null) {
		pp.es.addAll(pt.es);
		pp.totalLength += pt.totalLength;
		//. We can consolidate this partition to an existing one
		toRun = false;
		pt.es.clear();
	    }
	}
	*/

	if (toRun) {
	    /* For better paralizability of memory management,
	     * we do not do partition consolidation
	     */
	    /*
	    synchronized(pendingPts) {
		pendingPts.put(pt.dpid, pt);
	    }
	    */


	    /* For better paralizability of memory management,
	     * we do not do partition consolidation
	     */
	    /*
	    // To avoid deadlock
	    synchronized(of.pendingPts) {
	    of.pendingPts.remove(pt.dpid);
	    }
	    */

	    int size = pt.es.size();
	    if (Parameters.batchOutput) {
		ByteBuffer pkt = pt.toPacket();
		boolean ret = SendPktOut(pt.dpid, pkt);
		if (Parameters.useMemoryMgnt) {
		    Parameters.am.memMgr.freeByteBuffer(pkt);
		}
	    } else {
		for (Event e : pt.es) {
		    ToSpecificSwitchEvent tsse = (ToSpecificSwitchEvent)e;
		    ByteBuffer pkt = ByteBuffer.allocate(tsse.getLength());
		    tsse.convertToBytes(pkt.array(), 0);
		    SendPktOut(tsse.dpid, pkt);
		    if (Parameters.useMemoryMgnt) {
			if (tsse instanceof FlowModEvent) {
			    Parameters.am.memMgr.freeFlowModEvent((FlowModEvent)tsse);
			}
			if (tsse instanceof PacketOutEvent) {
			    Parameters.am.memMgr.freePacketInEventDataPayload(((PacketOutEvent)tsse).data);
			    Parameters.am.memMgr.freePacketOutEvent((PacketOutEvent)tsse);
			}
		    }
		}
	    }

	    
	    if ( pt.es.get(0) instanceof PacketOutEvent) {
		target.poPushed += size;
	    }
	    
	    
	    pt.es.clear();
	}

	return true;
    }
	
    public void print() {
	PrintWriter log = null;
	try {
	    log = new PrintWriter(new File("histogram.txt"));
	} catch (FileNotFoundException e) {
	    System.err.println("Not found!!!!!!!!");
	} 
	for (long i=1;i<=dpid2switch.values().size();i++) {
	    Switch sw = dpid2switch.get(i);
	    if (sw == null) {
		break;
	    }
	    log.println(String.format("#%d %d %d %d %d", i, sw.c0, sw.c1, sw.c10, sw.c100));
	    /*
	    if (sw.totalSize > 100000)
		System.err.println("Switch # "+i+" chances "+sw.chances+" processed "+sw.totalProcessed+" POpushed "+sw.poPushed+" totalSize "+sw.totalSize+" zeroes "+sw.zeroes);
	    */
	}
	log.close();
    }

    public static class OpenFlowTask implements Runnable {
	private openflow of;

	public OpenFlowTask(openflow o) {
	    of = o;
	}

	public void select() {
	    if (of.sel == null) {
		return;
	    }

	    try {
		if (Parameters.mode == 1) {
		    for (Switch sw : of.sws) {
			sw.marked = false;
		    }
		}
	    } catch (ConcurrentModificationException e) {
		return;
	    } catch (NoSuchElementException e) {
		return;
	    } catch (NullPointerException e) {
		return;
	    }
		

	    try {
		if (of.sel.selectNow() > 0) {
		    Set<SelectionKey> readyKeys = of.sel.selectedKeys();
		    for (SelectionKey k : readyKeys) {
			if (k.isReadable()) {
			    Switch sw = of.chnl2switch.get((SocketChannel)k.channel());
			    sw.marked = true;
			}
		    }
		    readyKeys.clear();
		    
		    if (Parameters.mode == 1) {
			for (Switch sw : of.sws) {
			    sw.active = sw.marked;
			}
		    }
		}
	    } catch (IOException e ) {
		e.printStackTrace();
		System.exit(0);
	    } catch (ConcurrentModificationException e) {
		return;
	    } catch (NoSuchElementException e) {
		return;
	    } catch (NullPointerException e) {
		return;
	    }
	}

	public void run() {
	    if (Parameters.mode == 2 || Parameters.mode == 4) {
		partition = new SwitchRRPool(of);
		if (Parameters.mode == 4) {
		    Parameters.bufferSize = 63000;
		}
	    }
	    int workerID = Parameters.am.workerMgr.getCurrentWorkerID();
	    ByteBuffer buffer = ByteBuffer.allocate(Parameters.bufferSize);
	    int voidRead = 0;
	    int idx = 0;
	    int trySkipped = 0;
	    final int HOWMANYTRIES = 20;
	    int ibt = Parameters.batchInputNum;
	    int maxIbt = Parameters.batchInputNum;
	    double bestScore = 0;
	    int bestIbt = 0;
	    long bestDelayForScore = 0;
	    final int step = 10;
	    boolean congested = false;
	    boolean increasing = true;
	    int batched = 0;
	    long begin = 0, finish = 0;
	    double lastScore = 0;
	    int count = 0;
	    final int MaxSteps = 1000; //. Maximum IBT is MaxSteps*step
	    final int HistoryWeight = 80; //. History value weighs 80%
	    final long MaxDelay = Parameters.maxDelay; //. MicroSecond
	    double[] history = new double[MaxSteps];
	    long lastRound = System.nanoTime();
	    long lastAssign = lastRound;
	    LinkedList<Switch> skipped = new LinkedList<Switch>();
	    
	    PrintWriter ibtlog = null;
	    try {
		ibtlog = new PrintWriter(new File("ibtlog.txt"));
	    } catch (FileNotFoundException e) {
		System.err.println("Not found!!!!!!!!");
	    }
	    PrintWriter delaylog = null;
	    try {
		delaylog = new PrintWriter(new File("delaylog.txt"));
	    } catch (FileNotFoundException e) {
		System.err.println("Not found!!!!!!!!");
	    }
	    long lastPrint = 0;
	    int timeStamp = 1;
	    boolean stepOne = true;
	    boolean stepTwo = true;
	    boolean stepThree = true;
	    boolean stepFour = true;

	    boolean rePartition = true;
	    /*
	    LinkedList<ArrayList<RawMessage>> myQ = null;
	    if (Parameters.mode == 3) {
		myQ = of.msgsQs.getQAt(workerID);
	    }
	    */
	    Selector readSelector = readSelectors.get(workerID);
	    Iterator<SelectionKey> key = null;

	    long thetime = 0, before = 0, num = 0;
		
	    while (true) {
		Switch sw = null;
		if (Parameters.mode == 1) {
		    if (idx < of.getRRPool().getSize()) {
			sw = of.getRRPool().getSwitchAt(idx++);
		    }
		    if (null == sw) {
			if (skipped.size() == 0 || trySkipped >= HOWMANYTRIES) {
			    //skipped.clear();
			    idx = 0;
			    trySkipped = 0;
			    /*
			    long now = System.nanoTime();
			    if (Parameters.warmuped) {
				thetime += now - before;
				num ++;

				if (num % 10000 == 0) {
				    System.err.println(thetime / (num*1000));
				}
			    }
			    before = now;
			    */

			    
			    //if (of.whoSel == workerID) {
			    if (workerID == 0) {
				//if (!of.selecting) {
				//of.selecting = true;
				select();
				//of.selecting = false;
				//of.whoSel = (of.whoSel+1)%Parameters.divide;
			    }
			    

			    continue;
			} else {
			    sw = skipped.removeFirst();
			    trySkipped ++;
			}
		    }

		    if (!sw.active) {
			continue;
		    }
		    
		    /*
		    if (sw.zeroes > 1000) {
			if (of.random.nextInt(100) >= 10) {
			    continue;
			}
		    }
		    */

		    /*
		    try {
			if (key != null && key.hasNext()) {
			    SelectionKey k = key.next();
			    if (k.isReadable()) {
				sw = chnl2switch.get((SocketChannel)k.channel());
				if (sw == null) {
				    System.err.println("Not found");
				    continue;
				}
			    }
			} else if (skipped.size() > 0) {
			    sw = skipped.removeFirst();
			} else {
			    if (readSelector != null && readSelector.selectNow() > 0) {
				key = readSelector.selectedKeys().iterator();
			    }
			    else if (Parameters.useIBTAdaptation) {
				trySkipped = 0;
				
				of.flush();
				voidRead = 0;
				batched = 0;
				increasing = false;
				congested = false;
				
				int myStep = step << 1 + step >> 1;
				if (ibt <= myStep)
				    ibt = step;
				else
				    ibt -= myStep;
				
			    }
			    continue;
			}
		    } catch (IOException e) {
			
		    }
		    */

		    synchronized (sw) {
			if (sw.chopping) {
			    skipped.addLast(sw);
			    if (skipped.size() > 50) {
				skipped.clear();
			    }
			    continue;
			}
			else {
			    sw.chopping = true;
			    //sw.active = false;
			}
		    }
		} else if (Parameters.mode == 2) {
		    /* //. Not good select code for flushing
		    idx ++;
		    if (Parameters.useIBTAdaptation && idx > partition.getSize()) {
			idx = 0;
			try {
			    if (readSelector != null && readSelector.selectNow() <= 0) {
				of.flush();
				voidRead = 0;
				batched = 0;
				increasing = false;
				congested = false;
				
				int myStep = step << 1 + step >> 1;
				if (ibt <= myStep)
				    ibt = step;
				else
				    ibt -= myStep;
			    }
			} catch (IOException e) {
			    
			}
		    }
		    */

		    if (/*!Parameters.warmuped &&*/ workerID == 0) {
			long now = System.nanoTime();
			long howoften = 2000000000L;
			if (Parameters.warmuped)
			    howoften = 10000000000L;
			if ((now-lastAssign) > howoften) {
			    lastAssign = now;
			    of.reassignSwitches();
			}
		    }

		    
		    sw = partition.nextSwitch();
		    
		    if (null == sw) {
			continue;
		    }
		    /*
		    if (sw.zeroes > 1000) {
			if (of.random.nextInt(100) >= 50) {
			    continue;
			}
		    }
		    */
		    //synchronized (sw) {
			if (!sw.chopping)
			    sw.chopping = true;
			//}		    
		} else if (Parameters.mode == 4) {
		    sw = partition.nextSwitch();
		    
		    if (null == sw) {
			continue;
		    }
		    if (!sw.chopping)
			sw.chopping = true;
		}
		
		ArrayList<RawMessage> msgs = null;

		if(Parameters.mode == 1 || Parameters.mode == 2 || Parameters.mode == 4) {
		    try {
			buffer.clear();
			int size = sw.channel.read(buffer);

			/*
			if (Parameters.warmuped) {
			    sw.chances ++;
			    if (size == 0)
				sw.c0++;
			    else if (size <= 10)
				sw.c1++;
			    else if (size <= 100)
				sw.c10++;
			    else
				sw.c100++;
			}
			*/
			
			if (size == -1) {
			    sw.chopping = false;
			    handleLeftSwitch(sw);
			    continue;
			} else if (size == 0) {
			    sw.zeroes ++;
			    sw.chopping = false;

			    
			    // Whether flush the batch if there is no pending requests left
			    voidRead ++;
			    if (Parameters.useIBTAdaptation) {
				if (voidRead > (of.getRRPool().getSize()) && batched > 0) {
				    of.flush();
				    voidRead = 0;
				    batched = 0;
				    increasing = false;
				    congested = false;
				    
				    //int myStep = step << 1 + step >> 1;
				    int myStep = step;
				    if (ibt <= myStep)
					ibt = step;
				    else
					ibt -= myStep;
				}
			    }
			    
			    continue;
			} else if (size == Parameters.bufferSize) {
			    congested = true;
			}
			sw.zeroes = 0;
			
			msgs = of.chopMessages(sw, buffer, size);
			sw.chopping = false;
			
			sw.totalSize += size;
			voidRead = 0;
			sw.totalProcessed += msgs.size();
			Parameters.totalProcessed += msgs.size();
		    } catch (IOException e) {
			//e.printStackTrace();
			//of.print();
			handleLeftSwitch(sw);
		    }
		} else if(Parameters.mode == 3) {
		    
		    synchronized (of.msgsQueue) {
			while (of.msgsQueue.isEmpty()) {
		    

		    /*
		    synchronized (myQ) {
			while (myQ.isEmpty()) {
		    */
			    if (Parameters.useIBTAdaptation && batched > 0) {
				of.flush();
				batched = 0;
				increasing = false;
				congested = false;

				int myStep = step << 1 + step >> 1;
				if (ibt <= myStep)
				    ibt = step;
				else
				    ibt -= myStep;
			    }
			    try {
				of.msgsQueue.wait();
				//myQ.wait();
			    } catch (InterruptedException ignored) {
				
			    }
			}
			msgs = of.msgsQueue.removeFirst();
			//msgs = myQ.removeFirst();
		    }
		}

		for (RawMessage msg : msgs) {
		    if (batched >= ibt) {
			if (of.dispatchPacket(msg.sw, msg.buf, msg.start, msg.length, true)) {
			    long now = System.nanoTime();
			    long allTime = (now - begin) / 1000; //. Microsecond
			    double score = ((double)batched) / (allTime);
			    //double score = ((double)(5000-allTime)*batched) / (5000*allTime);
			    //double score = ((double)batched*10000) / (allTime) - ((double)allTime);
			    
			    double realScore = score;
			    //. Adding history into the evaluation
			    int hisIdx = ibt / step;
			    if (history[hisIdx] == 0) {
				history[hisIdx] = score;
			    } else {
				score = (score*(100-HistoryWeight) + history[hisIdx]*HistoryWeight) / 100;
				history[hisIdx] = score;
			    }

			    if (Parameters.dynamicExp && Parameters.warmuped && workerID == 0) {
				if ((now - lastPrint) > 10000000L) {
				    //System.err.println(ibt+" "+increasing+" "+score+" ("+realScore+") "+lastScore+" "+allTime+" || best "+bestScore+" at "+bestDelayForScore);
				    ibtlog.println((timeStamp++)*10+" "+ibt);
				    ibtlog.flush();
				}
				
				if (Parameters.whenWarmuped != 0) {
				    if (stepOne && (now - Parameters.whenWarmuped) > 10000000000L) {
					Parameters.bufferId = 4;
					ibtlog.println("Now starting rate 4");
					System.err.println("Now starting rate 4");
					stepOne = false;
				    }

				    if (stepTwo && (now - Parameters.whenWarmuped) > 20000000000L) {
					Parameters.bufferId = 8;
					ibtlog.println("Now starting rate 8");
					System.err.println("Now starting rate 8");
					stepTwo = false;
				    }

				    if (stepThree && (now - Parameters.whenWarmuped) > 30000000000L) {
					Parameters.bufferId = 4;
					ibtlog.println("Now starting rate 4");
					System.err.println("Now starting rate 4");
					stepThree = false;
				    }

				    if (stepFour && (now - Parameters.whenWarmuped) > 40000000000L) {
					Parameters.bufferId = 0;
					ibtlog.println("Now starting rate 0");
					System.err.println("Now starting rate 0");
					stepFour = false;
				    }
				}
			    }

			    
			    if (Parameters.warmuped && workerID == 0) {
				if ((now - lastPrint) > 10000000L) {
				    lastPrint = now;
				    delaylog.println(allTime);
				    delaylog.flush();
				}
			    }
			    
			    
			    if (Parameters.useIBTAdaptation) {
				/*
				if (score > bestScore) {
				    bestScore = score;
				    bestIbt = ibt;
				    bestDelayForScore = allTime;
				}
				*/
				
				if (allTime > MaxDelay || !congested
				    /*||(allTime > (bestDelayForScore<<1) && ibt > bestIbt)*/) {
				    increasing = false;
				    if (ibt <= step)
					ibt = step;
				    else
					ibt -= step;
				    
				} else {
				    if (score > lastScore) { //. Better
					
				    } else { //. Worse
					increasing = !increasing;				    
				    }
				    if (increasing && congested) {
					ibt += step;
					/*
					if (ibt > maxIbt)
					    maxIbt = ibt;
					*/
				    } else {
					if (ibt <= step)
					    ibt = step;
					else
					    ibt -= step;
				    }
				}
			    }

			    if (Parameters.mode == 4) {
				ibt = 750;
			    }
			    
			    
			    lastScore = score;
			    congested = false;
			    batched = 0;
			    begin = System.nanoTime();
			}
		    } else {
			batched += of.dispatchPacket(msg.sw, msg.buf, msg.start, msg.length, false) ? 1 : 0;
			/*
			if (batched == 1) {
			    begin = System.nanoTime();
			}
			*/
		    }
		} //. End of for loop

		/*
		if (Parameters.totalProcessed > 40000000 && workerID == 0) {
		    //of.print();
		    Parameters.am.dataLogMgr.dumpLogs();
		    Utilities.ForceExit(0);
		}
	        */
	    } //. End of while loop
	} //. End of run1()

	public void handleLeftSwitch(Switch sw) {
	    try {
		sw.channel.close();
		//Utilities.printlnDebug("Switch "+sw.dpid+" has left the network");
	    } catch (IOException e) {
		
	    }
	    of.getRRPool().removeSwitch(sw);
	    if (partition != null)
		partition.removeSwitch(sw);
	    //. TODO: write logs in memory to disk here
	    //Parameters.am.dataLogMgr.dumpLogs();

	    //. TODO: Generate a switch leave event
	}

	private SwitchRRPool partition = null;
    }

    public void reassignSwitches() {
	class SwitchLoad implements Comparable<SwitchLoad> {
	    public long load;
	    public Switch sw;
	    
	    public SwitchLoad(long l, Switch s) {
		load = l;
		sw = s;
	    }

	    public int compareTo(SwitchLoad other) {
		if (load > other.load)
		    return 1;
		else if (load < other.load)
		    return -1;
		else
		    return 0;
	    }
	}
	
	ArrayList<SwitchLoad> loads = new ArrayList<SwitchLoad>();
	synchronized(swRRPool.pool) {
	    for(Switch sw : swRRPool.pool) {
		loads.add(new SwitchLoad(sw.totalProcessed-sw.lastProcessed, sw));
		sw.lastProcessed = sw.totalProcessed;
	    }
	}

	Collections.sort(loads);

	class LoadAssignment implements Comparable<LoadAssignment> {
	    public long assigned = 0;
	    public OpenFlowTask worker = null;
	    public ArrayList<Switch> toAdd = null;

	    public LoadAssignment(OpenFlowTask w) {
		worker = w;
		toAdd = new ArrayList<Switch>();
	    }

	    public int compareTo(LoadAssignment other) {
		if (assigned > other.assigned)
		    return 1;
		else if (assigned < other.assigned)
		    return -1;
		else
		    return 0;
	    }

	    public void addLoad(SwitchLoad load) {
		assigned += load.load;
		toAdd.add(load.sw);
	    }
	}
	
	PriorityQueue<LoadAssignment> assignment = new PriorityQueue<LoadAssignment>();
	synchronized(workers) {
	    for(OpenFlowTask w : workers) {
		assignment.add(new LoadAssignment(w));
	    }
	}

	int n = loads.size();
	for(int i=0;i<n;i++) {
	    SwitchLoad sl = loads.get(n-i-1);
	    LoadAssignment a = assignment.poll();
	    a.addLoad(sl);
	    assignment.add(a);
	}

	for(LoadAssignment a : assignment) {
	    a.worker.partition.clearAndAdd(a.toAdd);
	}	
    }
    
    public Runnable newTask() {
	OpenFlowTask ret = new OpenFlowTask(this);
	synchronized(workers) {
	    workers.add(ret);
	}
	return ret;
    }

    public void prepareDriverPage(ByteBuffer buffer) {
	buffer.put(String.format("DRIVER\n").getBytes());
	buffer.put(String.format("SystemTime %d\n", System.nanoTime()).getBytes());
	buffer.put(String.format("TotalSwitches %d\n", dpid2switch.size()).getBytes());
	synchronized(dpid2switch) {
	    for (Switch sw : dpid2switch.values()) {
		buffer.put(String.format("Switch %d Processed %d\n", sw.dpid, sw.totalProcessed).getBytes());
	    }
	}
	buffer.flip();
    }

    public String getCounters() {
	String ret = "";
	synchronized(dpid2switch) {
	    for (Switch sw : dpid2switch.values()) {
		ret += String.format(" %d %d", sw.dpid, sw.totalProcessed);
	    }
	}
	return ret;
    }
}