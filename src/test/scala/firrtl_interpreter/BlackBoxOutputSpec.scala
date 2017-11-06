// See LICENSE for license details.

package firrtl_interpreter

import firrtl.ir.Type
import org.scalatest.{FreeSpec, Matchers}

//scalastyle:off magic.number

/**
  * Illustrate a black box that has multiple outputs
  * This one creates 3 outputs each with a different increment of the input
  */
class FanOutAdder extends BlackBoxImplementation {
  override def name: String = "FanOutAdder"

  override def execute(inputValues: Seq[BigInt], tpe: Type, outputName: String): BigInt = {
    val inc = outputName match {
      case "out1" => 1
      case "out2" => 2
      case "out3" => 3
    }
    inputValues.head + BigInt(inc)
  }

  override def cycle(): Unit = {}

  override def outputDependencies(outputName: String): Seq[String] = {
    outputName match {
      case "out1" => Seq("in")
      case "out2" => Seq("in")
      case "out3" => Seq("in")
      case _ => throw InterpreterException(s"$name was asked for input dependency for unknown output $outputName")
    }
  }
}

class FanOutAdderFactory extends BlackBoxFactory {
  override def createInstance(instanceName: String, blackBoxName: String): Option[BlackBoxImplementation] = {
    Some(add(new FanOutAdder))
  }
}

class BlackBoxCounter extends BlackBoxImplementation {
  val name: String = "BlackBoxCounter"
  var counter = BigInt(0)

  def execute(inputValues: Seq[BigInt], tpe: Type, outputName: String): BigInt = {
    if(inputValues.head == Big1) {
      counter = 0
    }
    counter
  }

  override def cycle(): Unit = {
    counter += 1
  }

  override def outputDependencies(outputName: String): Seq[String] = {
    Seq("clear")
  }
}

class BlackBoxCounterFactory extends BlackBoxFactory {
  override def createInstance(instanceName: String, blackBoxName: String): Option[BlackBoxImplementation] = {
    Some(add(new BlackBoxCounter))
  }
}

class BlackBoxOutputSpec extends FreeSpec with Matchers {
  "this tests black box implmentation that have multiple outputs" - {
    val adderInput =
      """
        |circuit FanOutTest :
        |  extmodule FanOut :
        |    output out1 : UInt<64>
        |    output out2 : UInt<64>
        |    output out3 : UInt<64>
        |    input in : UInt<64>
        |
        |
        |  module FanOutTest :
        |    input clk : Clock
        |    input reset : UInt<1>
        |    input in : UInt<64>
        |    output out1 : UInt<64>
        |    output out2 : UInt<64>
        |    output out3 : UInt<64>
        |
        |    inst fo of FanOut
        |    fo.in <= in
        |    out1 <= fo.out1
        |    out2 <= fo.out2
        |    out3 <= fo.out3
      """.stripMargin

    "each output should hold a different values" in {

      val factory = new FanOutAdderFactory

      val optionsManager = new InterpreterOptionsManager {
        interpreterOptions = InterpreterOptions(blackBoxFactories = Seq(factory), randomSeed = 0L)
      }
      val tester = new InterpretiveTester(adderInput, optionsManager)
      tester.interpreter.verbose = true
      tester.interpreter.setVerbose()

      for(i <- 0 until 10) {
        tester.poke("in", i)
        tester.expect("out1", i + 1)
        tester.expect("out2", i + 2)
        tester.expect("out3", i + 3)
        tester.step()
      }
    }
  }

  "this test a black box of an accumulator that implements reset" - {
    val input =
      """
        |circuit CounterTest :
        |  extmodule BlackBoxCounter :
        |    output counter : UInt<64>
        |    input clear : UInt<1>
        |
        |
        |  module CounterTest :
        |    input clk : Clock
        |    input reset : UInt<1>
        |    input clear : UInt<64>
        |    output counter : UInt<64>
        |
        |    inst bbc of BlackBoxCounter
        |    bbc.clear <= clear
        |    counter <= bbc.counter
      """.stripMargin

    "each output should hold a different values" in {

      val factory = new BlackBoxCounterFactory

      val optionsManager = new InterpreterOptionsManager {
        interpreterOptions = InterpreterOptions(blackBoxFactories = Seq(factory), randomSeed = 0L)
      }
      val tester = new InterpretiveTester(input, optionsManager)
      tester.interpreter.verbose = true
      tester.interpreter.setVerbose()

      tester.poke("clear", 1)
      tester.step()
      tester.poke("clear", 0)

      for(i <- 0 until 10) {
        tester.expect("counter", i)
        tester.step()
      }
    }
  }
}