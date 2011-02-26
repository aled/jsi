//   RTree.java
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

import gnu.trove.TIntArrayList;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntProcedure;
import gnu.trove.TIntStack;

import java.util.Properties;

import org.apache.log4j.Logger;

import com.infomatiq.jsi.Point;
import com.infomatiq.jsi.Rectangle;
import com.infomatiq.jsi.PriorityQueue;
import com.infomatiq.jsi.SpatialIndex;

/**
 * <p>This is a lightweight RTree implementation, specifically designed 
 * for the following features (in order of importance): 
 * <ul>
 * <li>Fast intersection query performance. To achieve this, the RTree 
 * uses only main memory to store entries. Obviously this will only improve
 * performance if there is enough physical memory to avoid paging.</li>
 * <li>Low memory requirements.</li>
 * <li>Fast add performance.</li>
 * </ul></p> 
 * 
 * <p>The main reason for the high speed of this RTree implementation is the 
 * avoidance of the creation of unnecessary objects, mainly achieved by using
 * primitive collections from the trove4j library.</p>
 * 
 * @author aled@sourceforge.net
 * @version 1.0b8
 */
public class RTree implements SpatialIndex {
  private static final Logger log = Logger.getLogger(RTree.class.getName());
  private static final Logger deleteLog = Logger.getLogger(RTree.class.getName() + "-delete");
  
  private static final String version = "1.0b8";
  
  // parameters of the tree
  private final static int DEFAULT_MAX_NODE_ENTRIES = 10;
  int maxNodeEntries;
  int minNodeEntries;
  
  // map of nodeId -> node object
  // TODO eliminate this map - it should not be needed. Nodes
  // can be found by traversing the tree.
  private TIntObjectHashMap nodeMap = new TIntObjectHashMap();
  
  // internal consistency checking - set to true if debugging tree corruption
  private final static boolean INTERNAL_CONSISTENCY_CHECKING = false;
  
  // used to mark the status of entries during a node split
  private final static int ENTRY_STATUS_ASSIGNED = 0;
  private final static int ENTRY_STATUS_UNASSIGNED = 1; 
  private byte[] entryStatus = null;
  private byte[] initialEntryStatus = null;
  
  // stacks used to store nodeId and entry index of each node 
  // from the root down to the leaf. Enables fast lookup
  // of nodes when a split is propagated up the tree.
  private TIntStack parents = new TIntStack();
  private TIntStack parentsEntry = new TIntStack();
  
  // initialisation
  private int treeHeight = 1; // leaves are always level 1
  private int rootNodeId = 0;
  private int size = 0;
  
  // Enables creation of new nodes
  private int highestUsedNodeId = rootNodeId; 
  
  // Deleted node objects are retained in the nodeMap, 
  // so that they can be reused. Store the IDs of nodes
  // which can be reused.
  private TIntStack deletedNodeIds = new TIntStack();
  
  // List of nearest rectangles. Use a member variable to
  // avoid recreating the object each time nearest() is called.
  private TIntArrayList nearestIds = new TIntArrayList();
  private TIntArrayList savedValues = new TIntArrayList();
  private float savedPriority = 0;

  // List of nearestN rectangles
  private SortedList nearestNIds = new SortedList();
  
  // List of nearestN rectanges, used in the alternative nearestN implementation.
  private PriorityQueue distanceQueue = 
    new PriorityQueue(PriorityQueue.SORT_ORDER_ASCENDING);
  
  /**
   * Constructor. Use init() method to initialize parameters of the RTree.
   */
  public RTree() {  
    return; // NOP    
  }
  
  //-------------------------------------------------------------------------
  // public implementation of SpatialIndex interface:
  //  init(Properties)
  //  add(Rectangle, int)
  //  delete(Rectangle, int)
  //  nearest(Point, TIntProcedure, float)
  //  intersects(Rectangle, TIntProcedure)
  //  contains(Rectangle, TIntProcedure)
  //  size()
  //-------------------------------------------------------------------------
  /**
   * <p>Initialize implementation dependent properties of the RTree.
   * Currently implemented properties are:
   * <ul>
   * <li>MaxNodeEntries</li> This specifies the maximum number of entries
   * in a node. The default value is 10, which is used if the property is
   * not specified, or is less than 2.
   * <li>MinNodeEntries</li> This specifies the minimum number of entries
   * in a node. The default value is half of the MaxNodeEntries value (rounded
   * down), which is used if the property is not specified or is less than 1.
   * </ul></p>
   * 
   * @see com.infomatiq.jsi.SpatialIndex#init(Properties)
   */
  public void init(Properties props) {
    if (props == null) {
      // use sensible defaults if null is passed in.
      maxNodeEntries = 50;
      minNodeEntries = 20;
    } else {
      maxNodeEntries = Integer.parseInt(props.getProperty("MaxNodeEntries", "0"));
      minNodeEntries = Integer.parseInt(props.getProperty("MinNodeEntries", "0"));
      
      // Obviously a node with less than 2 entries cannot be split.
      // The node splitting algorithm will work with only 2 entries
      // per node, but will be inefficient.
      if (maxNodeEntries < 2) { 
        log.warn("Invalid MaxNodeEntries = " + maxNodeEntries + " Resetting to default value of " + DEFAULT_MAX_NODE_ENTRIES);
        maxNodeEntries = DEFAULT_MAX_NODE_ENTRIES;
      }
      
      // The MinNodeEntries must be less than or equal to (int) (MaxNodeEntries / 2)
      if (minNodeEntries < 1 || minNodeEntries > maxNodeEntries / 2) {
        log.warn("MinNodeEntries must be between 1 and MaxNodeEntries / 2");
        minNodeEntries = maxNodeEntries / 2;
      }
    }
    
    entryStatus = new byte[maxNodeEntries];  
    initialEntryStatus = new byte[maxNodeEntries];
    
    for (int i = 0; i < maxNodeEntries; i++) {
      initialEntryStatus[i] = ENTRY_STATUS_UNASSIGNED;
    }
    
    Node root = new Node(rootNodeId, 1, maxNodeEntries);
    nodeMap.put(rootNodeId, root);
    
    log.debug("init() " + " MaxNodeEntries = " + maxNodeEntries + ", MinNodeEntries = " + minNodeEntries);
  }
  
  /**
   * @see com.infomatiq.jsi.SpatialIndex#add(Rectangle, int)
   */
  public void add(Rectangle r, int id) {
    if (log.isDebugEnabled()) {
      log.debug("Adding rectangle " + r + ", id " + id);
    }
    
    add(r.minX, r.minY, r.maxX, r.maxY, id, 1); 
    
    size++;
    
    if (INTERNAL_CONSISTENCY_CHECKING) {
      checkConsistency();
    }
  }
  
