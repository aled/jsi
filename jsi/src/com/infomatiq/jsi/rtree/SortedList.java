package com.infomatiq.jsi.rtree;

import gnu.trove.TFloatArrayList;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntProcedure;

/**
 * @author aled.morris@infomatiq.co.uk
 * 
 * Sorted List, backed by a TArrayList.
 * 
 * The elements in the list are always ordered by priority.
 * Methods exists to remove elements with the highest and lowest priorities.
 * 
 * If more than one element has the highest priority, they will
 * all be removed by calling removeHighest. Ditto for the lowest priority.
 * 
 * The list has a preferred maximum size. If possible, entries with the lowest priority
 * will be removed to limit the maximum size. Note that entries with the lowest priority
 * will not be removed if this would leave fewer than the preferred maximum number
 * of entries.
 * 
 * This class is not optimised for large values of preferredMaximumSize. Values greater than,
 * say, 5, are not recommended.
 */
public class SortedList {
  private static final int DEFAULT_PREFERRED_MAXIMUM_SIZE = 10;
  
  private int preferredMaximumSize = 1;
  private TIntArrayList ids = null;
  private TFloatArrayList priorities = null;
  
  public void init(int preferredMaximumSize) {
    this.preferredMaximumSize = preferredMaximumSize;
    ids.clear(preferredMaximumSize);
    priorities.clear(preferredMaximumSize); 
  }  
 
  public void reset() {
    ids.reset();
    priorities.reset();
  }
 
  public SortedList() {
    ids = new TIntArrayList(DEFAULT_PREFERRED_MAXIMUM_SIZE);
    priorities = new TFloatArrayList(DEFAULT_PREFERRED_MAXIMUM_SIZE);
  }
  
  public void add(int id, float priority) {
    float lowestPriority = Float.NEGATIVE_INFINITY;
    
    if (priorities.size() > 0) {
      lowestPriority = priorities.get(priorities.size() - 1);
    }
    
    if ((priority == lowestPriority) ||
        (priority < lowestPriority && ids.size() < preferredMaximumSize)) { 
      // simply add the new entry at the lowest priority end
      ids.add(id);
      priorities.add(priority);
    } else if (priority > lowestPriority) {
      if (ids.size() >= preferredMaximumSize) {
        int lowestPriorityIndex = ids.size() - 1;
        while ((lowestPriorityIndex - 1 >= 0) &&
               (priorities.get(lowestPriorityIndex - 1) == lowestPriority)) {
          lowestPriorityIndex--;
        }
        
        if (lowestPriorityIndex >= preferredMaximumSize - 1) {
          ids.remove(lowestPriorityIndex, ids.size() - lowestPriorityIndex); 
          priorities.remove(lowestPriorityIndex, priorities.size() - lowestPriorityIndex); 
        }
      }
    
      // put the new entry in the correct position. Could do a binary search here if the 
      // preferredMaximumSize was large.
      int insertPosition = ids.size();
      while (insertPosition - 1 >= 0 && priority > priorities.get(insertPosition - 1)) {
        insertPosition--;  
      }
      
      ids.insert(insertPosition, id);
      priorities.insert(insertPosition, priority);
    }
  }
  
  /**
   * return the lowest priority currently stored, or Float.NEGATIVE_INFINITY if no
   * entries are stored
   */
  public float getLowestPriority() {
    float lowestPriority = Float.NEGATIVE_INFINITY;
    if (priorities.size() >= preferredMaximumSize) {
      lowestPriority = priorities.get(priorities.size() - 1);
    }
    return lowestPriority;
  }
  
  public void forEachId(TIntProcedure v) {
    for (int i = 0; i < ids.size(); i++) {
      if (!v.execute(ids.get(i))) {
        break;
      }
    }
  }
  
  public int[] toNativeArray() {
    return ids.toNativeArray(); 
  }
}
