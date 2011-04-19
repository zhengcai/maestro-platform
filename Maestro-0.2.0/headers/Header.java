/*
  Header.java

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
 * The base abstract class for a Header in Maestro
 *
 * @author Zheng Cai
 *
 */
public abstract class Header {
    /** The header one layer outer */
    public Header outer;

    /** The header one layer inner */
    public Header inner;

    /**
     * Constructor of Packet
     */
    public Header() {
	outer = null;
	inner = null;
    }

    /**
       Interface of the visitor design pattern
    */
    public abstract void accept(HeaderVisitor visitor);

    /**
     * Parse the header of this type of packet accordingly
     * @param buf Buffer which stores the whole packet data
     * @param pos Position of where the header starts in the buffer
     * @return The position in the buffer after parsing the header
     */
    public abstract int parseHeader(byte[] buf, int pos);

    public void free() {
	    
    }
}