  /**
   * Adds a new entry at a specified level in the tree
   */
  private void add(float minX, float minY, float maxX, float maxY, int id, int level) {
    // I1 [Find position for new record] Invoke ChooseLeaf to select a 
    // leaf node L in which to place r
    Node n = chooseNode(minX, minY, maxX, maxY, level);
    Node newLeaf = null;
    
    // I2 [Add record to leaf node] If L has room for another entry, 
    // install E. Otherwise invoke SplitNode to obtain L and LL containing
    // E and all the old entries of L
    if (n.entryCount < maxNodeEntries) {
      n.addEntry(minX, minY, maxX, maxY, id);
    } else {
      newLeaf = splitNode(n, minX, minY, maxX, maxY, id);  
    }
    
    // I3 [Propagate changes upwards] Invoke AdjustTree on L, also passing LL
    // if a split was performed
    Node newNode = adjustTree(n, newLeaf); 

    // I4 [Grow tree taller] If node split propagation caused the root to 
    // split, create a new root whose children are the two resulting nodes.
    if (newNode != null) {
      int oldRootNodeId = rootNodeId;
      Node oldRoot = getNode(oldRootNodeId);
      
      rootNodeId = getNextNodeId();
      treeHeight++;
      Node root = new Node(rootNodeId, treeHeight, maxNodeEntries);
      root.addEntry(newNode.mbrMinX, newNode.mbrMinY, newNode.mbrMaxX, newNode.mbrMaxY, newNode.nodeId);
      root.addEntry(oldRoot.mbrMinX, oldRoot.mbrMinY, oldRoot.mbrMaxX, oldRoot.mbrMaxY, oldRoot.nodeId);
      nodeMap.put(rootNodeId, root);
    }    
  } 
  
  /**
   * @see com.infomatiq.jsi.SpatialIndex#delete(Rectangle, int)
   */
  public boolean delete(Rectangle r, int id) {
   // FindLeaf algorithm inlined here. Note the "official" algorithm 
   // searches all overlapping entries. This seems inefficient to me, 
   // as an entry is only worth searching if it contains (NOT overlaps)
   // the rectangle we are searching for.
   //
   // Also the algorithm has been changed so that it is not recursive.
    
    // FL1 [Search subtrees] If root is not a leaf, check each entry 
    // to determine if it contains r. For each entry found, invoke
    // findLeaf on the node pointed to by the entry, until r is found or
    // all entries have been checked.
  	parents.reset();
  	parents.push(rootNodeId);
  	
  	parentsEntry.reset();
  	parentsEntry.push(-1);
  	Node n = null;
  	int foundIndex = -1;  // index of entry to be deleted in leaf
  	
  	while (foundIndex == -1 && parents.size() > 0) {
  	  n = getNode(parents.peek());
  	  int startIndex = parentsEntry.peek() + 1;
      
      if (!n.isLeaf()) {
        deleteLog.debug("searching node " + n.nodeId + ", from index " + startIndex);
	  	  boolean contains = false;
        for (int i = startIndex; i < n.entryCount; i++) {
	  	    if (Rectangle.contains(n.entriesMinX[i], n.entriesMinY[i], n.entriesMaxX[i], n.entriesMaxY[i],
                                 r.minX, r.minY, r.maxX, r.maxY)) { 
	  	      parents.push(n.ids[i]);
	  	      parentsEntry.pop();
	  	      parentsEntry.push(i); // this becomes the start index when the child has been searched
	  	      parentsEntry.push(-1);
	  	      contains = true;
            break; // ie go to next iteration of while()
	  	    }
	  	  }
        if (contains) {
          continue;
        }
      } else {
        foundIndex = n.findEntry(r.minX, r.minY, r.maxX, r.maxY, id);        
      }
      
      parents.pop();
      parentsEntry.pop();
  	} // while not found
  	
  	if (foundIndex != -1) {
  	  n.deleteEntry(foundIndex);
      condenseTree(n);
      size--;
  	}
  	
    // shrink the tree if possible (i.e. if root node has exactly one entry,and that 
    // entry is not a leaf node, delete the root (it's entry becomes the new root)
    Node root = getNode(rootNodeId);
    while (root.entryCount == 1 && treeHeight > 1)
    {
        deletedNodeIds.push(rootNodeId);
        root.entryCount = 0;
        rootNodeId = root.ids[0];
        treeHeight--;
        root = getNode(rootNodeId);
    }
    
    // if the tree is now empty, then set the MBR of the root node back to it's original state
    // (this is only needed when the tree is empty, as this is the only state where an empty node
    // is not eliminated)
    if (size == 0) {
      root.mbrMinX = Float.MAX_VALUE;
      root.mbrMinY = Float.MAX_VALUE;
      root.mbrMaxX = -Float.MAX_VALUE;
      root.mbrMaxY = -Float.MAX_VALUE;
    }

    if (INTERNAL_CONSISTENCY_CHECKING) {
      checkConsistency();
    }
        
    return (foundIndex != -1);
  }
  
  /**
   * @see com.infomatiq.jsi.SpatialIndex#nearest(Point, TIntProcedure, float)
   */
  public void nearest(Point p, TIntProcedure v, float furthestDistance) {
    Node rootNode = getNode(rootNodeId);
   
    float furthestDistanceSq = furthestDistance * furthestDistance;
    nearest(p, rootNode, furthestDistanceSq);
   
    nearestIds.forEach(v);
    nearestIds.reset();
  }
   
