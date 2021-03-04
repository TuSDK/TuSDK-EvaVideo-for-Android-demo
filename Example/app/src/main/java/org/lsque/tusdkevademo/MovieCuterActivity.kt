/**
 *  TuSDK
 *  droid-sdk-eva$
 *  org.lsque.tusdkevademo$
 *  @author  H.ys
 *  @Date    2019/7/1$ 18:10$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lsque.tusdkevademo

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.RectF
import android.graphics.Typeface
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintLayout
import android.text.TextUtils
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.RelativeLayout
import com.alexvasilkov.gestures.GestureController
import com.alexvasilkov.gestures.State
import com.tusdk.pulse.Producer
import com.tusdk.pulse.Transcoder
import kotlinx.android.synthetic.main.activity_movie_editor_cut.*
import kotlinx.android.synthetic.main.activity_movie_editor_cut.lsq_change_media
import kotlinx.android.synthetic.main.activity_movie_editor_cut.lsq_cutRegionView
import kotlinx.android.synthetic.main.activity_movie_editor_cut.lsq_editor_cut_load
import kotlinx.android.synthetic.main.activity_movie_editor_cut.lsq_editor_cut_load_parogress
import kotlinx.android.synthetic.main.model_editor_activity.*
import kotlinx.android.synthetic.main.title_item_layout.*
import org.jetbrains.anko.textColor
import org.lasque.tusdk.core.TuSdk
import org.lasque.tusdk.core.api.extend.TuSdkMediaPlayerListener
import org.lasque.tusdk.core.api.extend.TuSdkMediaProgress
import org.lasque.tusdk.core.media.codec.extend.TuSdkMediaFormat
import org.lasque.tusdk.core.media.codec.extend.TuSdkMediaTimeSlice
import org.lasque.tusdk.core.media.codec.extend.TuSdkMediaUtils
import org.lasque.tusdk.core.media.codec.suit.TuSdkMediaFilePlayer
import org.lasque.tusdk.core.media.codec.suit.mutablePlayer.TuSdkMediaFilesCuterImpl
import org.lasque.tusdk.core.media.codec.suit.mutablePlayer.TuSdkMediaMutableFilePlayerImpl.TuSdkMediaPlayerStatus.Playing
import org.lasque.tusdk.core.media.codec.suit.mutablePlayer.TuSdkVideoImageExtractor
import org.lasque.tusdk.core.media.codec.video.TuSdkVideoQuality
import org.lasque.tusdk.core.media.suit.TuSdkMediaSuit
import org.lasque.tusdk.core.seles.output.SelesView
import org.lasque.tusdk.core.struct.TuSdkMediaDataSource
import org.lasque.tusdk.core.struct.TuSdkSize
import org.lasque.tusdk.core.struct.TuSdkSizeF
import org.lasque.tusdk.core.utils.StringHelper
import org.lasque.tusdk.core.utils.TLog
import org.lasque.tusdk.core.utils.ThreadHelper
import org.lasque.tusdk.core.utils.ThreadHelper.postDelayed
import org.lasque.tusdk.core.utils.image.ImageOrientation
import org.lasque.tusdk.core.view.widget.TuMaskRegionView
import org.lsque.tusdkevademo.playview.TuSdkRangeSelectionBar
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class MovieCuterActivity : ScreenAdapterActivity() {

    //播放器
    private var mVideoPlayer: TuSdkMediaFilePlayer? = null

    //播放视图
    private var mVideoView: SelesView? = null


    /** 当前剪裁后的持续时间   微秒  */
    private var mDurationTimeUs: Long = 0

    /** 左边控件选择的时间     微秒  */
    private var mLeftTimeRangUs: Long = 0

    /** 右边控件选择的时间     微秒 */
    private var mRightTimeRangUs: Long = 0

    /** 最小裁切时间  */
    private val mMinCutTimeUs = (1 * 1000000).toLong()

    /** 裁切工具  */
    private var cuter: TuSdkMediaFilesCuterImpl? = null

    /** 是否已经设置总时间  */
    private var isSetDuration = false

    /** 是否正在裁剪中  */
    private var isCutting = false

    private var mScaleDetector: ScaleGestureDetector? = null

    private var mCurrentZoom = 1.0f
    private var mCurrentX = 0.0f
    private var mCurrentY = 0.0f

    private var mDefaultCropRect: RectF = RectF(0f, 0f, 1f, 1f)
    private var mCurrentCropRect: RectF = RectF(0f, 0f, 1f, 1f)

    private var maxPercent = 0f

    private var isFirstSetRightBarPercent = true

    private var isFirstOnResume = true


    //播放器回调
    private val mMediaPlayerListener = object : TuSdkMediaPlayerListener {
        override fun onStateChanged(state: Int) {
            runOnUiThread {
                lsq_play_btn.setVisibility(if (state == 0) View.GONE else View.VISIBLE)
            }
        }

        override fun onFrameAvailable() {
            mVideoView!!.requestRender()
        }

        override fun onProgress(playbackTimeUs: Long, mediaDataSource: TuSdkMediaDataSource?, totalTimeUs: Long) {
            val playPercent = playbackTimeUs.toFloat() / totalTimeUs.toFloat()
            lsq_range_line.setPercent(playPercent)
        }

        override fun onCompleted(e: Exception?, mediaDataSource: TuSdkMediaDataSource?) {
            if (e != null) TLog.e(e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movie_editor_cut)
    }



    private fun initView() {
        lsq_title_item_title.text = "编辑"
        lsq_range_line.setType(1)
        lsq_range_line.isShowSelectBar = true
        lsq_range_line.setNeedShowCursor(true)
        lsq_range_line.setProgressChangeListener { percent ->
            if (!mVideoPlayer!!.isPause()) {
                mVideoPlayer!!.pause()
                lsq_play_btn.setVisibility(View.VISIBLE)
            }
            mVideoPlayer!!.seekToPercentage(percent)
        }


        lsq_range_line.setSelectRangeChangedListener { leftPercent, rightPercent, type ->
            if (type == 0) {
                mLeftTimeRangUs = (leftPercent * mVideoPlayer!!.durationUs()).toLong()
                var selectTime = (mRightTimeRangUs - mLeftTimeRangUs) / 1000000.0f
                if (!mVideoPlayer!!.isPause()) {
                    mVideoPlayer!!.pause()
                    lsq_play_btn.setVisibility(View.VISIBLE)
                }
                lsq_range_line.setPercent(leftPercent)
                mVideoPlayer!!.seekToPercentage(leftPercent)
            } else if (type == 1) {
                mRightTimeRangUs = (rightPercent * mVideoPlayer!!.durationUs()).toLong()
                var selectTime = (mRightTimeRangUs - mLeftTimeRangUs) / 1000000.0f
                if (!mVideoPlayer!!.isPause()) {
                    mVideoPlayer!!.pause()
                    lsq_play_btn.setVisibility(View.VISIBLE)
                }
                lsq_range_line.setPercent(rightPercent)
                mVideoPlayer!!.seekToPercentage(rightPercent)
            }
        }

        lsq_range_line.setExceedCriticalValueListener(object : TuSdkRangeSelectionBar.OnExceedCriticalValueListener {
            override fun onMinValueExceed() {
                val minTime = (mMinCutTimeUs / 1000000).toInt()
                @SuppressLint("StringFormatMatches") val tips = String.format(getString(R.string.lsq_min_time_effect_tips), minTime)
                TuSdk.messageHub()!!.showToast(this@MovieCuterActivity, tips)
            }

            override fun onMaxValueExceed() {

            }
        })

        lsq_back.setOnClickListener { finish() }
        lsq_next.text = "确定"
        lsq_next.textColor = Color.parseColor("#007aff")
        lsq_next.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
        lsq_next.textSize = 17f
        lsq_next.setOnClickListener {
            setEnable(false)
            mVideoPlayer!!.pause()
            lsq_play_btn.visibility = View.GONE
            startCompound()
            lsq_editor_cut_load.visibility = View.VISIBLE
        }
        lsq_change_media.setOnClickListener { finish() }
        lsq_play_btn.setOnClickListener {
            if (mVideoPlayer == null) return@setOnClickListener
            if (mVideoPlayer!!.elapsedUs() == mVideoPlayer!!.durationUs()) {
                mVideoPlayer!!.pause()
                //增加延时等待seek时间
                postDelayed({ mVideoPlayer!!.resume() }, 100)
            }
            if (mVideoPlayer!!.isPause()) {
                mVideoPlayer!!.resume()
                lsq_play_btn.visibility = View.GONE
            } else {
                mVideoPlayer!!.pause()
                lsq_play_btn.visibility = View.VISIBLE
            }
        }

        lsq_scroll_wrap.setOnClickListener {
            if (mVideoPlayer!!.isPause) {
                mVideoPlayer!!.resume()
                lsq_play_btn.visibility = View.GONE
            } else {
                mVideoPlayer!!.pause()
                lsq_play_btn.visibility = View.VISIBLE
            }
        }
        val width = intent.getIntExtra("width", 0)
        val height = intent.getIntExtra("height", 0)
        val videoPath = intent.getStringExtra("videoPath")
        val maxDurationUs = intent.getFloatExtra("videoDuration", 0f)
        mDurationTimeUs = getVideoFormat(videoPath)!!.getLong(MediaFormat.KEY_DURATION)
        initPlayer()
        setStateChangedListener()
        loadVideoThumbList()
        getCutRegionView()
        lsq_range_line.selectRange.viewTreeObserver.addOnGlobalLayoutListener {
            if (isFirstSetRightBarPercent && lsq_range_line.selectRange.measuredWidth != 0) {
                lsq_range_line.setRightBarPosition(maxPercent)
                isFirstSetRightBarPercent = false
                val globalLayoutListener = this
                lsq_range_line.selectRange.viewTreeObserver.removeOnGlobalLayoutListener { globalLayoutListener }
            }
        }
        val regionRatio = lsq_cutRegionView.regionRatio
        val regionRect = lsq_cutRegionView.regionRect
        lsq_cutRegionView.visibility = View.VISIBLE
        TLog.e("regionRect = $regionRect regionRatio = $regionRatio width = $width height = $height")
        lsq_scroll_wrap.visibility = View.INVISIBLE
        postDelayed({
            lsq_scroll_wrap.visibility = View.VISIBLE
            val params = lsq_scroll_wrap.layoutParams as ConstraintLayout.LayoutParams
            params.width = lsq_cutRegionView.regionRect.width()
            params.height = lsq_cutRegionView.regionRect.height()
            params.topToBottom = R.id.lsq_title
            params.bottomToTop = R.id.lsq_range_line
            lsq_scroll_wrap.layoutParams = params
            var percent = lsq_cutRegionView.regionRect.height().toFloat() / mVideoView!!.height.toFloat()
            mCurrentCropRect.bottom = percent
            TLog.e("mDefaultCropRect = $mDefaultCropRect")
        }, 1000)
    }

    private fun setStateChangedListener() {
        lsq_scroll_wrap.controller.addOnStateChangeListener(object : GestureController.OnStateChangeListener {
            override fun onStateReset(oldState: State?, newState: State?) {
                TLog.e("oldState = $oldState newState = $newState")
            }

            override fun onStateChanged(state: State?) {
                TLog.e("state = " + state.toString())
                if (mVideoView!!.width == 0 || mVideoView!!.height == 0) return

                var top:Float = 0.0f
                var left : Float = 0.0f
                var bottom : Float = 1.0f
                var right : Float = 1.0f

                val videoViewSize = TuSdkSize.create(mVideoView!!.width,mVideoView!!.height)
                val regionSize = TuSdkSize.create(lsq_cutRegionView.regionRect.width(),lsq_cutRegionView.regionRect.height())
                val currentVideoViewSize = videoViewSize.scale(state!!.zoom)
                val regionSizePercent = TuSdkSizeF.create(regionSize.width.toFloat()  / currentVideoViewSize.width.toFloat(),regionSize.height.toFloat()/ currentVideoViewSize.height.toFloat())
                left = if (regionSize.width >= currentVideoViewSize.width){
                    0.0f
                } else {
                    max(0.0f, abs(state.x) / currentVideoViewSize.width)
                }
                top = if (regionSize.height >= currentVideoViewSize.height){
                    0.0f
                } else {
                    max(0.0f,abs(state.y) / currentVideoViewSize.height)
                }
                bottom = min(1.0f,top + regionSizePercent.height)
                right = min(1.0f,left + regionSizePercent.width)
                mCurrentCropRect.set(left, top, right, bottom)
            }

        })
    }

    override fun onResume() {
        super.onResume()

    }

    override fun onStart() {
        super.onStart()
        if (isFirstOnResume){
            initView()
            isFirstOnResume = false
        }
    }


    private fun startCompound() {

        mVideoPlayer!!.pause()
        isDestory = true

        val videoPath = intent.getStringExtra("videoPath")
        isCutting = true

        val transcoder : Transcoder = Transcoder()
        val outputPath = getOutputTempFilePath().path

        transcoder.setListener { state, ts ->
            if (state == Producer.State.kWRITING){
                runOnUiThread {
                    lsq_editor_cut_load.setVisibility(View.VISIBLE)
                    lsq_editor_cut_load_parogress.setValue((ts / transcoder.duration.toFloat()) * 100f)
                }
            } else if (state == Producer.State.kEND){
                runOnUiThread {
                    transcoder.release()
                    setEnable(true)
                    lsq_editor_cut_load.setVisibility(View.GONE)
                    lsq_editor_cut_load_parogress.setValue(0f)
                    val intent = Intent()
                    val rectf = mCurrentCropRect
                    TLog.e("current rect = ${rectf}")
                    intent.putExtra("videoPath", outputPath)
                    intent.putExtra("zoom", floatArrayOf(rectf.left, rectf.top, min(1f, rectf.right), min(1f, rectf.bottom)))
                    setResult(ModelEditorActivity.ALBUM_REQUEST_CODE_VIDEO, intent)
                    finish()
                }
            }
        }
        var size = TuSdkMediaFormat.getVideoKeySize(getVideoFormat(videoPath))
        val rotation = ImageOrientation.getValue(TuSdkMediaFormat.getVideoKeyRotation(getVideoFormat(videoPath)),false)
        size = size.transforOrientation(rotation)
        val width = intent.getIntExtra("width", 0)
        val height = intent.getIntExtra("height", 0)
        val rangeStartTs = mLeftTimeRangUs / 1000
        val rangeEndTs = mRightTimeRangUs / 1000
        val targetSize = TuSdkSize(width,height)
        val config = Producer.OutputConfig()
        if (size.maxSide() > targetSize.maxSide()){
            val scale = size.maxSide().toFloat() / targetSize.maxSide().toFloat()
            size = size.scale(scale)
        }
        if (size.maxSide() > 1080){
            val scale = 1080f / size.maxSide().toFloat()
            size = size.scale(scale)
        }
        config.width = size.width
        config.height = size.height
        config.rangeStart = rangeStartTs
        config.rangeDuration = rangeEndTs - rangeStartTs
        config.keyint = 0
        transcoder.setOutputConfig(config)

        if (!transcoder.init(outputPath,videoPath)){
            return
        }
        transcoder.start()
        setEnable(false)
    }

    private var w_h_p = 0f

    /** 初始化播放器  */
    fun initPlayer() {
        val videoPath = intent.getStringExtra("videoPath")
        val maxDurationUs = intent.getFloatExtra("videoDuration", 0f)
        mDurationTimeUs = getVideoFormat(videoPath)!!.getLong(MediaFormat.KEY_DURATION)
        val sourceList = ArrayList<TuSdkMediaDataSource>()

        val video = TuSdkMediaDataSource(videoPath)
        sourceList.add(video)

        maxPercent = min((maxDurationUs / mDurationTimeUs), 1f)
        lsq_range_line.setMaxWidth(maxPercent)
        lsq_range_tips.text = "需截取视频的时长范围为 : 1.0s 至 ${maxDurationUs / 1000f / 1000f}s"

        val duration = mDurationTimeUs / 1000000.0f
        mRightTimeRangUs = (mDurationTimeUs * maxPercent).toLong()


        /** 创建预览视图  */
        mVideoView = SelesView(this)
        mVideoView!!.fillMode = SelesView.SelesFillModeType.PreserveAspectRatioAndFill

        mVideoPlayer = TuSdkMediaSuit.playMedia(video, true, mMediaPlayerListener) as TuSdkMediaFilePlayer
        if (mVideoPlayer == null) {
            TLog.e("%s directorPlayer create failed.", "TAG")
            return
        }
        mVideoView!!.isEnableRenderer = true
        mVideoView!!.setRenderer(mVideoPlayer!!.getExtenalRenderer())


        val params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        params.height = video.mediaMetadataRetriever.frameAtTime.height
        params.width = video.mediaMetadataRetriever.frameAtTime.width
        w_h_p = video.mediaMetadataRetriever.frameAtTime.width.toFloat() / video.mediaMetadataRetriever.frameAtTime.height.toFloat()
        lsq_scroll_wrap.addView(mVideoView!!, 0, params)
        /** Step 3: 连接视图对象  */
        mVideoPlayer!!.getFilterBridge().addTarget(mVideoView, 0)

    }

    /**
     * 获取视频格式信息
     *
     * @param dataSource
     * 文件地址
     * @return
     */
    private fun getVideoFormat(videoPath: String): MediaFormat? {

        val extractor = MediaExtractor()

        try {
            if (!TextUtils.isEmpty(videoPath))
                extractor.setDataSource(videoPath)
            else
                return null
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }

        val videoFormat = getVideoFormat(extractor)
        extractor.release()

        return videoFormat
    }

    /**
     * 获取视频格式
     *
     * @param extractor
     * @return
     */
    private fun getVideoFormat(extractor: MediaExtractor): MediaFormat? {
        for (index in 0 until extractor.trackCount) {
            if (isVideoFormat(extractor.getTrackFormat(index))) {
                return extractor.getTrackFormat(index)
            }
        }
        return null
    }

    /**
     * 判断是否是视频格式
     *
     * @param format
     * @return
     */
    private fun isVideoFormat(format: MediaFormat): Boolean {
        return getMimeTypeFor(format).startsWith("video/")
    }

    /**
     * @param format
     * @return
     */
    private fun getMimeTypeFor(format: MediaFormat): String {
        return format.getString(MediaFormat.KEY_MIME)
    }


    private var imageThumbExtractor: TuSdkVideoImageExtractor? = null

    /** 加载视频缩略图  */
    private fun loadVideoThumbList() {
        val videoPath = intent.getStringExtra("videoPath")
        val sourceList = ArrayList<TuSdkMediaDataSource>()
        sourceList.add(TuSdkMediaDataSource(videoPath))

        /** 准备视频缩略图抽取器  */
        imageThumbExtractor = TuSdkVideoImageExtractor(sourceList)
        imageThumbExtractor!!
                //.setOutputImageSize(TuSdkSize.create(50,50)) // 设置抽取的缩略图大小
                .setExtractFrameCount(20) // 设置抽取的图片数量
                .setImageListener(object : TuSdkVideoImageExtractor.TuSdkVideoImageExtractorListener {

                    /**
                     * 输出一帧略图信息
                     *
                     * @param videoImage 视频图片
                     * @since v3.2.1
                     */
                    override fun onOutputFrameImage(videoImage: TuSdkVideoImageExtractor.VideoImage) {
                        if (isDestory) return
                        ThreadHelper.post {
                            lsq_range_line.addBitmap(videoImage.bitmap)
                            lsq_range_line.setMinWidth((mMinCutTimeUs / mDurationTimeUs).toFloat())
                        }
                    }

                    /**
                     * 抽取器抽取完成
                     *
                     * @since v3.2.1
                     */
                    override fun onImageExtractorCompleted(videoImagesList: List<TuSdkVideoImageExtractor.VideoImage>) {
                        /** 注意： videoImagesList 需要开发者自己释放 bitmap  */
                        imageThumbExtractor!!.release()
                        imageThumbExtractor = null


                    }
                })
                .extractImages() // 抽取图片
    }

    private fun setEnable(enable: Boolean) {
        lsq_back.isEnabled = enable
        lsq_next.isEnabled = enable
        lsq_play_btn.isEnabled = enable
        lsq_range_line.isEnabled = enable
        lsq_scroll_wrap.isEnabled = enable
    }

    private fun getCutRegionView(): TuMaskRegionView? {
        val width = intent.getIntExtra("width", 0)
        val height = intent.getIntExtra("height", 0)
        lsq_cutRegionView.edgeMaskColor = Color.parseColor("#00ffffff")
        lsq_cutRegionView.edgeSideColor = 0xff007aff.toInt()
        lsq_cutRegionView.setEdgeSideWidthDP(2)
        lsq_cutRegionView.regionSize = TuSdkSize(width, height)
        lsq_cutRegionView.addOnLayoutChangeListener(mRegionLayoutChangeListener)
        return lsq_cutRegionView
    }

    /** 裁剪选取视图布局改变  */
    private var mRegionLayoutChangeListener: View.OnLayoutChangeListener = View.OnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
        // 视图布局改变
        if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
//            onRegionLayoutChanged(getCutRegionView())
        }
    }

    /** 裁剪选取视图布局改变  */
    private fun onRegionLayoutChanged(cutRegionView: TuMaskRegionView?) {
        mVideoView?.displayRect = RectF(cutRegionView!!.regionRect)
    }

    /** 获取临时文件路径  */
    protected fun getOutputTempFilePath(): File {
        return File(TuSdk.getAppTempPath(), String.format("lsq_%s.mp4", StringHelper.timeStampString()))
    }

    override fun onPause() {
        super.onPause()
        mVideoPlayer?.pause()
    }

    var isDestory = false

    override fun onDestroy() {
        super.onDestroy()
        isDestory = true
        mVideoPlayer?.release()
    }
}