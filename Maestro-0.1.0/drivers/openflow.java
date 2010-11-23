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

package drivers;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

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
import drivers.OFPConstants;
import headers.EthernetHeader;

public class openflow extends Driver {
    Random random;
    public class MyInteger {
	int value;
		
	public MyInteger(int v) {
	    value = v;
	}
    }
    public MyInteger pendingTasks = new MyInteger(0);
    public void evaluateAndResume() {
	synchronized(pendingTasks) {
	    pendingTasks.value --;

	    //. Otherwise the suspend created by the ApplicatinonManager will be suppressed by this resmue
	    if (pendingTasks.value <= Parameters.queueUpperBound /*&& Parameters.am.running.size() < Parameters.maxWaitingDAGIns*/) {
		resume();
	    }
	    /*
	      if (0 == pendingTasks.value) {
	      Parameters.pipeDrained ++;
	      }
	    */
	}
    }
	
    private static class Switch {
    	public class WorkerThread implements Runnable {
	    openflow of;
	    Switch sw;
	    ByteBuffer buffer;
	    int size;
	    public WorkerThread(openflow o, Switch s, ByteBuffer b, int si) {
		of = o;
		sw = s;
		buffer = b;
		size = si;
	    }
	    public void run() {
		of.handleMessage(sw, sw.channel, buffer, size);
		of.evaluateAndResume();
		synchronized(sw.workQueue) {
		    synchronized(sw.running) {
			if (sw.workQueue.size() == 0) {
			    sw.running.value = false;
			} else {
			    WorkerThread r = sw.workQueue.removeFirst();
			    Parameters.am.enqueueTask(r, Constants.PRIORITY_LOW);
			}
		    }
		}
	    }
    	}
    	
	public long dpid = 0;
	public SocketChannel channel = null;
	public int bufferSize = 0;
	public byte[] buffer = new byte[BUFFERSIZE];
	public LinkedList<WorkerThread> workQueue;
		
	class MyBoolean {
	    public boolean value;
			
	    public MyBoolean(boolean v) {
		value = v;
	    }
	}
	private MyBoolean running = new MyBoolean(false);
		
	/** For those lldps received before the dpid of this switch is known */
	private LinkedList<LLDPPacketInEvent> lldpQueue;
		
	public Switch() {
	    workQueue = new LinkedList<WorkerThread>();
	    lldpQueue = new LinkedList<LLDPPacketInEvent>();
	}
		
	public void enqueueTask(openflow o, ByteBuffer b, int si) {
	    synchronized(workQueue) {
		synchronized(running) {
		    if (workQueue.size() == 0 && !running.value) {
			running.value = true;
			Parameters.am.enqueueTask(new WorkerThread(o, this, b, si), Constants.PRIORITY_LOW);
		    } else {
			workQueue.addLast(new WorkerThread(o, this, b, si));
		    }
		}
	    }
	}

	synchronized public int send(ByteBuffer pkt) {
	    int ret = 0;
	    try {
		int remain = pkt.capacity();
		byte[] buf = pkt.array();
		int pos = 0;
		while ((ret = channel.write(pkt)) < remain) {
		    pos += ret;
		    pkt = ByteBuffer.wrap(buf, pos, remain-ret);
		    remain -= ret;
		}
	    } catch (Exception e) {
		//e.printStackTrace();
	    }
	    return ret;
	}
    }
	
    private final static int BUFFERSIZE = 4096;
	
    private HashMap<Long, Switch> dpid2switch;
    private HashMap<SocketChannel, Switch> chnl2switch;
    private Selector s;
	
    public int SendPktOut(long dpid, ByteBuffer pkt, int length) {
	long before = 0;
	if (Parameters.measurePerf) {
	    before = System.nanoTime();
	}
	Switch target;
	synchronized(dpid2switch) {
	    target = dpid2switch.get(dpid);
	}
	Utilities.Assert(target != null, "Cannot find target switch for dpid "+dpid);
	int ret = target.send(pkt);
	if (Parameters.measurePerf) {
	    Parameters.t1 += System.nanoTime() - before;
	}
	return ret;
    }
    
