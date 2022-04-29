# TuSDK-EVA Release Notes for 3.1.1

## 简介


涂图·视频融合 SDK 服务是涂图推出的一款适用于 Android 平台的 SDK，主要功能为基于 Adobe After Effects 生成的视频模板，通过 Eva SDK 使其可以在移动端显示，并在此基础上支持对模板中的素材进行替换（编辑），当前支持的素材替换（编辑）包括视频、图片、文字、音频。Eva SDK 支持 Adobe After Effects 制作的绝大多数动画效果，解决了 Designer、Engineer、Product Manager们对于跨平台后引起的效果还原问题。


## 功能

* 新增竖版文字支持。

* 修复 windows预览器可替换图片坑位缩略图显示错乱及替换后的刷新。
* 修复 移动端png图片颜色异常问题。
* 修复 运动模糊有斑块的问题。
* 修复 修剪路径动画显示错误的问题。
* 修复 线性差除，百叶窗显示不全的问题。
* 修复 径向擦除中心点偏移问题。
* 修复 形状图层之间无缝隙拼接的时候会出现一个像素点的缝隙的问题。
* 修复 预合成画布大小，解决旋转边角被裁剪的问题。
* 替换视频如果不裁剪，默认为居中裁剪，使图片不拉伸。
* 插件导出模型后在根目录下生成model.eva，无需再上传控制台。


## 注意事项

* 在 Manifest 清单文件内的 Application 标签下 largeHeap 选项必须为 True 否则在中低端机型上 会出现内存溢出的问题。
* TuEVASDK-3.1.1 版本对应 导出插件 TuSDK EVA Template Export Tool-2.4.3(+)。
