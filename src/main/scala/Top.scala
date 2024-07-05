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

  val reg_b = Reg(Vec(dataNumToPe, inputType))   //锁存需要的d_in
  when (io.in_valid) {//检测d_in 是否有数进入
        when (io.in_b(0).asInstanceOf[SInt] =/= 0.S || io.in_b(1).asInstanceOf[SInt] =/= 0.S ||
              io.in_b(2).asInstanceOf[SInt] =/= 0.S || io.in_b(3).asInstanceOf[SInt] =/= 0.S ||
              io.in_b(4).asInstanceOf[SInt] =/= 0.S || io.in_b(5).asInstanceOf[SInt] =/= 0.S ||
              io.in_b(6).asInstanceOf[SInt] =/= 0.S || io.in_b(7).asInstanceOf[SInt] =/= 0.S ) {
            reg_b := io.in_b  
        }.otherwise{
          reg_b := reg_b
        }
  }


  val valid = RegInit(false.B)
  val sign = Reg(Vec(dataNumToPe, Bool()))
  //改：UInt((config.manti_len + 1).W)))
  val wmanti = Reg(Vec(dataNumToPe, UInt((config.manti_len + 2).W)))//(64,7.w)
  val amanti = Reg(Vec(dataNumToPe, UInt((config.manti_len + 2).W)))
  val RRregModule = Module(new RRreg(dataNumToPe, config.exp_width, config.manti_len, config.is_int8, 8))
  val shiftOrchestModule = Module(new ShiftOrchest(dataNumToPe, config.is_int8, config.manti_len, config.exp_width))
  //24_6_18 :AdderTree 与gemmini接口不同 
  // 原先： val adderTreeModule = Module(new AdderTree(dataNumToPe, config.exp_width, 27, config.is_int8, config.data_width, config.reg_num))
  val adderTreeModule = Module(new AdderTree(inputType, outputType, accType, dataNumToPe, config.exp_width, config.manti_len, config.is_int8, config.data_width, config.reg_num))
  //24_6_18 gemmini新增
  val d_toAddtree = Reg(inputType)
  val d_addertree_valid = ShiftRegister(io.in_valid,3)//chen : 这里延后3个时钟？
  when(io.in_valid) {
      d_toAddtree := io.in_d
  }

    println(" ##########分隔符########## ")
    println("dataNumToPe=" + dataNumToPe)
    println("config.manti_len+1= " + (config.manti_len + 1))
    println("config.exp_width= " + config.exp_width)
    println("config.manti_len尾数(小数部分)的长度= " + config.manti_len)
    println("config.is_int8= " + config.is_int8)
    println("config.data_width= " + config.data_width)
    println("config.reg_num= " + config.reg_num)
    println("config.sign_where= " + config.sign_where)
    println(" ##########分隔符########## ")


  for (i <- 0 until dataNumToPe) {
    //符号位做异或，相同为0，不同为1（作为相乘后的结果）
    sign(i) := reg_b(i).asUInt(config.sign_where) ^ io.in_a(i).asUInt(config.sign_where) 
                      //符号位为1，则
    //改：wmanti(i) := Mux(io.in_b(i).asUInt(config.sign_where),
    //改：                ~io.in_b(i).asUInt(config.manti_len, 0) + 1.U,
    //改：                io.in_b(i).asUInt(config.manti_len, 0))
      wmanti(i) := Mux(reg_b(i).asUInt(config.sign_where),
                        ~reg_b(i).asUInt(config.manti_len+1,0 ) + 1.U,
                        reg_b(i).asUInt(config.manti_len+1,0 ))            

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
  io.out_valid := adderTreeModule.io.valid
  io.next := RRregModule.io.ochi.valid

}

object top extends App {
  emitVerilog(new gemminiPE(SInt(8.W), SInt(32.W),SInt(32.W), 64, BitletConfigs(SInt(8.W))), Array("--target-dir", "generated"))
}