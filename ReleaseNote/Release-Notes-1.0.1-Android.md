# TuSDK-EVA Release Notes for 1.0.1

## 简介


涂图·视频融合 SDK 服务是涂图推出的一款适用于 Android 平台的 SDK，主要功能为基于 Adobe After Effects 生成的视频模板，通过 Eva SDK 使其可以在移动端显示，并在此基础上支持对模板中的素材进行替换（编辑），当前支持的素材替换（编辑）包括视频、图片、文字、音频。Eva SDK 支持 Adobe After Effects 制作的绝大多数动画效果，解决了 Designer、Engineer、Product Manager们对于跨平台后引起的效果还原问题。


## 功能

* 增加了读取加密资源的逻辑


## 注意事项

* 在Manifest清单文件内的Application标签下 largeHeap 选项必须为True 否则在中低端机型上 会出现内存溢出的问题
