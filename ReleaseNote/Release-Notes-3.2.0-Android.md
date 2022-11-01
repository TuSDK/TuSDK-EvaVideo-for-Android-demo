# TuSDK-EVA Release Notes for 3.2.0

## 简介


涂图·视频融合 SDK 服务是涂图推出的一款适用于 Android 平台的 SDK，主要功能为基于 Adobe After Effects 生成的视频模板，通过 Eva SDK 使其可以在移动端显示，并在此基础上支持对模板中的素材进行替换（编辑），当前支持的素材替换（编辑）包括视频、图片、文字、音频。Eva SDK 支持 Adobe After Effects 制作的绝大多数动画效果，解决了 Designer、Engineer、Product Manager们对于跨平台后引起的效果还原问题。


## 功能

* 添加 预合成层转换序列帧视频支持。
* 添加 常用表达式支持。
* 添加 `形状图层`->`渐变填充`。
* 添加 `形状图层`->`渐变描边`。
* 添加 `效果` -> `颜色校正`-> `通道混合器`。
* 添加 `效果` -> `颜色校正`-> `颜色平衡`。
* 修复 不支持的文字动画选择器会被错误的添加到选择器集合中的问题。
* 修复 `摄像机`->`缩放` 属性会导致画面异常的问题。
* 修复 同时存在`轨道遮罩`与`动态拼贴`时 `轨道遮罩`渲染位置异常的问题。


## 注意事项

* 在 Manifest 清单文件内的 Application 标签下 largeHeap 选项必须为 True 否则在中低端机型上 会出现内存溢出的问题。
* TuSDKEVA-3.2.0 版本对应 导出插件 TuSDK EVA Template Export Tool-2.2.6(+) 或TuSDK EVA Designer-(1.0.0)。