    int pkts = 1;
    long cycles = 0;
    public openflow() {
    	random = new Random();
    	dpid2switch = new HashMap<Long, Switch>();
    	chnl2switch = new HashMap<SocketChannel, Switch>();
    }
    
    public void start() {
    	try {
	    int port = 2525;
	    s = Selector.open();
	    ServerSocketChannel acceptChannel = ServerSocketChannel.open();
	    acceptChannel.configureBlocking(false);
	    byte[] ip = {0, 0, 0, 0};
	    InetAddress lh = InetAddress.getByAddress(ip);
	    InetSocketAddress isa = new InetSocketAddress(lh, port);
	    acceptChannel.socket().bind(isa);
	    acceptChannel.socket().setReuseAddress(true);
			
	    SelectionKey acceptKey = acceptChannel.register(s, SelectionKey.OP_ACCEPT);
	    while (s.select() > 0) {
		Set<SelectionKey> readyKeys = s.selectedKeys();
		for (SelectionKey k : readyKeys) {
		    try {
			if (k.isAcceptable()) {
			    SocketChannel channel = ((ServerSocketChannel)k.channel()).accept();
			    channel.configureBlocking(false);
			    SelectionKey clientKey = channel.register(s, SelectionKey.OP_READ);
			    Switch sw = new Switch();
			    sw.channel = channel;
			    chnl2switch.put(channel, sw);
			    sendHelloMessage(sw);
			} else if (k.isReadable()) {
			    Switch sw = chnl2switch.get((SocketChannel)k.channel());
			    Utilities.Assert(sw.channel == k.channel(), "Channels do not match!");
			    ByteBuffer buffer = ByteBuffer.allocate(BUFFERSIZE);
			    int size = sw.channel.read(buffer);
			    if (size == -1) {
				sw.channel.close();
				return;
			    } else if (size == 0) {
				return;
			    }

			    if (Parameters.divide == 0) {
				handleMessage(sw, sw.channel, buffer, size);
			    } else {
				if (pendingTasks.value > Parameters.queueUpperBound) {
				    suspend();
				}
							
				whetherContinue();
				    			
				sw.enqueueTask(this, buffer, size);
				synchronized(pendingTasks) {
				    pendingTasks.value ++;
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
	}
    }

    /** Handle one raw message */
    public void handleMessage(Switch sw, SocketChannel channel, ByteBuffer buffer, int size) {
	byte[] buf = buffer.array();
	int bufPos = 0;
	if (sw.bufferSize >= OFPConstants.OfpConstants.OFP_HEADER_LEN) {
	    int length = Utilities.getNetworkBytesUint16(sw.buffer, 2);
	    
	    if ((length - sw.bufferSize) <= size) {
		Utilities.memcpy(sw.buffer, sw.bufferSize, buf, bufPos, length-sw.bufferSize);                 
		size -= (length-sw.bufferSize);                                              
		bufPos += (length-sw.bufferSize);                                               
		dispatchPacket(sw, sw.buffer, 0, length);                                 
		sw.bufferSize = 0;                                                           
	    } else {                                                                        
		Utilities.memcpy(sw.buffer, sw.bufferSize, buf, bufPos, size);                                  
		sw.bufferSize += size;
		return;
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
		    Utilities.memcpy(sw.buffer, sw.bufferSize, buf, bufPos, length-sw.bufferSize);               
		    size -= (length-sw.bufferSize);                                            
		    bufPos += (length-sw.bufferSize);                                             
		    dispatchPacket(sw, sw.buffer, 0, length);                               
		    sw.bufferSize = 0;
		} else {                                                                      
		    Utilities.memcpy(sw.buffer, sw.bufferSize, buf, bufPos, size);                                
		    sw.bufferSize += size;
		    return;                                                                     
		}                                                                             
	    } else {                                                                        
		//. Still not enough for holding ofp_header                                    
		Utilities.memcpy(sw.buffer, sw.bufferSize, buf, bufPos, size);                                  
		sw.bufferSize += size;
		return;                                                                       
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
	    dispatchPacket(sw, buf, bufPos, length);
	    size -= length;                                                                 
	    bufPos += length;                                                                  
	}
    }
    
    public void dispatchPacket(Switch sw, byte[] buffer, int pos, int size) {
    	
    	short type = Utilities.getNetworkBytesUint8(buffer, pos+1);
	int length = Utilities.getNetworkBytesUint16(buffer, pos+2);
	switch(type) {
	case OFPConstants.PacketTypes.OFPT_HELLO:
	    //System.err.println("Switch "+sw.dpid+" received hello");
	    sendFeatureRequest(sw);
	    //System.err.println("Switch "+sw.dpid+" sent feature request");
	    break;
	case OFPConstants.PacketTypes.OFPT_ECHO_REQUEST:
	    Utilities.setNetworkBytesUint8(buffer, pos+1, OFPConstants.PacketTypes.OFPT_ECHO_REPLY);	    
	    ByteBuffer buf = ByteBuffer.allocate(length);
	    if (size != length) {
		System.err.println("BAD! In handling echo_request: size != length");
	    } else {
		buf.put(buffer, pos, length);
	    }
	    sw.send(buf);
	    //System.err.println("Sending back an echo_reply with size = "+length);
	    break;
	case OFPConstants.PacketTypes.OFPT_FEATURES_REPLY:
	    //System.err.println(Thread.currentThread().getId()+" -> Switch "+sw.dpid+" received feature reply");
	    handleFeaturesReply(sw, buffer, pos, length);
	    break;
	case OFPConstants.PacketTypes.OFPT_PACKET_IN:
	    handlePacketIn(sw, buffer, pos, length);
	    break;
	default:
	    break;
	}
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

    public void handlePacketIn(Switch sw, byte[] buffer, int pos, int length) {
	PacketInEvent pi;
	if (Parameters.useMemoryMgnt) {
	    pi = Parameters.am.memMgr.allocPacketInEvent();
	} else {
	    pi = new PacketInEvent();
	}
    	
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
	EthernetHeader eth = new EthernetHeader();
	pos = eth.parseHeader(buffer, pos);
	pi.extractFlowInfo(eth);

	if (OFPConstants.OfpConstants.ETH_TYPE_LLDP == pi.flow.dlType) {
	    LLDPPacketInEvent lldp = new LLDPPacketInEvent();
	    lldp.srcDpid = Utilities.GetLongFromBytesInt(pi.data.data,
							 OFPConstants.OfpConstants.OFP_ETH_ALEN*2 + 2);
	    lldp.srcPort = Utilities.GetIntFromBytesInt(pi.data.data,
							OFPConstants.OfpConstants.OFP_ETH_ALEN*2 + 2 + Long.SIZE/8);
	    lldp.dstDpid =pi.dpid;
	    lldp.dstPort = pi.inPort;
	    if (sw.dpid == 0) {
		synchronized(sw.lldpQueue) {
		    sw.lldpQueue.addLast(lldp);
		}
	    } else {
		vm.postEvent(lldp);
	    }
	} else {
	    if (Parameters.divide > 0) {
		int toWhich = Parameters.am.taskMgr.getCurrentWorkerID() + 1;
		vm.postEventToSpecificView(pi, "packets_in_"+toWhich);
	    } else {
		vm.postEventToSpecificView(pi, "packets_in_1");
	    }
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
    
    @Override
	public boolean commitEvent(LinkedList<Event> events) {
	if (events.size() == 0) {
	    return false;
	}
	Event e = events.getFirst();
	if (e == null) {
	    return false;
	}
	if (e instanceof PacketOutEvent) {
	    boolean ret = processToSpecificSwitchEvent(events);

	    //. Performance measuring code
	    if (Parameters.count.value >= Parameters.countDone) {
		long theCount = Parameters.count.value;
		Parameters.count.value = (long)0;			    
		if (Parameters.warmuped) {
		    long time = System.nanoTime() - Parameters.before;
		    Utilities.Log().println(1000000000*theCount/time);
		    //Utilities.Log().println(Parameters.pipeDrained);
		    //Utilities.Log().println(Parameters.waiting+" "+Parameters.running);
		    Utilities.Log().flush();
		} else {
		    Parameters.warmuped = true;
		}
		Parameters.blocked.value = (long)0;
		Parameters.ran.value = (long)0;
		Parameters.before = (long)0;
		Parameters.pipeDrained = 0;
		Parameters.waiting = (long)0;
		Parameters.running = (long)0;
	    }
	    //. End of performance measuring code
	    
	    return ret;
	}
	if (e instanceof FlowModEvent) {
	    return processToSpecificSwitchEvent(events);
	}
	return true;
    }

    class Partition {
	public LinkedList<Event> es = new LinkedList<Event>();
	public int totalLength = 0;
	public long dpid;
		
	public ByteBuffer toPacket() {
	    ByteBuffer pkt = ByteBuffer.allocate(totalLength);
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
	    return pkt;
    	}
    }
	
    /* For better paralizability of memory management,
     * we do not do partition consolidation
     */
    /*
      HashMap<Long, Partition> pendingPts = new HashMap<Long, Partition>();
    */
	
    private boolean processToSpecificSwitchEvent(LinkedList<Event> events) {

	class WorkerThread implements Runnable {
	    openflow of;
	    Partition pt = null;
	    public WorkerThread(openflow o) {
		of = o;
	    }
	    public void run() {
		long before = 0;
		if (Parameters.measurePerf) {
		    before = System.nanoTime();
		}
	    		
		/* For better paralizability of memory management,
		 * we do not do partition consolidation
		 */
		/*
		// To avoid deadlock
		synchronized(of.pendingPts) {
		of.pendingPts.remove(pt.dpid);
		}
		*/
	    		
		if (Parameters.measurePerf) {
		    Parameters.c3 += pt.es.size();
		}
		synchronized(pt) {
		    if (Parameters.batchOutput) {
			ByteBuffer pkt = pt.toPacket();
			of.SendPktOut(pt.dpid, pkt, pkt.array().length);
		    } else {
			for (Event e : pt.es) {
			    ToSpecificSwitchEvent tsse = (ToSpecificSwitchEvent)e;
			    ByteBuffer pkt = ByteBuffer.allocate(tsse.getLength());
			    tsse.convertToBytes(pkt.array(), 0);
			    of.SendPktOut(tsse.dpid, pkt, pkt.array().length);
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
		    pt.es.clear();
		}
		if (Parameters.measurePerf) {
		    Parameters.t2 += System.nanoTime() - before;
		}
	    }
	}

	long before = 0;
	if (Parameters.measurePerf) {
	    before = System.nanoTime();
	}
		
	// Assume that the dpids in "events" are the same
	long dpid = ((ToSpecificSwitchEvent)events.getFirst()).dpid;
	Partition pt = new Partition();
	pt.dpid = dpid;
	for (Event e : events) {
	    ToSpecificSwitchEvent tsse = (ToSpecificSwitchEvent)e;
	    pt.totalLength += tsse.getLength();
	}
	pt.es = events;
		
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
	  // We can consolidate this partition to an existing one
	  toRun = false;
	  pt.es.clear();
	  }
	  }
	*/
	if (toRun) {
	    WorkerThread worker = new WorkerThread(this);
	    worker.pt = pt;
	    /* For better paralizability of memory management,
	     * we do not do partition consolidation
	     */
	    /*
	      synchronized(pendingPts) {
	      pendingPts.put(pt.dpid, pt);
	      }
	    */
	    Parameters.am.enqueueBindingTask(worker, Constants.PRIORITY_HIGH);
	}

	if (Parameters.measurePerf) {
	    Parameters.t3 += System.nanoTime() - before;
	}
	return true;
    }
	
    public void print() {
	
    }    
}