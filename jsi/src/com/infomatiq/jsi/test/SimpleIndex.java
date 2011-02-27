//   SimpleIndex.java
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

package com.infomatiq.jsi.test;

import gnu.trove.TFloatArrayList;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntObjectIterator;
import gnu.trove.TIntProcedure;

import java.util.Properties;

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
 * @author  aled@sourceforge.net
 * @version 1.0b8
 */
public class SimpleIndex implements SpatialIndex {
  TIntObjectHashMap m_map = new TIntObjectHashMap();
  private static final String version = "1.0b8";
  
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
  public void nearest(Point p, final TIntProcedure v, float furthestDistance) {
    TIntArrayList nearestList = nearest(p, furthestDistance); 
    nearestList.forEach(new TIntProcedure() {
      public boolean execute(int id) {
        v.execute(id);	
        return true;
      }	
    });
  } 
  
  private TIntArrayList nearestN(Point p, int n, float furthestDistance) {
    TIntArrayList ids = new TIntArrayList();
    TFloatArrayList distances = new TFloatArrayList();
    
    TIntObjectIterator iter = m_map.iterator();
    while (iter.hasNext()) {
      iter.advance();
      int currentId = iter.key();
      Rectangle currentRectangle = (Rectangle) iter.value(); 
      float distance = currentRectangle.distance(p);
      
      if (distance <= furthestDistance) {
        int insertionIndex = 0;
        while (ids.size() > insertionIndex && distances.get(insertionIndex) <= distance) {
          insertionIndex++;
        }
        
        ids.insert(insertionIndex, currentId);
        distances.insert(insertionIndex, distance);
        
        // remove the entries with the greatest distance, if necessary.
        if (ids.size() > n) {
          // check that removing all entries with equal greatest distance
          // would leave at least N entries.
          int maxDistanceCount = 1;
          int currentIndex = distances.size() - 1;
          float maxDistance = distances.get(currentIndex);
          while (currentIndex - 1 >= 0 && distances.get(currentIndex - 1) == maxDistance) {
            currentIndex--;
            maxDistanceCount++; 
          }
          if (ids.size() - maxDistanceCount >= n) {
            ids.remove(currentIndex, maxDistanceCount);
            distances.remove(currentIndex, maxDistanceCount);
          }
        }
      }
    }
    
    return ids;
  }
  
  /**
   * @see com.infomatiq.jsi.SpatialIndex#nearestN(com.infomatiq.jsi.Point, com.infomatiq.jsi.IntProcedure, int, float)
   */
  public void nearestN(Point p, final TIntProcedure v, int n, float furthestDistance) {
    TIntArrayList nearestList = nearestN(p, n, furthestDistance); 
    nearestList.forEach(new TIntProcedure() {
      public boolean execute(int id) {
        v.execute(id);  
        return true;
      } 
    });
  }  

  /**
   * Same as nearestN
   * 
   * @see com.infomatiq.jsi.SpatialIndex#nearestNUnsorted(com.infomatiq.jsi.Point, com.infomatiq.jsi.IntProcedure, int, float)
   */
  public void nearestNUnsorted(Point p, final TIntProcedure v, int n, float furthestDistance) {
    nearestN(p, v, n, furthestDistance);
  } 
  
  /**
   * @see com.infomatiq.jsi.SpatialIndex#intersects(Rectangle, IntProcedure)
   */
  public void intersects(Rectangle r, TIntProcedure v) {
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
  public void contains(Rectangle r, TIntProcedure v) {
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
