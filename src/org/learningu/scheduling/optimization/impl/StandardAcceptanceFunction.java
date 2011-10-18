package org.learningu.scheduling.optimization.impl;

import java.util.Random;

import org.learningu.scheduling.optimization.AcceptanceFunction;

import com.google.inject.Inject;

public final class StandardAcceptanceFunction implements AcceptanceFunction {
  private final Random random;

  @Inject
  StandardAcceptanceFunction(Random random) {
    this.random = random;
  }

  @Override
  public boolean acceptNewState(double originalScore, double newScore, double temperature) {
    if (newScore >= originalScore) {
      return true;
    }
    double probability = Math.exp((newScore - originalScore) / temperature);
    return random.nextLong() < probability;
  }
}
