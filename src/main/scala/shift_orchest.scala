package bitlet

import chisel3._
import chisel3.util._
  //将UInt每位拆开，转为Bool类型
class ShiftOrchest(macnum: Int, is_int8: Boolean, manti_len: Int, exp_width: Int) extends Module {
  val io = IO(new Bundle {
    //改：val wManti = Input(Vec(macnum, UInt(7.W)))
    val wManti = Input(Vec(macnum, UInt(8.W)))
    //改：val sewo = Output(Vec(7, Vec(64, Bool())))
    val sewo = Output(Vec(8, Vec(macnum, Bool())))
  })
    //wManti是64个7位的数，然后转成sewo就变7行64个Bool类型了
    //改：for (j <- 0 until 7) {
    for (j <- 0 until 8) {
      for (i <- 0 until macnum) {
          //wManti 64x7 -> sewo 7x64 
        io.sewo(j)(i) := io.wManti(i)(j) 
    }
  }
}

// object ShiftOrchest extends App {
//   emitVerilog(new ShiftOrchest(64, false, 23, 8), Array("--target-dir", "generated"))
// }
