package com.infomatiq.jsi.test;

import java.util.Arrays;

import junit.framework.TestCase;

import com.infomatiq.jsi.rtree.SortedList;

/**
 * @author aled.morris@infomatiq.co.uk
 */
public class SortedListTest extends TestCase {

  public SortedListTest(String s) {
    super(s);
  }
  
  private boolean checkExpected(SortedList sl, int[] ids) { 
    if (Arrays.equals(sl.toNativeArray(), ids)) {
      return true;
    }
    return false;  
  }
  
  public void testSortedList() {
    SortedList sl = new SortedList();
    sl.init(1);
  
    sl.add(10, 10.0F);
    checkExpected(sl, new int[] {10});
  
    sl.add(9, 9.0F);
    checkExpected(sl, new int[] {10});
      
    sl.init(3);
  
    // add in reverse priority order
    sl.add(10, 10.0F);
    checkExpected(sl, new int[] {10});
  
    sl.add(9, 9.0F);
    checkExpected(sl, new int[] {10, 9});
  
    sl.add(8, 8.0F);
    checkExpected(sl, new int[] {10, 9, 8});
    
    // add elements with priority lower than lowest priority; 
    // when current size = preferredMaxSize
    sl.add(7, 7.0F);
    checkExpected(sl, new int[] {10, 9, 8});
    
    sl.add(6, 6.0F);
    checkExpected(sl, new int[] {10, 9, 8});
    
    // add element with priority equal to lowest priority
    // when currentSize = preferredMaxSize
    sl.add(8, 8.0F);
    checkExpected(sl, new int[] {10, 9, 8, 8});
    
    // add elements with priority lower than lowest priority; 
    // when current size = preferredMaxSize + 1
    sl.add(7, 7.0F);
    checkExpected(sl, new int[] {10, 9, 8, 8});
    
    // add element with priority equal to lowest priority
    // when current size = preferredMaxSize + 1
    sl.add(8, 8.0F);
    checkExpected(sl, new int[] {10, 9, 8, 8, 8});
  
    // add elements with priority lower than lowest priority; 
    // when current size = preferredMaxSize + 2
    sl.add(7, 7.0F);
    checkExpected(sl, new int[] {10, 9, 8, 8, 8});
  
    // add element that will remove multiple entries of lowest priority  
    sl.add(9, 9.0F);
    checkExpected(sl, new int[] {10, 9, 9});
    
    
    // add in priority order  
    sl.init(3);
    sl.add(1, 1.0F);
    checkExpected(sl, new int[] {1});
  
    sl.add(2, 2.0F);
    checkExpected(sl, new int[] {2, 1});
  
    sl.add(3, 3.0F);
    checkExpected(sl, new int[] {3, 2, 1});

    sl.add(4, 4.0F);
    checkExpected(sl, new int[] {4, 3, 2});
  }
}
