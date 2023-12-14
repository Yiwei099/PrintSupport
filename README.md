# PrintSupport

## 概述
>· 集成常见品牌打印机的SDK，支持 GPrinter(佳博)，XPrinter(芯烨)，Epson(爱普森)，Bixolon(必胜龙)，Element(元素)  
>· 开发者只需要关心打印的数据，无需关心打印的过程，减轻各SDK的对接成本  
>· 需要打印时自动建立连接，除USB外打印完成自动销毁链接，减少通道占用率，稳定可靠  

## 详情
### 1. GPrinter(佳博)
>· 详细测试用例请看 **GPrinterActivity.kt**  
>· 实测此SDK可通讯大多数品牌的打印机；如：GPrinter(佳博)，XPrinter(芯烨)，Epson(爱普森)，Bixolon(必胜龙)，Element(元素)  

#### a. 创建
```
//局域网打印
val key = "192.168.100.150"
val printer = EscNetPrinter(context, netKey)

//USB打印
val usbKey = "vendorId+productId+SerialNumber"
val printer = EscUsbPrinter(context,usbKey)
```
#### b. 打印
```
//以图片的形式打印
val mission = GraphicParam(bitmapArray)

//以打印SDK指令组装的文本指令
val mission = CommandParam(escCommand)

//调用addMission即可
printer.addMission(mission)
```
#### c. 销毁(必要时)
```
printer.onDestroy()
```

### 2. Epson(爱普森)
>· 详细测试用例请看 **EpsonPrinterActivity.kt**  
>· 实测只能与自己品牌的打印机通讯

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
val mission = GraphicParam(bitmapArray)

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
更新中...

There are coming!!

#### Print support by android  
