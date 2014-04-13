//   SpatialIndexFactory.java
//   Java Spatial Index Library
//   Copyright (C) 2002-2005 Infomatiq Limited.
//  
//  This library is free software; you can redistribute it and/or
//  modify it under the terms of the GNU Lesser General Public
//  License as published by the Free Software Foundation; either
//  version 2.1 of the License, or (at your option) any later version.
//  
//  This library is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//  Lesser General Public License for more details.
//  
//  You should have received a copy of the GNU Lesser General Public
//  License along with this library; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

package net.sf.jsi;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.jsi.SpatialIndex;

/**
 * Factory class used to create instances of spatial indexes
 */
public class SpatialIndexFactory {
  
  private final static Logger log = 
    LoggerFactory.getLogger(SpatialIndexFactory.class);

  public static SpatialIndex newInstance(String type) {
    return newInstance(type, null);
  }
  
  public static SpatialIndex newInstance(String type, Properties props) {
    SpatialIndex si = null;
    String className = "net.sf.jsi." + type;
    try {
      si = (SpatialIndex) Class.forName(className).newInstance();
      si.init(props);
    } catch (ClassNotFoundException cnfe) {
      log.error(cnfe.toString());
    } catch (IllegalAccessException iae) {
      log.error(iae.toString());    
    } catch (InstantiationException ie) {
      log.error(ie.toString());   
    }
    
    return si;
  }
}
