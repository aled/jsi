//   SpatialIndex.java
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

package com.infomatiq.jsi;

import java.util.Properties;

/**
 * Defines methods that must be implemented by all 
 * spatial indexes. This includes the RTree and its variants.
 * 
 * @author  aled.morris@infomatiq.co.uk
 * @version 1.0b2
 */
public interface SpatialIndex {
  
  /**
   * Initializes any implementation dependent properties
   * of the spatial index. For example, RTree implementations
   * will have a NodeSize property.
   * 
   * @param props The set of properties used to initialize the spatial index.
   */
  public void init(Properties props);
  
  /**
   * Adds a new rectangle to the spatial index
   * 
   * @param r  The rectangle to add to the spatial index.
   * @param id The ID of the rectangle to add to the spatial index.
   *           The result of adding more than one rectangle with
   *           the same ID is undefined.
   */ 
  public void add(Rectangle r, int id);
  
  /**
   * Deletes a rectangle from the spatial index
   * 
   * @param r  The rectangle to delete from the spatial index
   * @param id The ID of the rectangle to delete from the spatial
   *           index
   * 
   * @return true  if the rectangle was deleted
   *         false if the rectangle was not found, or the 
   *               rectangle was found but with a different ID
   */
  public boolean delete(Rectangle r, int id);
   
  /**
   * Finds all rectangles that are nearest to the passed rectangle, and calls 
   * execute() on the passed IntProcedure for each one. 
   * 
   * @param p The point for which this method finds the
   * nearest neighbours.
   * 
   * @param ip The IntProcedure whose execute() method is is called
   * for each nearest neighbour.
   * 
   * @param distance The furthest distance away from the rectangle
   * to search. Rectangles further than this will not be found. 
   * 
   * This should be as small as possible to minimise
   * the search time.
   *                         
   * Use Float.POSITIVE_INFINITY to guarantee that the nearest rectangle is found,
   * no matter how far away, although this will slow down the algorithm.
   */
  public void nearest(Point p, IntProcedure v, float distance);
  
  /**
   * Finds all rectangles that intersect the passed rectangle.
   * 
   * @param  r The rectangle for which this method finds
   *           intersecting rectangles.
   * 
   * @param ip The IntProcedure whose execute() method is is called
   *           for each intersecting rectangle.
   */
  public void intersects(Rectangle r, IntProcedure ip);  

  /**
   * Finds all rectangles contained by the passed rectangle.
   * 
   * @param r The rectangle for which this method finds
   *           contained rectangles.
   * 
   * @param v The visitor whose visit() method is is called
   *           for each contained rectangle.
   */
  public void contains(Rectangle r, IntProcedure ip); 
  
  /**
   * Returns the number of entries in the spatial index
   */
  public int size();
  
  
  /**
   * Returns the bounds of all the entries in the spatial index,
   * or null if there are no entries.
   */
  public Rectangle getBounds();
  
  /**
   * Returns a string identifying the type of
   * spatial index, and the version number, 
   * eg "SimpleIndex-0.1"
   */
  public String getVersion();
  
}
