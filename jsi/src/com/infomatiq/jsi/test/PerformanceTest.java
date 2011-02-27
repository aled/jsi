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

package com.infomatiq.jsi.test;

import java.util.Date;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * PerformanceTest
 *  
 * Generates results used for comparing the performance of the Java Spatial 
 * Index library against alternative implementations.
 * 
 * @author  aled@sourceforge.net
 * @version 1.0b8
 */
public class PerformanceTest extends SpatialIndexTest {

  private static final Logger log = Logger.getLogger(PerformanceTest.class.getName());
  
  public PerformanceTest(String s) {
    super(s);
  }
  
  // Tests add, intersect, nearest, nearestn, contains.
  // Can optimize for add performance, memory efficiency, or query performance.
  //
  public void xtestQueryPerformance() {
    Date currentDate = new Date();
    
    addPerformanceLog.info(currentDate);
    addPerformanceLog.info("IndexType,TestId,MinNodeEntries,MaxNodeEntries,TreeVariant,TreeSize,AddCount,AverageAddTime");
    
    intersectPerformanceLog.info(currentDate);
    intersectPerformanceLog.info("IndexType,TestId,MinNodeEntries,MaxNodeEntries,TreeVariant,TreeSize,QueryCount,AverageIntersectCount,AverageQueryTime");

    nearestPerformanceLog.info(currentDate);
    nearestPerformanceLog.info("IndexType,TestId,MinNodeEntries,MaxNodeEntries,TreeVariant,TreeSize,QueryCount,AverageNearestCount,AverageQueryTime");

    nearestNPerformanceLog.info(currentDate);
    nearestNPerformanceLog.info("IndexType,TestId,MinNodeEntries,MaxNodeEntries,TreeVariant,TreeSize,QueryCount,AverageNearestNCount,AverageQueryTime");

    containsPerformanceLog.info(currentDate);
    containsPerformanceLog.info("IndexType,TestId,MinNodeEntries,MaxNodeEntries,TreeVariant,TreeSize,QueryCount,AverageContainsCount,AverageQueryTime");
    
    deletePerformanceLog.info(currentDate);
    deletePerformanceLog.info("IndexType,TestId,MinNodeEntries,MaxNodeEntries,TreeVariant,FinalTreeSize,DeleteCount,AverageDeleteTime");
    
    // Test 1: add performance. 
    // To acheive maximum add performance, it is necessary to minimize the number of
    // node splits. Therefore set the MinNodeEntries to 1.
    // do each test 3 times to see if there is any variance due to hotspot VM (or something else).
    Properties p = new Properties();
    
    p.setProperty("MinNodeEntries", "1");
    p.setProperty("MaxNodeEntries", "10");
    p.setProperty("TreeVariant", "Linear");
  
    runScript("rtree.RTree",       p, "allqueries-10000", PERFORMANCE_TEST);
    runScript("rtree.RTree",       p, "allqueries-10000", PERFORMANCE_TEST);
    runScript("rtree.RTree",       p, "allqueries-10000", PERFORMANCE_TEST);
    
//    runScript("test.RTreeWrapper", p, "allqueries-10000", PERFORMANCE_TEST);  
//    runScript("test.RTreeWrapper", p, "allqueries-10000", PERFORMANCE_TEST);  
//    runScript("test.RTreeWrapper", p, "allqueries-10000", PERFORMANCE_TEST);  
//    
//    runScript("test.SILWrapper",   p, "allqueries-10000", PERFORMANCE_TEST);
//    runScript("test.SILWrapper",   p, "allqueries-10000", PERFORMANCE_TEST);
//    runScript("test.SILWrapper",   p, "allqueries-10000", PERFORMANCE_TEST);
//    
//    p.setProperty("TreeVariant", "Quadratic");
//    runScript("test.SILWrapper",   p, "allqueries-10000", PERFORMANCE_TEST);
//    runScript("test.SILWrapper",   p, "allqueries-10000", PERFORMANCE_TEST);
//    runScript("test.SILWrapper",   p, "allqueries-10000", PERFORMANCE_TEST);
//    
//    p.setProperty("TreeVariant", "Rstar");
//    runScript("test.SILWrapper",   p, "allqueries-10000", PERFORMANCE_TEST);
//    runScript("test.SILWrapper",   p, "allqueries-10000", PERFORMANCE_TEST);
//    runScript("test.SILWrapper",   p, "allqueries-10000", PERFORMANCE_TEST);
  }

  public void testNearestN() {
    Properties p = new Properties();
    p.setProperty("MinNodeEntries", "5");
    p.setProperty("MaxNodeEntries", "10");
    
    Date currentDate = new Date();
    nearestPerformanceLog.info(currentDate);
    nearestPerformanceLog.info("IndexType,TestId,MinNodeEntries,MaxNodeEntries,TreeVariant,TreeSize,QueryCount,AverageNearestCount,AverageQueryTime");
    
    runScript("rtree.RTree",       p, "nearestN-100", PERFORMANCE_TEST);
    runScript("rtree.RTree",       p, "nearestN-1000", PERFORMANCE_TEST);
    runScript("rtree.RTree",       p, "nearestN-10000", PERFORMANCE_TEST);
    
  }
  
