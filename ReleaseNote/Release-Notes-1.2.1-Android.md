# TuSDK-EVA Release Notes for 1.2.1

## 简介


涂图·视频融合 SDK 服务是涂图推出的一款适用于 Android 平台的 SDK，主要功能为基于 Adobe After Effects 生成的视频模板，通过 Eva SDK 使其可以在移动端显示，并在此基础上支持对模板中的素材进行替换（编辑），当前支持的素材替换（编辑）包括视频、图片、文字、音频。Eva SDK 支持 Adobe After Effects 制作的绝大多数动画效果，解决了 Designer、Engineer、Product Manager们对于跨平台后引起的效果还原问题。


## 功能

* 新增视频图层；
* 新增模板帧率获取接口；
* 新增设置保存区间的接口；
* 新增设置预览区间的接口；
* TuSdkEvaPlayerProgressListener 中新增对于设置预览区间后的进度回调 onProgressWithRange()；
* 新增预览、保存时错误回调；
* 新增替换图片素材尺寸大于 2K 时的压缩处理；
* 修复在骁龙652(620)系列机型上的图片渲染问题；
* 修复视频导出时,模板中无可替换音频坑位导致的音频导出失败；


## 注意事项

* 在Manifest清单文件内的Application标签下 largeHeap 选项必须为True 否则在中低端机型上 会出现内存溢出的问题
* TuEVASDK-1.2.1 版本对应 TuSDK EVA Template Export Tool-1.2.1 , TuSDK EVA Previewer-2.0.1
