package gemmini

import chisel3._
import chisel3.util._

// 加法树模块

//val adderTreeModule = Module(new AdderTree(inputType, outputType, accType, dataNumToPe, config.exp_width, config.manti_len, config.is_int8, config.data_width, config.reg_num))
class AdderTree[T <: Data : Arithmetic](inputType: T, outputType: T, accType: T,macnum: Int, exp_width: Int, manti_len: Int, is_int8: Boolean, data_width: Int, reg_num: Int) extends Module {
  val io = IO(new Bundle {
    // 输入信号e_valid
    val e_valid = Input(Bool())
    // 输入数据ochi，使用Flipped以适应Decoupled接口
    val ochi = Flipped(Decoupled(Vec(8, SInt((32).W))))
    // 输出数据data
    val data = Output(SInt(32.W))
    // 输出信号valid
    val valid = Output(Bool())

  //24_6_18 gemmini 新增
    val in_d = Input(SInt((inputType.getWidth).W))
    // val expMax = Input((UInt((exp_width + 1).W)))
    // val shift = Input(UInt(log2Up(accType.getWidth).W))
    // val in_flush = Input(Bool())

  })

  // 定义并初始化两个32位的累加寄存器sumReg
  //24_6_18 修改 val sumReg = RegInit(VecInit(Seq.fill(2)(0.S(32.W))))
  val sumReg = RegInit(VecInit(Seq.fill(reg_num)(0.S(32.W))))
  // 定义并初始化32位的累加结果sum
  val sum = RegInit(0.S(32.W))

  // 将ochi的前四个元素相加并赋值给sumReg(0)
  sumReg(0) := io.ochi.bits(0) + io.ochi.bits(1) + io.ochi.bits(2) + io.ochi.bits(3)+ io.in_d //add in_d
  // 将ochi的后四个元素相加并赋值给sumReg(1)
  sumReg(1) := io.ochi.bits(4) + io.ochi.bits(5) + io.ochi.bits(6) + io.ochi.bits(7)

  // 将sumReg中的值相加，再加上sum的当前值，并赋值给sum
  sum := sumReg.reduce(_ + _) + sum

  // 将sum的值赋给输出端口data
  io.data := sum

  // 定义并初始化移位寄存器shiftReg
  val shiftReg = RegInit(false.B)
  // 定义并初始化rs_to_as_allow信号
  val rs_to_as_allow = RegInit(true.B)

  // 当rs_to_as_allow为真时，允许ochi准备好接收数据
  io.ochi.ready := rs_to_as_allow
  // 当ochi的fire信号有效时，设置shiftReg
  shiftReg := io.ochi.fire
  // 将shiftReg的值赋给输出端口valid
  io.valid := shiftReg
}