  /**
   * Tests performance of all the RTree variants for add() and intersect(),
   * for up to 10,000,000 entries
   */
  public void XtestAllFunctions() {
    log.debug("testAllFunctions()");
    
    Date currentDate = new Date();
    intersectPerformanceLog.info(currentDate);
    intersectPerformanceLog.info("IndexType,TestId,MinNodeEntries,MaxNodeEntries,TreeVariant,TreeSize,QueryCount,AverageIntersectCount,AverageQueryTime");
    
    nearestPerformanceLog.info(currentDate);
    nearestPerformanceLog.info("IndexType,TestId,MinNodeEntries,MaxNodeEntries,TreeVariant,TreeSize,QueryCount,AverageNearestCount,AverageQueryTime");
    
    containsPerformanceLog.info(currentDate);
    containsPerformanceLog.info("IndexType,TestId,MinNodeEntries,MaxNodeEntries,TreeVariant,TreeSize,QueryCount,AverageContainsCount,AverageQueryTime");
    
    addPerformanceLog.info(currentDate);
    addPerformanceLog.info("IndexType,TestId,MinNodeEntries,MaxNodeEntries,TreeVariant,TreeSize,AddCount,AverageAddTime");
    
    deletePerformanceLog.info(currentDate);
    deletePerformanceLog.info("IndexType,TestId,MinNodeEntries,MaxNodeEntries,TreeVariant,TreeSize,DeleteCount,AverageDeleteTime");
    
    Properties p = new Properties();
       
    // SimpleIndex and NullIndex do not use Min/MaxNodeEntries, so do them first.
    runScript("test.SimpleIndex", p, "allfunctions-100", PERFORMANCE_TEST);
    runScript("test.SimpleIndex", p, "allfunctions-1000", PERFORMANCE_TEST);
    runScript("test.SimpleIndex", p, "allfunctions-10000", PERFORMANCE_TEST);
    // Only go up to 10000 for simple index, as it takes too int
    
    p.setProperty("TreeVariant", "null");
    runScript("test.NullIndex",   p, "allfunctions-100", PERFORMANCE_TEST);
    runScript("test.NullIndex",   p, "allfunctions-1000", PERFORMANCE_TEST);
    runScript("test.NullIndex",   p, "allfunctions-10000", PERFORMANCE_TEST);
    //runScript("test.NullIndex",   p, "allfunctions-100000", PERFORMANCE_TEST);
    
    p.setProperty("MinNodeEntries", "5");
    p.setProperty("MaxNodeEntries", "20");  // reasonable values?
    
    p.setProperty("TreeVariant", "Linear");
    runScript("test.RTreeWrapper", p, "allfunctions-100", PERFORMANCE_TEST);
    runScript("test.RTreeWrapper", p, "allfunctions-1000", PERFORMANCE_TEST);
    runScript("test.RTreeWrapper", p, "allfunctions-10000", PERFORMANCE_TEST);
    //runScript("test.RTreeWrapper", p, "allfunctions-100000", PERFORMANCE_TEST);
      
    p.setProperty("TreeVariant", "Linear");
    runScript("rtree.RTree",       p, "allfunctions-100", PERFORMANCE_TEST);
    runScript("rtree.RTree",       p, "allfunctions-1000", PERFORMANCE_TEST);
    runScript("rtree.RTree",       p, "allfunctions-10000", PERFORMANCE_TEST);
    //runScript("rtree.RTree",       p, "allfunctions-100000", PERFORMANCE_TEST);
      
    p.setProperty("TreeVariant", "Linear");
    runScript("test.SILWrapper",   p, "allfunctions-100", PERFORMANCE_TEST);
    runScript("test.SILWrapper",   p, "allfunctions-1000", PERFORMANCE_TEST);
    runScript("test.SILWrapper",   p, "allfunctions-10000", PERFORMANCE_TEST);
    //runScript("test.SILWrapper",   p, "allfunctions-100000", PERFORMANCE_TEST);
      
    p.setProperty("TreeVariant", "Quadratic");
    runScript("test.SILWrapper",   p, "allfunctions-100", PERFORMANCE_TEST);
    runScript("test.SILWrapper",   p, "allfunctions-1000", PERFORMANCE_TEST);
    runScript("test.SILWrapper",   p, "allfunctions-10000", PERFORMANCE_TEST);
    //runScript("test.SILWrapper",   p, "allfunctions-100000", PERFORMANCE_TEST);
      
    p.setProperty("TreeVariant", "Rstar");
    runScript("test.SILWrapper",   p, "allfunctions-100", PERFORMANCE_TEST);
    runScript("test.SILWrapper",   p, "allfunctions-1000", PERFORMANCE_TEST);
    runScript("test.SILWrapper",   p, "allfunctions-10000", PERFORMANCE_TEST);
    //runScript("test.SILWrapper",   p, "allfunctions-100000", PERFORMANCE_TEST);
  }
}
