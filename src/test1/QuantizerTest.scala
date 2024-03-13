package bitlet

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class QuantizerTest extends AnyFlatSpec with ChiselScalatestTester {

   "DUT" should "pass" in {
        test(new Quantizer) { dut =>

            dut.io.input.poke(100.S)
            dut.clock.step()

            println("输出结果: " + dut.io.output.peek().toString)
            println("期望的输出结果:12.S")

            dut.io.input.poke(-50.S)
            dut.clock.step()

            println("Result is: " + dut.io.output.peek().toString)
            println("期望的输出结果:-6.S")
            
        }
    }

}