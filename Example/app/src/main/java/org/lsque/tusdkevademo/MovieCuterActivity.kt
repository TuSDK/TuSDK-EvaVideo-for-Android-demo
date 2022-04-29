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
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Bundle
import android.text.TextUtils
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewTreeObserver
import android.widget.RelativeLayout
import com.alexvasilkov.gestures.GestureController
import com.alexvasilkov.gestures.State
import com.tusdk.pulse.MediaInspector
import com.tusdk.pulse.Producer
import com.tusdk.pulse.ThumbnailMaker
import com.tusdk.pulse.Transcoder
import kotlinx.android.synthetic.main.activity_movie_editor_cut.*
import kotlinx.android.synthetic.main.activity_movie_editor_cut.lsq_change_media
import kotlinx.android.synthetic.main.activity_movie_editor_cut.lsq_cutRegionView
import kotlinx.android.synthetic.main.activity_movie_editor_cut.lsq_editor_cut_load
import kotlinx.android.synthetic.main.activity_movie_editor_cut.lsq_editor_cut_load_parogress
import kotlinx.android.synthetic.main.model_editor_activity.*
import kotlinx.android.synthetic.main.title_item_layout.*
import org.jetbrains.anko.textColor
import org.lasque.tusdkpulse.core.TuSdk
import org.lasque.tusdkpulse.core.TuSdkContext


