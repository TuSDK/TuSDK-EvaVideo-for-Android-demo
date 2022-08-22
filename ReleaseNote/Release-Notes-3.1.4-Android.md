# TuSDK-EVA Release Notes for 3.1.4

## 简介


涂图·视频融合 SDK 服务是涂图推出的一款适用于 Android 平台的 SDK，主要功能为基于 Adobe After Effects 生成的视频模板，通过 Eva SDK 使其可以在移动端显示，并在此基础上支持对模板中的素材进行替换（编辑），当前支持的素材替换（编辑）包括视频、图片、文字、音频。Eva SDK 支持 Adobe After Effects 制作的绝大多数动画效果，解决了 Designer、Engineer、Product Manager们对于跨平台后引起的效果还原问题。


## 功能

* 添加`文字-梯度渐变`支持。
* 添加`预合成-梯度渐变`支持。
* 修复`梯度渐变`与AE显示不一致的问题。
* 修复`形状图层`对`椭圆路径`描边时,描边路径大于椭圆最大边时会出现中空的问题。
* 修复视频`时间重映射`的问题。
* 修复`形状图层`-`抗锯齿`参数错误的问题。
* 优化视频素材解码逻辑,避免因视频素材导致的渲染时间过长的问题。


## 注意事项

* 在 Manifest 清单文件内的 Application 标签下 largeHeap 选项必须为 True 否则在中低端机型上 会出现内存溢出的问题。
* TuEVASDK-3.1.4 版本对应 导出插件 TuSDK EVA Template Export Tool-2.4.3(+)。
