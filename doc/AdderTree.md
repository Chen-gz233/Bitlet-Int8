# AdderTree 模块功能总结

## 模块定义
- `AdderTree` 继承自 `Module`，参数包括：
  - `macnum`：乘加器的数量
  - `exp_width`：指数的宽度
  - `manti_len`：尾数的长度
  - `is_int8`：布尔标志，指示是否使用8位整数模式
  - `data_width`：数据宽度
  - `reg_num`：寄存器数量

## 输入输出接口
- `e_valid`：输入有效信号
- `ochi`：输入数据向量，大小为8，每个元素为32位有符号整数（SInt(32.W)）
- `data`：输出累加结果，为32位有符号整数（SInt(32.W)）
- `valid`：输出有效信号

## 功能描述
- **累加操作**：
  - 定义并初始化两个32位的累加寄存器 `sumReg`
  - 定义并初始化32位的累加结果 `sum`
  - 将 `ochi` 的前四个元素相加并赋值给 `sumReg(0)`
  - 将 `ochi` 的后四个元素相加并赋值给 `sumReg(1)`
  - 将 `sumReg` 中的值相加，再加上 `sum` 的当前值，并赋值给 `sum`
  - 将 `sum` 的值赋给输出端口 `data`

- **控制信号**：
  - 定义并初始化移位寄存器 `shiftReg`
  - 定义并初始化 `rs_to_as_allow` 信号
  - 当 `rs_to_as_allow` 为真时，允许 `ochi` 准备好接收数据
  - 当 `ochi` 的 `fire` 信号有效时，设置 `shiftReg`
  - 将 `shiftReg` 的值赋给输出端口 `valid`

## 输出
- `data`：输出累加结果，为32位有符号整数（SInt(32.W)）
- `valid`：指示输出值是否有效的信号

这个模块通过对输入数据向量的各个元素进行累加操作，计算出累加结果，并提供一个有效信号。
