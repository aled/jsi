//   ReferenceTest.java
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

import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * ReferenceTest
 * 
 * Generates reference results used for testing the Java Spatial Index.
 * Reference results are generated using alternative spatial index
 * implementations, specifically SimpleIndex and Spatial Index Library.
 * 
 * @author  aled@sourceforge.net
 * @version 1.0b8
 */
public class ReferenceGenerateTest extends SpatialIndexTest {

  private static final Logger log = Logger.getLogger(ReferenceGenerateTest.class.getName());
  
  protected int entriesToTest = 100;
  
  public ReferenceGenerateTest(String s) {
    super(s);
  }
  
  public void testReferenceGenerateAllFunctions() {
    log.debug("testReferenceGenerateAllFunctions()");
    
    Properties p = new Properties();
    
    if (entriesToTest >= 100) {
      log.info("Creating reference test results (all functions) for 100 entries.");
      runScript("test.SimpleIndex", p, "allfunctions-100", REFERENCE_GENERATE);
    }
  
    if (entriesToTest >= 1000) {
      log.info("Creating reference test results (all functions) for 1000 entries.");
      runScript("test.SimpleIndex", p, "allfunctions-1000", REFERENCE_GENERATE);
    }
    
    if (entriesToTest >= 10000) {
      log.info("Creating reference test results (all functions) for 10,000 entries.");
      runScript("test.SimpleIndex", p, "allfunctions-10000", REFERENCE_GENERATE);
    }

    // Use SIL library to create test results for 100000+ entries
    p.setProperty("MinNodeEntries", "1");
    p.setProperty("MaxNodeEntries", "13"); // different to other tests
    p.setProperty("TreeVariant", "Rstar");

    if (entriesToTest >= 100000) {    
      log.info("Creating reference test results (all functions) for 100,000 entries.");
      runScript("test.SILWrapper", p, "allfunctions-100000", REFERENCE_GENERATE);
    }
  }
  
  public void testReferenceGenerateDelete() {
    log.debug("testReferenceGenerateDelete()");
   
    Properties p = new Properties();
    
    if (entriesToTest >= 100) {
      log.info("Creating reference testDelete results for 100 entries.");
      runScript("test.SimpleIndex", p, "delete-100", REFERENCE_GENERATE);
    }
    
    if (entriesToTest >= 1000) {
      log.info("Creating reference testDelete results for 1000 entries.");
      runScript("test.SimpleIndex", p, "delete-1000", REFERENCE_GENERATE);
    }
    
    if (entriesToTest >= 10000) {
      log.info("Creating reference testDelete results for 10,000 entries.");
      runScript("test.SimpleIndex", p, "delete-10000", REFERENCE_GENERATE);
    }

    // Use SIL library to create test results for 100000+ entries
    p.setProperty("MinNodeEntries", "1");
    p.setProperty("MaxNodeEntries", "13"); // different to other tests
    p.setProperty("TreeVariant", "Rstar");

    if (entriesToTest >= 100000) {
      log.info("Creating reference testDelete results for 100,000 entries.");
      runScript("test.SILWrapper", p, "delete-100000", REFERENCE_GENERATE);
    }
  }
  
  public void testReferenceGenerateIntersect() {
    log.debug("testReferenceGenerateIntersect()");
    
    Properties p = new Properties();
    
    if (entriesToTest >= 100) {
      log.info("Creating reference testIntersect results for 100 entries.");
      runScript("test.SimpleIndex",  p, "intersect-100", REFERENCE_GENERATE);
    }

    if (entriesToTest >= 1000) {
      log.info("Creating reference testIntersect results for 1000 entries.");
      runScript("test.SimpleIndex",  p, "intersect-1000", REFERENCE_GENERATE);
    }
    
    if (entriesToTest >= 10000) {
      log.info("Creating reference testIntersect results for 10,000 entries.");
      runScript("test.SimpleIndex",  p, "intersect-10000", REFERENCE_GENERATE);
    }

    // Use SIL library to create test results for 100000+ entries
    p.setProperty("MinNodeEntries", "1");
    p.setProperty("MaxNodeEntries", "13"); // different to other tests
    p.setProperty("TreeVariant", "Rstar");
      
    if (entriesToTest >= 100000) {
      log.info("Creating reference testIntersect results for 100,000 entries.");
      runScript("test.SILWrapper",  p, "intersect-100000", REFERENCE_GENERATE);
    }
  }
  
