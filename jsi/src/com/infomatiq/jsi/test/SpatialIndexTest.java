//   SpatialIndexTest.java
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;

import junit.framework.TestCase;

import org.apache.log4j.Logger;

import com.infomatiq.jsi.Point;
import com.infomatiq.jsi.Rectangle;
import com.infomatiq.jsi.SpatialIndex;

/**
 * SpatialIndexTest
 * 
 * @author  aled@sourceforge.net
 * @version 1.0b8
 */
public class SpatialIndexTest extends TestCase {
  
  private static final Logger log = 
    Logger.getLogger(SpatialIndexTest.class.getName());
    
  protected static final Logger intersectPerformanceLog = 
    Logger.getLogger("intersectPerformance");
  
  protected static final Logger containsPerformanceLog = 
    Logger.getLogger("containsPerformance");
  
  protected static final Logger nearestPerformanceLog = 
    Logger.getLogger("nearestPerformance");
    
  protected static final Logger nearestNPerformanceLog = 
    Logger.getLogger("nearestNPerformance");
    
  protected static final Logger addPerformanceLog = 
    Logger.getLogger("addPerformance");
  
  protected static final Logger deletePerformanceLog = 
    Logger.getLogger("deletePerformance");
    
  protected static final int PERFORMANCE_TEST = 0;
  protected static final int REFERENCE_COMPARISON_TEST = 1;
  protected static final int REFERENCE_GENERATE = 2;
  
  private float canvasSize = 100000F;
  
  public SpatialIndexTest(String s) {
    super(s);
  }
  
  private void writeOutput(String outputLine, PrintWriter outputFile, LineNumberReader referenceFile) {
   try {
      outputFile.println(outputLine);
      outputFile.flush();
      if (referenceFile != null) { 
        String referenceLine = referenceFile.readLine();
        if (!outputLine.equals(referenceLine)) {
          log.error("Output does not match reference on line " + referenceFile.getLineNumber());
          log.error(" Reference result: " + referenceLine);
          log.error(" Test result:      " + outputLine);
          assertTrue("Output does not match reference on line " + referenceFile.getLineNumber(), false);
        }
      }
    }  catch (IOException e) {
       log.error("IOException while writing test results");
    }  
  }
  
  private float quantize(double d, int quantizer) {
    if (quantizer <= 0) {
      return (float) d;
    }
    
    d /= quantizer;
    d = Math.round(d);
    d *= quantizer;
    
    return (float) d;
  }
  
  private Rectangle getRandomRectangle(Random r, float rectangleSize, float canvasSize, int quantizer) {
    float x1 = quantize(r.nextGaussian() * canvasSize, quantizer);
    float y1 = quantize(r.nextGaussian() * canvasSize, quantizer);
    float x2 = x1 + quantize(r.nextGaussian() * rectangleSize, quantizer);
    float y2 = y1 + quantize(r.nextGaussian() * rectangleSize, quantizer);
    
    return new Rectangle(x1, y1, x2, y2);
  }
              
