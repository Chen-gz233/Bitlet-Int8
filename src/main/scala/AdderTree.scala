package gemmini

import chisel3._
import chisel3.util._

// 加法树模块
class AdderTree[T <: Data : Arithmetic](inputType: T, outputType: T, accType: T,macnum: Int, exp_width: Int, manti_len: Int, is_int8: Boolean, data_width: Int, reg_num: Int) extends Module {
  val io = IO(new Bundle {
    val in_valid = Input(Bool())
    val RRregOut = Flipped(Decoupled(Vec(8, SInt((32).W))))
    val data = Output(SInt(32.W))
    val out_valid = Output(Bool())

  //24_6_18 gemmini 新增
    val in_d = Input(SInt((inputType.getWidth).W))
    // val expMax = Input((UInt((exp_width + 1).W)))
    // val shift = Input(UInt(log2Up(accType.getWidth).W))
    // val in_flush = Input(Bool())

  })

  // 定义并初始化两个32位的累加寄存器sumReg
  //24_6_18 修改 val sumReg = RegInit(VecInit(Seq.fill(2)(0.S(32.W))))
  val sumReg = RegInit(VecInit(Seq.fill(reg_num)(0.S(32.W))))
  val sum = RegInit(0.S(32.W))
  sumReg(0) := io.RRregOut.bits(0) + io.RRregOut.bits(1) + io.RRregOut.bits(2) + io.RRregOut.bits(3)+ io.in_d //add in_d
  sumReg(1) := io.RRregOut.bits(4) + io.RRregOut.bits(5) + io.RRregOut.bits(6) + io.RRregOut.bits(7)

  sum := sumReg.reduce(_ + _) + sum
  io.data := sum

  //TODO 
  val rs_to_as_allow = RegInit(true.B)
  io.RRregOut.ready := rs_to_as_allow

  val out_valid = RegInit(false.B)
  out_valid := io.RRregOut.fire

  io.out_valid := out_valid
}
