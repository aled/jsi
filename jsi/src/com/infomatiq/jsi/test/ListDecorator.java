//   ListDecorator.java
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

import java.util.ArrayList;
import java.util.List;

import com.infomatiq.jsi.Point;
import com.infomatiq.jsi.Rectangle;
import com.infomatiq.jsi.SpatialIndex;

/**
 * ListDecorator
 * 
 * @author aled@sourceforge.net
 * @version 1.0b8
 */
public class ListDecorator {
 SpatialIndex m_si = null;
 
 public ListDecorator(SpatialIndex si) {
   m_si = si;
 }
 
 class AddToListProcedure implements TIntProcedure {
   private List m_list = new ArrayList();
   
   public boolean execute(int id) {
     m_list.add(new Integer(id));
     return true;
   }
   
   public List getList() {
     return m_list;	
   }	
 }
 
 /**
   * Finds all rectangles that are nearest to the passed 
   * rectangle.
   * 
   * @param  p The p point which this method finds
   *           the nearest neighbours.
   * 
   * @return List of IDs of rectangles that are nearest
   *         to the passed rectangle, ordered by distance (nearest first).
   */
  public List nearest(Point p, float furthestDistance) {
  	AddToListProcedure v = new AddToListProcedure();
    m_si.nearest(p, v, furthestDistance);	
    return v.getList();
  }
  
  /**
   * Finds all rectangles that are nearest to the passed 
   * rectangle.
   * 
   * @param  p The p point which this method finds
   *           the nearest neighbours.
   * 
   * @return List of IDs of rectangles that are nearest
   *         to the passed rectangle, ordered by distance (nearest first).
   *         If multiple rectangles have the same distance, order by ID.
   */
  public List nearestN(Point p, int maxCount, float furthestDistance) {
    AddToListProcedure v = new AddToListProcedure();
    m_si.nearestN(p, v, maxCount, furthestDistance); 
    return v.getList();
  }
  
  /**
   * Finds all rectangles that intersect the passed rectangle.
   * 
   * @param  r The rectangle for which this method finds
   *           intersecting rectangles.
   * 
   * @return List of IDs of rectangles that intersect the passed
   *         rectangle.
   */
  public List intersects(Rectangle r) {
  	AddToListProcedure v = new AddToListProcedure();
    m_si.intersects(r, v);	
    return v.getList();
  }
  
  /**
   * Finds all rectangles contained by the passed rectangle.
   * 
   * @param  r The rectangle for which this method finds
   *           contained rectangles.
   * 
   * @return Collection of IDs of rectangles that are contained by the
   *         passed rectangle.
   */
  public List contains(Rectangle r) {
  	AddToListProcedure v = new AddToListProcedure();
    m_si.contains(r, v);	
    return v.getList();
  }
 
}
