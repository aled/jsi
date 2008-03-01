//   Rectangle.java
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
//  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA

package com.infomatiq.jsi;

import java.util.Arrays;

/**
 * Currently hardcoded to 2 dimensions, but could be extended.
 * 
 * @author  aled.morris@infomatiq.co.uk
 * @version 1.0b2
 */
public class Rectangle {
  /**
   * Number of dimensions in a rectangle. In theory this
   * could be exended to three or more dimensions.
   */
  public final static int DIMENSIONS = 2;
  
  /**
   * array containing the minimum value for each dimension; ie { min(x), min(y) }
   */
  public float[] max;
  
  /**
   * array containing the maximum value for each dimension; ie { max(x), max(y) }
   */
  public float[] min;

  /**
   * Constructor.
   * 
   * @param x1 coordinate of any corner of the rectangle
   * @param y1 (see x1)
   * @param x2 coordinate of the opposite corner
   * @param y2 (see x2)
   */
  public Rectangle(float x1, float y1, float x2, float y2) {
    min = new float[DIMENSIONS];
    max = new float[DIMENSIONS]; 
    set(x1, y1, x2, y2);
  }

  /**
   * Constructor.
   * 
   * @param min array containing the minimum value for each dimension; ie { min(x), min(y) }
   * @param max array containing the maximum value for each dimension; ie { max(x), max(y) }
   */  
  public Rectangle(float[] min, float[] max) {
    if (min.length != DIMENSIONS || max.length != DIMENSIONS) {
      throw new RuntimeException("Error in Rectangle constructor: " +
                "min and max arrays must be of length " + DIMENSIONS); 
    }
   
    this.min = new float[DIMENSIONS];
    this.max = new float[DIMENSIONS];
    
    set(min, max);
  }
  
 /**
   * Sets the size of the rectangle.
   * 
   * @param x1 coordinate of any corner of the rectangle
   * @param y1 (see x1)
   * @param x2 coordinate of the opposite corner
   * @param y2 (see x2)
   */
  public void set(float x1, float y1, float x2, float y2) {
    min[0] = Math.min(x1, x2);
    min[1] = Math.min(y1, y2);
    max[0] = Math.max(x1, x2);        
    max[1] = Math.max(y1, y2); 
  }
  
  /**
   * Sets the size of the rectangle.
   * 
   * @param min array containing the minimum value for each dimension; ie { min(x), min(y) }
   * @param max array containing the maximum value for each dimension; ie { max(x), max(y) }
   */
  public void set(float[] min, float[] max) {
    System.arraycopy(min, 0, this.min, 0, DIMENSIONS);
    System.arraycopy(max, 0, this.max, 0, DIMENSIONS);
  }
  
  /**
   * Make a copy of this rectangle
   * 
   * @return copy of this rectangle
   */
  public Rectangle copy() {
    return new Rectangle(min, max); 
  }
  
  /**
   * Determine whether an edge of this rectangle overlies the equivalent 
   * edge of the passed rectangle
   */
  public boolean edgeOverlaps(Rectangle r) {
    for (int i = 0; i < DIMENSIONS; i++) {
      if (min[i] == r.min[i] || max[i] == r.max[i]) {
        return true; 
      } 
    }  
    return false;
  }
  
  /**
   * Determine whether this rectangle intersects the passed rectangle
   * 
   * @param r The rectangle that might intersect this rectangle
   * 
   * @return true if the rectangles intersect, false if they do not intersect
   */
  public boolean intersects(Rectangle r) {
    // Every dimension must intersect. If any dimension
    // does not intersect, return false immediately.
    for (int i = 0; i < DIMENSIONS; i++) {
      if (max[i] < r.min[i] || min[i] > r.max[i]) {
        return false;
      }
    }
    return true;
  }
 
  /**
   * Determine whether this rectangle contains the passed rectangle
   * 
   * @param r The rectangle that might be contained by this rectangle
   * 
   * @return true if this rectangle contains the passed rectangle, false if
   *         it does not
   */
  public boolean contains(Rectangle r) {
    for (int i = 0; i < DIMENSIONS; i++) {
      if (max[i] < r.max[i] || min[i] > r.min[i]) {
        return false;
      }
    }
    return true;     
  }
 
  /**
   * Determine whether this rectangle is contained by the passed rectangle
   * 
   * @param r The rectangle that might contain this rectangle
   * 
   * @return true if the passed rectangle contains this rectangle, false if
   *         it does not
   */
  public boolean containedBy(Rectangle r) {
    for (int i = 0; i < DIMENSIONS; i++) {
      if (max[i] > r.max[i] || min[i] < r.min[i]) {
        return false;
      }
    }
    return true;  
  }
  
