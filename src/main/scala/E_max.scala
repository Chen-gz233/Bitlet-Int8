package bitlet

import math._
import chisel3._
import chisel3.util._

//TODO Add output valid signal
class ExpMax(MacNum: Int, is_int8: Boolean, exp_width: Int) extends Module {
  val io = IO(new Bundle {
    val exp = Input((Vec(MacNum, UInt((exp_width + 1).W))))
    val expMax = Output(UInt((exp_width + 1).W))
    val on = Input(Bool())
    val valid = Output(Bool())
  })


  if (is_int8) {
    //在int8模式下，模块不会执行实际的最大值计算，而是忽略这些输入信号
    io.expMax := DontCare
    io.valid := DontCare
  }
  else {
    //ShiftRegister默认右移 
    val cntReg = ShiftRegister(io.on,log2Ceil(MacNum))
    val regTree = Reg(Vec(MacNum - 1, UInt((exp_width + 1).W)))
    for (i <- MacNum / 2 - 1 until MacNum - 1) {
      regTree(i) := Mux(io.exp(2 * (i - MacNum / 2 + 1)) >= io.exp(2 * (i - MacNum / 2 + 1) + 1), io.exp(2 * (i - MacNum / 2 + 1)), io.exp(2 * (i - MacNum / 2 + 1) + 1))

    }
    for (i <- 1 until (log2Ceil(MacNum))) {
      for (k <- 0 until pow(2, i - 1).toInt) {
        val idx = (pow(2, i - 1) - 1 + k).toInt
        val next = Mux(regTree(2 * idx + 1) >= regTree(2 * idx + 2), regTree(2 * idx + 1), regTree(2 * idx + 2))
        regTree(idx) := next
      }
    }
    io.expMax := regTree(0)
    io.valid := cntReg
  }
}

object ExpMax extends App {
  emitVerilog(new ExpMax(32,false,8), Array("--target-dir", "generated"))
}
