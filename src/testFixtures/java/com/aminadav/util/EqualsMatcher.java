package com.aminadav.util;

@FunctionalInterface
public interface EqualsMatcher {
  boolean equals(Object o1, Object o2);
  /*
  // if you want to ignore a field just implement like this:
    ((o1, o2) -> true)
   */
}
