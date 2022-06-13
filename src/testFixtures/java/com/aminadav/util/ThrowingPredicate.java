package com.aminadav.util;

import java.util.function.Predicate;

@FunctionalInterface
public interface ThrowingPredicate<T> extends Predicate<T> {

  @Override
  default boolean test(final T t) {
    try {
      return testThrows(t);
    }
    catch (final Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  boolean testThrows(T t) throws Exception;
}
