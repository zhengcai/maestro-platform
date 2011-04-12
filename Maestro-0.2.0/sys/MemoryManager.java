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

/**
 * Manually manage heap memory allocation, instead of using Java's GC
 * @author Zheng Cai
 *
 * TODO: Right now the implementation is very static and non-flexible
 * Need a re-design and re-implementation
 */
public class MemoryManager {
    public class MemoryPool {
	public ArrayList<Stack<FlowModEvent>> fm;
	public ArrayList<Stack<PacketOutEvent>> po;
	public ArrayList<Stack<PacketInEvent>> pi;
	public ArrayList<Stack<PacketInEvent.DataPayload>> data;
	public ArrayList<Stack<ByteBuffer>> buffer;

	public Stack<FlowModEvent> sfm;
	public Stack<PacketOutEvent> spo;
	public Stack<PacketInEvent> spi;
	public Stack<PacketInEvent.DataPayload> sdata;
	public Stack<ByteBuffer> sbuffer;
		
	public MemoryPool() {
	    fm = new ArrayList<Stack<FlowModEvent>>();
	    po = new ArrayList<Stack<PacketOutEvent>>();
	    pi = new ArrayList<Stack<PacketInEvent>>();
	    data = new ArrayList<Stack<PacketInEvent.DataPayload>>();
	    buffer = new ArrayList<Stack<ByteBuffer>>();
	    
	    for (int i=0;i<Parameters.divide;i++) {
		fm.add(new Stack<FlowModEvent>());
		po.add(new Stack<PacketOutEvent>());
		pi.add(new Stack<PacketInEvent>());
		data.add(new Stack<PacketInEvent.DataPayload>());
		buffer.add(new Stack<ByteBuffer>());
	    }

	    sfm = new Stack<FlowModEvent>();
	    spo = new Stack<PacketOutEvent>();
	    spi = new Stack<PacketInEvent>();
	    sdata = new Stack<PacketInEvent.DataPayload>();
	    sbuffer = new Stack<ByteBuffer>();
	}
    }

    public static final int DATA_SIZE = 1024;
    public static final int BUFFER_SIZE = 1024 * 128;
	
    public MemoryPool pool;
	
    public MemoryManager() {
	pool = new MemoryPool();
    }
	
    public FlowModEvent allocFlowModEvent() {
	int which = Parameters.am.workerMgr.getCurrentWorkerID();
	Stack<FlowModEvent> s;
	if (which == -1)
	    s = pool.sfm;
	else
	    s = pool.fm.get(which);
	if (s.size() > 0)
	    return s.pop();
	else {
	    Parameters.newCountfm ++;
	    FlowModEvent ret = new FlowModEvent();
	    ret.actions = new PacketOutEvent.Action[1];
	    ret.actions[0] = new PacketOutEvent.Action();
	    return ret;
	}
    }
	
    public void freeFlowModEvent(FlowModEvent fm) {
	int which = Parameters.am.workerMgr.getCurrentWorkerID();
	if (which == -1) {
	    pool.sfm.push(fm);
	} else {
	    pool.fm.get(which).push(fm);
	}
    }
	
    public PacketOutEvent allocPacketOutEvent() {
	int which = Parameters.am.workerMgr.getCurrentWorkerID();
	Stack<PacketOutEvent> s;
	if (which == -1)
	    s = pool.spo;
	else
	    s = pool.po.get(which);
	if (s.size() > 0)
	    return s.pop();
	else {
	    Parameters.newCountpo ++;
	    //. TODO: right now there is only one action supported by the memory manager
	    PacketOutEvent ret = new PacketOutEvent();
	    ret.actions = new PacketOutEvent.Action[1];
	    ret.actions[0] = new PacketOutEvent.Action();
	    return ret;
	}
    }
	
    public void freePacketOutEvent(PacketOutEvent po) {
	int which = Parameters.am.workerMgr.getCurrentWorkerID();
	if (which == -1) {
	    pool.spo.push(po);
	} else {
	    pool.po.get(which).push(po);
	}
    }
	
    public PacketInEvent allocPacketInEvent() {
	int which = Parameters.am.workerMgr.getCurrentWorkerID();
	Stack<PacketInEvent> s;
	if (which == -1)
	    s = pool.spi;
	else
	    s = pool.pi.get(which);
	if (s.size() > 0)
	    return s.pop();
	else {
	    Parameters.newCountpi ++;
	    return new PacketInEvent();
	}
    }
	
    public void freePacketInEvent(PacketInEvent pi) {
	int which = Parameters.am.workerMgr.getCurrentWorkerID();
	if (which == -1) {
	    pool.spi.push(pi);
	} else {
	    pool.pi.get(which).push(pi);
	}
    }
	
    public PacketInEvent.DataPayload allocPacketInEventDataPayload(int size) {
	// TODO: right now the size is kinda ignored, all sizes are DATA_SIZE bytes in the pool
	int which = Parameters.am.workerMgr.getCurrentWorkerID();
	Stack<PacketInEvent.DataPayload> s;
	if (which == -1)
	    s = pool.sdata;
	else
	    s = pool.data.get(which);
	if (s.size() > 0) {
	    PacketInEvent.DataPayload ret = s.pop();
	    /*
	    if (ret.data.length < size) {
		return new PacketInEvent.DataPayload(DATA_SIZE);
	    }
	    */
	    return ret;
	}
	else {
	    Parameters.newCountdata ++;
	    return new PacketInEvent.DataPayload(DATA_SIZE);
	}
    }
	
    public void freePacketInEventDataPayload(PacketInEvent.DataPayload data) {
	int which = Parameters.am.workerMgr.getCurrentWorkerID();
	if (which == -1) {
	    pool.sdata.push(data);
	} else {
	    pool.data.get(which).push(data);
	}
    }

    public ByteBuffer allocByteBuffer(int size) {
	// TODO: right now the size is kinda ignored, all sizes are BUFFER_SIZE bytes in the pool
	int which = Parameters.am.workerMgr.getCurrentWorkerID();
	Stack<ByteBuffer> s;
	if (which == -1)
	    s = pool.sbuffer;
	else
	    s = pool.buffer.get(which);
	if (s.size() > 0) {
	    ByteBuffer ret = s.pop();
	    if (ret.capacity() < size) {
		s.push(ret);
		Parameters.newCountbuffer ++;
		return ByteBuffer.allocate(size);
	    }
	    return ret;
	}
	else {
	    Parameters.newCountbuffer ++;
	    return ByteBuffer.allocate(BUFFER_SIZE);
	}
    }
	
    public void freeByteBuffer(ByteBuffer buffer) {
	buffer.clear();
	int which = Parameters.am.workerMgr.getCurrentWorkerID();
	if (which == -1) {
	    pool.sbuffer.push(buffer);
	} else {
	    pool.buffer.get(which).push(buffer);
	}
    }
}
