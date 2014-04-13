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

package net.sf.jsi;

import gnu.trove.procedure.TIntProcedure;

import java.util.Properties;

/**
 * An implementation of SpatialIndex that does absolutely nothing.
 * The purpose of this class is to measure the overhead of the
 * testing framework.
 */
public class NullIndex implements SpatialIndex {
    
  /**
   * @see net.sf.jsi.SpatialIndex#init(Properties)
   */
  public void init(Properties props) {
  }

  /**
   * @see net.sf.jsi.SpatialIndex#nearest(Point, gnu.trove.TIntProcedure, float)
   */
  public void nearest(Point p, TIntProcedure v, float distance) {
  }

  /**
   * @see net.sf.jsi.SpatialIndex#nearestN(Point, gnu.trove.TIntProcedure, int, float)
   */
  public void nearestN(Point p, TIntProcedure v, int n, float distance) {
  }
 
  /**
   * @see net.sf.jsi.SpatialIndex#nearestNUnsorted(Point, gnu.trove.TIntProcedure, int, float)
   */
  public void nearestNUnsorted(Point p, TIntProcedure v, int n, float distance) {
  }
  
  /**
   * @see net.sf.jsi.SpatialIndex#intersects(Rectangle, gnu.trove.TIntProcedure)
   */
  public void intersects(Rectangle r, TIntProcedure ip) {
  }

  /**
   * @see net.sf.jsi.SpatialIndex#contains(Rectangle, gnu.trove.TIntProcedure)
   */
  public void contains(Rectangle r, TIntProcedure ip) {
  }

  /**
   * @see net.sf.jsi.SpatialIndex#add(Rectangle, int)
   */
  public void add(Rectangle r, int id) {
  }

  /**
   * @see net.sf.jsi.SpatialIndex#delete(Rectangle, int)
   */
  public boolean delete(Rectangle r, int id) {
    return false;
  }

  /**
   * @see net.sf.jsi.SpatialIndex#size()
   */
  public int size() {
    return 0;
  }
  
  /**
   * @see net.sf.jsi.SpatialIndex#getBounds()
   */
  public Rectangle getBounds() {
    return null; 
  }

  /**
   * @see net.sf.jsi.SpatialIndex#getVersion()
   */
  public String getVersion() {
    return "NullIndex-" + BuildProperties.getVersion();
  }
}
