package bitlet

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import scala.util.Random

class Int8Test_1024 extends AnyFlatSpec with ChiselScalatestTester {

  "Waveform" should "pass" in { 
    for(kk <- 0 until 1024){
      test(new bitletPE(SInt(8.W), SInt(32.W), 64, BitletConfigs(SInt(8.W)))).withAnnotations(Seq(WriteVcdAnnotation))  {
        dut =>
          //初始化
          var re = 0
          println("")
          println("")
          println("")
          println("初始化："+re,"循环："+kk)
          val weight = (1 to 64).map(_ => Random.nextInt(256) - 128).toArray
          val activate = (1 to 64).map(_ => Random.nextInt(256) - 128).toArray
          dut.io.in_valid.poke(false.B)
          for (i <- 0 until 64) {
            dut.io.in_b(i).poke(0.S)
            dut.io.in_a(i).poke(0.S)
          }

          //用于存储软件模拟结果 
          println("#########weight：#########")
          for(i <- 0 until 64){
              print(weight(i)+",") 
              if(i%8 === 0 & i !=0){
                  println("")
              }
          }
          println("")
          println("")
          println("")
          println("#########activate#########")
          for(i <- 0 until 64){
              print(activate(i)+",") 
              if(i%8 === 0 & i !=0){
                  println("")
              }
          }
          println("")
          for(i <- 0 until 64){
              re = weight(i)*activate(i) + re 
          }
          println("")
          println("软件模拟计算输出："+re)
          println("")


          //用于硬件计算
          dut.clock.step(1)
          dut.io.in_valid.poke(true.B)

          //开始赋值
          for (i <- 0 until 64) {
            dut.io.in_b(i).poke(weight(i).S)
            dut.io.in_a(i).poke(activate(i).S)
          }
          dut.clock.step(1)
          dut.io.in_valid.poke(false.B)

          dut.clock.step(20)
          println("")

          println("量化计算输出："+dut.io.dataOut.peek().toString)
          println("")
          println("")
          var ree = re.toString
          ree = {"SInt<32>("+ree+")"}
          println(""+ree)
          assert(ree ===(dut.io.dataOut.peek().toString) )

      }
    }
  }

}
