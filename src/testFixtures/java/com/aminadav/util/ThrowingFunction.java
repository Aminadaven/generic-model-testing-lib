package com.aminadav.util;

import java.util.function.Function;

@FunctionalInterface
public interface ThrowingFunction<T, R> extends Function<T, R> {

  @Override
  default R apply(final T t) {
    try {
      return applyThrows(t);
    }
    catch (final Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  R applyThrows(T t) throws Exception;

}
