package gemmini

import chisel3._
import chisel3.util._


class RRreg(macnum: Int, exp_width: Int, manti_len: Int, is_int8: Boolean, windowsize: Int) extends Module {
  val io = IO(new Bundle {
    val in_valid = Input(Bool())
    val sign = Input(Vec(macnum, Bool()))
    val aManti = Input(Vec(macnum, UInt((manti_len + 2).W)))
    val sewo = Input(Vec(8, UInt(64.W)))
    val RRregOut = Decoupled(Output(Vec(8, SInt((32).W))))
  })

  val outResult = WireInit(VecInit(Seq.fill(8)(0.S((32).W))))
  val done = WireInit(VecInit(Seq.fill(8)(0.B)))


  val decoder = (0 until 8).map { i => Module(new decoderRR_not_fixed(macnum, exp_width, manti_len, is_int8, i , windowsize)) }

  for (i <- 0 until 8) {
    
      decoder(i).io.sign := io.sign
      decoder(i).io.aManti := io.aManti
      decoder(i).io.sewo := io.sewo(i)
      outResult(i) := decoder(i).io.outNum
      decoder(i).io.in_valid := io.in_valid
      done(i) := decoder(i).io.out_valid

  }
  io.RRregOut.bits := outResult
  io.RRregOut.valid := done.asUInt.andR


}



