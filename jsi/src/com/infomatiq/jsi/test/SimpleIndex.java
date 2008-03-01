//   SimpleIndex.java
//   Java Spatial Index Library
//   Copyright (C) 2002 Infomatiq Limited
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

package com.infomatiq.jsi.test;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntObjectIterator;
import gnu.trove.TIntProcedure;
import java.util.Properties;

import com.infomatiq.jsi.IntProcedure;
import com.infomatiq.jsi.Point;
import com.infomatiq.jsi.Rectangle;
import com.infomatiq.jsi.SpatialIndex;

/**
 * SimpleIndex
 * 
 * <p>A very simple (and slow!) spatial index implementation,
 * intended only for generating test results.</p>
 * 
 * <p>All of the search methods, ie nearest(), contains() and intersects(),
 * run in linear time, so performance will be very slow with more
 * than 1000 or so entries.</p>
 * 
 * <p>On the other hand, the add() and delete() methods are very fast :-)</p>
 * 
 * @author  aled.morris@infomatiq.co.uk
 * @version 1.0b2
 */
public class SimpleIndex implements SpatialIndex {
  TIntObjectHashMap m_map = new TIntObjectHashMap();
  private static final String version = "1.0b2";
  
  /**
   * Does nothing. There are no implementation dependent properties for 
   * the SimpleIndex spatial index.
   */
  public void init(Properties p) {
   return; 
  }
  
  /**
   * Nearest
   */
  private TIntArrayList nearest(Point p, float furthestDistance) {
    TIntArrayList ret = new TIntArrayList();
    float nearestDistance = furthestDistance;
    TIntObjectIterator i = m_map.iterator();
    while (i.hasNext()) {
      i.advance();
      int currentId = i.key();
      Rectangle currentRectangle = (Rectangle) i.value(); 
      float distance = currentRectangle.distance(p);
      if (distance < nearestDistance) {
        nearestDistance = distance;
        ret.clear();         
      }
      if (distance <= nearestDistance) {
        ret.add(currentId);  
      }
    } 
    return ret;   
  }
  
  /**
   * @see com.infomatiq.jsi.SpatialIndex#nearest(IntProcedure, float)
   */
  public void nearest(Point p, final IntProcedure v, float furthestDistance) {
    TIntArrayList nearestList = nearest(p, furthestDistance); 
    nearestList.forEach(new TIntProcedure() {
      public boolean execute(int id) {
        v.execute(id);	
        return true;
      }	
    });
  } 
  
  /**
   * @see com.infomatiq.jsi.SpatialIndex#intersects(Rectangle, IntProcedure)
   */
  public void intersects(Rectangle r, IntProcedure v) {
    TIntObjectIterator i = m_map.iterator();
    while (i.hasNext()) {
      i.advance();
      int currentId = i.key();
      Rectangle currentRectangle = (Rectangle) i.value(); 
      if (r.intersects(currentRectangle)) {
        v.execute(currentId);      
      }
    } 
  }
  
  /**
   * @see com.infomatiq.jsi.SpatialIndex#contains(Rectangle, IntProcedure)
   */
  public void contains(Rectangle r, IntProcedure v) {
   TIntObjectIterator i = m_map.iterator();
   while (i.hasNext()) {
      i.advance();
      int currentId = i.key();
      Rectangle currentRectangle = (Rectangle) i.value(); 
      if (r.contains(currentRectangle)) {
        v.execute(currentId);      
      }
    }
    return;
  }
  
  /**
   * @see com.infomatiq.jsi.SpatialIndex#add(Rectangle, int)
   */
  public void add(Rectangle r, int id) {
    m_map.put(id, r.copy());
  }
  
  /**
   * @see com.infomatiq.jsi.SpatialIndex#delete(Rectangle, int)
   */
  public boolean delete(Rectangle r, int id) {
    Rectangle value = (Rectangle) m_map.get(id);
    
    if (r.equals(value)) {
      m_map.remove(id);
      return true;
    }
    return false;
  }
  
  /**
   * @see com.infomatiq.jsi.SpatialIndex#size()
   */
  public int size() {
    return m_map.size();
  }
  
  /**
   * @see com.infomatiq.jsi.SpatialIndex#getBounds()
   */
  public Rectangle getBounds() {
    Rectangle bounds = null;
    TIntObjectIterator i = m_map.iterator();
    while (i.hasNext()) {
      i.advance();
      int currentId = i.key();
      Rectangle currentRectangle = (Rectangle) i.value(); 
      if (bounds == null) {
        bounds = currentRectangle.copy(); 
      } else {
        bounds.add(currentRectangle);
      }
    }
    return bounds;
  }
  
  /**
   * @see com.infomatiq.jsi.SpatialIndex#Version
   */
  public String getVersion() {
    return "SimpleIndex-" + version;
  }
  
  
}
