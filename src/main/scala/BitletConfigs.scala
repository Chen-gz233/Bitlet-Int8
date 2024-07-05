package gemmini

import chisel3._
import chisel3.SInt

//inputType: 这个参数是一个泛型类型T，它扩展自Data，
//并且具有可用的隐式Arithmetic类型类。这个参数用于指定输入的数据类型。

case class BitletConfigs[T <: Data : Arithmetic](inputType:T,
//                          inputType: Datatype.Value = _,
//                          macnum: Int = 32,
                          windowsize: Int = 2
                        ) {
    //模式匹配
    //是否表示一个8位整数
  val is_int8 = inputType match {
    case Float(8, 24) => false    //FP32:浮点数，8位指数，24位尾数
    case Float(5, 11) => false    //FP16:浮点数，5位指数，11位尾数
    case Float(8, 8) => false     //FP16:浮点数，8位指数，8位尾数
    case _ => true                //INT8
  }
  //数据宽度
  val data_width = inputType match {
    case Float(8,24) => 32
    case Float(5,11) => 16
    case Float(8,8) => 16
    case _ => 8
  }
  //尾数（小数部分）的长度
  val manti_len = inputType match {
    case Float(8,24) => 23
    case Float(5,11) => 10
    case Float(8,8) => 7
    case _ => 6 // 7-1
  }
    //寄存器数量
  val reg_num = inputType match {
    case Float(8,24) => 24 //24->12->6->3->2->1
    case Float(5,11) => 12 //11->6->3->2->1
    case Float(8,8) => 7 //8->4->2->1
    case _ => 28 //7->4->2->1
  }
    //指数部分的宽度
  val exp_width = inputType match {
    case Float(8,24) => 8
    case Float(5,11) => 5
    case Float(8,8) => 8
    case _ => 0
  }
    //符号位的位置
  val sign_where = inputType match {
    case Float(8,24) => 31
    case Float(5,11) => 15
    case Float(8,8) => 15
    case _ => 7
  }
    //指数的最高位的位置
  val exp_head = inputType match {
    case Float(8,24) => 30
    case Float(5,11) => 14
    case Float(8,8) => 14
    case _ => 0
  }
  //指数的最低位的位置
  val exp_tail = inputType match {
    case Float(8,24) => 23
    case Float(5,11) => 10
    case Float(8,8) => 7
    case _ => 0
  }
}
