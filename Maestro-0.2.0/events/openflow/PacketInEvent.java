/*
  PacketInEvent.java

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

package events.openflow;

import drivers.OFPConstants;
import events.Event;
import events.MemoryPoolEvent;
import views.openflow.RegisteredHostsView;
import headers.*;

/**
 * @author Zheng Cai
 */
public class PacketInEvent extends MemoryPoolEvent {
    public static class FlowInfo implements HeaderVisitor {
	public long nwSrc;
	public long nwDst;
	public int inPort;
	public int dlVlan;
	public int dlType;
	public short dlVlanPcp;
	public int tpSrc;
	public int tpDst;
	public short[] dlSrc;
	public short[] dlDst;
	public short nwProto;
	public short nwTos;
	public short icmpType;
	public short icmpCode;
		
	public FlowInfo() {
	    dlSrc = new short[OFPConstants.OfpConstants.OFP_ETH_ALEN];
	    dlDst = new short[OFPConstants.OfpConstants.OFP_ETH_ALEN];
	}

	public void visit(EthernetHeader eth) {
	    dlVlan = eth.dlVlan;
	    dlType = eth.dlType;
	    dlVlanPcp = eth.dlVlanPcp;
	    dlSrc = eth.dlSrc;
	    dlDst = eth.dlDst;
	}

	public void visit(ARPHeader arp) {

	}

	public void visit(LLDPHeader lldp) {

	}

	public void visit(IPV4Header ipv4) {
	    nwSrc = ipv4.nwSrc;
	    nwDst = ipv4.nwDst;
	    nwProto = ipv4.nwProto;
	    nwTos = ipv4.nwTos;
	}

	public void visit(TCPHeader tcp) {
	    tpSrc = tcp.tpSrc;
	    tpDst = tcp.tpDst;
	}

	public void visit(UDPHeader udp) {
	    tpSrc = udp.tpSrc;
	    tpDst = udp.tpDst;
	}

	public void visit(ICMPHeader icmp) {
	    icmpType = icmp.type;
	    icmpCode = icmp.code;
	}
		
	public String toString() {
	    String sIP = String.format("%d.%d.%d.%d",
				       (short) (nwSrc & 0x00FF),
				       (short) ((nwSrc >> 8) & 0x000000FF),
				       (short) ((nwSrc >> 16) & 0x000000FF),
				       (short) ((nwSrc >> 24) & 0x000000FF));
	    String dIP = String.format("%d.%d.%d.%d",
				       (short) (nwDst & 0x00FF),
				       (short) ((nwDst >> 8) & 0x000000FF),
				       (short) ((nwDst >> 16) & 0x000000FF),
				       (short) ((nwDst >> 24) & 0x000000FF));
	    return String.format("Flow: sIP=%s dIP=%s inPort=%d dlVlan=%d dlType=%d tpSrc=%d tpDst=%d dlSrc=%d-%d-%d-%d-%d-%d dlDst=%d-%d-%d-%d-%d-%d nwProto=%d",
				 sIP, dIP, inPort, dlVlan, dlType, tpSrc, tpDst,
				 dlSrc[0], dlSrc[1], dlSrc[2], dlSrc[3], dlSrc[4], dlSrc[5],
				 dlDst[0], dlDst[1], dlDst[2], dlDst[3], dlDst[4], dlDst[5], 
				 nwProto);
	}
    }
	
    public static class DataPayload {
	public int poolIdx;
	public boolean valid;
	public byte[] data;
		
	/** Real size of the payload
	 * For memory management reason data.length might not be equal to size
	 * So always use size
	 */
	public int size;
		
	public DataPayload(int s) {
	    poolIdx = -1;
	    data = new byte[s];
	    size = s;
	    valid = false;
	}
		
	public DataPayload(int idx, int s) {
	    poolIdx = idx;
	    data = new byte[s];
	    size = s;
	    valid = false;
	}
    }
	
    public long dpid;
    public long xid;
    public long bufferId;
    public int totalLen;	//. Length of data
    public int inPort;
    public short reason;
    public DataPayload data;
    public FlowInfo flow;
    public EthernetHeader header; //. Assume all packets are ethernet frames
    public RegisteredHostsView.Location dst;
    public boolean flush = false; //. Whether to flush the input batching queue
    public boolean dummy = false; //. Whether this is a dummy packet

    public static final PacketInEvent flushDummy = new PacketInEvent(true);

    public PacketInEvent() {
	super(-1);
	flow = new FlowInfo();
    }
	
    public PacketInEvent(int idx) {
	super(idx);
	flow = new FlowInfo();
    }

    public PacketInEvent(boolean d) {
	super(-1);
	dummy = d;
	flush = true;
    }

    /**
     * Extract the FlowInfo from a ethernet frame, currently only accepts ethernet frame
     * Also remember the header, if going to be used later in applications
     * @param eth the ethernet frame header
     */
    public void extractFlowInfo(EthernetHeader eth) {
	header = eth;
	
	flow.inPort = inPort;
	
	Header current = eth;
	while (null != current) {
	    current.accept(flow);
	    current = current.inner;
	}
    }
	
    public String toString() {
	return String.format("PacketIn from Switch=%d bufferId=%d totalLen=%d inPort=%d reason=%d, %s",
			     dpid, bufferId, totalLen, inPort, reason, flow);
    }

    @Override
	public int convertToBytes(byte[] buf, int index) {
	return 0;
    }
}
