# ExpMax 模块功能总结

## 模块定义
- `ExpMax` 继承自 `Module`，参数包括：
  - `MacNum`：指数向量的大小
  - `is_int8`：布尔标志，指示是否使用8位整数模式
  - `exp_width`：指数的宽度

## 输入输出接口
- `exp`：输入指数向量，大小为 `MacNum`，每个元素的宽度为 `exp_width + 1`
- `expMax`：输出最大指数，宽度为 `exp_width + 1`
- `on`：输入控制信号
- `valid`：输出有效信号

## 操作模式
- **int8模式**：如果 `is_int8` 为真，模块将忽略输入信号，不执行实际的最大值计算，并将 `expMax` 和 `valid` 设为 `DontCare`
- **正常模式**：如果 `is_int8` 为假，模块将执行最大值计算

## 最大值计算
- 使用 `ShiftRegister` 将 `on` 信号延迟 `log2Ceil(MacNum)` 个时钟周期，以生成计数寄存器 `cntReg`
- 定义一个寄存器向量 `regTree` 来存储比较结果
- 初始化 `regTree` 的叶子节点，比较相邻的指数值，并将较大值存储在对应位置
- 使用嵌套的 `for` 循环构建加法树，逐层比较获取最大值
- 最终，将 `regTree` 的根节点值赋给 `expMax`，将 `cntReg` 的值赋给 `valid`

## 输出
- `expMax`：输出最大指数值
- `valid`：指示输出值是否有效的信号

这个模块通过比较多个指数值，计算出其中的最大值，并在特定条件下输出有效信号。
