/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.japi.pf;

import scala.PartialFunction;
import scala.runtime.BoxedUnit;

public class Function {
  public static <I, R> PartialFunction<I, R> f(FI.Apply<I, R> apply) {
    return new CaseStatement<I, I, R>(new FI.Predicate() {
      @Override
      public boolean defined(Object o) {
        return true;
      }
    }, apply);
  }

  public static <I> PartialFunction<I, BoxedUnit> vf(FI.UnitApply<I> apply) {
    return new UnitCaseStatement<I, I>(new FI.Predicate() {
      @Override
      public boolean defined(Object o) {
        return true;
      }
    }, apply);
  }

}