  private void createNearestNDistanceQueue(Point p, int count, float furthestDistance) {
    distanceQueue.reset();
    distanceQueue.setSortOrder(PriorityQueue.SORT_ORDER_DESCENDING);
    
    //  return immediately if given an invalid "count" parameter
    if (count <= 0) {
      return;
    }    
    
    parents.reset();
    parents.push(rootNodeId);
    
    parentsEntry.reset();
    parentsEntry.push(-1);
    
    // TODO: possible shortcut here - could test for intersection with the 
    //       MBR of the root node. If no intersection, return immediately.
    
    float furthestDistanceSq = furthestDistance * furthestDistance;
    
    while (parents.size() > 0) {
      Node n = getNode(parents.peek());
      int startIndex = parentsEntry.peek() + 1;
      
      if (!n.isLeaf()) {
        // go through every entry in the index node to check
        // if it could contain an entry closer than the farthest entry
        // currently stored.
        boolean near = false;
        for (int i = startIndex; i < n.entryCount; i++) {
          if (Rectangle.distanceSq(n.entriesMinX[i], n.entriesMinY[i], 
                                 n.entriesMaxX[i], n.entriesMaxY[i], 
                                 p.x, p.y) <= furthestDistanceSq) {
            parents.push(n.ids[i]);
            parentsEntry.pop();
            parentsEntry.push(i); // this becomes the start index when the child has been searched
            parentsEntry.push(-1);
            near = true;
            break; // ie go to next iteration of while()
          }
        }
        if (near) {
          continue;
        }
      } else {
        // go through every entry in the leaf to check if 
        // it is currently one of the nearest N entries.
        for (int i = 0; i < n.entryCount; i++) {
          float entryDistanceSq = Rectangle.distanceSq(n.entriesMinX[i], n.entriesMinY[i],
                                                   n.entriesMaxX[i], n.entriesMaxY[i],
                                                   p.x, p.y);
          int entryId = n.ids[i];
          
          if (entryDistanceSq <= furthestDistanceSq) {
            distanceQueue.insert(entryId, entryDistanceSq);
            
            while (distanceQueue.size() > count) {
              // normal case - we can simply remove the lowest priority (highest distance) entry
              int value = distanceQueue.getValue();
              float distanceSq = distanceQueue.getPriority();
              distanceQueue.pop();
              
              // rare case - multiple items of the same priority (distance)
              if (distanceSq == distanceQueue.getPriority()) {
                savedValues.add(value);
                savedPriority = distanceSq;
              } else {
                savedValues.reset();
              }
            }
            
            // if the saved values have the same distance as the
            // next one in the tree, add them back in.
            if (savedValues.size() > 0 && savedPriority == distanceQueue.getPriority()) {
              for (int svi = 0; svi < savedValues.size(); svi++) {
                distanceQueue.insert(savedValues.get(svi), savedPriority);
              }
              savedValues.reset();
            }
            
            // narrow the search, if we have already found N items
            if (distanceQueue.getPriority() < furthestDistanceSq && distanceQueue.size() >= count) {
              furthestDistanceSq = distanceQueue.getPriority();  
            }
          } 
        }                       
      }
      parents.pop();
      parentsEntry.pop();  
    }
  }
  
  /**
   * @see com.infomatiq.jsi.SpatialIndex#nearestNUnsorted(Point, TIntProcedure, int, float)
   */
  public void nearestNUnsorted(Point p, TIntProcedure v, int count, float furthestDistance) {
    // This implementation is designed to give good performance
    // where
    //   o N is high (100+)
    //   o The results do not need to be sorted by distance.
    //     
    // Uses a priority queue as the underlying data structure. 
    //   
    // The behaviour of this algorithm has been carefully designed to
    // return exactly the same items as the the original version (nearestN_orig), in particular,
    // more than N items will be returned if items N and N+x have the
    // same priority. 
    createNearestNDistanceQueue(p, count, furthestDistance);
   
    while (distanceQueue.size() > 0) {
      v.execute(distanceQueue.getValue());
      distanceQueue.pop();
    }
  }
  
  /**
   * @see com.infomatiq.jsi.SpatialIndex#nearestN(Point, TIntProcedure, int, float)
   */
  public void nearestN(Point p, TIntProcedure v, int count, float furthestDistance) {
    createNearestNDistanceQueue(p, count, furthestDistance);
    
    distanceQueue.setSortOrder(PriorityQueue.SORT_ORDER_ASCENDING);
    
    while (distanceQueue.size() > 0) {
      v.execute(distanceQueue.getValue());
      distanceQueue.pop();
    }  
  }
    
  /**
   * @see com.infomatiq.jsi.SpatialIndex#nearestN(Point, TIntProcedure, int, float)
   * @deprecated Use new NearestN or NearestNUnsorted instead.
   * 
   * This implementation of nearestN is only suitable for small values of N (ie less than 10).
   */ 
  public void nearestN_orig(Point p, TIntProcedure v, int count, float furthestDistance) {
    // return immediately if given an invalid "count" parameter
    if (count <= 0) {
      return;
    }
    
    parents.reset();
    parents.push(rootNodeId);
    
    parentsEntry.reset();
    parentsEntry.push(-1);
    
    nearestNIds.init(count);
    
    // TODO: possible shortcut here - could test for intersection with the 
    //       MBR of the root node. If no intersection, return immediately.
    
    float furthestDistanceSq = furthestDistance * furthestDistance;
    
    while (parents.size() > 0) {
      Node n = getNode(parents.peek());
      int startIndex = parentsEntry.peek() + 1;
      
      if (!n.isLeaf()) {
        // go through every entry in the index node to check
        // if it could contain an entry closer than the farthest entry
        // currently stored.
        boolean near = false;
        for (int i = startIndex; i < n.entryCount; i++) {
          if (Rectangle.distanceSq(n.entriesMinX[i], n.entriesMinY[i], 
                                 n.entriesMaxX[i], n.entriesMaxY[i], 
                                 p.x, p.y) <= furthestDistanceSq) {
            parents.push(n.ids[i]);
            parentsEntry.pop();
            parentsEntry.push(i); // this becomes the start index when the child has been searched
            parentsEntry.push(-1);
            near = true;
            break; // ie go to next iteration of while()
          }
        }
        if (near) {
          continue;
        }
      } else {
        // go through every entry in the leaf to check if 
        // it is currently one of the nearest N entries.
        for (int i = 0; i < n.entryCount; i++) {
          float entryDistanceSq = Rectangle.distanceSq(n.entriesMinX[i], n.entriesMinY[i],
                                                   n.entriesMaxX[i], n.entriesMaxY[i],
                                                   p.x, p.y);
          int entryId = n.ids[i];
          
          if (entryDistanceSq <= furthestDistanceSq) {
            // add the new entry to the tree. Note that the higher the distance, the lower the priority
            nearestNIds.add(entryId, -entryDistanceSq);
            
            float tempFurthestDistanceSq = -nearestNIds.getLowestPriority();
            if (tempFurthestDistanceSq < furthestDistanceSq) {
              furthestDistanceSq = tempFurthestDistanceSq;  
            }
          } 
        }                       
      }
      parents.pop();
      parentsEntry.pop();  
    }
   
    nearestNIds.forEachId(v);
  }
   
  /**
   * @see com.infomatiq.jsi.SpatialIndex#intersects(Rectangle, TIntProcedure)
   */
  public void intersects(Rectangle r, TIntProcedure v) {
    Node rootNode = getNode(rootNodeId);
    intersects(r, v, rootNode);
  }

