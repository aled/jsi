//   PerformanceTest.java
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

import java.util.Properties;

import junit.framework.TestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PerformanceTest
 *  
 * Generates results used for comparing the performance of the Java Spatial 
 * Index library against alternative implementations.
 */
public class PerformanceTest extends TestCase {

  private static final Logger log = LoggerFactory.getLogger(PerformanceTest.class);
  private Script script = new Script();
  
  public PerformanceTest(String s) {
    super(s);
  }
  
  // Tests add, intersect, nearest, nearestn, contains.
  // Can optimize for add performance, memory efficiency, or query performance.
  //
  public void testQueryPerformance() {
    
    // Test 1: add performance. 
    // To acheive maximum add performance, it is necessary to minimize the number of
    // node splits. Therefore set the MinNodeEntries to 1.
    // do each test 3 times to see if there is any variance due to hotspot VM (or something else).
    Properties p = new Properties();
    
    p.setProperty("MinNodeEntries", "1");
    p.setProperty("MaxNodeEntries", "10");
    p.setProperty("TreeVariant", "Linear");
  
    script.run("rtree.RTree", p, "allqueries-10000", Script.PERFORMANCE);
    script.run("rtree.RTree", p, "allqueries-10000", Script.PERFORMANCE);
    script.run("rtree.RTree", p, "allqueries-10000", Script.PERFORMANCE);
    
//    script.run("test.RTreeWrapper", p, "allqueries-10000", Script.PERFORMANCE);  
//    script.run("test.RTreeWrapper", p, "allqueries-10000", Script.PERFORMANCE);  
//    script.run("test.RTreeWrapper", p, "allqueries-10000", Script.PERFORMANCE);  
//    
//    script.run("SILWrapper",   p, "allqueries-10000", Script.PERFORMANCE);
//    script.run("SILWrapper",   p, "allqueries-10000", Script.PERFORMANCE);
//    script.run("SILWrapper",   p, "allqueries-10000", Script.PERFORMANCE);
//    
//    p.setProperty("TreeVariant", "Quadratic");
//    script.run("SILWrapper",   p, "allqueries-10000", Script.PERFORMANCE);
//    script.run("SILWrapper",   p, "allqueries-10000", Script.PERFORMANCE);
//    script.run("SILWrapper",   p, "allqueries-10000", Script.PERFORMANCE);
//    
//    p.setProperty("TreeVariant", "Rstar");
//    script.run("SILWrapper",   p, "allqueries-10000", Script.PERFORMANCE);
//    script.run("SILWrapper",   p, "allqueries-10000", Script.PERFORMANCE);
//    script.run("SILWrapper",   p, "allqueries-10000", Script.PERFORMANCE);
  }

  public void testNearestN() {
    Properties p = new Properties();
    p.setProperty("MinNodeEntries", "5");
    p.setProperty("MaxNodeEntries", "10");
     
    script.run("rtree.RTree",       p, "nearestN-100", Script.PERFORMANCE);
    script.run("rtree.RTree",       p, "nearestN-1000", Script.PERFORMANCE);
    script.run("rtree.RTree",       p, "nearestN-10000", Script.PERFORMANCE);    
  }
  
  /**
   * Tests performance of all the RTree variants for add() and intersect(),
   * for up to 10,000,000 entries
   */
  public void testAllFunctions() {
    log.debug("testAllFunctions()");
    
    Properties p = new Properties();
       
    // SimpleIndex and NullIndex do not use Min/MaxNodeEntries, so do them first.
    script.run("SimpleIndex", p, "allfunctions-100", Script.PERFORMANCE);
    script.run("SimpleIndex", p, "allfunctions-1000", Script.PERFORMANCE);
    script.run("SimpleIndex", p, "allfunctions-10000", Script.PERFORMANCE);
    // Only go up to 10000 for simple index, as it takes too int
    
    p.setProperty("TreeVariant", "null");
    script.run("NullIndex",   p, "allfunctions-100", Script.PERFORMANCE);
    script.run("NullIndex",   p, "allfunctions-1000", Script.PERFORMANCE);
    script.run("NullIndex",   p, "allfunctions-10000", Script.PERFORMANCE);
    //script.run("NullIndex",   p, "allfunctions-100000", Script.PERFORMANCE);
    
    p.setProperty("MinNodeEntries", "5");
    p.setProperty("MaxNodeEntries", "20");  // reasonable values?
    
    p.setProperty("TreeVariant", "Linear");
    script.run("RTreeWrapper", p, "allfunctions-100", Script.PERFORMANCE);
    script.run("RTreeWrapper", p, "allfunctions-1000", Script.PERFORMANCE);
    script.run("RTreeWrapper", p, "allfunctions-10000", Script.PERFORMANCE);
    //script.run("RTreeWrapper", p, "allfunctions-100000", Script.PERFORMANCE);
      
    p.setProperty("TreeVariant", "Linear");
    script.run("rtree.RTree",       p, "allfunctions-100", Script.PERFORMANCE);
    script.run("rtree.RTree",       p, "allfunctions-1000", Script.PERFORMANCE);
    script.run("rtree.RTree",       p, "allfunctions-10000", Script.PERFORMANCE);
    //script.run("rtree.RTree",       p, "allfunctions-100000", Script.PERFORMANCE);
      
    p.setProperty("TreeVariant", "Linear");
    script.run("SILWrapper",   p, "allfunctions-100", Script.PERFORMANCE);
    script.run("SILWrapper",   p, "allfunctions-1000", Script.PERFORMANCE);
    script.run("SILWrapper",   p, "allfunctions-10000", Script.PERFORMANCE);
    //script.run("SILWrapper",   p, "allfunctions-100000", Script.PERFORMANCE);
      
    p.setProperty("TreeVariant", "Quadratic");
    script.run("SILWrapper",   p, "allfunctions-100", Script.PERFORMANCE);
    script.run("SILWrapper",   p, "allfunctions-1000", Script.PERFORMANCE);
    script.run("SILWrapper",   p, "allfunctions-10000", Script.PERFORMANCE);
    //script.run("SILWrapper",   p, "allfunctions-100000", Script.PERFORMANCE);
      
    p.setProperty("TreeVariant", "Rstar");
    script.run("SILWrapper",   p, "allfunctions-100", Script.PERFORMANCE);
    script.run("SILWrapper",   p, "allfunctions-1000", Script.PERFORMANCE);
    script.run("SILWrapper",   p, "allfunctions-10000", Script.PERFORMANCE);
    //script.run("SILWrapper",   p, "allfunctions-100000", Script.PERFORMANCE);
  }
}
