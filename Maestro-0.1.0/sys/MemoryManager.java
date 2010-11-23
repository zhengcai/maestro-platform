package sys;

import events.openflow.*;

/**
 * Manually manage heap memory allocation, instead of using Java's GC
 * @author zc1
 *
 * TODO: Right now the implementation is very static and non-flexible
 * Need a re-design and re-implementation
 */
public class MemoryManager {
	public static int FM_POOL_SIZE = 81920;
	public static int PO_POOL_SIZE = 81920;
	public static int PI_POOL_SIZE = 81920;
	public static int DATA_POOL_SIZE = 81920;

	public class MemoryPool {
		public class FlowModPool {
			public FlowModEvent[] pool;
			public boolean[] bitmap;
			public int pos;
			public int freeNum;
		}
		
		public class PacketOutPool {
			public PacketOutEvent[] pool;
			public boolean[] bitmap;
			public int pos;
			public int freeNum;
		}
		
		public class PacketInPool {
			public PacketInEvent[] pool;
			public boolean[] bitmap;
			public int pos;
			public int freeNum;
		}
		
		public class PacketInDataPayloadPool {
			public PacketInEvent.DataPayload[] pool;
			public boolean[] bitmap;
			public int pos;
			public int freeNum;
		}
		
		public FlowModPool fm;
		public PacketOutPool po;
		public PacketInPool pi;
		public PacketInDataPayloadPool data;
		
		public MemoryPool() {
			fm = new FlowModPool();
			po = new PacketOutPool();
			pi = new PacketInPool();
			data = new PacketInDataPayloadPool();
		}
	}
	
	public MemoryPool pool;
	
	public MemoryManager() {
		pool = new MemoryPool();
		
		pool.fm.pool = new FlowModEvent[FM_POOL_SIZE];
		pool.fm.bitmap = new boolean[FM_POOL_SIZE];
		for (int i=0;i<FM_POOL_SIZE;i++) {
			pool.fm.pool[i] = new FlowModEvent(i);
			pool.fm.pool[i].actions = new PacketOutEvent.Action[1];
			pool.fm.pool[i].actions[0] = new PacketOutEvent.Action();
			pool.fm.bitmap[i] = false;
		}
		pool.fm.pos = 0;
		pool.fm.freeNum = FM_POOL_SIZE;
		
		pool.po.pool = new PacketOutEvent[PO_POOL_SIZE];
		pool.po.bitmap = new boolean[PO_POOL_SIZE];
		for (int i=0;i<PO_POOL_SIZE;i++) {
			pool.po.pool[i] = new PacketOutEvent(i);
			pool.po.pool[i].actions = new PacketOutEvent.Action[1];
			pool.po.pool[i].actions[0] = new PacketOutEvent.Action();
			pool.po.bitmap[i] = false;
		}
		pool.po.pos = 0;
		pool.po.freeNum = PO_POOL_SIZE;
		
		// TODO: right now actions can only contain one action,
		// need to fix this to allow flexible actions allocation
		
		pool.pi.pool = new PacketInEvent[PI_POOL_SIZE];
		pool.pi.bitmap = new boolean[PI_POOL_SIZE];
		for (int i=0;i<PI_POOL_SIZE;i++) {
			pool.pi.pool[i] = new PacketInEvent(i);
			pool.pi.bitmap[i] = false;
		}
		pool.pi.pos = 0;
		pool.pi.freeNum = PI_POOL_SIZE;
		
		pool.data.pool = new PacketInEvent.DataPayload[DATA_POOL_SIZE];
		pool.data.bitmap = new boolean[DATA_POOL_SIZE];
		for (int i=0;i<DATA_POOL_SIZE;i++) {
			// TODO: now fixed at 60 bytes, must be modified later!!!
			pool.data.pool[i] = new PacketInEvent.DataPayload(i, 60);
			pool.data.bitmap[i] = false;
		}
		pool.data.pos = 0;
		pool.data.freeNum = DATA_POOL_SIZE;
	}
	
