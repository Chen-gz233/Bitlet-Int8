package gemmini

import math._
import chisel3._
import chisel3.util._

// TODO: 添加输出的有效信号
class ExpMax(MacNum: Int, is_int8: Boolean, exp_width: Int) extends Module {
  val io = IO(new Bundle {
    // 输入指数向量exp
    val exp = Input(Vec(MacNum, UInt((exp_width + 1).W)))
    // 输出最大指数expMax
    val expMax = Output(UInt((exp_width + 1).W))
    // 控制信号on
    val on = Input(Bool())
    // 输出有效信号valid
    val valid = Output(Bool())
  })

  if (is_int8) {
    // 在int8模式下，模块不会执行实际的最大值计算，而是忽略这些输入信号
    io.expMax := DontCare
    io.valid := DontCare
  } else {
    // 在正常模式下，执行最大值计算
    // ShiftRegister默认右移，cntReg用于计数寄存器
    val cntReg = ShiftRegister(io.on, log2Ceil(MacNum))
    // regTree用于存储比较结果
    val regTree = Reg(Vec(MacNum - 1, UInt((exp_width + 1).W)))
    
    // 初始化regTree的叶子节点，进行初步比较
    for (i <- MacNum / 2 - 1 until MacNum - 1) {
      regTree(i) := Mux(io.exp(2 * (i - MacNum / 2 + 1)) >= io.exp(2 * (i - MacNum / 2 + 1) + 1), io.exp(2 * (i - MacNum / 2 + 1)), io.exp(2 * (i - MacNum / 2 + 1) + 1))
    }

    // 使用for循环构建加法树，逐层比较获取最大值
    for (i <- 1 until log2Ceil(MacNum)) {
      for (k <- 0 until pow(2, i - 1).toInt) {
        val idx = (pow(2, i - 1) - 1 + k).toInt
        val next = Mux(regTree(2 * idx + 1) >= regTree(2 * idx + 2), regTree(2 * idx + 1), regTree(2 * idx + 2))
        regTree(idx) := next
      }
    }

    // 将最终计算出的最大值赋给输出端口expMax
    io.expMax := regTree(0)
    // 将cntReg的值赋给输出端口valid
    io.valid := cntReg
  }
}
