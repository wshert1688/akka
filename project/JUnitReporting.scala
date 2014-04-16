package akka

import java.util.concurrent.ConcurrentLinkedQueue
import sbt.testing.{Status, Event}
import sbt.{TestResult, File, TestEvent, TestReportListener}
import java.io.File

object JUnitReporting {

  case class TestListener(val target: File) extends TestReportListener {

    println(s"!woho $target")

    // TODO make this concurrent as well
    var currentOutput: Option[XmlWriter] = None

    def testEvent(event: TestEvent) {
      println(s">>> $event")
      currentOutput.foreach(_.addEvent(event))
    }

    def endGroup(name: String, result: TestResult.Value) {
      println(s">>> EndGroupR $name $result")
      flushOutput()
    }

    def endGroup(name: String, t: Throwable) {
      println(s">>> EndGroupT $name $t")
      flushOutput()
    }

    def startGroup(name: String) {
      println(s">>> StartGroup $name")
      currentOutput = Some(new XmlWriter(name))
    }

    private def flushOutput() {
      //val file = new File(targetPath)
      //file.mkdirs()

      currentOutput.foreach(_.write(target))
    }

  }

  class XmlWriter(val name: String) {

    val testEvents: ConcurrentLinkedQueue[TestEvent] = new ConcurrentLinkedQueue[TestEvent]()

    def addEvent(testEvent: TestEvent): Unit = {
      testEvents.add(testEvent)
    }

    def write(path: File): Unit = {

      val events: Array[TestEvent] = Array.ofDim(testEvents.size())
      testEvents.toArray(events)
      events.foreach { case event =>
        println(s"<<< Event $name ${event.result.map(_.toString).getOrElse("Undefined")}")
        event.detail.foreach { case devent =>
          println(s"<<< Duration: ${devent.duration}")
          println(s"<<<      FQN: ${devent.fullyQualifiedName}")
          println(s"<<<   Status: ${devent.status}")
          println(s"<<< Selector: ${devent.selector}")
        }
      }

      val xml = """<?xml version="1.0" encoding="UTF-8"?>
                  |<testsuites disabled="" errors="" failures="" name="" tests="" time="">
                  |    <testsuite disabled="" errors="" failures="" hostname="" id=""
                  |        name="" package="" skipped="" tests="" time="" timestamp="">
                  |        <properties>
                  |            <property name="" value=""/>
                  |            <property name="" value=""/>
                  |        </properties>
                  |        <testcase assertions="" classname="" name="" status="" time="">
                  |            <skipped/>
                  |            <error message="" type=""/>
                  |            <error message="" type=""/>
                  |            <failure message="" type=""/>
                  |            <failure message="" type=""/>
                  |            <system-out/>
                  |            <system-out/>
                  |            <system-err/>
                  |            <system-err/>
                  |        </testcase>
                  |        <testcase assertions="" classname="" name="" status="" time="">
                  |            <skipped/>
                  |            <error message="" type=""/>
                  |            <error message="" type=""/>
                  |            <failure message="" type=""/>
                  |            <failure message="" type=""/>
                  |            <system-out/>
                  |            <system-out/>
                  |            <system-err/>
                  |            <system-err/>
                  |        </testcase>
                  |        <system-out/>
                  |        <system-err/>
                  |    </testsuite>
                  |    <testsuite disabled="" errors="" failures="" hostname="" id=""
                  |        name="" package="" skipped="" tests="" time="" timestamp="">
                  |        <properties>
                  |            <property name="" value=""/>
                  |            <property name="" value=""/>
                  |        </properties>
                  |        <testcase assertions="" classname="" name="" status="" time="">
                  |            <skipped/>
                  |            <error message="" type=""/>
                  |            <error message="" type=""/>
                  |            <failure message="" type=""/>
                  |            <failure message="" type=""/>
                  |            <system-out/>
                  |            <system-out/>
                  |            <system-err/>
                  |            <system-err/>
                  |        </testcase>
                  |        <testcase assertions="" classname="" name="" status="" time="">
                  |            <skipped/>
                  |            <error message="" type=""/>
                  |            <error message="" type=""/>
                  |            <failure message="" type=""/>
                  |            <failure message="" type=""/>
                  |            <system-out/>
                  |            <system-out/>
                  |            <system-err/>
                  |            <system-err/>
                  |        </testcase>
                  |        <system-out/>
                  |        <system-err/>
                  |    </testsuite>
                  |</testsuites>"""

    }

  }
}