  /**
   * Return the distance between this rectangle and the passed point.
   * If the rectangle contains the point, the distance is zero.
   * 
   * @param p Point to find the distance to
   * 
   * @return distance beween this rectangle and the passed point.
   */
  public float distance(Point p) {
    float distanceSquared = 0;
    for (int i = 0; i < DIMENSIONS; i++) {
      float greatestMin = Math.max(min[i], p.coordinates[i]);
      float leastMax    = Math.min(max[i], p.coordinates[i]);
      if (greatestMin > leastMax) {
        distanceSquared += ((greatestMin - leastMax) * (greatestMin - leastMax)); 
      }
    }
    return (float) Math.sqrt(distanceSquared);
  }
  
  /**
   * Return the distance between this rectangle and the passed rectangle.
   * If the rectangles overlap, the distance is zero.
   * 
   * @param r Rectangle to find the distance to
   * 
   * @return distance between this rectangle and the passed rectangle
   */

  public float distance(Rectangle r) {
    float distanceSquared = 0;
    for (int i = 0; i < DIMENSIONS; i++) {
      float greatestMin = Math.max(min[i], r.min[i]);
      float leastMax    = Math.min(max[i], r.max[i]);
      if (greatestMin > leastMax) {
        distanceSquared += ((greatestMin - leastMax) * (greatestMin - leastMax)); 
      }
    }
    return (float) Math.sqrt(distanceSquared);
  }
   
  /**
   * Return the squared distance from this rectangle to the passed point
   */
  private float distanceSquared(int dimension, float point) {
    float distanceSquared = 0;
    float tempDistance = point - max[dimension];
    for (int i = 0; i < 2; i++) {
      if (tempDistance > 0) {
        distanceSquared = (tempDistance * tempDistance);
        break;
      } 
      tempDistance = min[dimension] - point;
    }
    return distanceSquared;
  }
  
  /**
   * Return the furthst possible distance between this rectangle and
   * the passed rectangle. 
   * 
   * Find the distance between this rectangle and each corner of the
   * passed rectangle, and use the maximum.
   *
   */
  public float furthestDistance(Rectangle r) {
     float distanceSquared = 0;
     
     for (int i = 0; i < DIMENSIONS; i++) {
       distanceSquared += Math.max(distanceSquared(i, r.min[i]), distanceSquared(i, r.max[i]));
     }
     
     return (float) Math.sqrt(distanceSquared);
  }
  
  /**
   * Calculate the area by which this rectangle would be enlarged if
   * added to the passed rectangle. Neither rectangle is altered.
   * 
   * @param r Rectangle to union with this rectangle, in order to 
   *          compute the difference in area of the union and the
   *          original rectangle
   */
  public float enlargement(Rectangle r) {
    float enlargedArea = (Math.max(max[0], r.max[0]) - Math.min(min[0], r.min[0])) *
                         (Math.max(max[1], r.max[1]) - Math.min(min[1], r.min[1]));
                         
    return enlargedArea - area();
  }
  
  /**
   * Compute the area of this rectangle.
   * 
   * @return The area of this rectangle
   */
  public float area() {
    return (max[0] - min[0]) * (max[1] - min[1]);
  }
  
  /**
   * Computes the union of this rectangle and the passed rectangle, storing
   * the result in this rectangle.
   * 
   * @param r Rectangle to add to this rectangle
   */
  public void add(Rectangle r) {
    for (int i = 0; i < DIMENSIONS; i++) {
      if (r.min[i] < min[i]) {
        min[i] = r.min[i];
      }
      if (r.max[i] > max[i]) {
        max[i] = r.max[i];
      }
    }
  }
  
  /**
   * Find the the union of this rectangle and the passed rectangle.
   * Neither rectangle is altered
   * 
   * @param r The rectangle to union with this rectangle
   */
  public Rectangle union(Rectangle r) {
    Rectangle union = this.copy();
    union.add(r);
    return union; 
  }
  
  /**
   * Determine whether this rectangle is equal to a given object.
   * Equality is determined by the bounds of the rectangle.
   * 
   * @param The object to compare with this rectangle
   */
  public boolean equals(Object o) {
    boolean equals = false;
    if (o instanceof Rectangle) {
      Rectangle r = (Rectangle) o;
      if (Arrays.equals(r.min, min) && Arrays.equals(r.max, max)) {
        equals = true;
      }
    } 
    return equals;       
  }

  /** 
   * Determine whether this rectangle is the same as another object
   * 
   * Note that two rectangles can be equal but not the same object, 
   * if they both have the same bounds.
   * 
   * @param o The object to compare with this rectangle.
   */  
  public boolean sameObject(Object o) {
    return super.equals(o); 
  }
  
  /**
   * Return a string representation of this rectangle, in the form: 
   * (1.2, 3.4), (5.6, 7.8)
   * 
   * @return String String representation of this rectangle.
   */
  public String toString() {
    StringBuffer sb = new StringBuffer();
    
    // min coordinates
    sb.append('(');
    for (int i = 0; i < DIMENSIONS; i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(min[i]);
    } 
    sb.append("), (");
    
    // max coordinates
    for (int i = 0; i < DIMENSIONS; i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(max[i]);
    } 
    sb.append(')');
    return sb.toString();
  }
}
