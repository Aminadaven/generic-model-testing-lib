package com.aminadav.util;

import java.util.function.Consumer;

@FunctionalInterface
public interface ThrowingConsumer<T> extends Consumer<T> {

  @Override
  default void accept(final T element) {
    try {
      acceptThrows(element);
    }
    catch (final Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  void acceptThrows(T element) throws Exception;
}