  /**
   * @see com.infomatiq.jsi.SpatialIndex#contains(Rectangle, TIntProcedure)
   */
  public void contains(Rectangle r, TIntProcedure v) {
    // find all rectangles in the tree that are contained by the passed rectangle
    // written to be non-recursive (should model other searches on this?)
        
    parents.reset();
    parents.push(rootNodeId);
    
    parentsEntry.reset();
    parentsEntry.push(-1);
    
    // TODO: possible shortcut here - could test for intersection with the 
    // MBR of the root node. If no intersection, return immediately.
    
    while (parents.size() > 0) {
      Node n = getNode(parents.peek());
      int startIndex = parentsEntry.peek() + 1;
      
      if (!n.isLeaf()) {
        // go through every entry in the index node to check
        // if it intersects the passed rectangle. If so, it 
        // could contain entries that are contained.
        boolean intersects = false;
        for (int i = startIndex; i < n.entryCount; i++) {
          if (Rectangle.intersects(r.minX, r.minY, r.maxX, r.maxY, 
                                   n.entriesMinX[i], n.entriesMinY[i], n.entriesMaxX[i], n.entriesMaxY[i])) {
            parents.push(n.ids[i]);
            parentsEntry.pop();
            parentsEntry.push(i); // this becomes the start index when the child has been searched
            parentsEntry.push(-1);
            intersects = true;
            break; // ie go to next iteration of while()
          }
        }
        if (intersects) {
          continue;
        }
      } else {
        // go through every entry in the leaf to check if 
        // it is contained by the passed rectangle
        for (int i = 0; i < n.entryCount; i++) {
          if (Rectangle.contains(r.minX, r.minY, r.maxX, r.maxY, 
                                 n.entriesMinX[i], n.entriesMinY[i], n.entriesMaxX[i], n.entriesMaxY[i])) {
            if (!v.execute(n.ids[i])) {
              return;
            }
          } 
        }                       
      }
      parents.pop();
      parentsEntry.pop();  
    }
  }

  /**
   * @see com.infomatiq.jsi.SpatialIndex#size()
   */
  public int size() {
    return size;
  }

  /**
   * @see com.infomatiq.jsi.SpatialIndex#getBounds()
   */
  public Rectangle getBounds() {
    Rectangle bounds = null;
    
    Node n = getNode(getRootNodeId());
    if (n != null && n.entryCount > 0) {
      bounds = new Rectangle();
      bounds.minX = n.mbrMinX;
      bounds.minY = n.mbrMinY;
      bounds.maxX = n.mbrMaxX;
      bounds.maxY = n.mbrMaxY;
    }
    return bounds;
  }
    
  /**
   * @see com.infomatiq.jsi.SpatialIndex#getVersion()
   */
  public String getVersion() {
    return "RTree-" + version;
  }
  //-------------------------------------------------------------------------
  // end of SpatialIndex methods
  //-------------------------------------------------------------------------
  
  /**
   * Get the next available node ID. Reuse deleted node IDs if
   * possible
   */
  private int getNextNodeId() {
    int nextNodeId = 0;
    if (deletedNodeIds.size() > 0) {
      nextNodeId = deletedNodeIds.pop();
    } else {
      nextNodeId = 1 + highestUsedNodeId++;
    }
    return nextNodeId;
  }

  /**
   * Get a node object, given the ID of the node.
   */
  public Node getNode(int id) {
    return (Node) nodeMap.get(id);
  }

  /**
   * Get the highest used node ID
   */  
  public int getHighestUsedNodeId() {
    return highestUsedNodeId;
  }

  /**
   * Get the root node ID
   */
  public int getRootNodeId() {
    return rootNodeId; 
  }
      
  /**
   * Split a node. Algorithm is taken pretty much verbatim from
   * Guttman's original paper.
   * 
   * @return new node object.
   */
  private Node splitNode(Node n, float newRectMinX, float newRectMinY, float newRectMaxX, float newRectMaxY, int newId) {
    // [Pick first entry for each group] Apply algorithm pickSeeds to 
    // choose two entries to be the first elements of the groups. Assign
    // each to a group.
    
    // debug code
    float initialArea = 0;
    if (log.isDebugEnabled()) {
      float unionMinX = Math.min(n.mbrMinX, newRectMinX);
      float unionMinY = Math.min(n.mbrMinY, newRectMinY);
      float unionMaxX = Math.max(n.mbrMaxX, newRectMaxX);
      float unionMaxY = Math.max(n.mbrMaxY, newRectMaxY);
      
      initialArea = (unionMaxX - unionMinX) * (unionMaxY - unionMinY);
    }
       
    System.arraycopy(initialEntryStatus, 0, entryStatus, 0, maxNodeEntries);
    
    Node newNode = null;
    newNode = new Node(getNextNodeId(), n.level, maxNodeEntries);
    nodeMap.put(newNode.nodeId, newNode);
    
    pickSeeds(n, newRectMinX, newRectMinY, newRectMaxX, newRectMaxY, newId, newNode); // this also sets the entryCount to 1
    
    // [Check if done] If all entries have been assigned, stop. If one
    // group has so few entries that all the rest must be assigned to it in 
    // order for it to have the minimum number m, assign them and stop. 
    while (n.entryCount + newNode.entryCount < maxNodeEntries + 1) {
      if (maxNodeEntries + 1 - newNode.entryCount == minNodeEntries) {
        // assign all remaining entries to original node
        for (int i = 0; i < maxNodeEntries; i++) {
          if (entryStatus[i] == ENTRY_STATUS_UNASSIGNED) {
            entryStatus[i] = ENTRY_STATUS_ASSIGNED;
            
            if (n.entriesMinX[i] < n.mbrMinX) n.mbrMinX = n.entriesMinX[i];
            if (n.entriesMinY[i] < n.mbrMinY) n.mbrMinY = n.entriesMinY[i];
            if (n.entriesMaxX[i] > n.mbrMaxX) n.mbrMaxX = n.entriesMaxX[i];
            if (n.entriesMaxY[i] > n.mbrMaxY) n.mbrMaxY = n.entriesMaxY[i];
            
            n.entryCount++;
          }
        }
        break;
      }   
      if (maxNodeEntries + 1 - n.entryCount == minNodeEntries) {
        // assign all remaining entries to new node
        for (int i = 0; i < maxNodeEntries; i++) {
          if (entryStatus[i] == ENTRY_STATUS_UNASSIGNED) {
            entryStatus[i] = ENTRY_STATUS_ASSIGNED;
            newNode.addEntry(n.entriesMinX[i], n.entriesMinY[i], n.entriesMaxX[i], n.entriesMaxY[i], n.ids[i]);
            n.ids[i] = -1; // an id of -1 indicates the entry is not in use
          }
        }
        break;
      }
      
      // [Select entry to assign] Invoke algorithm pickNext to choose the
      // next entry to assign. Add it to the group whose covering rectangle 
      // will have to be enlarged least to accommodate it. Resolve ties
      // by adding the entry to the group with smaller area, then to the 
      // the one with fewer entries, then to either. Repeat from S2
      pickNext(n, newNode);   
    }
      
    n.reorganize(this);
    
    // check that the MBR stored for each node is correct.
    if (INTERNAL_CONSISTENCY_CHECKING) {
      Rectangle nMBR = new Rectangle(n.mbrMinX, n.mbrMinY, n.mbrMaxX, n.mbrMaxY);
      if (!nMBR.equals(calculateMBR(n))) {
        log.error("Error: splitNode old node MBR wrong");
      }
      Rectangle newNodeMBR = new Rectangle(newNode.mbrMinX, newNode.mbrMinY, newNode.mbrMaxX, newNode.mbrMaxY);
      if (!newNodeMBR.equals(calculateMBR(newNode))) {
        log.error("Error: splitNode new node MBR wrong");
      }
    }
    
    // debug code
    if (log.isDebugEnabled()) {
      float newArea = Rectangle.area(n.mbrMinX, n.mbrMinY, n.mbrMaxX, n.mbrMaxY) + 
                      Rectangle.area(newNode.mbrMinX, newNode.mbrMinY, newNode.mbrMaxX, newNode.mbrMaxY);
      float percentageIncrease = (100 * (newArea - initialArea)) / initialArea;
      log.debug("Node " + n.nodeId + " split. New area increased by " + percentageIncrease + "%");   
    }
      
    return newNode;
  }
  
