# TuSDK-EVA Release Notes for 1.2.2

## 简介


涂图·视频融合 SDK 服务是涂图推出的一款适用于 Android 平台的 SDK，主要功能为基于 Adobe After Effects 生成的视频模板，通过 Eva SDK 使其可以在移动端显示，并在此基础上支持对模板中的素材进行替换（编辑），当前支持的素材替换（编辑）包括视频、图片、文字、音频。Eva SDK 支持 Adobe After Effects 制作的绝大多数动画效果，解决了 Designer、Engineer、Product Manager们对于跨平台后引起的效果还原问题。


## 功能

* 新增，不支持的图层、特效以及功能的错误码。
* 新增，允许模板中使用 kmplayer编码格式（渐进式）的 JPEG 图片。
* 修复时间精度读取不正确的问题。
* 修复高斯模糊阈值异常的问题。


## 注意事项

* 在 Manifest 清单文件内的 Application 标签下 largeHeap 选项必须为 True 否则在中低端机型上 会出现内存溢出的问题。
* TuEVASDK-1.2.2 版本对应 导出插件 TuSDK EVA Template Export Tool-2.1.1(+)。
