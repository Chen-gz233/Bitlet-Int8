package bitlet

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class RRregTest extends AnyFlatSpec with ChiselScalatestTester {
  // Test input values
  val valid = true.B
  val sign1 = Seq(true, false, true, false, true, false, true)
  val aManti1 = Seq(5, 10, 15, 20, 25, 30, 35)
  val sewo1 = Seq(100, 200, 300, 400, 500, 600, 700)

  // Apply input values
  "DUT" should "pass" in {
    test(new RRreg(macnum = 7, exp_width = 8, manti_len = 16, is_int8 = false, windowsize = 4)) { dut =>
      dut.io.valid.poke(false.B)
      dut.clock.step(1)

      // Apply input values
      dut.io.valid.poke(valid)
      for (i <- 0 until 7) {
        dut.io.sign(i).poke(sign1(i).B)
        dut.io.aManti(i).poke(aManti1(i).U)
        dut.io.sewo(i).poke(sewo1(i).U)
      }

      // Ignore the rest of the decoderRR_not_fixed's sign outputs
      for (i <- 7 until 64) {
        dut.io.sign(i).poke(false.B)
      }

      // Wait for a few cycles
      dut.clock.step(5)

      // Read output values
      val outputValid = dut.io.ochi.valid.peek()
      val outputData = dut.io.ochi.bits.peek()

      // Print output values
      println(s"Output Valid: $outputValid")
      println(s"Output Data: $outputData")
    }
  }
}