  /**
   * Pick the seeds used to split a node.
   * Select two entries to be the first elements of the groups
   */
  private void pickSeeds(Node n, float newRectMinX, float newRectMinY, float newRectMaxX, float newRectMaxY, int newId, Node newNode) {
    // Find extreme rectangles along all dimension. Along each dimension,
    // find the entry whose rectangle has the highest low side, and the one 
    // with the lowest high side. Record the separation.
    float maxNormalizedSeparation = -1; // initialize to -1 so that even overlapping rectangles will be considered for the seeds
    int highestLowIndex = -1;
    int lowestHighIndex = -1;
    
    // for the purposes of picking seeds, take the MBR of the node to include
    // the new rectangle aswell.
    if (newRectMinX < n.mbrMinX) n.mbrMinX = newRectMinX;
    if (newRectMinY < n.mbrMinY) n.mbrMinY = newRectMinY;
    if (newRectMaxX > n.mbrMaxX) n.mbrMaxX = newRectMaxX;
    if (newRectMaxY > n.mbrMaxY) n.mbrMaxY = newRectMaxY;
    
    float mbrLenX = n.mbrMaxX - n.mbrMinX;
    float mbrLenY = n.mbrMaxY - n.mbrMinY;
    
    if (log.isDebugEnabled()) {
      log.debug("pickSeeds(): NodeId = " + n.nodeId);
    }
    
    float tempHighestLow = newRectMinX;
    int tempHighestLowIndex = -1; // -1 indicates the new rectangle is the seed
    
    float tempLowestHigh = newRectMaxX;
    int tempLowestHighIndex = -1; // -1 indicates the new rectangle is the seed 
    
    for (int i = 0; i < n.entryCount; i++) {
      float tempLow = n.entriesMinX[i];
      if (tempLow >= tempHighestLow) {
         tempHighestLow = tempLow;
         tempHighestLowIndex = i;
      } else {  // ensure that the same index cannot be both lowestHigh and highestLow
        float tempHigh = n.entriesMaxX[i];
        if (tempHigh <= tempLowestHigh) {
          tempLowestHigh = tempHigh;
          tempLowestHighIndex = i;
        }
      }
      
      // PS2 [Adjust for shape of the rectangle cluster] Normalize the separations
      // by dividing by the widths of the entire set along the corresponding
      // dimension
      float normalizedSeparation = mbrLenX == 0 ? 1 : (tempHighestLow - tempLowestHigh) / mbrLenX;
      if (normalizedSeparation > 1 || normalizedSeparation < -1) {
        log.error("Invalid normalized separation X");
      }
      
      if (log.isDebugEnabled()) {
              log.debug("Entry " + i + ", dimension X: HighestLow = " + tempHighestLow + 
                        " (index " + tempHighestLowIndex + ")" + ", LowestHigh = " +
                        tempLowestHigh + " (index " + tempLowestHighIndex + ", NormalizedSeparation = " + normalizedSeparation);
      }
          
      // PS3 [Select the most extreme pair] Choose the pair with the greatest
      // normalized separation along any dimension.
      // Note that if negative it means the rectangles overlapped. However still include
      // overlapping rectangles if that is the only choice available.
      if (normalizedSeparation >= maxNormalizedSeparation) {
        highestLowIndex = tempHighestLowIndex;
        lowestHighIndex = tempLowestHighIndex;
        maxNormalizedSeparation = normalizedSeparation;
      }
    }
    
    // Repeat for the Y dimension
    tempHighestLow = newRectMinY;
    tempHighestLowIndex = -1; // -1 indicates the new rectangle is the seed
    
    tempLowestHigh = newRectMaxY;
    tempLowestHighIndex = -1; // -1 indicates the new rectangle is the seed 
    
    for (int i = 0; i < n.entryCount; i++) {
      float tempLow = n.entriesMinY[i];
      if (tempLow >= tempHighestLow) {
         tempHighestLow = tempLow;
         tempHighestLowIndex = i;
      } else {  // ensure that the same index cannot be both lowestHigh and highestLow
        float tempHigh = n.entriesMaxY[i];
        if (tempHigh <= tempLowestHigh) {
          tempLowestHigh = tempHigh;
          tempLowestHighIndex = i;
        }
      }
      
      // PS2 [Adjust for shape of the rectangle cluster] Normalize the separations
      // by dividing by the widths of the entire set along the corresponding
      // dimension
      float normalizedSeparation = mbrLenY == 0 ? 1 : (tempHighestLow - tempLowestHigh) / mbrLenY;
      if (normalizedSeparation > 1 || normalizedSeparation < -1) {
        log.error("Invalid normalized separation Y");
      }
      
      if (log.isDebugEnabled()) {
        log.debug("Entry " + i + ", dimension Y: HighestLow = " + tempHighestLow + 
                  " (index " + tempHighestLowIndex + ")" + ", LowestHigh = " +
                  tempLowestHigh + " (index " + tempLowestHighIndex + ", NormalizedSeparation = " + normalizedSeparation);
      }
          
      // PS3 [Select the most extreme pair] Choose the pair with the greatest
      // normalized separation along any dimension.
      // Note that if negative it means the rectangles overlapped. However still include
      // overlapping rectangles if that is the only choice available.
      if (normalizedSeparation >= maxNormalizedSeparation) {
        highestLowIndex = tempHighestLowIndex;
        lowestHighIndex = tempLowestHighIndex;
        maxNormalizedSeparation = normalizedSeparation;
      }
    }
    
    // At this point it is possible that the new rectangle is both highestLow and lowestHigh.
    // This can happen if all rectangles in the node overlap the new rectangle.
    // Resolve this by declaring that the highestLowIndex is the lowest Y and,
    // the lowestHighIndex is the largest X (but always a different rectangle)
    if (highestLowIndex == lowestHighIndex) { 
      highestLowIndex = -1;
      float tempMinY = newRectMinY;
      lowestHighIndex = 0;
      float tempMaxX = n.entriesMaxX[0];
      
      for (int i = 1; i < n.entryCount; i++) {
        if (n.entriesMinY[i] < tempMinY) {
          tempMinY = n.entriesMinY[i];
          highestLowIndex = i;
        }
        else if (n.entriesMaxX[i] > tempMaxX) {
          tempMaxX = n.entriesMaxX[i];
          lowestHighIndex = i;
        }
      }
    }
    
    // highestLowIndex is the seed for the new node.
    if (highestLowIndex == -1) {
      newNode.addEntry(newRectMinX, newRectMinY, newRectMaxX, newRectMaxY, newId);
    } else {
      newNode.addEntry(n.entriesMinX[highestLowIndex], n.entriesMinY[highestLowIndex], 
                       n.entriesMaxX[highestLowIndex], n.entriesMaxY[highestLowIndex], 
                       n.ids[highestLowIndex]);
      n.ids[highestLowIndex] = -1;
      
      // move the new rectangle into the space vacated by the seed for the new node
      n.entriesMinX[highestLowIndex] = newRectMinX;
      n.entriesMinY[highestLowIndex] = newRectMinY;
      n.entriesMaxX[highestLowIndex] = newRectMaxX;
      n.entriesMaxY[highestLowIndex] = newRectMaxY;
      
      n.ids[highestLowIndex] = newId;
    }
    
    // lowestHighIndex is the seed for the original node. 
    if (lowestHighIndex == -1) {
      lowestHighIndex = highestLowIndex;
    }
    
    entryStatus[lowestHighIndex] = ENTRY_STATUS_ASSIGNED;
    n.entryCount = 1;
    n.mbrMinX = n.entriesMinX[lowestHighIndex];
    n.mbrMinY = n.entriesMinY[lowestHighIndex];
    n.mbrMaxX = n.entriesMaxX[lowestHighIndex];
    n.mbrMaxY = n.entriesMaxY[lowestHighIndex];
  }

