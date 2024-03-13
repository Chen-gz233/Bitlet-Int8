package bitlet

import chisel3._
import chisel3.util._
//bitletPE(SInt(8.W), SInt(32.W), 64, BitletConfigs(SInt(8.W)))
//T是Data的子类（UInt、SInt、Bool、Bundle），并且支持Arithmetic中的算数操作
class bitletPE[T <: Data : Arithmetic](inputType: T, outputType: T,
                                        val dataNumToPe: Int,
                                        val config: BitletConfigs[T]) 
                                        extends Module {
  val io = IO(new Bundle {
    val in_a = Input(Vec(dataNumToPe, inputType))//activate
    val in_b = Input(Vec(dataNumToPe, inputType)) //weight
    val dataOut = Output(outputType)
    val in_valid = Input(Bool())
    val out_valid = Output(Bool())
    val next = Output(Bool())
  })


  val valid = RegInit(false.B)
  val sign = Reg(Vec(dataNumToPe, Bool()))
  //改：UInt((config.manti_len + 1).W)))
  val wmanti = Reg(Vec(dataNumToPe, UInt((config.manti_len + 2).W)))//(64,7.w)
  val amanti = Reg(Vec(dataNumToPe, UInt((config.manti_len + 2).W)))
  val RRregModule = Module(new RRreg(dataNumToPe, config.exp_width, config.manti_len, config.is_int8, 8))
  val shiftOrchestModule = Module(new ShiftOrchest(dataNumToPe, config.is_int8, config.manti_len, config.exp_width))
  val adderTreeModule = Module(new AdderTree(dataNumToPe, config.exp_width, 27, config.is_int8, config.data_width, config.reg_num))

    println(" ##########分隔符########## ")
    println("dataNumToPe=" + dataNumToPe)
    println("config.manti_len+1= " + (config.manti_len + 1))
    println("config.exp_width= " + config.exp_width)
    println("config.manti_len尾数（小数部分）的长度= " + config.manti_len)
    println("config.is_int8= " + config.is_int8)
    println("config.data_width= " + config.data_width)
    println("config.reg_num= " + config.reg_num)
    println("config.sign_where= " + config.sign_where)
    println(" ##########分隔符########## ")


  for (i <- 0 until dataNumToPe) {
    //符号位做异或，相同为0，不同为1（作为相乘后的结果）
    sign(i) := io.in_b(i).asUInt(config.sign_where) ^ io.in_a(i).asUInt(config.sign_where) 
                      //符号位为1，则
    //改：wmanti(i) := Mux(io.in_b(i).asUInt(config.sign_where),
    //改：                ~io.in_b(i).asUInt(config.manti_len, 0) + 1.U,
    //改：                io.in_b(i).asUInt(config.manti_len, 0))
      wmanti(i) := Mux(io.in_b(i).asUInt(config.sign_where),
                        ~io.in_b(i).asUInt(config.manti_len+1,0 ) + 1.U,
                        io.in_b(i).asUInt(config.manti_len+1,0 ))            

    //改：amanti(i) := Mux(io.in_a(i).asUInt(config.sign_where),
    //改：                ~io.in_a(i).asUInt(config.manti_len, 0) + 1.U,
    //改：                io.in_a(i).asUInt(config.manti_len, 0))
    amanti(i) := Mux(io.in_a(i).asUInt(config.sign_where),
                    ~io.in_a(i).asUInt(config.manti_len+1,0 ) + 1.U, 
                      io.in_a(i).asUInt(config.manti_len+1,0 ))
  }

  when(io.in_valid){
    valid := true.B
  }.otherwise{
    valid := false.B
  }
  shiftOrchestModule.io.wManti := wmanti
  RRregModule.io.aManti := amanti
  RRregModule.io.sign := sign
  //改：RRregModule.io.sewo := shiftOrchestModule.io.sewo.asTypeOf(Vec(7, UInt(64.W)))
  RRregModule.io.sewo := shiftOrchestModule.io.sewo.asTypeOf(Vec(8, UInt(64.W)))
  //改：for (i <- 0 until config.manti_len + 1)
  for (i <- 0 until config.manti_len + 2) { 
    RRregModule.io.sewo(i) := shiftOrchestModule.io.sewo(i).asUInt
  }
  RRregModule.io.valid := valid
  adderTreeModule.io.e_valid := RRregModule.io.ochi.valid
  adderTreeModule.io.ochi <> RRregModule.io.ochi
  io.dataOut := adderTreeModule.io.data
  io.out_valid := adderTreeModule.io.valid
  io.next := RRregModule.io.ochi.valid

}

object top extends App {
  emitVerilog(new bitletPE(SInt(8.W), SInt(32.W), 64, BitletConfigs(SInt(8.W))), Array("--target-dir", "generated"))
}