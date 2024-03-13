package bitlet

import chisel3._
import chisel3.util._

class Quantizer extends Module {
  val io = IO(new Bundle {
    val input = Input(SInt(32.W))
    val output = Output(SInt(8.W))
  })

  //scaling factor 
  //val scaleFactor = 127/((BigInt(1) << 32) - 1)
  val scaleFactor = 127/65504
  
  println(" ##########分隔符########## ")
  println("scaleFactor=" + scaleFactor)
  println(" ##########分隔符########## ")

  // 量化操作
  val quantizedValue = Wire(SInt(32.W))
  quantizedValue := io.input * scaleFactor.S
  
  println(" ##########分隔符########## ")
  println("quantizedValue=" + scaleFactor)
  println(" ##########分隔符########## ")
  // 输出结果
  io.output := Mux(quantizedValue > 127.S, 127.S,
               Mux(quantizedValue < -128.S, -128.S, quantizedValue))
}

object Quantizer extends App {
    emitVerilog(new Quantizer(), Array("--target-dir", "generated"))
}