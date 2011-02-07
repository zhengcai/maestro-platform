/*
  OFPConstants.java

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

/**
 * class that holds all OpenFlow protocol constants used in this project
 * @author Zheng Cai
 */
public class OFPConstants {
    public static class PacketTypes {
	/* Immutable messages. */
	public static final short OFPT_HELLO = 0;               /* Symmetric message */
	public static final short OFPT_ERROR = 1;               /* Symmetric message */
	public static final short OFPT_ECHO_REQUEST = 2;        /* Symmetric message */
	public static final short OFPT_ECHO_REPLY = 3;          /* Symmetric message */
	public static final short OFPT_VENDOR = 4;              /* Symmetric message */

	/* Switch configuration messages. */
	public static final short OFPT_FEATURES_REQUEST = 5;    /* Controller/switch message */
	public static final short OFPT_FEATURES_REPLY = 6 ;     /* Controller/switch message */
	public static final short OFPT_GET_CONFIG_REQUEST = 7;  /* Controller/switch message */
	public static final short OFPT_GET_CONFIG_REPLY = 8;    /* Controller/switch message */
	public static final short OFPT_SET_CONFIG = 9;          /* Controller/switch message */

	/* Asynchronous messages. */
	public static final short OFPT_PACKET_IN = 10;          /* Async message */
	public static final short OFPT_FLOW_EXPIRED = 11;       /* Async message */
	public static final short OFPT_PORT_STATUS = 12;        /* Async message */

	/* Controller command messages. */
	public static final short OFPT_PACKET_OUT = 13;         /* Controller/switch message */
	public static final short OFPT_FLOW_MOD = 14;           /* Controller/switch message */
	public static final short OFPT_PORT_MOD = 15;           /* Controller/switch message */

	/* Statistics messages. */
	public static final short OFPT_STATS_REQUEST = 16;      /* Controller/switch message */
	public static final short OFPT_STATS_REPLY = 17;        /* Controller/switch message */
    }
	
    public static class OfpActionType {
	public static final int OFPAT_OUTPUT = 0;          /* Output to switch port. */
	public static final int OFPAT_SET_VLAN_VID = 1;    /* Set the 802.1q VLAN id. */
	public static final int OFPAT_SET_VLAN_PCP = 2;    /* Set the 802.1q priority. */
	public static final int OFPAT_STRIP_VLAN = 3;      /* Strip the 802.1q header. */
	public static final int OFPAT_SET_DL_SRC = 4;      /* Ethernet source address. */
	public static final int OFPAT_SET_DL_DST = 5;      /* Ethernet destination address. */
	public static final int OFPAT_SET_NW_SRC = 6;      /* IP source address. */
	public static final int OFPAT_SET_NW_DST = 7;      /* IP destination address. */
	public static final int OFPAT_SET_TP_SRC = 8;      /* TCP/UDP source port. */
	public static final int OFPAT_SET_TP_DST = 9;      /* TCP/UDP destination port. */
	public static final int OFPAT_VENDOR = 0xffff;
    }
	
    public static class OfpFlowModCommand {
	public static final int OFPFC_ADD = 0;              /* New flow. */                                                                                                                  
	public static final int OFPFC_MODIFY = 1;           /* Modify all matching flows. */                                                                                                 
	public static final int OFPFC_MODIFY_STRICT = 2;    /* Modify entry strictly matching wildcards */                                                                                   
	public static final int OFPFC_DELETE = 3;           /* Delete all matching flows. */                                                                                                 
	public static final int OFPFC_DELETE_STRICT = 4;    /* Strictly match wildcards and priority. */
    }
	
    public static class OfpConstants {
	public static final short IP_TYPE_ICMP = 1;
	public static final short IP_TYPE_TCP = 6;
	public static final short IP_TYPE_UDP = 17;
	    
	public static final int OFP_ETH_ALEN = 6;
	public static final int OFP_MAX_PORT_NAME_LEN = 16;
	public static final int ETH_TYPE_IPV4 = 0x0800;
	public static final int ETH_TYPE_ARP = 0x0806;
	public static final byte ETH_TYPE_LLDP_B0 = -52;
	public static final byte ETH_TYPE_LLDP_B1 = -120;
	public static final int ETH_TYPE_LLDP = 0x88cc;
	public static final byte[] NDP_MULTICAST = {(byte)0x01, 
						    (byte)0x23, (byte)0x20, (byte)0x00, (byte)0x00, (byte)0x01};
	public static final int OFPAT_OUTPUT_LENGTH = 8;
	    
	public static final int OFP_FLOW_MOD_LENGTH = 72; // for openflow version 0x01
	public static final int OFP_PACKET_OUT_LENGTH = 16;
	public static final short OFP_VERSION = 0x01;
	public static final int OFP_HEADER_LEN = 8; // openflow packet header length
	public static final int OFP_SWITCH_FEATURES_LEN = 32;
	public static final int OFP_PHY_PORT_LEN = 48;
	public static final int OFP_PACKET_IN_LEN = 18;
	    
	public static final int ETH_HEADER_LEN = 14;
	public static final int ETH_PAYLOAD_MIN = 46;
	public static final int ETH_PAYLOAD_MAX = 1500;
	public static final int IP_HEADER_LEN = 20;
	public static final int UDP_HEADER_LEN = 8;
    }
	
    public class OfpPort {                                                                                                                                              
	/** Maximum number of physical switch ports. */                                                                                                           
	public static final int OFPP_MAX = 0xff00;
	    
	/* Fake output "ports". */                                                                                                                               
	public static final int OFPP_IN_PORT    = 0xfff8;  /* Send the packet out the input port. This
							      virtual port must be explicitly used
							      in order to send back out of the input port. */
	public static final int OFPP_TABLE      = 0xfff9;  /* Perform actions in flow table.                                                                                             
							      NB: This can only be the destination                                                                                       
							      port for packet-out messages. */                                                                                           
	public static final int OFPP_NORMAL     = 0xfffa;  /* Process with normal L2/L3 switching. */                                                                                    
	public static final int OFPP_FLOOD      = 0xfffb;  /* All physical ports except input port and                                                                                   
							      those disabled by STP. */                                                                                                  
	public static final int OFPP_ALL        = 0xfffc;  /* All physical ports except input port. */                                                                                   
	public static final int OFPP_CONTROLLER = 0xfffd;  /* Send to controller. */                                                                                                     
	public static final int OFPP_LOCAL      = 0xfffe;  /* Local openflow "port". */                                                                                                  
	public static final int OFPP_NONE       = 0xffff;   /* Not associated with a physical port. */                                                                                    
    }
	
    public static final int OP_TAP0_PORTNO = 65534;

    public static final long OP_UNBUFFERED_BUFFER_ID = (((long)1) << 32) - 1; /* That is, (uint32_t)(-1) */
}
