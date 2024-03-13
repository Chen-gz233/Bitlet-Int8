package bitlet

import chisel3._
import chisel3.util._

class AccumulationModule extends Module {
  val io = IO(new Bundle {
    val input1 = Input(SInt(8.W))
    val input2 = Input(SInt(8.W))
    val accumulate = Output(SInt(16.W)) // 16位宽用于记录累加结果
  })

  // 定义一个寄存器（Reg）来记录累加结果，初始值为0
  val accumulator = RegInit(0.S(16.W))

  // 使用 * 运算符进行相乘，然后累加到 accumulator 变量中
  accumulator := accumulator + (io.input1 * io.input2)

  io.accumulate := accumulator
}
object AccumulationModule extends App {
  emitVerilog(new AccumulationModule(), Array("--target-dir", "generated"))
}