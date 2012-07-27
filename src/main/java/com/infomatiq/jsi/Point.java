//   Point.java
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

/**
 * Currently hardcoded to 2 dimensions, but could be extended.
 *
 * @author  aled@sourceforge.net
 * @version 1.0
 */
public class Point {
  /**
   * The (x, y) coordinates of the point.
   */
  public float x, y;

  /**
   * Constructor.
   *
   * @param x The x coordinate of the point
   * @param y The y coordinate of the point
   */
  public Point(float x, float y) {
    this.x = x;
    this.y = y;
  }

  /**
   * Copy from another point into this one
   */
  public void set(Point other) {
    x = other.x;
    y = other.y;
  }

  /**
   * Print as a string in format "(x, y)"
   */
  @Override
  public String toString() {
    return "(" + x + ", " + y + ")";
  }

  /**
   * @return X coordinate rounded to an int
   */
  public int xInt() {
    return Math.round(x);
  }

  /**
   * @return Y coordinate rounded to an int
   */
  public int yInt() {
    return Math.round(y);
  }
}
