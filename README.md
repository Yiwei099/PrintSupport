# PrintSupport(专注于 **收据** 与 **标签** 的打印工具)

## 概述
> ① 集成多个品牌打印机的SDK：GPrinter(佳博)，Epson(爱普森)，Bixolon(必胜龙)，StarX(待接入)  
> ② 已调试支持的打印机品牌：GPrinter(佳博)，Epson(爱普森)，Bixolon(必胜龙)，XPrinter(芯烨)，Element(元素)  
> ③ 支持局域网，USB，蓝牙通讯(具体情况取决于打印机以及使用的SDK策略)  
> ④ 开发者只需要关心打印的数据，无需关心打印的过程，减轻各SDK的对接成本  
> ⑤ 需要打印时自动建立连接，外打印完成自动销毁链接，减少通道占用率，稳定可靠  
> ⑥ 内置任务定时器，定时轮询任务队列，保证每个任务都被执行，防止任务丢失  
> ⑥ 只支持**Esc(收据)**，**Tsc(标签)** 两种指令；其他如针式，A4打印不在对接范围内所以不考虑对接  
> ⑦ 由于打印机有限，需要使用的朋友可以直接拉取源码进行调试  
> ⑧ SDK选择优先级建议：GPrinter > Epson > Bixolon > StarX  
> ⑨ 使用 **图像** 打印时效果更佳：[DrawingSupport](https://github.com/Yiwei099/DrawingSupport)

## 获取使用
### 1. Add it in your root build.gradle at the end of repositories:
```
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
  }
}
```

### 2. Add the dependency:
```
implementation 'com.github.Yiwei099:PrintSupport:$releaseVersion'
```

## 详情
### 1. GPrinter SDK(佳博)
> 指路：[佳博打印机官网](https://cn.gainscha.com/default.php)  
> 详细测试用例请看 **GPrinterActivity.kt**  
> 调试状态：Esc✅，Tsc✅，局域网✅，USB✅，蓝牙✖️，Esc图像✅，Esc指令✅，Tsc图像✅，Tsc指令✖️  
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
> 调试状态：Esc✅，Tsc✖️，局域网✅，USB✅，蓝牙✖️，Esc图像✅，Esc指令✖️，Tsc图像✅，Tsc指令✖️  
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
#### d. 检索打印机
```
//检索参数(局域网/蓝牙/USB)，(打印机/扫码枪/输入设备)
val option = FilterOption()
//监听结果(返回单个设备)
val listener = DiscoveryListener()

//开始检索
Discovery.start(mContext,option,listener)

//结束检索
Discovery.stop()
```

### 3.Bixolon(必胜龙标签)
> 指路：[Bixolon 打印机官网](https://cn.bixolon.com/company.php)  
> 详细测试用例请看 **BixolonPrinterActivity.kt**  
> 调试状态：Tsc✅，Esc✖️，局域网✅，USB✖️，蓝牙✖️，Esc图像✖️，Esc指令✖️，Tsc图像✅，Tsc指令✖️

#### a. 初始化JNI
```
//建议在 Application 初始化时调用
BixolonUtils.getInstance().initLibrary()
```
#### b. 创建
```
//局域网通讯
val key = "192.168.100.155"
val printer = BixolonNetLabelPrinter(mContext)

```
#### c. 打印
```
//以图片的形式打印
val mission = GraphicMission(bitmapArray)
printer.addMission(mission)
```
#### d. 销毁(必要时)
```
printer.onDestroy()
```
#### e. 检索 **Net** 打印机
```
//任意存在或不存在的IP地址
val ip = "192.168.3.12"

//创建任意必胜龙打印机
val printer = BixolonNetLabelPrinter(mContext,ip).apply{
    setOnFindPrinterCallBack {
        it.forEach {content->
            //返回的结果，结构为：macAddress + address + port
        }
    }
}

//开始检索
printer.startFindPrinter()
```

## 常见问题
### 1. 标签打印图片时宽不完整
> ① 调整生成图片时的宽度，控制调试在标签打印机的有效打印范围(如我所用于调试打印的标签 LabelProvide()，创建的图片宽度为 300 )  
> ② 指路生成图片工具：[DrawingSupport](https://github.com/Yiwei099/DrawingSupport)
![Image Text](https://github.com/Yiwei099/PrintSupport/blob/master/app/src/main/res/drawable/printer_width.png)

### 2. 使用 Bixolon(必胜龙)标签打印机SDK 时崩溃
> 检查是否已经初始化该 SDK 的 JNI
```
//建议在 Application 初始化时调用
BixolonUtils.getInstance().initLibrary()
```

## Print support by android(不定期更新)  
