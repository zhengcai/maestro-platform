/*
  HeaderVisitor.java

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

package headers;

/**
 * The visitor interface for Headers
 *
 * @author Zheng Cai
 *
 */
public interface HeaderVisitor {
    void visit(EthernetHeader eth);
    void visit(LLDPHeader lldp);
    void visit(ARPHeader arp);
    void visit(IPV4Header ipv4);
    void visit(TCPHeader tcp);
    void visit(UDPHeader udp);
    void visit(ICMPHeader icmp);
}