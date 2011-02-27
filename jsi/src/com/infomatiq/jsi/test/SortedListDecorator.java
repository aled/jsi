//   SortedListDecorator.java
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

import java.util.Collections;
import java.util.List;

import com.infomatiq.jsi.Point;
import com.infomatiq.jsi.Rectangle;
import com.infomatiq.jsi.SpatialIndex;

/**
 * SortedListDecorator
 * 
 * @author aled@sourceforge.net
 * @version 1.0b8
 */
public class SortedListDecorator extends ListDecorator {
   
  public SortedListDecorator(SpatialIndex si) {
    super(si);
  }
   
  private List sort(List l) {
    Collections.sort(l);
   	return l;
  }
  
  public List nearestN(Point p, int maxCount, float furthestDistance) {
    return sort(super.nearestN(p, maxCount, furthestDistance));
  }
   
  public List nearest(Point p, float furthestDistance) {
    return sort(super.nearest(p, furthestDistance));
  }
   
  public List intersects(Rectangle r) {
    return sort(super.intersects(r));
  }
  	 	
  public List contains(Rectangle r) {
  	return sort(super.contains(r));
  }
}
