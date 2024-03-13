package bitlet

import chisel3._
import chisel3.util._
import Util._

class MeshReq[T <: Data : Arithmetic](accType: T, block_size: Int) extends Bundle {
  val shift = UInt(log2Up(accType.getWidth).W)
  val a_transpose = Bool()
//  val bd_transpose = Bool()
  val total_rows = UInt(log2Up(block_size + 1).W)
  val flush = UInt(2.W)
}

class MeshResp[T <: Data : Arithmetic](outType: T, meshCols: Int, meshRows: Int, block_size: Int) extends Bundle {
  val data = Vec(meshRows, Vec(meshCols, outType))
  //  val total_rows = UInt(log2Up(block_size + 1).W)
//  val valid = Bool()
}

class MeshNew[T <: Data : Arithmetic]
(inputType: T, val outputType: T, accType: T,
// tileRows: Int, tileColumns: Int,
 dataNumToPe: Int,
 meshRows: Int, meshColumns: Int)//meshRows：矩阵行；meshRows：矩阵列；tilerows\tilecolumns：合并为dataNumToPe
  extends Module {
  val A_TYPE = Vec(meshRows, Vec(dataNumToPe, inputType))
  val B_TYPE = Vec(meshColumns, Vec(dataNumToPe, inputType))
  //  val C_TYPE = Vec(meshColumns, Vec(tileColumns, outputType))
//  val D_TYPE = Vec(meshColumns, Vec(tileColumns, inputType))
  //  val S_TYPE = Vec(meshColumns, Vec(tileColumns, new PEControl(accType)))
  val block_size = meshRows * meshColumns

  val io = IO(new Bundle {
    val a = Flipped(Decoupled(A_TYPE))
    val b = Flipped(Decoupled(B_TYPE))
//    val d = Flipped(Decoupled(D_TYPE))

    val req = Flipped(Decoupled(new MeshReq(accType, block_size)))

    val resp = Valid(new MeshResp(outputType, meshColumns, meshRows, block_size))

  })

  val req = Reg(UDValid(new MeshReq(accType, block_size)))

  val total_fires = req.bits.total_rows
  val fire_counter = RegInit(0.U(log2Up(block_size).W))

  val a_buf = RegEnable(io.a.bits, io.a.fire)
  val b_buf = RegEnable(io.b.bits, io.b.fire)
//  val d_buf = RegEnable(io.d.bits, io.d.fire)

  val a_written = RegInit(false.B)
  val b_written = RegInit(false.B)
//  val d_written = RegInit(false.B)

  val input_next_row_into_spatial_array = req.valid && ((a_written && b_written ) || req.bits.flush > 0.U)
  val last_fire = fire_counter === total_fires - 1.U && input_next_row_into_spatial_array
  when(io.req.fire) {
    req.push(io.req.bits)
  }.elsewhen(last_fire) {
    req.valid := req.bits.flush > 1.U
    req.bits.flush := req.bits.flush - 1.U
  }

  when(input_next_row_into_spatial_array) {
    a_written := false.B
    b_written := false.B
//    d_written := false.B

    fire_counter := wrappingAdd(fire_counter, 1.U, total_fires)
  }

  when(io.a.fire) {
    a_written := true.B
  }

  when(io.b.fire) {
    b_written := true.B
  }

//  when(io.d.fire) {
//    d_written := true.B
//  }

  io.a.ready := !a_written || input_next_row_into_spatial_array || io.req.ready
  io.b.ready := !b_written || input_next_row_into_spatial_array || io.req.ready
//  io.d.ready := !d_written || input_next_row_into_spatial_array || io.req.ready

  val pause = !req.valid || !input_next_row_into_spatial_array

  val a_is_from_transposer = req.bits.a_transpose
  //  val b_is_from_transposer = req.bits.pe_control.dataflow === Dataflow.OS.id.U && req.bits.bd_transpose
//  val d_is_from_transposer = req.bits.bd_transpose
  val transposer = Module(new AlwaysOutTransposer(block_size, inputType))

//  transposer.io.inRow.valid := !pause && (a_is_from_transposer || d_is_from_transposer)
  transposer.io.inRow.valid := !pause && (a_is_from_transposer )
  transposer.io.inRow.bits := VecInit(a_buf.flatten)

  transposer.io.outCol.ready := true.B
  val transposer_out = VecInit(transposer.io.outCol.bits.grouped(dataNumToPe).map(t => VecInit(t)).toSeq)

  val mesh = Seq.fill(meshRows, meshColumns)(Module(new bitletPE(inputType, outputType, dataNumToPe,BitletConfigs(inputType, 4))))
//  val meshT = mesh.transpose

  val a_shifter_in = WireInit(Mux(a_is_from_transposer, transposer_out.asTypeOf(A_TYPE), a_buf))
  val b_shifter_in = WireInit(b_buf)
//  val d_shifter_in = WireInit(Mux(d_is_from_transposer,
//    VecInit(transposer_out.flatten.reverse.grouped(tileRows).map(VecInit(_)).toSeq).asTypeOf(D_TYPE), d_buf))

  //  val mesh: Seq[bitletPE[T]] = Seq.fill(meshRows)(Module(new bitletPE(inputType, outputType, tileRows, tileColumns)))

  for (r <- 0 until meshRows) {
    for(c <- 0 until meshColumns)
    {
      mesh(r)(c).io.in_a := b_shifter_in(c)
    }
  }
  for (c <- 0 until meshColumns) {
    for (r <- 0 until meshRows) {
      mesh(r)(c).io.in_b := a_shifter_in(r)
    }
  }
  //  for (r <- 0 until meshRows) {
  //    mesh.io.in_d := d_shifter_in
  //  }
  val result_shift = RegNext(req.bits.shift)

  val not_paused_vec = !pause
  for (r <- 0 until meshRows) {
    for (c <- 0 until meshColumns) {
      mesh(r)(c).io.in_valid := not_paused_vec
    }
  }

  //  val matmul_last_vec = last_fire


  //    mesh.io.in_valid := matmul_last_vec

  for (r <- 0 until meshRows) {
    for (c <- 0 until meshColumns) {
      io.resp.bits.data(r)(c) := mesh(r)(c).io.dataOut
    }
  }
  io.req.ready := (!req.valid || last_fire)

  val validBundle = Wire(Vec(meshRows, Bool()))


  for (r <- 0 until meshRows) {
      validBundle(r) := mesh(r).map(_.io.out_valid).reduce(_&&_)
  }

  io.resp.valid := validBundle.reduce(_&&_)


}
