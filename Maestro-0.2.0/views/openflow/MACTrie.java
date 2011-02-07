/*
  MACTrie.java

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

package views.openflow;

import java.util.LinkedList;

import sys.Utilities;

/**
 * Trie data structure for MAC addresses
 * @author Zheng Cai
 */
public class MACTrie {
    private static final int FinalLevel = 5;
    private static final int FanOut = 256;
    
    public abstract class Node {

    }

    public class MidNode extends Node {
	public Node[] children;

	public MidNode(int level) {
	    if (level < FinalLevel-1)
		children = new MidNode[FanOut];
	    else
		children = new BottomNode[FanOut];
	}
    }

    public class BottomNode extends Node {
	public short[] children;

	public BottomNode() {
	    children = new short[FanOut];
	}
    }

    MidNode root;    

    public MACTrie() {
	root = new MidNode(0);
    }

    public void addEntry(short[] mac, short e) {
	Utilities.Assert(FinalLevel+1 == mac.length, "MAC has more than 6 bytes!");
	MidNode node = root;
	for (int i=0; i<FinalLevel-1; i++) {
	    if (null == node.children[mac[i]]) {
		node.children[mac[i]] = new MidNode(i+1);
	    }
	    node = (MidNode)node.children[mac[i]];
	}

	BottomNode bottom;
	if (null == node.children[mac[FinalLevel-1]]) {
	    node.children[mac[FinalLevel-1]] = new BottomNode();
	}
	bottom = (BottomNode)node.children[mac[FinalLevel-1]];
	bottom.children[mac[FinalLevel]] = e;
    }

    public short getEntry(short[] mac) {
	Utilities.Assert(FinalLevel+1 == mac.length, "MAC has more than 6 bytes!");
	MidNode node = root;
	for (int i=0; i<FinalLevel-1; i++) {
	    if (null == node.children[mac[i]]) {
		return -1;
	    }
	    node = (MidNode)node.children[mac[i]];
	}

	BottomNode bottom;
	if (null == node.children[mac[FinalLevel-1]]) {
	    return -1;
	}
	bottom = (BottomNode)node.children[mac[FinalLevel-1]];
	return bottom.children[mac[FinalLevel]];
    }
}
