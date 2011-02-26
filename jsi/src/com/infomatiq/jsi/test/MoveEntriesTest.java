//   MoveEntriesTest.java
//   Java Spatial Index Library
//   Copyright (C) 2011 Aled Morris <aled@sourceforge.net>
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

import java.util.Properties;
import java.util.Random;

import com.infomatiq.jsi.Rectangle;
import com.infomatiq.jsi.rtree.RTree;

import junit.framework.TestCase;

public class MoveEntriesTest extends TestCase {

  Random r = new Random();

  public MoveEntriesTest(String name) {
    super(name);
    r.setSeed(0);
  }

  private Rectangle nextRect() {
    return new Rectangle(r.nextInt(100), r.nextInt(100), r.nextInt(100), r.nextInt(100));
  }

  public void testMoveEntries() {
    System.out.println("testMoveEntries");
    run(4, 50, 4, 10);
  }

  private void run(int minNodeEntries, int maxNodeEntries, int numRects, int numMoves) {
    Properties p = new Properties();
    p.setProperty("MinNodeEntries", Integer.toString(minNodeEntries));
    p.setProperty("MaxNodeEntries", Integer.toString(maxNodeEntries));
    RTree rtree = (RTree) SpatialIndexFactory.newInstance("rtree.RTree", p);

    Rectangle[] rects = new Rectangle[numRects];

    // first add the rects
    for (int i = 0; i < numRects; i++) {
      rects[i] = nextRect();
      rtree.add(rects[i], i);
    }

    // now move each one in turn
    for (int move = 0; move < numMoves; move++) {
      for (int i = 0; i < numRects; i++) {
        rtree.delete(rects[i], i);
        rects[i].set(nextRect());
        rtree.add(rects[i], i);
        assertTrue(rtree.checkConsistency());
      }
    }
  }
}
