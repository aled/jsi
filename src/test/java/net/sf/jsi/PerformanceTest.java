//   PerformanceTest.java
//   Java Spatial Index Library
//   Copyright (C) 2002-2005 Infomatiq Limited.
//   Copyright (C) 2013 Aled Morris <aled@users.sourceforge.net>
//
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

package net.sf.jsi;

import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.sf.jsi.rtree.RTree;
import gnu.trove.procedure.TIntProcedure;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PerformanceTest
 *  
 * Generates results used for comparing the performance of the Java Spatial 
 * Index library against alternative implementations.
 *
 * The idea is for the raw data to be imported into a database, and results
 * extracted from that.
 *
 * This test requires 1024M memory (i.e. use -Xmx1024M)
 */
public class PerformanceTest {

  private static final Logger log = LoggerFactory.getLogger(PerformanceTest.class);
  private SpatialIndex si;

  private float randomFloat(Random r, float min, float max) {
    return (r.nextFloat() * (max - min)) + min;
  }

  protected Point randomPoint(Random r) {
    return new Point(randomFloat(r, 0, 100), randomFloat(r, 0, 100));
  }

  private Rectangle randomRectangle(Random r, float size) {
    float x = randomFloat(r, 0, 100);
    float y = randomFloat(r, 0, 100);
    return new Rectangle(x, y, x + randomFloat(r, 0, size), y + randomFloat(r, 0, size));
  }

  abstract class Operation {
    private final int count[] = new int[1];
    private String description;
    public Random r;

    public Operation(String description, Random r) {
      this.description = description;
      this.r = r;
    }

    protected TIntProcedure countProc = new TIntProcedure() {
      public boolean execute(int value) {
        count[0]++;
        return true;
      }
    };

    public int callbackCount() {
      return count[0];
    }

    public String getDescription() {
      return description;
    }

    abstract void execute(SpatialIndex si, Random r);
  }

  private void benchmark(Operation o, int repetitions) {
    long duration = 0;
    long startTime = System.nanoTime();
    for (int j = 0; j < repetitions; j++) o.execute(si, o.r);
    duration += (System.nanoTime() - startTime);

    log.info(o.getDescription() + ", " +
            "avg callbacks = " + ((float) o.callbackCount() / repetitions) + ", " +
            "avg time = " + (duration / repetitions) + " ns");
  }

  /**
   * First attempt at a benchmark
   */
  @Test
  public void benchmark_1() {
    Random rand  = new Random(0);
    Properties p = new Properties();
    p.setProperty("MinNodeEntries", "20");
    p.setProperty("MaxNodeEntries", "50");
    si = new RTree();
    si.init(p);

    final int rectangleCount = 1000000;
    final Rectangle[] rects = new Rectangle[rectangleCount];
    for (int i = 0; i < rectangleCount; i++) {
      rects[i] = randomRectangle(rand, 0.01f);
    }

    long duration;
    long startTime;

    for (int j = 0; j < 5; j++) {
      duration = 0;
      startTime = System.nanoTime();
      for (int i = 0; i < rectangleCount; i++) {
        si.add(rects[i], i);
      }
      duration += (System.nanoTime() - startTime);
      log.info("add " + rectangleCount + " avg tme = " + (duration / rectangleCount) + " ns");

      if (j == 4) break; // don't do the delete on the last iteration

      duration = 0;
      startTime = System.nanoTime();
      for (int i = 0; i < rectangleCount; i++) {
        si.delete(rects[i], i);
      }
      duration += (System.nanoTime() - startTime);
      log.info("delete " + rectangleCount + " avg tme = " + (duration / rectangleCount) + " ns");
    }

    ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    try {
      for (int i = 0; i < 100; i++) {
        exec.submit(new Runnable() {public void run() {benchmark(new Operation("nearest", new Random(0)) {void execute(SpatialIndex si, Random r) {si.nearest(randomPoint(r), countProc, 0.1f);}}, 100); }});
        exec.submit(new Runnable() {public void run() {benchmark(new Operation("nearestNUnsorted", new Random(0)) {void execute(SpatialIndex si, Random r) {si.nearestNUnsorted(randomPoint(r), countProc, 10, 0.16f);}}, 100); }});
        exec.submit(new Runnable() {public void run() {benchmark(new Operation("nearestN", new Random(0)) {void execute(SpatialIndex si, Random r) {si.nearestN(randomPoint(r), countProc, 10, 0.16f);}}, 100); }});
        exec.submit(new Runnable() {public void run() {benchmark(new Operation("intersects", new Random(0)) {void execute(SpatialIndex si, Random r) {si.intersects(randomRectangle(r, 0.6f), countProc);}}, 100); }});
        exec.submit(new Runnable() {public void run() {benchmark(new Operation("contains", new Random(0)) {void execute(SpatialIndex si, Random r) {si.contains(randomRectangle(r, 0.65f), countProc);}}, 100); }});
      }
      try { exec.awaitTermination(1, TimeUnit.DAYS); } catch (Exception e) {}
    }
    finally {
      exec.shutdown();
    }
  }
}
