# TuSDK-EVA Release Notes for 3.1.2

## 简介


涂图·视频融合 SDK 服务是涂图推出的一款适用于 Android 平台的 SDK，主要功能为基于 Adobe After Effects 生成的视频模板，通过 Eva SDK 使其可以在移动端显示，并在此基础上支持对模板中的素材进行替换（编辑），当前支持的素材替换（编辑）包括视频、图片、文字、音频。Eva SDK 支持 Adobe After Effects 制作的绝大多数动画效果，解决了 Designer、Engineer、Product Manager们对于跨平台后引起的效果还原问题。


## 功能

* 添加 Camera 支持。
* 添加 视频重映射 支持。
* 添加 图层样式-描边 支持。
* 修复 轨道遮罩 时长与上层不一致会显示黑色的问题。
* 修复 运动模糊进度设置问题。
* 修复 形状图层-多边星形与AE显示不一致的问题。
* SDK 增加字体外部读取接口。
* SDK 修复替换图片后首帧倒置的问题。


## 注意事项

* 在 Manifest 清单文件内的 Application 标签下 largeHeap 选项必须为 True 否则在中低端机型上 会出现内存溢出的问题。
* TuEVASDK-3.1.2 版本对应 导出插件 TuSDK EVA Template Export Tool-2.4.3(+)。
