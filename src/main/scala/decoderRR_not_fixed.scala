package gemmini

import scala.math
import chisel3.{Mux, _}
import chisel3.util._
import firrtl.Utils.False

//根据sewo中的数据，每8位取为window，
//根据window在num_table获取29位宽的数据，并解码为10组数据
//根据这10组数据，将io.sign和io.aManti数据结合，转为有符号数(31位)
//将这些有符号数相加
class decoderRR_not_fixed(macnum: Int, exp_width: Int, manti_len: Int,
                is_int8: Boolean, shiftNum: Int, windowsize: Int) extends Module {
  val io = IO(new Bundle {
    val sign = Input(Vec(macnum, Bool()))
    val sewo = Input(UInt(macnum.W))    //weight重排列
    val aManti = Input(Vec(macnum, UInt((8).W)))
    val in_valid = Input(Bool())
    val out_valid = Output(Bool())
    val outNum = Output(SInt((32.W)))
  })
    //无符号数右移+符号位扩展转为有符号数
    //UIntShiftToSInt_int8(io.sign(8.U * cnt),io.aManti(8.U * cnt),shiftNum, 31)
  def UIntShiftToSInt_int8[T <: Data](sign: Bool, sourceNum: UInt, shiftNum: Int, NumWidth: Int): SInt = {
    val destNumNoSign = Wire(UInt(NumWidth.W)) //存储无符号整数的 绝对值
    val source = Wire(UInt(NumWidth.W))
    val result = Wire(UInt((NumWidth + 1).W)) //存储结果的绝对值和符号位
    source := sourceNum
    when(sign) {
      destNumNoSign := ~(source << shiftNum.U) + 1.U  //左移，取反，+1
      //新增：对 -0 的操作，避免1-00000的出现
      when(destNumNoSign === 0.U){
        result := Cat(0.U, destNumNoSign)
      }.otherwise{
        result := Cat(1.U, destNumNoSign)
      }
    }.otherwise {
      destNumNoSign := source << shiftNum.U
      result := Cat(0.U, destNumNoSign)
    }
    result.asSInt //转换为sint
  }



  val idle :: dect :: did :: Nil = Enum(3)
  val state = RegInit(idle)

  val cnt = RegInit(0.U(3.W))


  //23.11.22改：Reg -> Wire 
  val windowReg0_0 = WireInit(0.S(32.W)) 
  val windowReg0_1 = WireInit(0.S(32.W))
  val windowReg0_2 = WireInit(0.S(32.W))
  val windowReg0_3 = WireInit(0.S(32.W))
  val windowReg0_4 = WireInit(0.S(32.W))
  val windowReg0_5 = WireInit(0.S(32.W))
  val windowReg0_6 = WireInit(0.S(32.W))
  val windowReg0_7 = WireInit(0.S(32.W))

  val windowReg1_0 = WireInit(0.S(32.W))
  val windowReg1_1 = WireInit(0.S(32.W))
  val windowReg1_2 = WireInit(0.S(32.W))
  val windowReg1_3 = WireInit(0.S(32.W))

  val windowReg2_0 = WireInit(0.S(32.W))
  val windowReg2_1 = WireInit(0.S(32.W))

  val windowReg3_0 = WireInit(0.S(32.W))


  val ones = Wire(UInt(2.W))

  val two_0 = Wire(UInt(3.W))
  val two_1 = Wire(UInt(3.W))

  val three_0 = Wire(UInt(3.W))
  val three_1 = Wire(UInt(3.W))
  val three_2 = Wire(UInt(3.W))

  val four_0 = Wire(UInt(3.W))
  val four_1 = Wire(UInt(3.W))
  val four_2 = Wire(UInt(3.W))
  val four_3 = Wire(UInt(3.W))

  val num_table_temp = Wire(UInt(29.W))
  val window = Wire(UInt(8.W))
  val done_addr = RegInit(0.U(8.W))
  val done_time = Wire(UInt(2.W))
  val done_time_reg = RegInit(0.U(2.W))
  val done_reg = RegInit(0.U(1.W))
            //278个数
  val num_table = RegInit(VecInit(Seq(
    0.U(29.W),0.U(29.W),0.U(29.W),33.U(29.W),0.U(29.W),65.U(29.W),69.U(29.W),34818.U(29.W),0.U(29.W),97.U(29.W),101.U(29.W),51202.U(29.W),105.U(29.W),53250.U(29.W),53506.U(29.W),219152386.U(29.W),0.U(29.W),129.U(29.W),133.U(29.W),67586.U(29.W),137.U(29.W),69634.U(29.W),69890.U(29.W),286261250.U(29.W),141.U(29.W),71682.U(29.W),71938.U(29.W),294649858.U(29.W),72194.U(29.W),295698434.U(29.W),295829506.U(29.W),3.U(29.W),0.U(29.W),161.U(29.W),165.U(29.W),83970.U(29.W),169.U(29.W),86018.U(29.W),86274.U(29.W),353370114.U(29.W),173.U(29.W),88066.U(29.W),88322.U(29.W),361758722.U(29.W),88578.U(29.W),362807298.U(29.W),362938370.U(29.W),3.U(29.W),177.U(29.W),90114.U(29.W),90370.U(29.W),370147330.U(29.W),90626.U(29.W),371195906.U(29.W),371326978.U(29.W),3.U(29.W),90882.U(29.W),372244482.U(29.W),372375554.U(29.W),3.U(29.W),372506626.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),0.U(29.W),193.U(29.W),197.U(29.W),100354.U(29.W),201.U(29.W),102402.U(29.W),102658.U(29.W),420478978.U(29.W),205.U(29.W),104450.U(29.W),104706.U(29.W),428867586.U(29.W),104962.U(29.W),429916162.U(29.W),430047234.U(29.W),3.U(29.W),209.U(29.W),106498.U(29.W),106754.U(29.W),437256194.U(29.W),107010.U(29.W),438304770.U(29.W),438435842.U(29.W),3.U(29.W),107266.U(29.W),439353346.U(29.W),439484418.U(29.W),3.U(29.W),439615490.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),213.U(29.W),108546.U(29.W),108802.U(29.W),445644802.U(29.W),109058.U(29.W),446693378.U(29.W),446824450.U(29.W),3.U(29.W),109314.U(29.W),447741954.U(29.W),447873026.U(29.W),3.U(29.W),448004098.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),109570.U(29.W),448790530.U(29.W),448921602.U(29.W),3.U(29.W),449052674.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),449183746.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),0.U(29.W),225.U(29.W),229.U(29.W),116738.U(29.W),233.U(29.W),118786.U(29.W),119042.U(29.W),487587842.U(29.W),237.U(29.W),120834.U(29.W),121090.U(29.W),495976450.U(29.W),121346.U(29.W),497025026.U(29.W),497156098.U(29.W),3.U(29.W),241.U(29.W),122882.U(29.W),123138.U(29.W),504365058.U(29.W),123394.U(29.W),505413634.U(29.W),505544706.U(29.W),3.U(29.W),123650.U(29.W),506462210.U(29.W),506593282.U(29.W),3.U(29.W),506724354.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),245.U(29.W),124930.U(29.W),125186.U(29.W),512753666.U(29.W),125442.U(29.W),513802242.U(29.W),513933314.U(29.W),3.U(29.W),125698.U(29.W),514850818.U(29.W),514981890.U(29.W),3.U(29.W),515112962.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),125954.U(29.W),515899394.U(29.W),516030466.U(29.W),3.U(29.W),516161538.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),516292610.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),249.U(29.W),126978.U(29.W),127234.U(29.W),521142274.U(29.W),127490.U(29.W),522190850.U(29.W),522321922.U(29.W),3.U(29.W),127746.U(29.W),523239426.U(29.W),523370498.U(29.W),3.U(29.W),523501570.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),128002.U(29.W),524288002.U(29.W),524419074.U(29.W),3.U(29.W),524550146.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),524681218.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),128258.U(29.W),525336578.U(29.W),525467650.U(29.W),3.U(29.W),525598722.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),525729794.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),525860866.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W),3.U(29.W)
    )))
      //256个数
  val time_table = RegInit(VecInit(Seq(
  0.U(2.W),1.U(2.W),2.U(2.W),3.U(2.W),1.U(2.W),1.U(2.W),2.U(2.W),3.U(2.W),1.U(2.W),2.U(2.W),
  2.U(2.W),3.U(2.W),2.U(2.W),2.U(2.W),3.U(2.W),3.U(2.W),1.U(2.W),1.U(2.W),2.U(2.W),3.U(2.W),
  1.U(2.W),1.U(2.W),2.U(2.W),3.U(2.W),1.U(2.W),2.U(2.W),2.U(2.W),3.U(2.W),2.U(2.W),2.U(2.W),
  3.U(2.W),3.U(2.W),1.U(2.W),1.U(2.W),2.U(2.W),3.U(2.W),2.U(2.W),2.U(2.W),2.U(2.W),3.U(2.W),
  2.U(2.W),2.U(2.W),2.U(2.W),3.U(2.W),2.U(2.W),2.U(2.W),3.U(2.W),3.U(2.W),1.U(2.W),2.U(2.W),
  2.U(2.W),3.U(2.W),2.U(2.W),2.U(2.W),2.U(2.W),3.U(2.W),2.U(2.W),3.U(2.W),3.U(2.W),3.U(2.W),
  2.U(2.W),3.U(2.W),3.U(2.W),3.U(2.W),1.U(2.W),1.U(2.W),2.U(2.W),3.U(2.W),1.U(2.W),1.U(2.W),
  2.U(2.W),3.U(2.W),1.U(2.W),2.U(2.W),2.U(2.W),3.U(2.W),2.U(2.W),2.U(2.W),3.U(2.W),3.U(2.W),
  1.U(2.W),1.U(2.W),2.U(2.W),3.U(2.W),1.U(2.W),1.U(2.W),2.U(2.W),3.U(2.W),1.U(2.W),2.U(2.W),
  2.U(2.W),3.U(2.W),2.U(2.W),2.U(2.W),3.U(2.W),3.U(2.W),1.U(2.W),1.U(2.W),2.U(2.W),3.U(2.W),
  2.U(2.W),2.U(2.W),2.U(2.W),3.U(2.W),2.U(2.W),2.U(2.W),2.U(2.W),3.U(2.W),2.U(2.W),2.U(2.W),
  3.U(2.W),3.U(2.W),1.U(2.W),2.U(2.W),2.U(2.W),3.U(2.W),2.U(2.W),2.U(2.W),2.U(2.W),3.U(2.W),
  2.U(2.W),3.U(2.W),3.U(2.W),3.U(2.W),2.U(2.W),3.U(2.W),3.U(2.W),3.U(2.W),1.U(2.W),1.U(2.W),
  2.U(2.W),3.U(2.W),1.U(2.W),1.U(2.W),2.U(2.W),3.U(2.W),1.U(2.W),2.U(2.W),2.U(2.W),3.U(2.W),
  2.U(2.W),2.U(2.W),3.U(2.W),3.U(2.W),2.U(2.W),2.U(2.W),2.U(2.W),3.U(2.W),2.U(2.W),2.U(2.W),
  2.U(2.W),3.U(2.W),2.U(2.W),2.U(2.W),2.U(2.W),3.U(2.W),2.U(2.W),2.U(2.W),3.U(2.W),3.U(2.W),
  2.U(2.W),2.U(2.W),2.U(2.W),3.U(2.W),2.U(2.W),2.U(2.W),2.U(2.W),3.U(2.W),2.U(2.W),2.U(2.W),
  2.U(2.W),3.U(2.W),2.U(2.W),2.U(2.W),3.U(2.W),3.U(2.W),2.U(2.W),2.U(2.W),2.U(2.W),3.U(2.W),
  2.U(2.W),2.U(2.W),2.U(2.W),3.U(2.W),2.U(2.W),3.U(2.W),3.U(2.W),3.U(2.W),2.U(2.W),3.U(2.W),
  3.U(2.W),3.U(2.W),1.U(2.W),1.U(2.W),2.U(2.W),3.U(2.W),2.U(2.W),2.U(2.W),2.U(2.W),3.U(2.W),
  2.U(2.W),2.U(2.W),2.U(2.W),3.U(2.W),2.U(2.W),2.U(2.W),3.U(2.W),3.U(2.W),2.U(2.W),2.U(2.W),
  2.U(2.W),3.U(2.W),2.U(2.W),2.U(2.W),2.U(2.W),3.U(2.W),2.U(2.W),2.U(2.W),2.U(2.W),3.U(2.W),
  2.U(2.W),2.U(2.W),3.U(2.W),3.U(2.W),2.U(2.W),2.U(2.W),2.U(2.W),3.U(2.W),3.U(2.W),3.U(2.W),
  3.U(2.W),3.U(2.W),3.U(2.W),3.U(2.W),3.U(2.W),3.U(2.W),3.U(2.W),3.U(2.W),3.U(2.W),3.U(2.W),
  2.U(2.W),2.U(2.W),2.U(2.W),3.U(2.W),3.U(2.W),3.U(2.W),3.U(2.W),3.U(2.W),3.U(2.W),3.U(2.W),
  3.U(2.W),3.U(2.W),3.U(2.W),3.U(2.W),3.U(2.W),3.U(2.W)  
  )))
  
  val start = cnt * windowsize.U //8.U            //cnt初始化为0，一次移8位
  window := (io.sewo >> start)(7,0)//先位移start位，然后切片提取[7：0],window (Uint)

  done_time := time_table(done_addr)  //无符号的8位数，查找time数组

  num_table_temp := num_table(window)   //根据window索引到一个29位宽的无符号整数
    //分四层decode 
    //one ,two, three 都是wire Uint

    //num_table中一个29位宽的数被解码为10组数据
    //"ones"给滑窗中"1"的稀疏情况分等级
     //ones=0 滑窗window有0-1个"1";ones=1 滑窗window有2个"1";ones=2 有3-4个"1";ones=3 不稀疏
  ones := num_table_temp(1,0) //ones=0 时：滑窗window中有0-1个"1",不需要标注"1"的位置
 
  two_0 := num_table_temp(4,2)  //ones=1 时：滑窗window中有2个"1",two_x 用于标注滑窗window中"1"的位置
  two_1 := num_table_temp(7,5)

  three_0 :=num_table_temp(10,8)  //ones=2 时：滑窗window中有3个"1",three_x 用于标注滑窗window中"1"的位置
  three_1 :=num_table_temp(13,11)
  three_2 :=num_table_temp(16,14)

  four_0 :=num_table_temp(19,17) //ones=2 时：滑窗window中有4个"1",four_x 用于标注滑窗window中"1"的位置
  four_1 :=num_table_temp(22,20)
  four_2 :=num_table_temp(25,23)
  four_3 :=num_table_temp(28,26)

  val zeros = 0.U(6.W)

  switch(state) {
    is(idle) {

      when (io.in_valid ) {
        state := dect
        cnt := 0.U
        done_reg := 0.U
      }.otherwise{
        state := idle
        done_addr := 0.U
        done_reg := 0.U
      }
    }
    is(dect) {
      //cnt从0开始+1，
      when(cnt === 7.U){
        state := did
        done_reg := 1.U
        done_time_reg := done_time
      }
      when(cnt >= 4.U){
        done_addr := (done_addr << 2.U) | Cat(zeros,ones)
      }
      when(ones === 0.U){//ones=0 window滑窗中有1个"1"
                          //window是sewo权重向量中的8位
                          //                                io中sign的第x个元素， io中aManti(7位宽)的第x个元素，左移位数，位宽
        windowReg0_0 := Mux(window(0), UIntShiftToSInt_int8(io.sign(8.U * cnt),io.aManti(8.U * cnt),shiftNum, 31),
                      Mux(window(1), UIntShiftToSInt_int8(io.sign(8.U * cnt + 1.U),io.aManti(8.U * cnt + 1.U),shiftNum, 31),
                      Mux(window(2), UIntShiftToSInt_int8(io.sign(8.U * cnt + 2.U),io.aManti(8.U * cnt + 2.U),shiftNum, 31),
                      Mux(window(3), UIntShiftToSInt_int8(io.sign(8.U * cnt + 3.U),io.aManti(8.U * cnt + 3.U),shiftNum, 31),
                      Mux(window(4), UIntShiftToSInt_int8(io.sign(8.U * cnt + 4.U),io.aManti(8.U * cnt + 4.U),shiftNum, 31),
                      Mux(window(5), UIntShiftToSInt_int8(io.sign(8.U * cnt + 5.U),io.aManti(8.U * cnt + 5.U),shiftNum, 31),
                      Mux(window(6), UIntShiftToSInt_int8(io.sign(8.U * cnt + 6.U),io.aManti(8.U * cnt + 6.U),shiftNum, 31),
                      Mux(window(7), UIntShiftToSInt_int8(io.sign(8.U * cnt + 7.U),io.aManti(8.U * cnt + 7.U),shiftNum, 31),0.S))))))))
        windowReg0_1 := 0.S
        windowReg0_2 := 0.S
        windowReg0_3 := 0.S
        windowReg0_4 := 0.S
        windowReg0_5 := 0.S
        windowReg0_6 := 0.S
        windowReg0_7 := 0.S
        cnt := cnt + 1.U
      }.elsewhen(ones === 1.U){//ones=0 window滑窗中有2个"1"
          windowReg0_0 := UIntShiftToSInt_int8(io.sign(8.U * cnt +two_0),io.aManti(8.U * cnt +two_0),shiftNum, 31) 
          windowReg0_1 := UIntShiftToSInt_int8(io.sign(8.U * cnt +two_1),io.aManti(8.U * cnt +two_1),shiftNum, 31)
          windowReg0_2 := 0.S
          windowReg0_3 := 0.S
          windowReg0_4 := 0.S
          windowReg0_5 := 0.S
          windowReg0_6 := 0.S
          windowReg0_7 := 0.S
          cnt := cnt + 1.U
      }.elsewhen(ones === 2.U){//ones=2 
          when(Cat(three_0,three_1,three_2) === 0.U){//window滑窗中有4个"1"
            windowReg0_0 := UIntShiftToSInt_int8(io.sign(8.U * cnt +four_0),io.aManti(8.U * cnt +four_0),shiftNum, 31) 
            windowReg0_1 := UIntShiftToSInt_int8(io.sign(8.U * cnt +four_1),io.aManti(8.U * cnt +four_1),shiftNum, 31)
            windowReg0_2 := UIntShiftToSInt_int8(io.sign(8.U * cnt +four_2),io.aManti(8.U * cnt +four_2),shiftNum, 31)
            windowReg0_3 := UIntShiftToSInt_int8(io.sign(8.U * cnt +four_3),io.aManti(8.U * cnt +four_3),shiftNum, 31)
            windowReg0_4 := 0.S
            windowReg0_5 := 0.S
            windowReg0_6 := 0.S
            windowReg0_7 := 0.S
            }.otherwise{//window滑窗中有4个"1"
            windowReg0_0 := UIntShiftToSInt_int8(io.sign(8.U * cnt +three_0),io.aManti(8.U * cnt +three_0),shiftNum, 31) 
            windowReg0_1 := UIntShiftToSInt_int8(io.sign(8.U * cnt +three_1),io.aManti(8.U * cnt +three_1),shiftNum, 31)
            windowReg0_2 := UIntShiftToSInt_int8(io.sign(8.U * cnt +three_2),io.aManti(8.U * cnt +three_2),shiftNum, 31)
            windowReg0_3 := 0.S
            windowReg0_4 := 0.S
            windowReg0_5 := 0.S
            windowReg0_6 := 0.S
            windowReg0_7 := 0.S
            }
          cnt := cnt + 1.U
      }.otherwise{ //ones === 3.U, 放弃稀疏性
          windowReg0_0 := Mux(window(0), UIntShiftToSInt_int8(io.sign(8.U * cnt),io.aManti(8.U * cnt),shiftNum, 31), 0.S)
          windowReg0_1 := Mux(window(1), UIntShiftToSInt_int8(io.sign(8.U * cnt + 1.U),io.aManti(8.U * cnt + 1.U),shiftNum, 31), 0.S)
          windowReg0_2 := Mux(window(2), UIntShiftToSInt_int8(io.sign(8.U * cnt + 2.U),io.aManti(8.U * cnt + 2.U),shiftNum, 31), 0.S)
          windowReg0_3 := Mux(window(3), UIntShiftToSInt_int8(io.sign(8.U * cnt + 3.U),io.aManti(8.U * cnt + 3.U),shiftNum, 31), 0.S)
          windowReg0_4 := Mux(window(4), UIntShiftToSInt_int8(io.sign(8.U * cnt + 4.U),io.aManti(8.U * cnt + 4.U),shiftNum, 31), 0.S)
          windowReg0_5 := Mux(window(5), UIntShiftToSInt_int8(io.sign(8.U * cnt + 5.U),io.aManti(8.U * cnt + 5.U),shiftNum, 31), 0.S)
          windowReg0_6 := Mux(window(6), UIntShiftToSInt_int8(io.sign(8.U * cnt + 6.U),io.aManti(8.U * cnt + 6.U),shiftNum, 31), 0.S)
          windowReg0_7 := Mux(window(7), UIntShiftToSInt_int8(io.sign(8.U * cnt + 7.U),io.aManti(8.U * cnt + 7.U),shiftNum, 31), 0.S)
          cnt := cnt + 1.U
      }

    }
    is(did) {
      done_reg := 0.U
      state := idle
    }
  }
  io.out_valid :=   done_reg

  windowReg1_0 := windowReg0_0 + windowReg0_1
  windowReg1_1 := windowReg0_2 + windowReg0_3
  windowReg1_2 := windowReg0_4 + windowReg0_5
  windowReg1_3 := windowReg0_6 + windowReg0_7

  windowReg2_0 := windowReg1_0 + windowReg1_1
  windowReg2_1 := windowReg1_2 + windowReg1_3

  windowReg3_0 := windowReg2_0 + windowReg2_1

  //23.11.22改：Reg -> Wire 
  //TODO 这里未来将用于计算结果前递
  val valid_num1 = WireInit(0.U(1.W))

  val valid_num2 = WireInit(0.U(1.W))
  // val valid_num2_r1 = WireInit(0.U(1.W))

  val valid_num3 = WireInit(0.U(1.W))
  // val valid_num3_r1 = WireInit(0.U(1.W))
  // val valid_num3_r2 = WireInit(0.U(1.W))
  

  val valid_num4 = WireInit(0.U(1.W))
  // val valid_num4_r1 = WireInit(0.U(1.W))
  // val valid_num4_r2 = WireInit(0.U(1.W))
  // val valid_num4_r3 = WireInit(0.U(1.W))

  //在解码时，29位数据num_table_temp的最低两位的情况
  //one的值，代表有几个1
  when((state === dect) && (ones === 0.U)){
    valid_num1 := 1.U
  }
  .otherwise{
    valid_num1 := 0.U
  }

  
  when((state === dect) && (ones === 1.U)){
    // valid_num2_r1 := 1.U
    valid_num2 := 1.U
  }
  .otherwise{
    // valid_num2_r1 := 0.U
    valid_num2 := 0.U
  }
  // valid_num2 := valid_num2_r1

  
  when((state === dect) && ((ones === 2.U))){
    // valid_num3_r1 := 1.U
    valid_num3 := 1.U
  }
  .otherwise{
    // valid_num3_r1 := 0.U
    valid_num3 := 0.U
  }
  // valid_num3_r2 := valid_num3_r1
  // valid_num3 := valid_num3_r2

  when((state === dect) && ((ones === 3.U))){
    // valid_num4_r1 := 1.U
    valid_num4 := 1.U
  }
  .otherwise{
    // valid_num4_r1 := 0.U
    valid_num4 := 0.U
  }
  // valid_num4_r2 := valid_num4_r1
  // valid_num4_r3 := valid_num4_r2
  // valid_num4 := valid_num4_r3
  

  val SpillOver1 = RegInit(0.S(32.W))
  val SpillOver2 = RegInit(0.S(32.W))
  val SpillOver3 = RegInit(0.S(32.W))
  val SP1 = RegInit(0.U(1.W))
  val SP2 = RegInit(0.U(1.W))
  val SP3 = RegInit(0.U(1.W))

  
  val valid_sp = Wire(UInt(4.W))
  //注意！！：valid_sp 的组成
  valid_sp := Cat(valid_num4,valid_num3,valid_num2,valid_num1)
  /*
  理论上valid_sp最终得到一个4位宽的数据，其中包含多个1
  但是当多个1存在，则代表有多个输出值同时需要输出，而输出口只能一次输出一个，
  因此借助SpillOver寄存器充当缓存队列，sp则代表是否有效
  
  */

  switch(valid_sp){
    is(0.U){  //0000 Cat(valid_num4,valid_num3,valid_num2,valid_num1)
      //此时输出sp1,此处是将sp1在下一个周期置0
      SP1 := 0.U          
      SpillOver1 := 0.S
      //如果sp1中有值，才此时钟周期内，sp2不输出（保持不变
      SpillOver2 := Mux(SP1.asBool,SpillOver2,0.S)  
      //当SpillOver2中还有值时，sp2不能置0
      SP2 := Mux((SP1.asBool)&(SpillOver2 =/=0.S),1.U,0.U)  

      SpillOver3 := Mux(((SP1.asBool)|(SP2.asBool))&SP3.asBool,SpillOver3,0.S) 
      SP3 := Mux(((SP1.asBool)|(SP2.asBool))&(SpillOver3 =/=0.S),1.U,0.U) 

    }
    is(3.U){  //0011   Cat(valid_num4,valid_num3,valid_num2,valid_num1)
      SpillOver1 := Mux((SP1.asBool),SpillOver1,windowReg0_0)
      SP1 := 1.U
       //当sp1中有值，sp2中没有值，则将数据放在sp2中保存
      SpillOver2 := Mux((SP1.asBool)&(!SP2.asBool),windowReg0_0,SpillOver2)
      SP2 := Mux((SP1.asBool)|(SP2.asBool),1.U,0.U)
      //当sp1，sp2中有值，sp3中没有值，则将数据放在sp3中保存
      SpillOver3 := Mux((SP1.asBool)&(SP2.asBool)&(!SP3.asBool),windowReg0_0,SpillOver3)
      SP3 := Mux(((SP1.asBool)&(SP2.asBool))|(SpillOver3 =/=0.S),1.U,0.U)
    }
    is(5.U){  //0101    Cat(valid_num4,valid_num3,valid_num2,valid_num1)
      //此时输出windowReg2_0，保存windowReg0_0

      SpillOver1 := Mux((SP1.asBool),SpillOver1,windowReg0_0)
      SP1 := 1.U

      SpillOver2 := Mux((SP1.asBool)&(!SP2.asBool),windowReg0_0,SpillOver2)
      SP2 := Mux((SP1.asBool)|(SP2.asBool),1.U,0.U)

      // 23.11.24 改：SpillOver3 := Mux(SpillOver3 =/=0.S,SpillOver3,Mux((SP1.asBool) & (SP2.asBool),windowReg0_0,0.S))
      SpillOver3 := Mux((SP1.asBool)&(SP2.asBool)&(!SP3.asBool),windowReg0_0,SpillOver3)
      SP3 := Mux(SpillOver3 =/=0.S,1.U,  Mux((SP1.asBool) & (SP2.asBool),1.U,0.U))
    }
    is(6.U){  //0110    Cat(valid_num4,valid_num3,valid_num2,valid_num1)
      SpillOver1 := Mux((SP1.asBool),SpillOver1,windowReg1_0)
      SP1 := 1.U

      SpillOver2 := Mux((SP1.asBool)&(!SP2.asBool),windowReg1_0,SpillOver2)
      SP2 := Mux((SP1.asBool)|(SP2.asBool),1.U,0.U)
      //23.11.24 改：SpillOver3 := Mux((SP1.asBool)&(SP2.asBool),windowReg1_0,0.S)
      SpillOver3 := Mux(SpillOver3 =/=0.S,SpillOver3,
        Mux((SP1.asBool) & (SP2.asBool),windowReg1_0,0.S))

      //23.11.24 改：SP3 := Mux(((!SP1.asBool)|(!SP2.asBool))&(SpillOver3 =/=0.S),1.U,0.U)
      SP3 := Mux(SpillOver3 =/=0.S,1.U,  Mux((SP1.asBool) & (SP2.asBool),1.U,0.U))
      

    }
    is(7.U){  //0111    Cat(valid_num4,valid_num3,valid_num2,valid_num1)
      //当多个1存在时，要特别注意 
      SpillOver1 := Mux((SP1.asBool),SpillOver1,windowReg1_0)
      SP1 := 1.U
      //SP1=1则Sp2记录windowReg1_0，如果sp2也有值，则保持不变，让sp3记录windowReg1_0
      SpillOver2 := Mux((SP1.asBool)&(!SP2.asBool),windowReg1_0,
                    (Mux((!SP1.asBool)&(!SP2.asBool),windowReg0_0,SpillOver2)))
      SP2 := Mux(((SP1.asBool)|(SP2.asBool)),1.U,0.U)
      
      //增 2023、10、23.SP3 :=Mux(((SP1.asBool)&(SP2.asBool))|((SP1.asBool)&(!SP2.asBool))|((!SP1.asBool)&(SP2.asBool)),1.U,0.U)
      SP3 := Mux(SpillOver3 =/=0.S,1.U,  Mux((SP1.asBool) | (SP2.asBool),1.U,0.U))
      //2023.11.22改：遗漏WindowReg0_0 问题，造成bug
      SpillOver3 := Mux(SpillOver3 =/=0.S,SpillOver3,
                        Mux((SP1.asBool)&(SP2.asBool),windowReg1_0,
                          Mux((SP1.asBool)|(SP2.asBool),windowReg0_0,0.S)))
    }
    is(9.U){  //1001    Cat(valid_num4,valid_num3,valid_num2,valid_num1)
      SpillOver1 := Mux((SP1.asBool),SpillOver1,windowReg0_0)
      SP1 := 1.U

      SpillOver2 := Mux((SP1.asBool)&(!SP2.asBool),windowReg0_0,SpillOver2)
      SP2 := Mux((SP1.asBool)|(SP2.asBool),1.U,0.U)
      
      SpillOver3 := Mux(SpillOver3 =/=0.S,SpillOver3,
        Mux((SP1.asBool) & (SP2.asBool),windowReg0_0,0.S))
      SP3 := Mux(SpillOver3 =/=0.S,1.U,  Mux((SP1.asBool) & (SP2.asBool),1.U,0.U))

    }
    is(10.U){ //1010    Cat(valid_num4,valid_num3,valid_num2,valid_num1)
      SpillOver1 := Mux((SP1.asBool),SpillOver1,windowReg1_0)
      SP1 := 1.U

      SpillOver2 := Mux((SP1.asBool)&(!SP2.asBool),windowReg1_0,SpillOver2)
      SP2 := Mux((SP1.asBool)|(SP2.asBool),1.U,0.U)

      SpillOver3 := Mux(SpillOver3 =/=0.S,SpillOver3,
        Mux((SP1.asBool) & (SP2.asBool),windowReg1_0,0.S))
      SP3 := Mux(SpillOver3 =/=0.S,1.U,  Mux((SP1.asBool) & (SP2.asBool),1.U,0.U))
      
    }
    is(11.U){ //1011    Cat(valid_num4,valid_num3,valid_num2,valid_num1)

      SpillOver1 := Mux((SP1.asBool),SpillOver1,windowReg1_0)
      SP1 := 1.U
      SpillOver2 := Mux((SP1.asBool)&(!SP2.asBool),windowReg1_0,
                    (Mux((!SP1.asBool)&(!SP2.asBool),windowReg0_0,SpillOver2)))
      SP2 := 1.U

      //10.23 新增
      SP3 := Mux(SpillOver3 =/=0.S,1.U,  Mux((SP1.asBool) | (SP2.asBool),1.U,0.U))
      SpillOver3 := Mux(SpillOver3 =/=0.S,SpillOver3,
                        Mux((SP1.asBool)&(SP2.asBool),windowReg1_0,
                          Mux((SP1.asBool)|(SP2.asBool),windowReg0_0,0.S)))
    }
    is(12.U){ //1100    Cat(valid_num4,valid_num3,valid_num2,valid_num1)
      SpillOver1 := Mux((SP1.asBool),SpillOver1,windowReg2_0)
      SP1 := 1.U

      SpillOver2 := Mux((SP1.asBool)&(!SP2.asBool),windowReg2_0,SpillOver2)
      SP2 := Mux((SP1.asBool)|(SP2.asBool),1.U,0.U)
      
      SpillOver3 := Mux(SpillOver3 =/=0.S,SpillOver3,
        Mux((SP1.asBool) & (SP2.asBool),windowReg2_0,0.S))
      SP3 := Mux(SpillOver3 =/=0.S,1.U,  Mux((SP1.asBool) & (SP2.asBool),1.U,0.U))
      
    }
    is(13.U){ //1101    Cat(valid_num4,valid_num3,valid_num2,valid_num1)

      SpillOver1 := Mux((SP1.asBool),SpillOver1,windowReg2_0)
      SP1 := 1.U

      SpillOver2 := Mux((SP1.asBool)&(!SP2.asBool),windowReg2_0,
                    Mux((!SP1.asBool)&(!SP2.asBool),windowReg0_0,SpillOver2))
      SP2 := 1.U

      //增：2023.11.24
      SP3 := Mux(SpillOver3 =/=0.S,1.U,  Mux((SP1.asBool) | (SP2.asBool),1.U,0.U))
      SpillOver3 := Mux(SpillOver3 =/=0.S,SpillOver3,
                        Mux((SP1.asBool)&(SP2.asBool),windowReg2_0,
                          Mux((SP1.asBool)|(SP2.asBool),windowReg0_0,0.S)))

    }
    is(14.U){ //1110    Cat(valid_num4,valid_num3,valid_num2,valid_num1)

      SP1 := 1.U
      SpillOver1 := Mux((SP1.asBool),SpillOver1,windowReg2_0)

      SP2 := 1.U
      SpillOver2 := Mux((SP1.asBool)&(!SP2.asBool),windowReg2_0,
              Mux((!SP1.asBool)&(!SP2.asBool),windowReg1_0,SpillOver2))

      SP3 := Mux(SpillOver3 =/=0.S,1.U,  Mux((SP1.asBool) | (SP2.asBool),1.U,0.U))
      SpillOver3 := Mux(SpillOver3 =/=0.S,SpillOver3,
                  Mux((SP1.asBool)&(SP2.asBool),windowReg2_0,
                    Mux((SP1.asBool)|(SP2.asBool),windowReg1_0,0.S)))
    }
    is(15.U){ //1111    Cat(valid_num4,valid_num3,valid_num2,valid_num1)

      SP1 := 1.U
      SpillOver1 := Mux((SP1.asBool),SpillOver1,windowReg2_0)

      SP2 := 1.U
      SpillOver2 := Mux((SP1.asBool)&(!SP2.asBool),windowReg2_0,
              Mux((!SP1.asBool)&(!SP2.asBool),windowReg1_0,SpillOver2))

      SP3 := 1.U
      SpillOver3 := Mux(SpillOver3 =/=0.S,SpillOver3,
                  Mux((SP1.asBool)&(SP2.asBool),windowReg2_0,
                    Mux((SP1.asBool)|(SP2.asBool),windowReg1_0,windowReg0_0)))
      
    }


  }




  io.outNum := Mux((valid_num4).asBool,(windowReg3_0).asSInt,
                Mux((valid_num3).asBool,(windowReg2_0).asSInt,
                Mux((valid_num2).asBool,(windowReg1_0).asSInt,
                Mux((valid_num1).asBool,(windowReg0_0).asSInt,
                Mux((SP1).asBool,(SpillOver1).asSInt,
                Mux((SP2).asBool,(SpillOver2).asSInt,
                Mux((SP3).asBool,(SpillOver3).asSInt,
                0.S)))))))
}