  /**
   * @return Time taken to execute method, in milliseconds.
   */
  protected long runScript(String indexType, Properties indexProperties, String testId, int testType) {
    if (log.isDebugEnabled()) {
      log.debug("runScript: " + indexType + ", testId=" + testId + 
               ", minEntries=" + indexProperties.getProperty("MinNodeEntries") + 
               ", maxEntries=" + indexProperties.getProperty("MaxNodeEntries") + 
               ", treeVariant=" + indexProperties.getProperty("TreeVariant"));
    }
    
    SpatialIndex si = SpatialIndexFactory.newInstance(indexType, indexProperties);
    
    ListDecorator ld = null;
    
    // Don't sort the results if we are testing the performance
    if (testType == PERFORMANCE_TEST) {
      ld = new ListDecorator(si); 
    } else {
      ld = new SortedListDecorator(si);
    } 
    
    Random random = new Random();
    DecimalFormat df = new DecimalFormat();
    df.setMinimumFractionDigits(4);
    df.setMaximumFractionDigits(4);
    df.setMinimumIntegerDigits(7);
    df.setMaximumIntegerDigits(7);
    df.setPositivePrefix(" ");
    df.setGroupingUsed(false);
    
    int quantizer = -1;
 
    String strTestInputRoot = "tests" + File.separator + "test-" + testId;
    String strTestResultsRoot = "test-results" + File.separator + "test-" + testId;
    
    // open test input file for read-only
    LineNumberReader inputFile = null;
    String inputFilename = strTestInputRoot + "-in";
    try {
      inputFile = new LineNumberReader(new InputStreamReader(new FileInputStream(inputFilename)));
    } catch (FileNotFoundException e) {
      log.error("Unable to open test input file " + inputFilename);
      assertTrue("Unable to open test input file " + inputFilename, false);
      return -1;
    }
    
    // open reference results file for read-only. Filename is of form:
    // test-testId-reference
    LineNumberReader referenceFile = null;
    if (testType == REFERENCE_COMPARISON_TEST) {
      String referenceFilename = strTestResultsRoot + "-reference";
      try {
        referenceFile = new LineNumberReader(new InputStreamReader(new FileInputStream(referenceFilename)));
      } catch (FileNotFoundException e) {
        log.error("Unable to open reference test results file " + referenceFilename);
        assertTrue("Unable to open reference test results file " + referenceFilename, false);
        return -1;
      }
    }
   
    // open actual results file for writing. Filename is of form
    // test-testId-indexType-revision-datetime, unless generating reference results.
    PrintWriter outputFile = null;
    if (testType == REFERENCE_COMPARISON_TEST || testType == REFERENCE_GENERATE) {
      String outputFilename = null;
      if (testType == REFERENCE_COMPARISON_TEST) {
        outputFilename = strTestResultsRoot + "-" + si.getVersion() + 
          "-" + new SimpleDateFormat("yyMMddHHmmss").format(new Date());
      } else {
        outputFilename = strTestResultsRoot + "-reference";
      }
      try {     
        outputFile = new PrintWriter(new FileOutputStream(outputFilename));
      } catch (FileNotFoundException e) {
        log.error("Unable to open test output results file " + outputFilename);
        assertTrue("Unable to open test output results file " + outputFilename, false);
        return -1;
      }
    }
    
    long scriptStartTime = System.currentTimeMillis();
    
    try {
      // read lines from the test input file
      while (inputFile.ready()) {
        String inputLine = inputFile.readLine();
        
        if (inputLine.startsWith("#")) {
          continue; 
        }
        
        StringBuffer outputBuffer = null;
        
        if (testType == REFERENCE_COMPARISON_TEST || testType == REFERENCE_GENERATE) {
          outputBuffer = new StringBuffer(inputLine);
        }
            
        StringTokenizer st = new StringTokenizer(inputLine);
        while (st.hasMoreTokens()) {
         String operation = st.nextToken().toUpperCase();
          if (operation.equals("DISTANCEQUANTIZER")) {
            quantizer = Integer.parseInt(st.nextToken());
          } else if (operation.equals("RANDOMIZE")) {
            random.setSeed(Integer.parseInt(st.nextToken()));
            if (testType == REFERENCE_COMPARISON_TEST || testType == REFERENCE_GENERATE) {
              writeOutput(outputBuffer.toString() + " : OK", outputFile, referenceFile);
            }
          } else if (operation.equals("ADDRANDOM")) {
            int count = Integer.parseInt(st.nextToken());
            int startId = Integer.parseInt(st.nextToken());
            float rectangleSize = Float.parseFloat(st.nextToken());
            
            if (testType == REFERENCE_COMPARISON_TEST || testType == REFERENCE_GENERATE) {
              writeOutput(outputBuffer.toString(), outputFile, referenceFile);
            }
            
            long startTime = System.currentTimeMillis();
               
            for (int id = startId; id < startId + count; id++) {
              Rectangle r = getRandomRectangle(random, rectangleSize, canvasSize, quantizer);
              si.add(r, id);
            
              if (testType == REFERENCE_COMPARISON_TEST || testType == REFERENCE_GENERATE) {
                String outputLine = "  " + id + " " + r.toString() + " : OK";
                writeOutput(outputLine, outputFile, referenceFile);
              }
            }
            long time = System.currentTimeMillis() - startTime;
            if (log.isDebugEnabled()) {
              log.debug("Added " + count + " entries in " + time +  "ms (" + time / (float) count + " ms per add)");
            }
            if (testType == PERFORMANCE_TEST) {
               addPerformanceLog.info(indexType + "," + 
                                            testId + "," + 
                                            indexProperties.getProperty("MinNodeEntries") + "," + 
                                            indexProperties.getProperty("MaxNodeEntries") + "," + 
                                            indexProperties.getProperty("TreeVariant") + "," +
                                            si.size() + "," + 
                                            count + "," + 
                                            (float) time / (float) count);
            }
          } else if (operation.equals("DELETERANDOM")) {
            int count = Integer.parseInt(st.nextToken());
            int startId = Integer.parseInt(st.nextToken());
            float rectangleSize = Float.parseFloat(st.nextToken());
            
            if (testType == REFERENCE_COMPARISON_TEST || testType == REFERENCE_GENERATE) {
              writeOutput(outputBuffer.toString(), outputFile, referenceFile);
            }
            
            long startTime = System.currentTimeMillis();
            
            int successfulDeleteCount = 0;   
            for (int id = startId; id < startId + count; id++) {
              Rectangle r = getRandomRectangle(random, rectangleSize, canvasSize, quantizer);
              boolean deleted = si.delete(r, id);
             
              if (deleted) {
                successfulDeleteCount++;
              }
              
              if (testType == REFERENCE_COMPARISON_TEST || testType == REFERENCE_GENERATE) {
                String outputLine = "  " + id + " " + r.toString() + " : " + deleted;
                writeOutput(outputLine, outputFile, referenceFile);
              }
            }
            long time = System.currentTimeMillis() - startTime;
            if (log.isDebugEnabled()) {
              log.debug("Attempted to delete " + count + " entries (" + successfulDeleteCount + " successful) in " + time +  "ms (" + time / (float) count + " ms per delete)");
            }
            if (testType == PERFORMANCE_TEST) {
               deletePerformanceLog.info(indexType + "," + 
                                            testId + "," + 
                                            indexProperties.getProperty("MinNodeEntries") + "," + 
                                            indexProperties.getProperty("MaxNodeEntries") + "," + 
                                            indexProperties.getProperty("TreeVariant") + "," +
                                            si.size() + "," + 
                                            count + "," + 
                                            (float) time / (float) count);
            }
          } 
          else if (operation.equals("NEARESTRANDOM")) {
            int queryCount = Integer.parseInt(st.nextToken());
            
            if (testType == REFERENCE_COMPARISON_TEST || testType == REFERENCE_GENERATE) {
              writeOutput(outputBuffer.toString(), outputFile, referenceFile);
            }
  
            long startTime = System.currentTimeMillis();
            int totalEntriesReturned = 0;
            
            for (int id = 0; id < queryCount; id++) {
              float x = (float) random.nextGaussian() * canvasSize;
              float y = (float) random.nextGaussian() * canvasSize;
              
              List l = ld.nearest(new Point(x, y), Float.POSITIVE_INFINITY);
              totalEntriesReturned += l.size();
              
              if (testType == REFERENCE_COMPARISON_TEST || testType == REFERENCE_GENERATE) {
                StringBuffer tempBuffer = new StringBuffer("  " + id + " " + 
                                                     df.format(x) + " " +
                                                     df.format(y) + " : OK");
  
                Iterator i = l.iterator();
                while (i.hasNext()) {
                  tempBuffer.append(' ');
                  tempBuffer.append((Integer)i.next()).toString();
                }
                writeOutput(tempBuffer.toString(), outputFile, referenceFile);
              }
            } 
            long time = System.currentTimeMillis() - startTime;
            if (log.isDebugEnabled()) {
              log.debug("NearestQueried " + queryCount + " times in " + time +  "ms. Per query: " + time / (float) queryCount + " ms, " + (totalEntriesReturned / (float) queryCount) + " entries");
            }
            if (testType == PERFORMANCE_TEST) {
               nearestPerformanceLog.info(indexType + "," + 
                                            testId + "," + 
                                            indexProperties.getProperty("MinNodeEntries") + "," + 
                                            indexProperties.getProperty("MaxNodeEntries") + "," + 
                                            indexProperties.getProperty("TreeVariant") + "," +
                                            si.size() + "," + 
                                            queryCount + "," + 
                                            (float) totalEntriesReturned / (float) queryCount + "," +
                                            (float) time / (float) queryCount);
            }
          } else if (operation.equals("NEARESTNRANDOM")) {
            int queryCount = Integer.parseInt(st.nextToken());
            int n = Integer.parseInt(st.nextToken());
            
            if (testType == REFERENCE_COMPARISON_TEST || testType == REFERENCE_GENERATE) {
              writeOutput(outputBuffer.toString(), outputFile, referenceFile);
            }
  
            long startTime = System.currentTimeMillis();
            int totalEntriesReturned = 0;
            
            for (int id = 0; id < queryCount; id++) {
              float x = (float) random.nextGaussian() * canvasSize;
              float y = (float) random.nextGaussian() * canvasSize;
              
              List l = ld.nearestN(new Point(x, y), n, Float.POSITIVE_INFINITY);
              
              totalEntriesReturned += l.size();
              
              if (testType == REFERENCE_COMPARISON_TEST || testType == REFERENCE_GENERATE) {
                StringBuffer tempBuffer = new StringBuffer("  " + id + " " + 
                                                     df.format(x) + " " +
                                                     df.format(y) + " : OK");
  
                Iterator i = l.iterator();
                while (i.hasNext()) {
                  tempBuffer.append(' ');
                  tempBuffer.append((Integer)i.next()).toString();
                }
              
                writeOutput(tempBuffer.toString(), outputFile, referenceFile);
              }
            } 
            long time = System.currentTimeMillis() - startTime;
            if (log.isDebugEnabled()) {
              log.debug("NearestNQueried " + queryCount + " times in " + time +  "ms. Per query: " + time / (float) queryCount + " ms, " + (totalEntriesReturned / (float) queryCount) + " entries");
            }
            if (testType == PERFORMANCE_TEST) {
               nearestNPerformanceLog.info(indexType + "," + 
                                            testId + "," + 
                                            indexProperties.getProperty("MinNodeEntries") + "," + 
                                            indexProperties.getProperty("MaxNodeEntries") + "," + 
                                            indexProperties.getProperty("TreeVariant") + "," +
                                            si.size() + "," + 
                                            queryCount + "," + 
                                            (float) totalEntriesReturned / (float) queryCount + "," +
                                            (float) time / (float) queryCount);
            }
          } else if (operation.equals("INTERSECTRANDOM")) {
            int queryCount = Integer.parseInt(st.nextToken());
            float rectangleSize = Float.parseFloat(st.nextToken());
            
            if (testType == REFERENCE_COMPARISON_TEST || testType == REFERENCE_GENERATE) {
              writeOutput(outputBuffer.toString(), outputFile, referenceFile);
            }
            
            long startTime = System.currentTimeMillis();
            int totalEntriesReturned = 0;
            
            for (int id = 0; id < queryCount; id++) {
              Rectangle r = getRandomRectangle(random, rectangleSize, canvasSize, quantizer);
              List l = ld.intersects(r);
              totalEntriesReturned += l.size();
              
              if (testType == REFERENCE_COMPARISON_TEST || testType == REFERENCE_GENERATE) {
                Iterator i = l.iterator();
                StringBuffer tempBuffer = new StringBuffer("  " + id + " " + r.toString() + " : OK");
  
                while (i.hasNext()) {
                  tempBuffer.append(' ');
                  tempBuffer.append((Integer)i.next()).toString();
                }
                writeOutput(tempBuffer.toString(), outputFile, referenceFile);
              }
            }
            long time = System.currentTimeMillis() - startTime;
            if (log.isDebugEnabled()) {
              log.debug("IntersectQueried " + queryCount + " times in " + time +  "ms. Per query: " + time / (float) queryCount + " ms, " + (totalEntriesReturned / (float) queryCount) + " entries");
            }
            if (testType == PERFORMANCE_TEST) {
               intersectPerformanceLog.info(indexType + "," + 
                                            testId + "," + 
                                            indexProperties.getProperty("MinNodeEntries") + "," + 
                                            indexProperties.getProperty("MaxNodeEntries") + "," + 
                                            indexProperties.getProperty("TreeVariant") + "," +
                                            si.size() + "," + 
                                            queryCount + "," + 
                                            (float) totalEntriesReturned / (float) queryCount + "," +
                                            (float) time / (float) queryCount);
            }
          } 
          else if (operation.equals("CONTAINSRANDOM")) {
            int queryCount = Integer.parseInt(st.nextToken());
            float rectangleSize = Float.parseFloat(st.nextToken());
            
            if (testType == REFERENCE_COMPARISON_TEST || testType == REFERENCE_GENERATE) {
              writeOutput(outputBuffer.toString(), outputFile, referenceFile);
            }
            
            long startTime = System.currentTimeMillis();
            int totalEntriesReturned = 0;
            
            for (int id = 0; id < queryCount; id++) {
              Rectangle r = getRandomRectangle(random, rectangleSize, canvasSize, quantizer);
              List l = ld.contains(r);
              totalEntriesReturned += l.size();
              
              if (testType == REFERENCE_COMPARISON_TEST || testType == REFERENCE_GENERATE) {
                Iterator i = l.iterator();
                StringBuffer tempBuffer = new StringBuffer("  " + id + " " + r.toString() + " : OK");
  
                while (i.hasNext()) {
                  tempBuffer.append(' ');
                  tempBuffer.append((Integer)i.next()).toString();
                }
                writeOutput(tempBuffer.toString(), outputFile, referenceFile);
              }
            }
            long time = System.currentTimeMillis() - startTime;
            if (log.isDebugEnabled()) {
              log.debug("ContainsQueried " + queryCount + " times in " + time +  "ms. Per query: " + time / (float) queryCount + " ms, " + (totalEntriesReturned / (float) queryCount) + " entries");
            }
            if (testType == PERFORMANCE_TEST) {
               containsPerformanceLog.info(indexType + "," + 
                                            testId + "," + 
                                            indexProperties.getProperty("MinNodeEntries") + "," + 
                                            indexProperties.getProperty("MaxNodeEntries") + "," + 
                                            indexProperties.getProperty("TreeVariant") + "," +
                                            si.size() + "," + 
                                            queryCount + "," + 
                                            (float) totalEntriesReturned / (float) queryCount + "," +
                                            (float) time / (float) queryCount);
            }
          } 
          else if (operation.equals("ADD")) {
            int id = Integer.parseInt(st.nextToken());
            float x1 = Float.parseFloat(st.nextToken());
            float y1 = Float.parseFloat(st.nextToken());
            float x2 = Float.parseFloat(st.nextToken());
            float y2 = Float.parseFloat(st.nextToken());
             
            si.add(new Rectangle(x1, y1, x2, y2), id);
             
            if (testType == REFERENCE_COMPARISON_TEST || testType == REFERENCE_GENERATE) {
              outputBuffer.append(" : OK");
              writeOutput(outputBuffer.toString(), outputFile, referenceFile);
            }
          } 
          else if (operation.equals("DELETE")) {
            int id = Integer.parseInt(st.nextToken());
            float x1 = Float.parseFloat(st.nextToken());
            float y1 = Float.parseFloat(st.nextToken());
            float x2 = Float.parseFloat(st.nextToken());
            float y2 = Float.parseFloat(st.nextToken());
             
            boolean deleted = si.delete(new Rectangle(x1, y1, x2, y2), id);
             
            if (testType == REFERENCE_COMPARISON_TEST || testType == REFERENCE_GENERATE) {
              if (deleted) {
                outputBuffer.append(" : OK");
              } else {
                outputBuffer.append(" : Not found");
              }
              writeOutput(outputBuffer.toString(), outputFile, referenceFile);
            }
          } 
          else if (operation.equals("NEAREST")) {
            float x = Float.parseFloat(st.nextToken());
            float y = Float.parseFloat(st.nextToken());
             
            List l = ld.nearest(new Point(x, y), Float.POSITIVE_INFINITY);
            
            if (testType == REFERENCE_COMPARISON_TEST || testType == REFERENCE_GENERATE) {
              outputBuffer.append(" : OK");
             
              Iterator i = l.iterator();
              while (i.hasNext()) {
                outputBuffer.append(" ");
                outputBuffer.append((Integer)i.next()).toString();
              }
              writeOutput(outputBuffer.toString(), outputFile, referenceFile);
            }
          } 
          else if (operation.equals("INTERSECT")) {
            float x1 = Float.parseFloat(st.nextToken());
            float y1 = Float.parseFloat(st.nextToken());
            float x2 = Float.parseFloat(st.nextToken());
            float y2 = Float.parseFloat(st.nextToken());
            
            List l = ld.intersects(new Rectangle(x1, y1, x2, y2));
            
            if (testType == REFERENCE_COMPARISON_TEST || testType == REFERENCE_GENERATE) {
              outputBuffer.append(" : OK");
             
              Iterator i = l.iterator();
              while (i.hasNext()) {
                outputBuffer.append(" ");
                outputBuffer.append((Integer)i.next()).toString();
              }
              writeOutput(outputBuffer.toString(), outputFile, referenceFile);
            }
          }
        } // for each token on the current input line
      } // for each input line
    } catch (IOException e) {
      log.error(e); 
      return -1;
    } 
    long scriptEndTime = System.currentTimeMillis();
    
    // try and clean up the largest objects to prevent garbage collection 
    // from slowing down a future run.
    ld = null;
    si = null; 
    System.gc();
    
    return scriptEndTime - scriptStartTime;
  }
}
