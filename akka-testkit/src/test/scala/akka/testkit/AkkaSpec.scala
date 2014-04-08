/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.testkit

import language.{ postfixOps, reflectiveCalls }

import org.scalatest.{ WordSpecLike, BeforeAndAfterAll }
import org.scalatest.Matchers
import akka.actor.ActorSystem
import akka.event.{ Logging, LoggingAdapter }
import scala.concurrent.duration._
import scala.concurrent.Future
import com.typesafe.config.{ Config, ConfigFactory }
import akka.dispatch.Dispatchers
import akka.testkit.TestEvent._
import java.lang.management.ManagementFactory
import java.io.File

object AkkaSpec {
  val testConf: Config = ConfigFactory.parseString("""
      akka {
        loggers = ["akka.testkit.TestEventListener"]
        loglevel = "WARNING"
        stdout-loglevel = "WARNING"
        actor {
          default-dispatcher {
            executor = "fork-join-executor"
            fork-join-executor {
              parallelism-min = 8
              parallelism-factor = 2.0
              parallelism-max = 8
            }
          }
        }
      }
                                                    """)

  def mapToConfig(map: Map[String, Any]): Config = {
    import scala.collection.JavaConverters._
    ConfigFactory.parseMap(map.asJava)
  }

  def getCallerName(clazz: Class[_]): String = {
    val s = (Thread.currentThread.getStackTrace map (_.getClassName) drop 1)
      .dropWhile(_ matches "(java.lang.Thread|.*AkkaSpec.?$)")
    val reduced = s.lastIndexWhere(_ == clazz.getName) match {
      case -1 ⇒ s
      case z  ⇒ s drop (z + 1)
    }
    reduced.head.replaceFirst(""".*\.""", "").replaceAll("[^a-zA-Z_0-9]", "_")
  }

}

abstract class AkkaSpec(_system: ActorSystem)
  extends TestKit(_system) with WordSpecLike with Matchers with BeforeAndAfterAll with WatchedByCoroner {

  def this(config: Config) = this(ActorSystem(AkkaSpec.getCallerName(getClass),
    ConfigFactory.load(config.withFallback(AkkaSpec.testConf))))

  def this(s: String) = this(ConfigFactory.parseString(s))

  def this(configMap: Map[String, _]) = this(AkkaSpec.mapToConfig(configMap))

  def this() = this(ActorSystem(AkkaSpec.getCallerName(getClass), AkkaSpec.testConf))

  val log: LoggingAdapter = Logging(system, this.getClass)

  override val invokeBeforeAllAndAfterAllEvenIfNoTestsAreExpected = true

  private val processorId: Option[String] = {
    val parts = ManagementFactory.getRuntimeMXBean().getName().split("@")
    if (parts.size > 1) Some(parts(0))
    else None
  }

  private def logOpenFiles(msg: String): Unit = {
    processorId.foreach { id ⇒
      new File("./logs").mkdir()
      val fName = msg.replace(' ', '_')
      val process = new ProcessBuilder("/bin/sh", "-c", s"lsof -p $id | tee ./logs/${id}.${system.name}.$fName.txt | wc -l").start()
      val retCode = process.waitFor()
      val lines = (scala.io.Source.fromInputStream(process.getInputStream)).getLines().to[Seq]
      if (retCode == 0 && lines.nonEmpty) {
        import scala.util.control.Exception._
        (catching(classOf[NumberFormatException]) opt lines.head.trim.toInt).foreach(files ⇒
          println(s"${system.name} $msg $files open files ($id)"))
      } else {
        (scala.io.Source.fromInputStream(process.getErrorStream)).getLines().foreach(l ⇒ println(s"${system.name} ERROR: $l"))
      }
      process.destroy()
    }
  }

  final override def beforeAll {
    logOpenFiles("starts with")
    startCoroner
    atStartup()
  }

  final override def afterAll {
    beforeTermination()
    shutdown()
    afterTermination()
    logOpenFiles("ends with")
    stopCoroner()
  }

  protected def atStartup() {}

  protected def beforeTermination() {}

  protected def afterTermination() {}

  def spawn(dispatcherId: String = Dispatchers.DefaultDispatcherId)(body: ⇒ Unit): Unit =
    Future(body)(system.dispatchers.lookup(dispatcherId))

  override def expectedTestDuration: FiniteDuration = 60 seconds

  def muteDeadLetters(messageClasses: Class[_]*)(sys: ActorSystem = system): Unit =
    if (!sys.log.isDebugEnabled) {
      def mute(clazz: Class[_]): Unit =
        sys.eventStream.publish(Mute(DeadLettersFilter(clazz)(occurrences = Int.MaxValue)))
      if (messageClasses.isEmpty) mute(classOf[AnyRef])
      else messageClasses foreach mute
    }

}