	public FlowModEvent allocFlowModEvent() {
		FlowModEvent ret = null;
		synchronized (pool.fm) {
			if (pool.fm.freeNum <= 0) {
				System.err.println("FM_POOL OUT OF MEMORY!!");
				System.exit(-1);
				return null;
			}
			while (pool.fm.bitmap[pool.fm.pos]) {
				pool.fm.pos = (pool.fm.pos+1)%FM_POOL_SIZE;
			}
			ret = pool.fm.pool[pool.fm.pos];
			ret.valid = true;
			pool.fm.bitmap[pool.fm.pos] = true;
			pool.fm.freeNum --;
			pool.fm.pos = (pool.fm.pos+1)%FM_POOL_SIZE;
		}
		return ret;
	}
	
	public void freeFlowModEvent(FlowModEvent fm) {
		if (fm.poolIdx < 0)
			return;
		synchronized (pool.fm) {
			pool.fm.bitmap[fm.poolIdx] = false;
			fm.valid = false;
			pool.fm.freeNum ++;
		}
	}
	
	public PacketOutEvent allocPacketOutEvent() {
		PacketOutEvent ret = null;
		synchronized (pool.po) {
			if (pool.po.freeNum <= 0) {
				System.err.println("PO_POOL OUT OF MEMORY!!");
				System.exit(-1);
				return null;
			}
			while (pool.po.bitmap[pool.po.pos]) {
				pool.po.pos = (pool.po.pos+1)%PO_POOL_SIZE;
			}
			ret = pool.po.pool[pool.po.pos];
			ret.valid = true;
			pool.po.bitmap[pool.po.pos] = true;
			pool.po.freeNum --;
			pool.po.pos = (pool.po.pos+1)%PO_POOL_SIZE;
		}
		return ret;
	}
	
	public void freePacketOutEvent(PacketOutEvent po) {
		if (po.poolIdx < 0)
			return;
		synchronized (pool.po) {
			po.data = null;
			pool.po.bitmap[po.poolIdx] = false;
			po.valid = false;
			pool.po.freeNum ++;
		}
	}
	
	public PacketInEvent allocPacketInEvent() {
		PacketInEvent ret = null;
		synchronized (pool.pi) {
			if (pool.pi.freeNum <= 0) {
				System.err.println("PI_POOL OUT OF MEMORY!!");
				System.exit(-1);
				return null;
			}
			while (pool.pi.bitmap[pool.pi.pos]) {
				pool.pi.pos = (pool.pi.pos+1)%PI_POOL_SIZE;
			}
			ret = pool.pi.pool[pool.pi.pos];
			ret.valid = true;
			pool.pi.bitmap[pool.pi.pos] = true;
			pool.pi.freeNum --;
			pool.pi.pos = (pool.pi.pos+1)%PI_POOL_SIZE;
		}
		return ret;
	}
	
	public void freePacketInEvent(PacketInEvent pi) {
		if (pi.poolIdx < 0)
			return;
		synchronized (pool.pi) {
			pi.data = null;
			pool.pi.bitmap[pi.poolIdx] = false;
			pi.valid = false;
			pool.pi.freeNum ++;
		}
	}
	
	public PacketInEvent.DataPayload allocPacketInEventDataPayload(int size) {
		// TODO: right now the size is kinda ignored, all sizes are 60 bytes in the pool
		PacketInEvent.DataPayload ret = null;
		synchronized (pool.data) {
			if (pool.data.freeNum <= 0) {
				System.err.println("DATA_POOL OUT OF MEMORY!!");
				System.exit(-1);
				return null;
			}
			while (pool.data.bitmap[pool.data.pos]) {
				pool.data.pos = (pool.data.pos+1)%DATA_POOL_SIZE;
			}
			ret = pool.data.pool[pool.data.pos];
			ret.valid = true;
			pool.data.bitmap[pool.data.pos] = true;
			pool.data.freeNum --;
			pool.data.pos = (pool.data.pos+1)%DATA_POOL_SIZE;
		}
		ret.size = size;
		return ret;
	}
	
	public void freePacketInEventDataPayload(PacketInEvent.DataPayload data) {
		if (data.poolIdx < 0)
			return;
		synchronized (pool.data) {
			pool.data.bitmap[data.poolIdx] = false;
			data.valid = false;
			pool.data.freeNum ++;
		}
	}
}
