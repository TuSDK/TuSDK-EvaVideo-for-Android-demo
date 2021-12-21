# TuSDK-EVA Release Notes for 3.0.1

## 简介


涂图·视频融合 SDK 服务是涂图推出的一款适用于 Android 平台的 SDK，主要功能为基于 Adobe After Effects 生成的视频模板，通过 Eva SDK 使其可以在移动端显示，并在此基础上支持对模板中的素材进行替换（编辑），当前支持的素材替换（编辑）包括视频、图片、文字、音频。Eva SDK 支持 Adobe After Effects 制作的绝大多数动画效果，解决了 Designer、Engineer、Product Manager们对于跨平台后引起的效果还原问题。


## 功能

* 新增摄像机图层(camera layer)支持。
* 新增调整图层支持。
* 重构文字图层,增加文字图层支持属性。
* 优化解码稳定性。


## 注意事项

* 在 Manifest 清单文件内的 Application 标签下 largeHeap 选项必须为 True 否则在中低端机型上 会出现内存溢出的问题。
* TuEVASDK-3.0.1 版本对应 导出插件 TuSDK EVA Template Export Tool-2.3.0(+)。