  /** 
   * Pick the next entry to be assigned to a group during a node split.
   * 
   * [Determine cost of putting each entry in each group] For each 
   * entry not yet in a group, calculate the area increase required
   * in the covering rectangles of each group  
   */
  private int pickNext(Node n, Node newNode) {
    float maxDifference = Float.NEGATIVE_INFINITY;
    int next = 0;
    int nextGroup = 0;
    
    maxDifference = Float.NEGATIVE_INFINITY;
   
    if (log.isDebugEnabled()) {
      log.debug("pickNext()");
    }
   
    for (int i = 0; i < maxNodeEntries; i++) {
      if (entryStatus[i] == ENTRY_STATUS_UNASSIGNED) {
        
        if (n.ids[i] == -1) {
          log.error("Error: Node " + n.nodeId + ", entry " + i + " is null");
        }
        
        float nIncrease = Rectangle.enlargement(n.mbrMinX, n.mbrMinY, n.mbrMaxX, n.mbrMaxY, 
                                                n.entriesMinX[i], n.entriesMinY[i], n.entriesMaxX[i], n.entriesMaxY[i]);
        float newNodeIncrease = Rectangle.enlargement(newNode.mbrMinX, newNode.mbrMinY, newNode.mbrMaxX, newNode.mbrMaxY,
                                                      n.entriesMinX[i], n.entriesMinY[i], n.entriesMaxX[i], n.entriesMaxY[i]);

        float difference = Math.abs(nIncrease - newNodeIncrease);
         
        if (difference > maxDifference) {
          next = i;
          
          if (nIncrease < newNodeIncrease) {
            nextGroup = 0; 
          } else if (newNodeIncrease < nIncrease) {
            nextGroup = 1;
          } else if (Rectangle.area(n.mbrMinX, n.mbrMinY, n.mbrMaxX, n.mbrMaxY) < Rectangle.area(newNode.mbrMinX, newNode.mbrMinY, newNode.mbrMaxX, newNode.mbrMaxY)) {
            nextGroup = 0;
          } else if (Rectangle.area(newNode.mbrMinX, newNode.mbrMinY, newNode.mbrMaxX, newNode.mbrMaxY) < Rectangle.area(n.mbrMinX, n.mbrMinY, n.mbrMaxX, n.mbrMaxY)) {
            nextGroup = 1;
          } else if (newNode.entryCount < maxNodeEntries / 2) {
            nextGroup = 0;
          } else {
            nextGroup = 1;
          }
          maxDifference = difference; 
        }
        if (log.isDebugEnabled()) {
          log.debug("Entry " + i + " group0 increase = " + nIncrease + ", group1 increase = " + newNodeIncrease +
                    ", diff = " + difference + ", MaxDiff = " + maxDifference + " (entry " + next + ")");
        }
      }
    }
    
    entryStatus[next] = ENTRY_STATUS_ASSIGNED;
      
    if (nextGroup == 0) {
      if (n.entriesMinX[next] < n.mbrMinX) n.mbrMinX = n.entriesMinX[next];
      if (n.entriesMinY[next] < n.mbrMinY) n.mbrMinY = n.entriesMinY[next];
      if (n.entriesMaxX[next] > n.mbrMaxX) n.mbrMaxX = n.entriesMaxX[next];
      if (n.entriesMaxY[next] > n.mbrMaxY) n.mbrMaxY = n.entriesMaxY[next];
      n.entryCount++;
    } else {
      // move to new node.
      newNode.addEntry(n.entriesMinX[next], n.entriesMinY[next], n.entriesMaxX[next], n.entriesMaxY[next], n.ids[next]);
      n.ids[next] = -1;
    }
    
    return next; 
  }

