package bitlet

import chisel3._
import chisel3.util._
import firrtl.Utils.False

class AdderTree(macnum: Int, exp_width: Int, manti_len: Int, is_int8: Boolean, data_width: Int, reg_num: Int) extends Module {
  val io = IO(new Bundle {
    val e_valid = Input(Bool())
    //改：val ochi = Flipped(Decoupled(Vec(7, SInt((32).W))))
    val ochi = Flipped(Decoupled(Vec(8, SInt((32).W))))
    val data = Output(SInt(32.W))
    val valid = Output(Bool())
  })
  
  val sumReg = RegInit(VecInit(Seq.fill(2)(0.S(32.W))))
  val sum = RegInit(0.S(32.W))
  //改：添加  + io.ochi.bits(7) 
  sumReg(0) := io.ochi.bits(0) + io.ochi.bits(1) + io.ochi.bits(2) + io.ochi.bits(3)
  sumReg(1) := io.ochi.bits(4) + io.ochi.bits(5) + io.ochi.bits(6) + io.ochi.bits(7) 

  sum := sumReg.reduce(_ + _) + sum

  io.data := sum

  val shiftReg = RegInit(false.B)
  val rs_to_as_allow = RegInit(true.B)


  io.ochi.ready := rs_to_as_allow

  shiftReg := io.ochi.fire
  io.valid := shiftReg



}



