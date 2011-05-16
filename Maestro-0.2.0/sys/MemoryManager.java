/*
  MemoryManager.java

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

import java.util.*;
import java.nio.ByteBuffer;

import events.openflow.*;
import headers.*;

/**
 * Manually manage heap memory allocation, instead of using Java's GC
 * @author Zheng Cai
 *
 * TODO: Right now the implementation is very static and non-flexible
 * Need a re-design and re-implementation
 */
public class MemoryManager {
    /*
    public class MemoryPool<E> {
	private ArrayList<Stack<E>> dedicated;
	private Stack<E> shared;

	public MemoryPool() {
	    dedicated = new ArrayList<Stack<E>>();
	    for (int i=0;i<Parameters.divide;i++) {
		dedicated.add(new Stack<E>());
	    }
	    shared = new Stack<E>();
	}

	public E pop(int which) {
	    Stack<E> s;
	    if (which == -1)
		s = shared;
	    else
		s = dedicated.get(which);
	    if (s.size() > 0)
		return s.pop();
	    else
		return null;
	}

	public void push(E object, int which) {
	    if (which == -1) {
		shared.push(object);
	    } else {
		dedicated.get(which).push(object);
	    }
	}
    }
    */

    public class MemoryPool<E> {
	private ArrayList<ArrayList<E>> dedicated;
	private ArrayList<E> shared;

	public MemoryPool() {
	    dedicated = new ArrayList<ArrayList<E>>();
	    for (int i=0;i<Parameters.divide;i++) {
		dedicated.add(new ArrayList<E>());
	    }
	    shared = new ArrayList<E>();
	}

	public E pop(int which) {
	    ArrayList<E> s;
	    if (which == -1)
		s = shared;
	    else
		s = dedicated.get(which);
	    if (s.size() > 0)
		return s.remove(s.size()-1);
	    else
		return null;
	}

	public void push(E object, int which) {
	    if (which == -1) {
		shared.add(object);
	    } else {
		dedicated.get(which).add(object);
	    }
	}
    }


    private MemoryPool<FlowModEvent> fms;
    private MemoryPool<PacketOutEvent> pos;
    private MemoryPool<PacketInEvent> pis;
    private MemoryPool<PacketInEvent.DataPayload> datas;
    private MemoryPool<ByteBuffer> buffers;
    private MemoryPool<DAGRuntime> drs;
    private MemoryPool<EthernetHeader> eths;
    private MemoryPool<IPV4Header> ipv4s;
    private MemoryPool<TCPHeader> tcps;
    private MemoryPool<UDPHeader> udps;
    
    
    private static final int DATA_SIZE = 1024;
    private static final int BUFFER_SIZE = 1024 * 128;
	
    public MemoryManager() {
	fms = new MemoryPool<FlowModEvent>();
	pos = new MemoryPool<PacketOutEvent>();
	pis = new MemoryPool<PacketInEvent>();
	datas = new MemoryPool<PacketInEvent.DataPayload>();
	buffers = new MemoryPool<ByteBuffer>();
	drs = new MemoryPool<DAGRuntime>();
	eths = new MemoryPool<EthernetHeader>();
	ipv4s = new MemoryPool<IPV4Header>();
	tcps = new MemoryPool<TCPHeader>();
	udps = new MemoryPool<UDPHeader>();
    }
	
    public FlowModEvent allocFlowModEvent() {
	int which = Parameters.am.workerMgr.getCurrentWorkerID();
	FlowModEvent ret = fms.pop(which);
	if (ret == null) {
	    Parameters.newCountfm ++;
	    ret = new FlowModEvent();
	    ret.actions = new PacketOutEvent.Action[1];
	    ret.actions[0] = new PacketOutEvent.Action();
	}
	return ret;
    }
	
    public void freeFlowModEvent(FlowModEvent fm) {
	int which = Parameters.am.workerMgr.getCurrentWorkerID();
	fms.push(fm, which);
    }
	
    public PacketOutEvent allocPacketOutEvent() {
	int which = Parameters.am.workerMgr.getCurrentWorkerID();
	PacketOutEvent ret = pos.pop(which);
	if (ret == null) {
	    Parameters.newCountpo ++;
	    //. TODO: right now there is only one action supported by the memory manager
	    ret = new PacketOutEvent();
	    ret.actions = new PacketOutEvent.Action[1];
	    ret.actions[0] = new PacketOutEvent.Action();
	}
	return ret;
    }
	
    public void freePacketOutEvent(PacketOutEvent po) {
	int which = Parameters.am.workerMgr.getCurrentWorkerID();
	pos.push(po, which);
    }
	
