package com.infomatiq.jsi.test;

import gnu.trove.TIntProcedure;

import java.util.Properties;

import junit.framework.TestCase;

import com.infomatiq.jsi.Point;
import com.infomatiq.jsi.Rectangle;
import com.infomatiq.jsi.SpatialIndex;

public class ManualTest extends TestCase {

  public ManualTest(String s) {
    super(s);
  }
  
  class PrintProc implements TIntProcedure {
    public boolean execute(int value) {
      System.out.println(value);
      return true;
    }
    
  }
  
  public void testNearestN() {
    Properties p = new Properties();
    p.setProperty("MinNodeEntries", "1");
    p.setProperty("MaxNodeEntries", "10");
    SpatialIndex si = SpatialIndexFactory.newInstance("rtree.RTree", p);
    
    si.add(new Rectangle(0L, 0L, 10L, 10L), 0);
    si.add(new Rectangle(0L, 0L, 10L, 10L), 0);
    si.add(new Rectangle(0L, 0L, 10L, 10L), 0);
    si.add(new Rectangle(0L, 0L, 10L, 10L), 0);
    si.add(new Rectangle(0L, 0L, 10L, 10L), 0);
    
    si.nearestN(new Point(5L, 5L), new PrintProc(), 2, 100L);
  }

}