  /**
   * Recursively searches the tree for the nearest entry. Other queries
   * call execute() on an IntProcedure when a matching entry is found; 
   * however nearest() must store the entry Ids as it searches the tree,
   * in case a nearer entry is found.
   * Uses the member variable nearestIds to store the nearest
   * entry IDs.
   * 
   * TODO rewrite this to be non-recursive?
   */
  private float nearest(Point p, Node n, float furthestDistanceSq) {
    for (int i = 0; i < n.entryCount; i++) {
      float tempDistanceSq = Rectangle.distanceSq(n.entriesMinX[i], n.entriesMinY[i], n.entriesMaxX[i], n.entriesMaxY[i], p.x, p.y);
      if (n.isLeaf()) { // for leaves, the distance is an actual nearest distance 
        if (tempDistanceSq < furthestDistanceSq) {
          furthestDistanceSq = tempDistanceSq;
          nearestIds.reset();
        }
        if (tempDistanceSq <= furthestDistanceSq) {
          nearestIds.add(n.ids[i]);
        }     
      } else { // for index nodes, only go into them if they potentially could have
               // a rectangle nearer than actualNearest
         if (tempDistanceSq <= furthestDistanceSq) {
           // search the child node
           furthestDistanceSq = nearest(p, getNode(n.ids[i]), furthestDistanceSq);
         }
      }
    }
    return furthestDistanceSq;
  }
  
  /** 
   * Recursively searches the tree for all intersecting entries.
   * Immediately calls execute() on the passed IntProcedure when 
   * a matching entry is found.
   * 
   * TODO rewrite this to be non-recursive? Make sure it
   * doesn't slow it down.
   */
  private boolean intersects(Rectangle r, TIntProcedure v, Node n) {
    for (int i = 0; i < n.entryCount; i++) {
      if (Rectangle.intersects(r.minX, r.minY, r.maxX, r.maxY, n.entriesMinX[i], n.entriesMinY[i], n.entriesMaxX[i], n.entriesMaxY[i])) {
        if (n.isLeaf()) {
          if (!v.execute(n.ids[i])) {
            return false;
          }
        } else {
          Node childNode = getNode(n.ids[i]);
          if (!intersects(r, v, childNode)) {
            return false;
          }
        }
      }
    }
    return true;
  }

  /**
   * Used by delete(). Ensures that all nodes from the passed node
   * up to the root have the minimum number of entries.
   * 
   * Note that the parent and parentEntry stacks are expected to
   * contain the nodeIds of all parents up to the root.
   */
  private void condenseTree(Node l) {
    // CT1 [Initialize] Set n=l. Set the list of eliminated
    // nodes to be empty.
    Node n = l;
    Node parent = null;
    int parentEntry = 0;
    
    TIntStack eliminatedNodeIds = new TIntStack();
  
    // CT2 [Find parent entry] If N is the root, go to CT6. Otherwise 
    // let P be the parent of N, and let En be N's entry in P  
    while (n.level != treeHeight) {
      parent = getNode(parents.pop());
      parentEntry = parentsEntry.pop();
      
      // CT3 [Eliminiate under-full node] If N has too few entries,
      // delete En from P and add N to the list of eliminated nodes
      if (n.entryCount < minNodeEntries) {
        parent.deleteEntry(parentEntry);
        eliminatedNodeIds.push(n.nodeId);
      } else {
        // CT4 [Adjust covering rectangle] If N has not been eliminated,
        // adjust EnI to tightly contain all entries in N
        if (n.mbrMinX != parent.entriesMinX[parentEntry] ||
            n.mbrMinY != parent.entriesMinY[parentEntry] ||
            n.mbrMaxX != parent.entriesMaxX[parentEntry] ||
            n.mbrMaxY != parent.entriesMaxY[parentEntry]) {
          float deletedMinX = parent.entriesMinX[parentEntry];
          float deletedMinY = parent.entriesMinY[parentEntry];
          float deletedMaxX = parent.entriesMaxX[parentEntry];
          float deletedMaxY = parent.entriesMaxY[parentEntry];
          parent.entriesMinX[parentEntry] = n.mbrMinX;
          parent.entriesMinY[parentEntry] = n.mbrMinY;
          parent.entriesMaxX[parentEntry] = n.mbrMaxX;
          parent.entriesMaxY[parentEntry] = n.mbrMaxY;
          parent.recalculateMBRIfInfluencedBy(deletedMinX, deletedMinY, deletedMaxX, deletedMaxY);
        }
      }
      // CT5 [Move up one level in tree] Set N=P and repeat from CT2
      n = parent;
    }
    
    // CT6 [Reinsert orphaned entries] Reinsert all entries of nodes in set Q.
    // Entries from eliminated leaf nodes are reinserted in tree leaves as in 
    // Insert(), but entries from higher level nodes must be placed higher in 
    // the tree, so that leaves of their dependent subtrees will be on the same
    // level as leaves of the main tree
    while (eliminatedNodeIds.size() > 0) {
      Node e = getNode(eliminatedNodeIds.pop());
      for (int j = 0; j < e.entryCount; j++) {
        add(e.entriesMinX[j], e.entriesMinY[j], e.entriesMaxX[j], e.entriesMaxY[j], e.ids[j], e.level); 
        e.ids[j] = -1;
      }
      e.entryCount = 0;
      deletedNodeIds.push(e.nodeId);
    }
  }

  /**
   *  Used by add(). Chooses a leaf to add the rectangle to.
   */
  private Node chooseNode(float minX, float minY, float maxX, float maxY, int level) {
    // CL1 [Initialize] Set N to be the root node
    Node n = getNode(rootNodeId);
    parents.reset();
    parentsEntry.reset();
     
    // CL2 [Leaf check] If N is a leaf, return N
    while (true) {
      if (n == null) {
        log.error("Could not get root node (" + rootNodeId + ")");  
      }
   
      if (n.level == level) {
        return n;
      }
      
      // CL3 [Choose subtree] If N is not at the desired level, let F be the entry in N 
      // whose rectangle FI needs least enlargement to include EI. Resolve
      // ties by choosing the entry with the rectangle of smaller area.
      float leastEnlargement = Rectangle.enlargement(n.entriesMinX[0], n.entriesMinY[0], n.entriesMaxX[0], n.entriesMaxY[0],
                                                     minX, minY, maxX, maxY);
      int index = 0; // index of rectangle in subtree
      for (int i = 1; i < n.entryCount; i++) {
        float tempMinX = n.entriesMinX[i];
        float tempMinY = n.entriesMinY[i];
        float tempMaxX = n.entriesMaxX[i];
        float tempMaxY = n.entriesMaxY[i];
        float tempEnlargement = Rectangle.enlargement(tempMinX, tempMinY, tempMaxX, tempMaxY, 
                                                      minX, minY, maxX, maxY);
        if ((tempEnlargement < leastEnlargement) ||
            ((tempEnlargement == leastEnlargement) && 
             (Rectangle.area(tempMinX, tempMinY, tempMaxX, tempMaxY) < 
              Rectangle.area(n.entriesMinX[index], n.entriesMinY[index], n.entriesMaxX[index], n.entriesMaxY[index])))) {
          index = i;
          leastEnlargement = tempEnlargement;
        }
      }
      
      parents.push(n.nodeId);
      parentsEntry.push(index);
    
      // CL4 [Descend until a leaf is reached] Set N to be the child node 
      // pointed to by Fp and repeat from CL2
      n = getNode(n.ids[index]);
    }
  }
  