import org.lasque.tusdkpulse.core.struct.TuSdkSize
import org.lasque.tusdkpulse.core.struct.TuSdkSizeF
import org.lasque.tusdkpulse.core.struct.ViewSize
import org.lasque.tusdkpulse.core.utils.StringHelper
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkpulse.core.utils.ThreadHelper
import org.lasque.tusdkpulse.core.utils.ThreadHelper.postDelayed
import org.lasque.tusdkpulse.core.utils.image.ImageOrientation
import org.lsque.tusdkevademo.playview.TuMaskRegionView
import org.lsque.tusdkevademo.playview.TuSDKMediaPlayer
import org.lsque.tusdkevademo.playview.TuSdkRangeSelectionBar
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class MovieCuterActivity : ScreenAdapterActivity() {
    //播放器
    private var mVideoPlayer: TuSDKMediaPlayer? = null

    private var mDurationTimeUs: Long = 0


    private var mLeftTimeRangUs: Long = 0


    private var mRightTimeRangUs: Long = 0


    private val mMinCutTimeUs = (1 * 1000000).toLong()


    private var cuter: Transcoder? = null


    private var isSetDuration = false


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

    private var mCurrentVideoSize = TuSdkSize.create(0)

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
            if (mVideoPlayer!!.isPlaying) {
                mVideoPlayer!!.pause()
                lsq_play_btn.setVisibility(View.VISIBLE)
            }
            mVideoPlayer!!.seekToPercentage(percent)
        }


        lsq_range_line.setSelectRangeChangedListener { leftPercent, rightPercent, type ->
            if (type == 0) {
                mLeftTimeRangUs = (leftPercent * mVideoPlayer!!.duration).toLong()
                var selectTime = (mRightTimeRangUs - mLeftTimeRangUs) / 1000000.0f
                if (mVideoPlayer!!.isPlaying()) {
                    mVideoPlayer!!.pause()
                    lsq_play_btn.setVisibility(View.VISIBLE)
                }
                lsq_range_line.setPercent(leftPercent)
                mVideoPlayer!!.seekToPercentage(leftPercent)
            } else if (type == 1) {
                mRightTimeRangUs = (rightPercent * mVideoPlayer!!.duration).toLong()
                var selectTime = (mRightTimeRangUs - mLeftTimeRangUs) / 1000000.0f
                if (mVideoPlayer!!.isPlaying()) {
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
//            setEnable(false)
            mVideoPlayer!!.pause()
            lsq_play_btn.visibility = View.GONE
            startCompound()
//            lsq_editor_cut_load.visibility = View.VISIBLE
        }
        lsq_change_media.setOnClickListener { finish() }
        lsq_play_btn.setOnClickListener {
            if (mVideoPlayer == null) return@setOnClickListener
            if (!mVideoPlayer!!.isPlaying()) {
                mVideoPlayer!!.start()
                lsq_play_btn.visibility = View.GONE
            } else {
                mVideoPlayer!!.pause()
                lsq_play_btn.visibility = View.VISIBLE
            }
        }

        lsq_gestureframe_wrap.setOnClickListener {
            if (!mVideoPlayer!!.isPlaying) {
                mVideoPlayer!!.start()
                lsq_play_btn.visibility = View.GONE
            } else {
                mVideoPlayer!!.pause()
                lsq_play_btn.visibility = View.VISIBLE
            }
        }
        val width = intent.getIntExtra("width", 0)
        val height = intent.getIntExtra("height", 0)
        val videoPath = intent.getStringExtra("videoPath")!!
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
            val params = lsq_scroll_wrap.layoutParams

            val screenSize = TuSdkContext.getScreenSize();
            params.width = screenSize.width
            params.height = (screenSize.width.toFloat() / width * height).toInt()
            lsq_scroll_wrap.layoutParams = params
            lsq_scroll_wrap.visibility = View.VISIBLE
            var percent = lsq_cutRegionView.regionRect.height().toFloat() / lsq_media_player!!.height.toFloat()
//            mCurrentCropRect.bottom = percent
            TLog.e("mDefaultCropRect = $mDefaultCropRect")
        }, 1000)
    }

    private fun setStateChangedListener() {
        lsq_scroll_wrap.controller.settings.minZoom = 1.0f
        lsq_scroll_wrap.controller.addOnStateChangeListener(object : GestureController.OnStateChangeListener {
            override fun onStateReset(oldState: State?, newState: State?) {
                TLog.e("oldState = $oldState newState = $newState")

            }

            override fun onStateChanged(state: State?) {
                TLog.e("state = " + state.toString())
                if (lsq_media_player!!.width == 0 || lsq_media_player!!.height == 0 || state == null) return



                val x = state.x
                val y = state.y

                val viewSize = TuSdkSize.create(lsq_media_player.layoutParams.width,lsq_media_player.layoutParams.height).scale(state.zoom)

                val top = abs(y)
                val left = abs(x)

                val bottom = abs(top + lsq_scroll_wrap.layoutParams.height)
                val right = abs(left + lsq_scroll_wrap.layoutParams.width)

                val fTop = top / viewSize.height
                val fLeft = left / viewSize.width
                val fBottom = bottom / viewSize.height
                val fRight = right / viewSize.width

                mCurrentCropRect.set(fLeft,fTop,fRight,fBottom)

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

        val mediaInfo = MediaInspector.shared().inspect(videoPath)
        for (info in mediaInfo.streams){
            if (info is MediaInspector.MediaInfo.Video){
                var size = TuSdkSize.create(info.width,info.height)
                val rotation = ImageOrientation.getValue(info.rotation,false)
                size = size.transforOrientation(rotation)
                val width = intent.getIntExtra("width", 0)
                val height = intent.getIntExtra("height", 0)
                val rangeStartTs = mLeftTimeRangUs / 1000
                val rangeEndTs = mRightTimeRangUs / 1000
                val targetSize = TuSdkSize(width,height)
                if (size.maxSide() > targetSize.maxSide()){
                    val scale = size.maxSide().toFloat() / targetSize.maxSide().toFloat()
                    size = size.scale(scale)
                }
                if (size.maxSide() > 1080){
                    val scale = 1080f / size.maxSide().toFloat()
                    size = size.scale(scale)
                }

                val intent = Intent()
                val rectf = mCurrentCropRect
                intent.putExtra("start",rangeStartTs)
                intent.putExtra("duration",rangeEndTs - rangeStartTs)
                intent.putExtra("videoPath", videoPath)
                intent.putExtra("zoom", floatArrayOf(rectf.left, rectf.top, min(1f, rectf.right), min(1f, rectf.bottom)))
                setResult(ModelEditorActivity.ALBUM_REQUEST_CODE_VIDEO, intent)
                finish()

            }
        }
    }

    private var w_h_p = 0f

    fun initPlayer() {
        val videoPath = intent.getStringExtra("videoPath")!!
        val maxDurationUs = intent.getFloatExtra("videoDuration", 0f)

        val width = intent.getIntExtra("width", 0)

        val format = getVideoFormat(videoPath)!!

        mDurationTimeUs = format.getLong(MediaFormat.KEY_DURATION)

        val videoWidth = format.getInteger(MediaFormat.KEY_WIDTH)
        val videoHeight = format.getInteger(MediaFormat.KEY_HEIGHT)

        var rotation = 0

        if (format.containsKey(MediaFormat.KEY_ROTATION)){
            rotation = format.getInteger("rotation-degrees")
        }

        mCurrentVideoSize.set(videoWidth,videoHeight)

        mCurrentVideoSize = mCurrentVideoSize.transforOrientation(ImageOrientation.getValue(rotation,false))


        lsq_media_player.setPlayerCallback(object : TuSDKMediaPlayer.PlayerCallback{
            override fun setStartPlayer(paramInt: Int) {
                lsq_media_player.seekTo(paramInt)
                lsq_play_btn.setVisibility(View.GONE)
            }

            override fun setEndPlayer() {
                lsq_play_btn.setVisibility(View.VISIBLE)
                lsq_media_player.seekTo(0)
            }

            override fun getProgress(paramFloat: Float) {
                val playPercent = paramFloat
                lsq_range_line.setPercent(playPercent)
            }

        })
        lsq_media_player.startPlay(videoPath)

        maxPercent = min((maxDurationUs.toFloat() / mDurationTimeUs), 1f)
        lsq_range_line.setMaxWidth(maxPercent)
        lsq_range_tips.text = "需截取视频的时长范围为 : 1.0s 至 ${maxDurationUs / 1000f / 1000f}s"

        val duration = mDurationTimeUs / 1000000.0f
        mRightTimeRangUs = (mDurationTimeUs * maxPercent).toLong()

        if (width > videoWidth){
            val scale = width.toDouble() / videoWidth

//            lsq_media_player.setScale(scale)

//            val params = lsq_media_player.layoutParams
//
//            params.width = (params.width * scale).toInt()
//            params.height = (params.height * scale).toInt()
//
//            lsq_media_player.layoutParams = params

//            mCurrentVideoSize = mCurrentVideoSize.scale(scale.toFloat())
        }

        mVideoPlayer = lsq_media_player
        mVideoPlayer?.start()


    }
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

    private fun getVideoFormat(extractor: MediaExtractor): MediaFormat? {
        for (index in 0 until extractor.trackCount) {
            if (isVideoFormat(extractor.getTrackFormat(index))) {
                return extractor.getTrackFormat(index)
            }
        }
        return null
    }

    private fun isVideoFormat(format: MediaFormat): Boolean {
        return getMimeTypeFor(format).startsWith("video/")
    }


    private fun getMimeTypeFor(format: MediaFormat): String {
        return format.getString(MediaFormat.KEY_MIME)!!
    }


    private var imageThumbExtractor: ThumbnailMaker? = null

    private fun loadVideoThumbList() {
        val videoPath = intent.getStringExtra("videoPath")

        ThreadHelper.runThread {
            imageThumbExtractor = ThumbnailMaker(videoPath,50)
            val mediaInfo = MediaInspector.shared().inspect(videoPath)
            for (info in mediaInfo.streams){
                if (info is MediaInspector.MediaInfo.Video){
                    val duration = info.duration
                    val unit  = duration / 20

                    for (i in 1..19){
                        val image = imageThumbExtractor!!.readImage(unit * i)
                        ThreadHelper.post {
                            lsq_range_line.addBitmap(image)
                            lsq_range_line.setMinWidth((mMinCutTimeUs / mDurationTimeUs).toFloat())
                        }
                    }

                    break
                }
            }
        }
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

    private var mRegionLayoutChangeListener: View.OnLayoutChangeListener = View.OnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
        // 视图布局改变
        if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
//            onRegionLayoutChanged(getCutRegionView())
        }
    }

    private fun onRegionLayoutChanged(cutRegionView: TuMaskRegionView?) {
//        lsq_media_player?.displayRect = RectF(cutRegionView!!.regionRect)
    }

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
        mVideoPlayer?.reset()
    }

}