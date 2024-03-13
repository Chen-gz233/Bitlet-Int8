package bitlet

import chisel3._
import chisel3.util._


class RRreg(macnum: Int, exp_width: Int, manti_len: Int, is_int8: Boolean, windowsize: Int) extends Module {
  val io = IO(new Bundle {
    val valid = Input(Bool())
    val sign = Input(Vec(macnum, Bool()))
    //改：val aManti = Input(Vec(macnum, UInt((manti_len + 1).W)))
    val aManti = Input(Vec(macnum, UInt((manti_len + 2).W)))
    //改：val sewo = Input(Vec(7, UInt(64.W)))
    val sewo = Input(Vec(8, UInt(64.W)))

    val ochi = Decoupled(Output(Vec(8, SInt((32).W))))
  })

  //改：val outReg = RegInit(VecInit(Seq.fill(7)(0.S((32).W))))
  //改：val done = RegInit(VecInit(Seq.fill(7)(0.B)))
  
  //23.11.21 Reg 改 wire
  //val outReg = RegInit(VecInit(Seq.fill(8)(0.S((32).W))))
  //val done = RegInit(VecInit(Seq.fill(8)(0.B)))
  val outReg = WireInit(VecInit(Seq.fill(8)(0.S((32).W))))
  val done = WireInit(VecInit(Seq.fill(8)(0.B)))



  //改：val decoder = (0 until 7).map                                           //原本64，改为macnum
  val decoder = (0 until 8).map { i => Module(new decoderRR_not_fixed(macnum, exp_width, manti_len, is_int8, i , windowsize)) }

  //改：for (i <- 0 until 7) {
  for (i <- 0 until 8) {
    
      decoder(i).io.sign := io.sign
      decoder(i).io.aManti := io.aManti
      decoder(i).io.sewo := io.sewo(i)
      outReg(i) := decoder(i).io.outNum
      decoder(i).io.start := io.valid
      done(i) := decoder(i).io.done

  }
  io.ochi.bits := outReg
  io.ochi.valid := done.asUInt.andR


}



