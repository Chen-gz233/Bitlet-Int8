package gemmini

import chisel3._
import chisel3.util._
//gemminiPE(SInt(8.W), SInt(32.W), 64, BitletConfigs(SInt(8.W)))
//T是Data的子类（UInt、SInt、Bool、Bundle），并且支持Arithmetic中的算数操作
class gemminiPE[T <: Data : Arithmetic](inputType: T, outputType: T, accType: T,
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
    //24_6_17: gemmini 新增
    val in_d = Input(inputType)
    val shift = Input(UInt(log2Up(accType.getWidth).W))
    val in_flush = Input(Bool())

  })

  val reg_b = Reg(Vec(dataNumToPe, inputType))   //WS模式需要锁存权重部分
  when (io.in_valid) {//检测是否有数进入
        when (io.in_b(0).asInstanceOf[SInt] =/= 0.S || io.in_b(1).asInstanceOf[SInt] =/= 0.S ||
              io.in_b(2).asInstanceOf[SInt] =/= 0.S || io.in_b(3).asInstanceOf[SInt] =/= 0.S ||
              io.in_b(4).asInstanceOf[SInt] =/= 0.S || io.in_b(5).asInstanceOf[SInt] =/= 0.S ||
              io.in_b(6).asInstanceOf[SInt] =/= 0.S || io.in_b(7).asInstanceOf[SInt] =/= 0.S ) {
            reg_b := io.in_b  
        }.otherwise{
          reg_b := reg_b
        }
  }

  val sign = Reg(Vec(dataNumToPe, Bool()))
  val wmanti = Reg(Vec(dataNumToPe, UInt((config.manti_len + 2).W)))
  val amanti = Reg(Vec(dataNumToPe, UInt((config.manti_len + 2).W)))

  val RRregModule = Module(new RRreg(dataNumToPe, config.exp_width, config.manti_len, config.is_int8, 8))
  val shiftOrchestModule = Module(new ShiftOrchest(dataNumToPe, config.is_int8, config.manti_len, config.exp_width))
  val adderTreeModule = Module(new AdderTree(inputType, outputType, accType, dataNumToPe, config.exp_width, config.manti_len, config.is_int8, config.data_width, config.reg_num))
  //24_6_18 gemmini新增
  val d_toAddtree = Reg(inputType)
  val d_addertree_valid = ShiftRegister(io.in_valid,3)
  when(io.in_valid) {
      d_toAddtree := io.in_d
  }

  for (i <- 0 until dataNumToPe) {
    //符号位做异或，相同为0，不同为1（作为相乘后的结果）
    sign(i) := reg_b(i).asUInt(config.sign_where) ^ io.in_a(i).asUInt(config.sign_where) 
                      //符号位为1，则是负数
    wmanti(i) := Mux(reg_b(i).asUInt(config.sign_where),
                        ~reg_b(i).asUInt(config.manti_len+1,0 ) + 1.U,
                        reg_b(i).asUInt(config.manti_len+1,0 ))            
    amanti(i) := Mux(io.in_a(i).asUInt(config.sign_where),
                    ~io.in_a(i).asUInt(config.manti_len+1,0 ) + 1.U, 
                      io.in_a(i).asUInt(config.manti_len+1,0 ))
  }


  shiftOrchestModule.io.wManti := wmanti

  RRregModule.io.aManti := amanti
  RRregModule.io.sign := sign
  RRregModule.io.sewo := shiftOrchestModule.io.sewo.asTypeOf(Vec(8, UInt(64.W)))

  for (i <- 0 until config.manti_len + 2) { 
    RRregModule.io.sewo(i) := shiftOrchestModule.io.sewo(i).asUInt
  }

  RRregModule.io.in_valid :=io.in_valid
  adderTreeModule.io.in_valid := RRregModule.io.RRregOut.valid
  adderTreeModule.io.RRregOut <> RRregModule.io.RRregOut
  //24_6_18 新增 
  // adderTreeModule.io.shift := io.shift
  when(d_addertree_valid) {
      adderTreeModule.io.in_d := d_toAddtree
      d_toAddtree := 0.S
  }.otherwise{
      adderTreeModule.io.in_d := 0.S
  }
  // adderTreeModule.io.in_flush := io.in_flush

  io.dataOut := adderTreeModule.io.data
  io.out_valid := adderTreeModule.io.out_valid
  io.next := RRregModule.io.RRregOut.valid

}

object top extends App {
  emitVerilog(new gemminiPE(SInt(8.W), SInt(32.W),SInt(32.W), 64, BitletConfigs(SInt(8.W))), Array("--target-dir", "generated"))
}