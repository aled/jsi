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

package com.infomatiq.jsi;

import java.util.Properties;
import java.util.Random;

import com.infomatiq.jsi.rtree.RTree;
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
  private Random r = new Random(0);

  private float randomFloat(float min, float max) {
    return (r.nextFloat() * (max - min)) + min;
  }

  protected Point randomPoint() {
    return new Point(randomFloat(0, 100), randomFloat(0, 100));
  }

  private Rectangle randomRectangle(float size) {
    float x = randomFloat(0, 100);
    float y = randomFloat(0, 100);
    return new Rectangle(x, y, x + randomFloat(0, size), y + randomFloat(0, size));
  }

  abstract class Operation {
    private final int count[] = new int[1];
    private String description;

    public Operation(String description) {
      this.description = description;
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

    abstract void execute(SpatialIndex si);
  }

  private void benchmark(Operation o, int repetitions) {
    long duration = 0;
    long startTime = System.nanoTime();
    for (int j = 0; j < repetitions; j++) o.execute(si);
    duration += (System.nanoTime() - startTime);

    log.info(o.getDescription() + ", " +
              "avg callbacks = " +  ((float)o.callbackCount() / repetitions) + ", " +
              "avg time = " + (duration / repetitions) + " ns");
  }

  @Test
  public void benchmark_1() {
    r = new Random(0);
    Properties p = new Properties();
    p.setProperty("MinNodeEntries", "20");
    p.setProperty("MaxNodeEntries", "50");
    si = new RTree();
    si.init(p);

    final int rectangleCount = 1000000;
    final Rectangle[] rects = new Rectangle[rectangleCount];
    for (int i = 0; i < rectangleCount; i++) {
      rects[i] = randomRectangle(0.01f);
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

      if (j == 4) break; // don't do the delete the last time

      duration = 0;
      startTime = System.nanoTime();
      for (int i = 0; i < rectangleCount; i++) {
        si.delete(rects[i], i);
      }
      duration += (System.nanoTime() - startTime);
      log.info("delete " + rectangleCount + " avg tme = " + (duration / rectangleCount) + " ns");
    }

    for (int i = 0; i < 100; i++) {
      benchmark(new Operation("nearest") {void execute(SpatialIndex si) {si.nearest(randomPoint(), countProc, 0.1f);}}, 100);
      benchmark(new Operation("nearestNUnsorted") {void execute(SpatialIndex si) {si.nearestNUnsorted(randomPoint(), countProc, 10, 0.16f);}}, 100);
      benchmark(new Operation("nearestN") {void execute(SpatialIndex si) {si.nearestN(randomPoint(), countProc, 10, 0.16f);}}, 100);
      benchmark(new Operation("intersects") {void execute(SpatialIndex si) {si.intersects(randomRectangle(0.6f), countProc);}}, 100);
      benchmark(new Operation("contains") {void execute(SpatialIndex si) {si.contains(randomRectangle(0.65f), countProc);}}, 100);
    }
  }
}
