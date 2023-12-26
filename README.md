# PrintSupport

## 概述
> 专注于 **收据** 与 **标签** 的打印工具
> 集成多个品牌打印机的SDK：GPrinter(佳博)，Epson(爱普森)，Bixolon(必胜龙)，StarX(带接入)  
> 已调试支持的打印机品牌：GPrinter(佳博)，Epson(爱普森)，Bixolon(必胜龙)，XPrinter(芯烨)，Element(元素)  
> 支持局域网，USB，蓝牙通讯(具体情况取决于打印机以及使用的SDK策略)  
> 开发者只需要关心打印的数据，无需关心打印的过程，减轻各SDK的对接成本  
> 需要打印时自动建立连接，外打印完成自动销毁链接，减少通道占用率，稳定可靠  
> 只支持**Esc(收据)**，**Tsc(标签)** 两种指令；其他如针式，A4打印机不在对接范围内所以不考虑对接  
> 由于打印机有限，需要使用的同学可以直接拉取源码进行调试  
> SDK选择优先级建议：GPrinter > Epson > Bixolon > StarX  

## 详情
### 1. GPrinter SDK(佳博)
> 指路：[佳博打印机官网](https://cn.gainscha.com/default.php)  
> 详细测试用例请看 **GPrinterActivity.kt**  
> 调试状态：Esc✅，Tsc✅，局域网✅，USB✅，蓝牙✖️(手上一直没有可蓝牙通讯的打印机)  
> 实测此SDK可通讯大多数品牌的打印机；如：GPrinter(佳博)，XPrinter(芯烨)，Epson(爱普森)，Bixolon(必胜龙)，Element(元素)  

#### a. 创建
```
//局域网通讯
val key = "192.168.100.150"
val printer = EscNetGPrinter(context, netKey) //打印Esc
val printer = TscNetGPrinter(context,netKey) //打印Tsc

//USB通讯
val usbKey = "vendorId+productId+SerialNumber"
val printer = EscUsbGPrinter(context,vendorId,productId,serialNumber) //打印Esc
val printer = TscUsbGPrinter(context,vendorId,productId,serialNumber) //打印Tsc
```
#### b. 打印
```
//以图片的形式打印
val mission = GraphicMission(bitmapArray)

//以打印SDK指令组装的文本指令
val mission = CommandMission(escCommand)

//调用addMission即可
printer.addMission(mission)
```
#### c. 销毁(必要时)
```
printer.onDestroy()
```

### 2. Epson SDK(爱普森)
> 指路：[Epson 打印机官网](https://www.epson.com.cn/)  
> 详细测试用例请看 **EpsonPrinterActivity.kt**  
> 调试状态：Esc✅，Tsc✖️，局域网✅，USB✅，蓝牙✖️  
> 实测只能与自己品牌的打印机通讯  

#### a. 创建
```
//通讯方式：局域网/USB/蓝牙
val interface = Net/Usb/BlueTooth

//通讯地址：局域网IP/USB地址/蓝牙地址
val target = "192.168.0.1"

//创建打印机，必要时可指定Epson打印机的型号
val printer = EpsonPrinter(context,interface,target)
```
#### b. 打印
```
//以图片的形式打印
val mission = GraphicMission(bitmapArray)

//以打印SDK指令组装的文本指令
...

//调用addMission即可
printer.addMission(mission)
```
#### c. 销毁(必要时)
```
printer.onDestroy()
```

### 3.Bixolon(必胜龙)
> 指路：[Bixolon 打印机官网](https://cn.bixolon.com/company.php)  
> 详细测试用例请看 **BixolonPrinterActivity.kt**  
> 调试状态：Tsc✅，Esc✖️，局域网✅，USB✖️，蓝牙✖️

#### a. 创建
```
//局域网通讯
val key = "192.168.100.155"
val printer = BixolonNetLabelPrinter(mContext)

```
#### b. 打印
```
//以图片的形式打印
val mission = GraphicMission(bitmapArray)
printer.addMission(mission)
```
#### c. 销毁(必要时)
```
printer.onDestroy()
```

不定期更新

#### Print support by android  
