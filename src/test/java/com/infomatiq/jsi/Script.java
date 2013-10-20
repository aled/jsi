//   Script.java
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

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Script
 */
public class Script {
  
  private static final Logger log = LoggerFactory.getLogger(Script.class);
    
  static final int PERFORMANCE = 0;
  static final int REFERENCE_COMPARISON = 1;
  static final int REFERENCE_GENERATE = 2;
  
  private double canvasSize = 100000d;
//  private TestCase testCase;
//  
//  public ScriptRunner(TestCase t) {
//    testCase = t;
//  }
  
  private void writePerformanceLog(String operation, String indexType, String testId, Properties p, int size, long timeMillis, int count) { 
    File f = new File("target/test-classes/performance-log-" + operation);
    f.getParentFile().mkdirs();
    
    try {
      boolean created = f.createNewFile();
      PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f)));
      
      if (created)
        pw.println("IndexType,TestId,MinNodeEntries,MaxNodeEntries,TreeVariant,TreeSize,AddCount,AverageAddTime");
            
      pw.println(
        indexType + "," + testId + "," + 
        p.getProperty("MinNodeEntries") + "," + 
        p.getProperty("MaxNodeEntries") + "," + 
        p.getProperty("TreeVariant") + "," +
        size + "," + 
        count + "," + 
        (timeMillis/1000.0) / (double) count);
      pw.flush();
      pw.close();
    } catch (Throwable t) {
      log.error("Could not write to performance log", t);
    }    
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
          TestCase.assertTrue("Output does not match reference on line " + referenceFile.getLineNumber(), false);
        }
      }
    }  catch (IOException e) {
       log.error("IOException while writing test results");
    }  
  }
  
  private double quantize(double d, int quantizer) {
    if (quantizer <= 0) {
      return (double) d;
    }
    
    d /= quantizer;
    d = Math.round(d);
    d *= quantizer;
    
    return (double) d;
  }
  
  private Rectangle getRandomRectangle(Random r, double rectangleSize, double canvasSize, int quantizer) {
    double x1 = quantize(r.nextGaussian() * canvasSize, quantizer);
    double y1 = quantize(r.nextGaussian() * canvasSize, quantizer);
    double x2 = x1 + quantize(r.nextGaussian() * rectangleSize, quantizer);
    double y2 = y1 + quantize(r.nextGaussian() * rectangleSize, quantizer);
    
    return new Rectangle(x1, y1, x2, y2);
  }
              
  /**
   * @return Time taken to execute method, in milliseconds.
   */
  public long run(String indexType, Properties indexProperties, String testId, int testType) {
    if (log.isDebugEnabled()) {
      log.debug("runScript: " + indexType + ", testId=" + testId + 
               ", minEntries=" + indexProperties.getProperty("MinNodeEntries") + 
               ", maxEntries=" + indexProperties.getProperty("MaxNodeEntries") + 
               ", treeVariant=" + indexProperties.getProperty("TreeVariant"));
    }
    
    SpatialIndex si = SpatialIndexFactory.newInstance(indexType, indexProperties);
    
    ListDecorator ld = null;
    
    // Don't sort the results if we are testing the performance
    if (testType == PERFORMANCE) {
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
 
    String strTestInputRoot = "/test-inputs" + File.separator + "test-" + testId;
    
    String strTestResultsRoot = "target/test-results" + File.separator + "test-" + testId;
    
    // open test input file for read-only
    LineNumberReader inputFile = null;
    String inputFilename = strTestInputRoot + "-in";
    
    try {
      inputFile = new LineNumberReader(new InputStreamReader(getClass().getResourceAsStream(inputFilename)));          
    }
    catch (Throwable t) {
      log.error("Unable to open test input file " + inputFilename);
      TestCase.assertTrue("Unable to open test input file " + inputFilename, false);
      return -1;
    }
    
    // open reference results file for read-only. Filename is of form:
    // test-testId-reference
    LineNumberReader referenceFile = null;
    if (testType == REFERENCE_COMPARISON) {
      String referenceFilename = strTestResultsRoot + "-reference";
      try {
        referenceFile = new LineNumberReader(new InputStreamReader(new FileInputStream(referenceFilename)));
      } catch (FileNotFoundException e) {
        log.error("Unable to open reference test results file " + referenceFilename);
        TestCase.assertTrue("Unable to open reference test results file " + referenceFilename, false);
        return -1;
      }
    }
   
    // open actual results file for writing. Filename is of form
    // test-testId-indexType-revision-datetime, unless generating reference results.
    PrintWriter outputFile = null;
    if (testType == REFERENCE_COMPARISON || testType == REFERENCE_GENERATE) {
      String outputFilename = null;
      if (testType == REFERENCE_COMPARISON) {
        outputFilename = strTestResultsRoot + "-" + si.getVersion() + 
          "-" + new SimpleDateFormat("yyMMddHHmmss").format(new Date());
      } else {
        outputFilename = strTestResultsRoot + "-reference";
        if (new File(outputFilename).exists()) {
          log.info("Reusing existing reference file: " + outputFilename);
          return 0;
        }
      }     
       
      new File(outputFilename).getParentFile().mkdirs();
      
      try { 
        outputFile = new PrintWriter(new FileOutputStream(outputFilename));     
      } catch (FileNotFoundException e) {
        log.error("Unable to open test output results file " + outputFilename);
        TestCase.assertTrue("Unable to open test output results file " + outputFilename, false);
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
        
        if (testType == REFERENCE_COMPARISON || testType == REFERENCE_GENERATE) {
          outputBuffer = new StringBuffer(inputLine);
        }
            
        StringTokenizer st = new StringTokenizer(inputLine);
        while (st.hasMoreTokens()) {
         String operation = st.nextToken().toUpperCase();
          if (operation.equals("DISTANCEQUANTIZER")) {
            quantizer = Integer.parseInt(st.nextToken());
          } else if (operation.equals("RANDOMIZE")) {
            random.setSeed(Integer.parseInt(st.nextToken()));
            if (testType == REFERENCE_COMPARISON || testType == REFERENCE_GENERATE) {
              writeOutput(outputBuffer.toString() + " : OK", outputFile, referenceFile);
            }
          } else if (operation.equals("ADDRANDOM")) {
            int count = Integer.parseInt(st.nextToken());
            int startId = Integer.parseInt(st.nextToken());
            double rectangleSize = Double.parseDouble(st.nextToken());
            
            if (testType == REFERENCE_COMPARISON || testType == REFERENCE_GENERATE) {
              writeOutput(outputBuffer.toString(), outputFile, referenceFile);
            }
            
            long startTime = System.currentTimeMillis();
               
            for (int id = startId; id < startId + count; id++) {
              Rectangle r = getRandomRectangle(random, rectangleSize, canvasSize, quantizer);
              si.add(r, id);
            
              if (testType == REFERENCE_COMPARISON || testType == REFERENCE_GENERATE) {
                String outputLine = "  " + id + " " + r.toString() + " : OK";
                writeOutput(outputLine, outputFile, referenceFile);
              }
            }
            long time = System.currentTimeMillis() - startTime;
            if (log.isDebugEnabled()) {
              log.debug("Added " + count + " entries in " + time +  "ms (" + time / (double) count + " ms per add)");
            }
            if (testType == PERFORMANCE) {
               writePerformanceLog("add", indexType, testId, indexProperties, si.size(), time, count);
            }
          } else if (operation.equals("DELETERANDOM")) {
            int count = Integer.parseInt(st.nextToken());
            int startId = Integer.parseInt(st.nextToken());
            double rectangleSize = Double.parseDouble(st.nextToken());
            
            if (testType == REFERENCE_COMPARISON || testType == REFERENCE_GENERATE) {
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
              
              if (testType == REFERENCE_COMPARISON || testType == REFERENCE_GENERATE) {
                String outputLine = "  " + id + " " + r.toString() + " : " + deleted;
                writeOutput(outputLine, outputFile, referenceFile);
              }
            }
            long time = System.currentTimeMillis() - startTime;
            if (log.isDebugEnabled()) {
              log.debug("Attempted to delete " + count + " entries (" + successfulDeleteCount + " successful) in " + time +  "ms (" + time / (double) count + " ms per delete)");
            }
            if (testType == PERFORMANCE) {
              writePerformanceLog("delete", indexType, testId, indexProperties, si.size(), time, count);              
            }
          } 
          else if (operation.equals("NEARESTRANDOM")) {
            int queryCount = Integer.parseInt(st.nextToken());
            
            if (testType == REFERENCE_COMPARISON || testType == REFERENCE_GENERATE) {
              writeOutput(outputBuffer.toString(), outputFile, referenceFile);
            }
  
            long startTime = System.currentTimeMillis();
            int totalEntriesReturned = 0;
            
            for (int id = 0; id < queryCount; id++) {
              double x = (double) random.nextGaussian() * canvasSize;
              double y = (double) random.nextGaussian() * canvasSize;
              
              List<Integer> l = ld.nearest(new Point(x, y), Double.POSITIVE_INFINITY);
              totalEntriesReturned += l.size();
              
              if (testType == REFERENCE_COMPARISON || testType == REFERENCE_GENERATE) {
                StringBuffer tempBuffer = new StringBuffer("  " + id + " " + 
                                                     df.format(x) + " " +
                                                     df.format(y) + " : OK");
  
                Iterator<Integer> i = l.iterator();
                while (i.hasNext()) {
                  tempBuffer.append(' ');
                  tempBuffer.append(i.next()).toString();
                }
                writeOutput(tempBuffer.toString(), outputFile, referenceFile);
              }
            } 
            long time = System.currentTimeMillis() - startTime;
            if (log.isDebugEnabled()) {
              log.debug("NearestQueried " + queryCount + " times in " + time +  "ms. Per query: " + time / (double) queryCount + " ms, " + (totalEntriesReturned / (double) queryCount) + " entries");
            }
            if (testType == PERFORMANCE) {
              writePerformanceLog("nearest", indexType, testId, indexProperties, si.size(), time, queryCount);              
            }
          } else if (operation.equals("NEARESTNRANDOM")) {
            int queryCount = Integer.parseInt(st.nextToken());
            int n = Integer.parseInt(st.nextToken());
            
            if (testType == REFERENCE_COMPARISON || testType == REFERENCE_GENERATE) {
              writeOutput(outputBuffer.toString(), outputFile, referenceFile);
            }
  
            long startTime = System.currentTimeMillis();
            int totalEntriesReturned = 0;
            
            for (int id = 0; id < queryCount; id++) {
              double x = (double) random.nextGaussian() * canvasSize;
              double y = (double) random.nextGaussian() * canvasSize;
              
              List<Integer> l = ld.nearestN(new Point(x, y), n, Double.POSITIVE_INFINITY);
              
              totalEntriesReturned += l.size();
              
              if (testType == REFERENCE_COMPARISON || testType == REFERENCE_GENERATE) {
                StringBuffer tempBuffer = new StringBuffer("  " + id + " " + 
                                                     df.format(x) + " " +
                                                     df.format(y) + " : OK");
  
                Iterator<Integer> i = l.iterator();
                while (i.hasNext()) {
                  tempBuffer.append(' ');
                  tempBuffer.append(i.next()).toString();
                }
              
                writeOutput(tempBuffer.toString(), outputFile, referenceFile);
              }
            } 
            long time = System.currentTimeMillis() - startTime;
            if (log.isDebugEnabled()) {
              log.debug("NearestNQueried " + queryCount + " times in " + time +  "ms. Per query: " + time / (double) queryCount + " ms, " + (totalEntriesReturned / (double) queryCount) + " entries");
            }
            if (testType == PERFORMANCE) {
              writePerformanceLog("nearestN", indexType, testId, indexProperties, si.size(), time, queryCount);              
            }
          } else if (operation.equals("INTERSECTRANDOM")) {
            int queryCount = Integer.parseInt(st.nextToken());
            double rectangleSize = Double.parseDouble(st.nextToken());
            
            if (testType == REFERENCE_COMPARISON || testType == REFERENCE_GENERATE) {
              writeOutput(outputBuffer.toString(), outputFile, referenceFile);
            }
            
            long startTime = System.currentTimeMillis();
            int totalEntriesReturned = 0;
            
            for (int id = 0; id < queryCount; id++) {
              Rectangle r = getRandomRectangle(random, rectangleSize, canvasSize, quantizer);
              List<Integer> l = ld.intersects(r);
              totalEntriesReturned += l.size();
              
              if (testType == REFERENCE_COMPARISON || testType == REFERENCE_GENERATE) {
                Iterator<Integer> i = l.iterator();
                StringBuffer tempBuffer = new StringBuffer("  " + id + " " + r.toString() + " : OK");
  
                while (i.hasNext()) {
                  tempBuffer.append(' ');
                  tempBuffer.append(i.next()).toString();
                }
                writeOutput(tempBuffer.toString(), outputFile, referenceFile);
              }
            }
            long time = System.currentTimeMillis() - startTime;
            if (log.isDebugEnabled()) {
              log.debug("IntersectQueried " + queryCount + " times in " + time +  "ms. Per query: " + time / (double) queryCount + " ms, " + (totalEntriesReturned / (double) queryCount) + " entries");
            }
            if (testType == PERFORMANCE) {
              writePerformanceLog("intersect", indexType, testId, indexProperties, si.size(), time, queryCount);              
            }
          } 
          else if (operation.equals("CONTAINSRANDOM")) {
            int queryCount = Integer.parseInt(st.nextToken());
            double rectangleSize = Double.parseDouble(st.nextToken());
            
            if (testType == REFERENCE_COMPARISON || testType == REFERENCE_GENERATE) {
              writeOutput(outputBuffer.toString(), outputFile, referenceFile);
            }
            
            long startTime = System.currentTimeMillis();
            int totalEntriesReturned = 0;
            
            for (int id = 0; id < queryCount; id++) {
              Rectangle r = getRandomRectangle(random, rectangleSize, canvasSize, quantizer);
              List<Integer> l = ld.contains(r);
              totalEntriesReturned += l.size();
              
              if (testType == REFERENCE_COMPARISON || testType == REFERENCE_GENERATE) {
                Iterator<Integer> i = l.iterator();
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
              log.debug("ContainsQueried " + queryCount + " times in " + time +  "ms. Per query: " + time / (double) queryCount + " ms, " + (totalEntriesReturned / (double) queryCount) + " entries");
            }
            if (testType == PERFORMANCE) {
              writePerformanceLog("contains", indexType, testId, indexProperties, si.size(), time, queryCount);              
            }
          } 
          else if (operation.equals("ADD")) {
            int id = Integer.parseInt(st.nextToken());
            double x1 = Double.parseDouble(st.nextToken());
            double y1 = Double.parseDouble(st.nextToken());
            double x2 = Double.parseDouble(st.nextToken());
            double y2 = Double.parseDouble(st.nextToken());
             
            si.add(new Rectangle(x1, y1, x2, y2), id);
             
            if (testType == REFERENCE_COMPARISON || testType == REFERENCE_GENERATE) {
              outputBuffer.append(" : OK");
              writeOutput(outputBuffer.toString(), outputFile, referenceFile);
            }
          } 
          else if (operation.equals("DELETE")) {
            int id = Integer.parseInt(st.nextToken());
            double x1 = Double.parseDouble(st.nextToken());
            double y1 = Double.parseDouble(st.nextToken());
            double x2 = Double.parseDouble(st.nextToken());
            double y2 = Double.parseDouble(st.nextToken());
             
            boolean deleted = si.delete(new Rectangle(x1, y1, x2, y2), id);
             
            if (testType == REFERENCE_COMPARISON || testType == REFERENCE_GENERATE) {
              if (deleted) {
                outputBuffer.append(" : OK");
              } else {
                outputBuffer.append(" : Not found");
              }
              writeOutput(outputBuffer.toString(), outputFile, referenceFile);
            }
          } 
          else if (operation.equals("NEAREST")) {
            double x = Double.parseDouble(st.nextToken());
            double y = Double.parseDouble(st.nextToken());
             
            List<Integer> l = ld.nearest(new Point(x, y), Double.POSITIVE_INFINITY);
            
            if (testType == REFERENCE_COMPARISON || testType == REFERENCE_GENERATE) {
              outputBuffer.append(" : OK");
             
              Iterator<Integer> i = l.iterator();
              while (i.hasNext()) {
                outputBuffer.append(" ");
                outputBuffer.append((Integer)i.next()).toString();
              }
              writeOutput(outputBuffer.toString(), outputFile, referenceFile);
            }
          } 
          else if (operation.equals("INTERSECT")) {
            double x1 = Double.parseDouble(st.nextToken());
            double y1 = Double.parseDouble(st.nextToken());
            double x2 = Double.parseDouble(st.nextToken());
            double y2 = Double.parseDouble(st.nextToken());
            
            List<Integer> l = ld.intersects(new Rectangle(x1, y1, x2, y2));
            
            if (testType == REFERENCE_COMPARISON || testType == REFERENCE_GENERATE) {
              outputBuffer.append(" : OK");
             
              Iterator<Integer> i = l.iterator();
              while (i.hasNext()) {
                outputBuffer.append(" ");
                outputBuffer.append(i.next()).toString();
              }
              writeOutput(outputBuffer.toString(), outputFile, referenceFile);
            }
          }
        } // for each token on the current input line
      } // for each input line
    } catch (IOException e) {
      log.error("IOException while running test script in SpatialIndexTest", e); 
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