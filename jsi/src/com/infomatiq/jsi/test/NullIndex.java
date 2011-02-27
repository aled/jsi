//   NullIndex.java
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
//  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA

package com.infomatiq.jsi.test;

import gnu.trove.TIntProcedure;

import java.util.Properties;

import com.infomatiq.jsi.Point;
import com.infomatiq.jsi.Rectangle;
import com.infomatiq.jsi.SpatialIndex;

/**
 * An implementation of SpatialIndex that does absolutely nothing.
 * The purpose of this class is to measure the overhead of the
 * testing framework.
 * 
 * @author  aled@sourceforge.net
 * @version 1.0b8
 */
public class NullIndex implements SpatialIndex {
  private static final String version = "1.0b8";
    
  /**
   * @see com.infomatiq.jsi.SpatialIndex#init(Properties)
   */
  public void init(Properties props) {
  }

  /**
   * @see com.infomatiq.jsi.SpatialIndex#nearest(Point, IntProcedure, float)
   */
  public void nearest(Point p, TIntProcedure v, float distance) {
  }

  /**
   * @see com.infomatiq.jsi.SpatialIndex#nearestN(com.infomatiq.jsi.Point, com.infomatiq.jsi.IntProcedure, int, float)
   */
  public void nearestN(Point p, TIntProcedure v, int n, float distance) {
  }
 
  /**
   * @see com.infomatiq.jsi.SpatialIndex#nearestNUnsorted(com.infomatiq.jsi.Point, com.infomatiq.jsi.IntProcedure, int, float)
   */
  public void nearestNUnsorted(Point p, TIntProcedure v, int n, float distance) {
  }
  
  /**
   * @see com.infomatiq.jsi.SpatialIndex#intersects(Rectangle, IntProcedure)
   */
  public void intersects(Rectangle r, TIntProcedure ip) {
  }

  /**
   * @see com.infomatiq.jsi.SpatialIndex#contains(Rectangle, IntProcedure)
   */
  public void contains(Rectangle r, TIntProcedure ip) {
  }

  /**
   * @see com.infomatiq.jsi.SpatialIndex#add(Rectangle, int)
   */
  public void add(Rectangle r, int id) {
  }

  /**
   * @see com.infomatiq.jsi.SpatialIndex#delete(Rectangle, int)
   */
  public boolean delete(Rectangle r, int id) {
    return false;
  }

  /**
   * @see com.infomatiq.jsi.SpatialIndex#size()
   */
  public int size() {
    return 0;
  }
  
  /**
   * @see com.infomatiq.jsi.SpatialIndex#getBounds()
   */
  public Rectangle getBounds() {
    return null; 
  }

  /**
   * @see com.infomatiq.jsi.SpatialIndex#getVersion()
   */
  public String getVersion() {
    return "NullIndex-" + version;
  }
}
