/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package docs.actor;

import akka.actor.ActorSystem;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import static akka.dispatch.Futures.*;
import static akka.japi.pf.Function.*;
import static org.junit.Assert.*;

public class FutureTest {

  static ActorSystem system = null;

  @BeforeClass
  public static void beforeClass() {
    system = ActorSystem.create("FutureTest");
  }

  @AfterClass
  public static void afterClass() {
    system.shutdown();
    system.awaitTermination(Duration.create("5 seconds"));
  }

  @Test
  public void useMapTest() throws Exception {
    final ExecutionContext ec = system.dispatcher();
    Future<String> f1 = future(() -> "Hello" + "World", ec);
    Future<Integer> f2 = f1.map(f(String::length), ec);
    f2.onSuccess(vf(r -> System.out.println("f2 length is " + r)), ec);
    int result1 = Await.result(f2, Duration.create(5, TimeUnit.SECONDS));
    assertEquals(10, result1);

    Future<String> f3 = future(() -> "The Future String", ec);
    int result2 = Await.result(f3.
      map(f(s -> "Some " + s.substring(4)), ec).
      map(f(String::length), ec), Duration.create(5, TimeUnit.SECONDS));
    assertEquals(18, result2);
  }
}
