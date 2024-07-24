package gemmini

import chisel3._
import chisel3.util._
  //将8位宽的UInt每位拆开，转为Bool类型
class ShiftOrchest(macnum: Int, is_int8: Boolean, manti_len: Int, exp_width: Int) extends Module {
  val io = IO(new Bundle {
    val wManti = Input(Vec(macnum, UInt(8.W)))
    val sewo = Output(Vec(8, Vec(macnum, Bool())))
  })
    //wManti是macnum个8位的UInt，然后转成sewo就变7行64个Bool类型
    for (j <- 0 until 8) {
      for (i <- 0 until macnum) {
          //wManti 64x8 -> sewo 8x64 
        io.sewo(j)(i) := io.wManti(i)(j) 
    }
  }
}

//TODO  is_int8,manti_len, exp_width 参数留作FP16,FP32开发使用