  /**
   * Ascend from a leaf node L to the root, adjusting covering rectangles and
   * propagating node splits as necessary.
   */
  private Node adjustTree(Node n, Node nn) {
    // AT1 [Initialize] Set N=L. If L was split previously, set NN to be 
    // the resulting second node.
    
    // AT2 [Check if done] If N is the root, stop
    while (n.level != treeHeight) {
    
      // AT3 [Adjust covering rectangle in parent entry] Let P be the parent 
      // node of N, and let En be N's entry in P. Adjust EnI so that it tightly
      // encloses all entry rectangles in N.
      Node parent = getNode(parents.pop());
      int entry = parentsEntry.pop(); 
      
      if (parent.ids[entry] != n.nodeId) {
        log.error("Error: entry " + entry + " in node " + 
             parent.nodeId + " should point to node " + 
             n.nodeId + "; actually points to node " + parent.ids[entry]);
      }
   
      if (parent.entriesMinX[entry] != n.mbrMinX ||
          parent.entriesMinY[entry] != n.mbrMinY ||
          parent.entriesMaxX[entry] != n.mbrMaxX ||
          parent.entriesMaxY[entry] != n.mbrMaxY) {
   
        parent.entriesMinX[entry] = n.mbrMinX;
        parent.entriesMinY[entry] = n.mbrMinY;
        parent.entriesMaxX[entry] = n.mbrMaxX;
        parent.entriesMaxY[entry] = n.mbrMaxY;

        parent.recalculateMBR();
      }
      
      // AT4 [Propagate node split upward] If N has a partner NN resulting from 
      // an earlier split, create a new entry Enn with Ennp pointing to NN and 
      // Enni enclosing all rectangles in NN. Add Enn to P if there is room. 
      // Otherwise, invoke splitNode to produce P and PP containing Enn and
      // all P's old entries.
      Node newNode = null;
      if (nn != null) {
        if (parent.entryCount < maxNodeEntries) {
          parent.addEntry(nn.mbrMinX, nn.mbrMinY, nn.mbrMaxX, nn.mbrMaxY, nn.nodeId);
        } else {
          newNode = splitNode(parent, nn.mbrMinX, nn.mbrMinY, nn.mbrMaxX, nn.mbrMaxY, nn.nodeId);
        }
      }
      
      // AT5 [Move up to next level] Set N = P and set NN = PP if a split 
      // occurred. Repeat from AT2
      n = parent;
      nn = newNode;
      
      parent = null;
      newNode = null;
    }
    
    return nn;
  }
  
  
  /**
   * Check the consistency of the tree.
   * 
   * @return false if an inconsistency is detected, true otherwise.
   */
  public boolean checkConsistency() {
    return checkConsistency(rootNodeId, treeHeight, null);
  }
  
  private boolean checkConsistency(int nodeId, int expectedLevel, Rectangle expectedMBR) {
    // go through the tree, and check that the internal data structures of 
    // the tree are not corrupted.        
    Node n = getNode(nodeId);
    
    if (n == null) {
      log.error("Error: Could not read node " + nodeId);
      return false;
    }
    
    // if tree is empty, then there should be exactly one node, at level 1
    // TODO: also check the MBR is as for a new node
    if (nodeId == rootNodeId && size() == 0) {
      if (n.level != 1) {
        log.error("Error: tree is empty but root node is not at level 1");
        return false;
      }
    }
    
    if (n.level != expectedLevel) {
      log.error("Error: Node " + nodeId + ", expected level " + expectedLevel + ", actual level " + n.level);
      return false;
    }
    
    Rectangle calculatedMBR = calculateMBR(n);
    Rectangle actualMBR = new Rectangle();
    actualMBR.minX = n.mbrMinX;
    actualMBR.minY = n.mbrMinY;
    actualMBR.maxX = n.mbrMaxX;
    actualMBR.maxY = n.mbrMaxY;
    if (!actualMBR.equals(calculatedMBR)) {
      log.error("Error: Node " + nodeId + ", calculated MBR does not equal stored MBR");
      if (actualMBR.minX != n.mbrMinX) log.error("  actualMinX=" + actualMBR.minX + ", calc=" + calculatedMBR.minX);
      if (actualMBR.minY != n.mbrMinY) log.error("  actualMinY=" + actualMBR.minY + ", calc=" + calculatedMBR.minY);
      if (actualMBR.maxX != n.mbrMaxX) log.error("  actualMaxX=" + actualMBR.maxX + ", calc=" + calculatedMBR.maxX);
      if (actualMBR.maxY != n.mbrMaxY) log.error("  actualMaxY=" + actualMBR.maxY + ", calc=" + calculatedMBR.maxY);
      return false;
    }
    
    if (expectedMBR != null && !actualMBR.equals(expectedMBR)) {
      log.error("Error: Node " + nodeId + ", expected MBR (from parent) does not equal stored MBR");
      return false;
    }
    
    // Check for corruption where a parent entry is the same object as the child MBR
    if (expectedMBR != null && actualMBR.sameObject(expectedMBR)) {
      log.error("Error: Node " + nodeId + " MBR using same rectangle object as parent's entry");
      return false;
    }
    
    for (int i = 0; i < n.entryCount; i++) {
      if (n.ids[i] == -1) {
        log.error("Error: Node " + nodeId + ", Entry " + i + " is null");
        return false;
      }     
      
      if (n.level > 1) { // if not a leaf
        if (!checkConsistency(n.ids[i], n.level - 1, new Rectangle(n.entriesMinX[i], n.entriesMinY[i], n.entriesMaxX[i], n.entriesMaxY[i]))) {
          return false;
        }
      }   
    }
    return true;
  }
  
  /**
   * Given a node object, calculate the node MBR from it's entries.
   * Used in consistency checking
   */
  private Rectangle calculateMBR(Node n) {
    Rectangle mbr = new Rectangle();
   
    for (int i = 0; i < n.entryCount; i++) {
      if (n.entriesMinX[i] < mbr.minX) mbr.minX = n.entriesMinX[i];
      if (n.entriesMinY[i] < mbr.minY) mbr.minY = n.entriesMinY[i];
      if (n.entriesMaxX[i] > mbr.maxX) mbr.maxX = n.entriesMaxX[i];
      if (n.entriesMaxY[i] > mbr.maxY) mbr.maxY = n.entriesMaxY[i];
    }
    return mbr; 
  }
}