    public PacketInEvent allocPacketInEvent() {
	int which = Parameters.am.workerMgr.getCurrentWorkerID();
	PacketInEvent ret = pis.pop(which);
	if (ret == null) {
	    Parameters.newCountpi ++;
	    ret = new PacketInEvent();
	}
	return ret;
    }
	
    public void freePacketInEvent(PacketInEvent pi) {
	int which = Parameters.am.workerMgr.getCurrentWorkerID();
	pis.push(pi, which);
    }
	
    public PacketInEvent.DataPayload allocPacketInEventDataPayload(int size) {
	// TODO: right now the size is kinda ignored, all sizes are DATA_SIZE bytes in the pool
	int which = Parameters.am.workerMgr.getCurrentWorkerID();
	PacketInEvent.DataPayload ret = datas.pop(which);
	if (ret == null) {
	    Parameters.newCountdata ++;
	    ret = new PacketInEvent.DataPayload(DATA_SIZE);
	}
	return ret;
    }
	
    public void freePacketInEventDataPayload(PacketInEvent.DataPayload data) {
	int which = Parameters.am.workerMgr.getCurrentWorkerID();
	datas.push(data, which);
    }

    public ByteBuffer allocByteBuffer(int size) {
	// TODO: right now the size is kinda ignored, all sizes are BUFFER_SIZE bytes in the pool
	int which = Parameters.am.workerMgr.getCurrentWorkerID();
	ByteBuffer ret = buffers.pop(which);
	if (ret == null) {
	    Parameters.newCountbuffer ++;
	    ret = ByteBuffer.allocate(BUFFER_SIZE);
	} else {
	    if (ret.capacity() < size) {
		buffers.push(ret, which);
		Parameters.newCountbuffer ++;
		ret = ByteBuffer.allocate(size);
	    }
	}
	return ret;
    }
	
    public void freeByteBuffer(ByteBuffer buffer) {
	buffer.clear();
	int which = Parameters.am.workerMgr.getCurrentWorkerID();
	buffers.push(buffer, which);
    }

    public DAGRuntime allocDAGRuntime(DAG d, Environment theEnv, int instance, ApplicationManager a) {
	int which = Parameters.am.workerMgr.getCurrentWorkerID();
	DAGRuntime ret = drs.pop(which);
	if (ret == null) {
	    Parameters.newCountdr ++;
	    ret = new DAGRuntime(d, theEnv, instance, a);
	} else {
	    ret.init(d, theEnv, instance, a);
	}
	return ret;
    }

    public void freeDAGRuntime(DAGRuntime dr) {
	int which = Parameters.am.workerMgr.getCurrentWorkerID();
	drs.push(dr, which);
    }

    public EthernetHeader allocEthernetHeader() {
	int which = Parameters.am.workerMgr.getCurrentWorkerID();
	EthernetHeader ret = eths.pop(which);
	if (ret == null) {
	    ret = new EthernetHeader();
	}
	return ret;
    }

    public void freeEthernetHeader(EthernetHeader eth) {
	int which = Parameters.am.workerMgr.getCurrentWorkerID();
	eths.push(eth, which);
    }

    public IPV4Header allocIPV4Header() {
	int which = Parameters.am.workerMgr.getCurrentWorkerID();
	IPV4Header ret = ipv4s.pop(which);
	if (ret == null) {
	    ret = new IPV4Header();
	}
	return ret;
    }

    public void freeIPV4Header(IPV4Header ipv4) {
	int which = Parameters.am.workerMgr.getCurrentWorkerID();
	ipv4s.push(ipv4, which);
    }

    public TCPHeader allocTCPHeader() {
	int which = Parameters.am.workerMgr.getCurrentWorkerID();
	TCPHeader ret = tcps.pop(which);
	if (ret == null) {
	    ret = new TCPHeader();
	}
	return ret;
    }

    public void freeTCPHeader(TCPHeader tcp) {
	int which = Parameters.am.workerMgr.getCurrentWorkerID();
	tcps.push(tcp, which);
    }

    public UDPHeader allocUDPHeader() {
	int which = Parameters.am.workerMgr.getCurrentWorkerID();
	UDPHeader ret = udps.pop(which);
	if (ret == null) {
	    ret = new UDPHeader();
	}
	return ret;
    }

    public void freeUDPHeader(UDPHeader udp) {
	int which = Parameters.am.workerMgr.getCurrentWorkerID();
	udps.push(udp, which);
    }
}
