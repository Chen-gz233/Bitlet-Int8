package gemmini

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import scala.util.Random

class Int8Test extends AnyFlatSpec with ChiselScalatestTester {
//用于存储软件模拟结果  
var re = 0

// val weight = (1 to 64).map(_ => Random.nextInt(256) - 128).toArray
// val activate = (1 to 64).map(_ => Random.nextInt(256) - 128).toArray


val weight = Array(
-109,60,-34,-96,-109,35,9,10,69,
21,-48,-102,-34,-53,101,107,57,
-12,20,25,-58,43,45,84,14,
88,-77,92,124,37,-18,73,116,
-118,34,-14,123,-68,-81,-62,-128,
110,-116,60,-26,-28,-54,19,-121,
95,-56,-1,-104,-18,33,13,-6,
-105,100,45,115,122,-92,83
)

val activate = Array( 
9,-31,45,-68,41,122,11,-87,35,
-47,-36,115,-1,-117,110,-85,-52,
-104,-8,92,-82,69,-10,32,-114,
-9,-16,30,-31,-86,22,-122,93,
-116,107,124,-102,14,-10,121,-2,
-57,43,33,23,59,-125,120,-67,
-128,-116,57,-84,121,70,-68,8,
5,-112,115,-121,100,-112,10
)
/*
val weight = (1 to 64).map(_ => Random.nextInt(256) - 128).toArray
val activate = (1 to 64).map(_ => Random.nextInt(256) - 128).toArray


*/
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
  println("")
  println("")
  println("软件模拟计算输出："+re)
  println("")
  println("")
  println("")

  "Waveform" should "pass" in { 
    test(new gemminiPE(SInt(8.W), SInt(32.W), SInt(32.W),64, BitletConfigs(SInt(8.W)))).withAnnotations(Seq(WriteVcdAnnotation))  {
      dut =>
        //初始化
        dut.io.in_valid.poke(false.B)
        for (i <- 0 until 64) {
          dut.io.in_b(i).poke(0.S)
          dut.io.in_a(i).poke(0.S)
        }
        dut.clock.step(1)
        dut.io.in_valid.poke(true.B)
        //开始赋值
        for (i <- 0 until 64) {
          dut.io.in_b(i).poke(weight(i).S)
          dut.io.in_a(i).poke(activate(i).S)
        }
        dut.clock.step(1)
        dut.io.in_valid.poke(false.B)

        dut.clock.step(100)
        println("量化计算输出："+dut.io.dataOut.peek().toString)
        
       
    }
  } 
}