  public void testReferenceGenerateNearest() {
    log.debug("testReferenceGenerateNearest()");
    
    Properties p = new Properties();
    
    if (entriesToTest >= 100) {
      log.info("Creating reference testNearest results for 100 entries.");
      runScript("test.SimpleIndex",  p, "nearest-100", REFERENCE_GENERATE);
    }
  
    if (entriesToTest >= 1000) {
      log.info("Creating reference testNearest results for 1000 entries.");
      runScript("test.SimpleIndex",  p, "nearest-1000", REFERENCE_GENERATE);
    }
    
    if (entriesToTest >= 10000) {
      log.info("Creating reference testNearest results for 10,000 entries.");
      runScript("test.SimpleIndex",  p, "nearest-10000", REFERENCE_GENERATE);
    }
    
    // Use SIL library to create test results for 100000+ entries
    p.setProperty("MinNodeEntries", "1");
    p.setProperty("MaxNodeEntries", "13"); // different to other tests
    p.setProperty("TreeVariant", "Rstar");
    
    if (entriesToTest >= 100000) {
      log.info("Creating reference testIntersect results for 100,000 entries.");
      runScript("test.SILWrapper",   p, "nearest-100000", REFERENCE_GENERATE);
    }
  }
  
  public void testReferenceGenerateNearestN() {
    log.debug("testReferenceGenerateNearestN()");
    
    Properties p = new Properties();
    
    if (entriesToTest >= 100) {
      log.info("Creating reference testNearestN results for 100 entries.");
      runScript("test.SimpleIndex",  p, "nearestN-100", REFERENCE_GENERATE);
    }
  
    if (entriesToTest >= 1000) {
      log.info("Creating reference testNearestN results for 1000 entries.");
      runScript("test.SimpleIndex",  p, "nearestN-1000", REFERENCE_GENERATE);
    }

    if (entriesToTest >= 10000) {
      log.info("Creating reference testNearestN results for 10,000 entries.");
      runScript("test.SimpleIndex",  p, "nearestN-10000", REFERENCE_GENERATE);
    }        
  
    // Use SIL library to create test results for 100000+ entries
    p.setProperty("MinNodeEntries", "1");
    p.setProperty("MaxNodeEntries", "13"); // different to other tests
    p.setProperty("TreeVariant", "Rstar");

    if (entriesToTest >= 100000) {
      log.info("Creating reference testIntersect results for 100,000 entries.");
      runScript("test.SILWrapper",   p, "nearest-100000", REFERENCE_GENERATE);
    }
  }
  
  public void testReferenceGenerateContains() {
    log.debug("testReferenceGenerateContains()");
    
    Properties p = new Properties();
    
    if (entriesToTest >= 100) {
      log.info("Creating reference testIntersect results for 100 entries.");
      runScript("test.SimpleIndex",  p, "contains-100", REFERENCE_GENERATE);
    }
    
    if (entriesToTest >= 1000) {
      log.info("Creating reference testIntersect results for 1000 entries.");
      runScript("test.SimpleIndex",  p, "contains-1000", REFERENCE_GENERATE);
    }

    if (entriesToTest >= 10000) {
      log.info("Creating reference testIntersect results for 10,000 entries.");
      runScript("test.SimpleIndex",  p, "contains-10000", REFERENCE_GENERATE);
    }

    // Use SIL library to create test results for 100000+ entries
    p.setProperty("MinNodeEntries", "6");
    p.setProperty("MaxNodeEntries", "13"); // different to other tests
    p.setProperty("TreeVariant", "Rstar");
    
    if (entriesToTest >= 100000) {
      log.info("Creating reference testIntersect results for 100,000 entries.");
      runScript("test.SILWrapper",   p, "contains-100000", REFERENCE_GENERATE);
    }
  }
}
