package io.urbanflow.testdata.generation;

import java.util.SplittableRandom;

/** Synchronized deterministic generator for reproducible failure replay. */
public final class DeterministicRandom {

  private final SplittableRandom random;

  /** Creates a generator from an explicit recorded seed. */
  public DeterministicRandom(long seed) {
    this.random = new SplittableRandom(seed);
  }

  /** Returns a bounded integer. */
  public synchronized int integer(int origin, int bound) {
    return random.nextInt(origin, bound);
  }

  /** Returns an alphanumeric string of the requested size. */
  public synchronized String alphanumeric(int length) {
    if (length < 1 || length > 256) {
      throw new IllegalArgumentException("length must be between 1 and 256");
    }
    String alphabet = "abcdefghijklmnopqrstuvwxyz0123456789";
    StringBuilder value = new StringBuilder(length);
    for (int index = 0; index < length; index++) {
      value.append(alphabet.charAt(random.nextInt(alphabet.length())));
    }
    return value.toString();
  }
}
