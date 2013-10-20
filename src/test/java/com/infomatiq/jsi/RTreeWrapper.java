//   RTreeWrapper.java
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

package com.infomatiq.jsi;

import com.infomatiq.jsi.rtree.RTree;
import gnu.trove.procedure.TIntProcedure;

import java.util.Properties;

/**
 * A completely useless wrapper class for the RTree class.
 * 
 * Actually the point to introduce the same overhead as 
 * the SILWrapper class, so that performance comparisons
 * can be made.
 */
public class RTreeWrapper implements SpatialIndex {
  private RTree tree;
  
  class IntProcedure2 implements TIntProcedure {
    private TIntProcedure m_intProcedure = null;
    
    public IntProcedure2(TIntProcedure ip) {
      m_intProcedure = ip;
    }
    
    public boolean execute(int i) {
      return m_intProcedure.execute(i);
    }
  }
  
  /**
   * @see com.infomatiq.jsi.SpatialIndex#init(Properties)
   */
  public void init(Properties props) {
    // create a memory-based storage manager
    
    tree = new RTree();
    tree.init(props);
  }

  /**
   * @see com.infomatiq.jsi.SpatialIndex#nearest(Point, gnu.trove.procedure.TIntProcedure, double)
   */
  public void nearest(Point p, TIntProcedure v, double furthestDistance) {
    tree.nearest(new Point(p.x, p.y),
                 new IntProcedure2(v),
                 Double.POSITIVE_INFINITY);
  }
  
  /**
   * @see com.infomatiq.jsi.SpatialIndex#nearestN(Point, gnu.trove.procedure.TIntProcedure, int, double)
   */
  public void nearestN(Point p, TIntProcedure v, int n, double furthestDistance) {
    tree.nearestN(new Point(p.x, p.y),
                 new IntProcedure2(v),
                 n,
                 furthestDistance);
  }

  /**
   * @see com.infomatiq.jsi.SpatialIndex#nearestNUnsorted(Point, gnu.trove.procedure.TIntProcedure, int, double)
   */
  public void nearestNUnsorted(Point p, TIntProcedure v, int n, double furthestDistance) {
    tree.nearestNUnsorted(new Point(p.x, p.y),
                 new IntProcedure2(v),
                 n,
                 furthestDistance);
  }
  
  /**
   * @see com.infomatiq.jsi.SpatialIndex#intersects(Rectangle, gnu.trove.procedure.TIntProcedure)
   */
  public void intersects(Rectangle r, TIntProcedure ip) {
    Rectangle r2 = new Rectangle(r.minX, r.minY, r.maxX, r.maxY);  
    tree.intersects(r2, new IntProcedure2(ip));
  }

  /**
   * @see com.infomatiq.jsi.SpatialIndex#contains(Rectangle, gnu.trove.procedure.TIntProcedure)
   */
  public void contains(Rectangle r, TIntProcedure ip) {
    Rectangle r2 = new Rectangle(r.minX, r.minY, r.maxX, r.maxY);
    tree.contains(r2, new IntProcedure2(ip));
  }

  /**
   * @see com.infomatiq.jsi.SpatialIndex#add(Rectangle, int)
   */
  public void add(Rectangle r, int id) {
    Rectangle r2 = new Rectangle(r.minX, r.minY, r.maxX, r.maxY);
    tree.add(r2, id);
  }

  /**
   * @see com.infomatiq.jsi.SpatialIndex#delete(Rectangle, int)
   */
  public boolean delete(Rectangle r, int id) {
    Rectangle r2 = new Rectangle(r.minX, r.minY, r.maxX, r.maxY);
    return tree.delete(r2, id);
  }

  /**
   * @see com.infomatiq.jsi.SpatialIndex#size()
   */
  public int size() {
    return tree.size();  
  }
  
  /**
   * @see com.infomatiq.jsi.SpatialIndex#getBounds()
   */
  public Rectangle getBounds() {
    return tree.getBounds(); 
  }
  
  /**
   * @see com.infomatiq.jsi.SpatialIndex#getVersion()
   */
  public String getVersion() {
    return "RTreeWrapper-" + BuildProperties.getVersion();
  }
  
}
