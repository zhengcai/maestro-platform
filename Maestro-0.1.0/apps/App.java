/*
  App.java

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

package apps;

import sys.ApplicationManager;

import sys.ViewManager;
import views.*;

/**
 * The base class for all applications
 * @author Zheng Cai
 */
public abstract class App{
    protected ViewManager viewmanager;
    protected ApplicationManager appmanager;
    
    /** Creates a new instance of OPApp */
    public App() {
        
    }
    
    public void initiate(ViewManager vmanager, ApplicationManager amanager) {
        viewmanager = vmanager;
        appmanager = amanager;
    }
    
    /** Main method, process the input, and generate output */
    public abstract ViewsIOBucket process(ViewsIOBucket input);
}
