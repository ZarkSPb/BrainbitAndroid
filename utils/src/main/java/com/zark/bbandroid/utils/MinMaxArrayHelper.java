package com.zark.bbandroid.utils;

import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

public class MinMaxArrayHelper {
   private final Number[] _data;
   private final SortedSet<Number> _dataOrdered;
   private final int _dataLen;
   private int _latestIndex;

   public MinMaxArrayHelper(int arraySize) {
      if (arraySize <= 0)
         arraySize = 1;
      _dataLen = arraySize;
      _data = new Number[arraySize];
      for (int i = 0; i < arraySize; i++)
         _data[i] = 0.0;
      _dataOrdered = new TreeSet<>(new Comparator<Number>() {
         @Override
         public int compare(Number o1, Number o2) {
            return !o1.equals(o2) ? Double.compare(o1.doubleValue(), o2.doubleValue()) : 0;
         }
      });
   }

   public void addValue(Number val) {
      _dataOrdered.remove(_data[_latestIndex]);
      _data[_latestIndex] = val;
      _dataOrdered.add(val);
      _latestIndex = (_latestIndex + 1) % _dataLen;
   }

   public Number getMin() {
      return _dataOrdered.first();
   }

   public Number getMax() {
      return _dataOrdered.last();
   }
}
