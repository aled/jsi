//   Node.java
//   Java Spatial Index Library
//   Copyright (C) 2002-2005 Infomatiq Limited
//   Copyright (C) 2008-2010 aled@sourceforge.net
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

package com.infomatiq.jsi.rtree;

/**
 * <p>Used by RTree. There are no public methods in this class.</p>
 * 
 * @author aled.morris@infomatiq.co.uk
 * @version 1.0b8
 */
public class Node {
  int nodeId = 0;
  float mbrMinX = Float.MAX_VALUE;
  float mbrMinY = Float.MAX_VALUE;
  float mbrMaxX = -Float.MAX_VALUE;
  float mbrMaxY = -Float.MAX_VALUE;
  
  float[] entriesMinX = null;
  float[] entriesMinY = null;
  float[] entriesMaxX = null;
  float[] entriesMaxY = null;
  
  int[] ids = null;
  int level;
  int entryCount;

  Node(int nodeId, int level, int maxNodeEntries) {
    this.nodeId = nodeId;
    this.level = level;
    entriesMinX = new float[maxNodeEntries];
    entriesMinY = new float[maxNodeEntries];
    entriesMaxX = new float[maxNodeEntries];
    entriesMaxY = new float[maxNodeEntries];
    ids = new int[maxNodeEntries];
  }
   
  void addEntry(float minX, float minY, float maxX, float maxY, int id) {
    ids[entryCount] = id;
    entriesMinX[entryCount] = minX;
    entriesMinY[entryCount] = minY;
    entriesMaxX[entryCount] = maxX;
    entriesMaxY[entryCount] = maxY;
   
    if (minX < mbrMinX) mbrMinX = minX;
    if (minY < mbrMinY) mbrMinY = minY;
    if (maxX > mbrMaxX) mbrMaxX = maxX;
    if (maxY > mbrMaxY) mbrMaxY = maxY;
    
    entryCount++;
  }
  
  // Return the index of the found entry, or -1 if not found
  int findEntry(float minX, float minY, float maxX, float maxY, int id) {
    for (int i = 0; i < entryCount; i++) {
    	if (id == ids[i] && 
          entriesMinX[i] == minX && entriesMinY[i] == minY &&
          entriesMaxX[i] == maxX && entriesMaxY[i] == maxY) {
    	  return i;	
    	}
    }
    return -1;
  }
  
  // delete entry. This is done by setting it to null and copying the last entry into its space.
  void deleteEntry(int i) {
	  int lastIndex = entryCount - 1;
    float deletedMinX = entriesMinX[i];
    float deletedMinY = entriesMinY[i];
    float deletedMaxX = entriesMaxX[i];
    float deletedMaxY = entriesMaxY[i];
    
    if (i != lastIndex) {
      entriesMinX[i] = entriesMinX[lastIndex];
      entriesMinY[i] = entriesMinY[lastIndex];
      entriesMaxX[i] = entriesMaxX[lastIndex];
      entriesMaxY[i] = entriesMaxY[lastIndex];
    	ids[i] = ids[lastIndex];
	  }
    entryCount--;
    
    // adjust the MBR
    recalculateMBRIfInfluencedBy(deletedMinX, deletedMinY, deletedMaxX, deletedMaxY);
  } 
  
  // deletedMin/MaxX/Y is a rectangle that has just been deleted or made smaller.
  // Thus, the MBR is only recalculated if the deleted rectangle influenced the old MBR
  void recalculateMBRIfInfluencedBy(float deletedMinX, float deletedMinY, float deletedMaxX, float deletedMaxY) {
    if (mbrMinX == deletedMinX || mbrMinY == deletedMinY || mbrMaxX == deletedMaxX || mbrMaxY == deletedMaxY) { 
      recalculateMBR();   
    }
  }
   
  void recalculateMBR() {
    mbrMinX = entriesMinX[0];
    mbrMinY = entriesMinY[0];
    mbrMaxX = entriesMaxX[0];
    mbrMaxY = entriesMaxY[0];

    for (int i = 1; i < entryCount; i++) {
      if (entriesMinX[i] < mbrMinX) mbrMinX = entriesMinX[i];
      if (entriesMinY[i] < mbrMinY) mbrMinY = entriesMinY[i];
      if (entriesMaxX[i] > mbrMaxX) mbrMaxX = entriesMaxX[i];
      if (entriesMaxY[i] > mbrMaxY) mbrMaxY = entriesMaxY[i];
    }
  }
    
  /**
   * eliminate null entries, move all entries to the start of the source node
   */
  void reorganize(RTree rtree) {
    int countdownIndex = rtree.maxNodeEntries - 1; 
    for (int index = 0; index < entryCount; index++) {
      if (ids[index] == -1) {
         while (ids[countdownIndex] == -1 && countdownIndex > index) {
           countdownIndex--;
         }
         entriesMinX[index] = entriesMinX[countdownIndex];
         entriesMinY[index] = entriesMinY[countdownIndex];
         entriesMaxX[index] = entriesMaxX[countdownIndex];
         entriesMaxY[index] = entriesMaxY[countdownIndex];
         ids[index] = ids[countdownIndex];    
         ids[countdownIndex] = -1;
      }
    }
  }
  
  public int getEntryCount() {
    return entryCount;
  }
 
  public int getId(int index) {
    if (index < entryCount) {
      return ids[index];
    }
    return -1;
  }
  
  boolean isLeaf() {
    return (level == 1);
  }
  
  public int getLevel() {
    return level; 
  }
}
