/**
 *  TuSDK
 *  EvaDemo$
 *  org.lsque.tusdkevademo$
 *  @author  H.ys
 *  @Date    2022/7/15$ 14:14$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lsque.tusdkevademo

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.SeekBar
import com.tusdk.pulse.eva.EvaModel
import kotlinx.android.synthetic.main.model_video_activity.*
import kotlinx.android.synthetic.main.title_item_layout.*
import org.jetbrains.anko.startActivity
import org.lasque.tusdkpulse.core.utils.AssetsHelper
import org.lasque.tusdkpulse.core.utils.TLog
import org.lsque.tusdkevademo.albumselect.AlbumSelectActivity
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * TuSDK
 * org.lsque.tusdkevademo
 * EvaDemo
 *
 * @author        H.ys
 * @Date        2022/7/15  14:14
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class ModelVideoActivity : ScreenAdapterActivity(){

    private var mEvaModel : EvaModel? = null
    private var isSeekbarTouch = false

    private var mEvaThreadPool : ExecutorService = Executors.newSingleThreadScheduledExecutor()

    private var mMediaPlayer : MediaPlayer? = null

    private var mMediaPlayerPreparedFinish = false

    private fun playerPause(){
        lsq_player_img.visibility = View.VISIBLE
        mMediaPlayer?.pause()
    }

    private var mCurrentModelItem : ModelItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.model_video_activity)
        initView()
    }

    private fun initView() {

        lsq_next.visibility = View.GONE

        lsq_title_item_title.text = "模板详情"
        lsq_back.setOnClickListener { finish() }
        val modelItem = intent.getParcelableExtra<ModelItem>("model")!!
        mCurrentModelItem = modelItem

        val videoUrl = modelItem.videoUrl

        lsq_model_name.text = modelItem.modelName

        initMediaPlayer(videoUrl)

        mEvaThreadPool.execute {
            mEvaModel = EvaModel()
            if (AssetsHelper.hasAssets(this,modelItem.templateName)){
                if (!mEvaModel!!.createFromAsset(this,modelItem.templateName)){
                    TLog.e("[Error] Assets File not fount")
                }
            } else {
                if (!mEvaModel!!.create(modelItem.modelDownloadFilePath)){
                    TLog.e("[Error]File not fount")

                }
            }

            var textCount = mEvaModel!!.listReplaceableTextAssets().size
            var imageVideoCount = mEvaModel!!.listReplaceableImageAssets().size + mEvaModel!!.listReplaceableVideoAssets().size
            var audioCount = mEvaModel!!.listReplaceableAudioAssets().size

            runOnUiThread {
                lsq_model_replace_info.text =
                    "文字 ${textCount}段\n" +
                            "图片/视频 ${imageVideoCount}个\n" +
                            "音频 ${audioCount}个"
            }

        }
    }

    private fun initMediaPlayer(videoUrl: String) {
        mMediaPlayer = MediaPlayer()
        mMediaPlayer!!.setDataSource(videoUrl)

        mMediaPlayer!!.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)

        val audioAttributes = AudioAttributes.Builder()
        audioAttributes.setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
        audioAttributes.setUsage(AudioAttributes.USAGE_MEDIA)

        mMediaPlayer!!.isLooping = true
        mMediaPlayer!!.setAudioAttributes(audioAttributes.build())

        mMediaPlayer!!.setOnPreparedListener {
            mMediaPlayerPreparedFinish = true
            runOnUiThread {
                lsq_seek.max = mMediaPlayer!!.duration
                lsq_seek.progress = 0;
                lsq_seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        if (!fromUser) return
                        mMediaPlayer!!.seekTo(progress)
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                        isSeekbarTouch = true
                        playerPause()
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {

                    }

                })

                mMediaPlayer!!.start()

            }
        }

        mMediaPlayer!!.setOnVideoSizeChangedListener(object : MediaPlayer.OnVideoSizeChangedListener{
            override fun onVideoSizeChanged(mp: MediaPlayer?, width: Int, height: Int) {
                val mH = lsq_model_video_seles.measuredHeight
                val mW = lsq_model_video_seles.measuredWidth

                var h = 0
                var w = 0

                if (height >= width) {
                    h = mH
                    w = (mH.toFloat() * width.toFloat() / height).toInt()
                } else {
                    w = mW
                    h = (mW * (height.toFloat() / width)).toInt()
                }

                TLog.e("video size ${width}:${height} view size ${w}:${h}")

                val lp = RelativeLayout.LayoutParams(w, h)
                lp.addRule(RelativeLayout.CENTER_IN_PARENT)
                lsq_model_video_seles.layoutParams = lp




            }

        })

        lsq_model_video_seles.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                mMediaPlayer!!.setSurface(holder.surface)
                mMediaPlayer!!.prepareAsync()
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {

            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {

            }

        })


        val progressTimer = Timer()
        progressTimer.schedule(object : TimerTask(){
            override fun run() {
                runOnUiThread {
                    if (mMediaPlayer!= null && mMediaPlayer!!.isPlaying){
                        lsq_seek.progress = mMediaPlayer!!.currentPosition
                    }
                    if (mMediaPlayer == null){
                        mMediaPlayerPreparedFinish = false
                        progressTimer.cancel()
                    }
                }
            }

        },0,50)


        lsq_model_video_seles.setOnClickListener {
            if (!mMediaPlayerPreparedFinish) return@setOnClickListener

            if (mMediaPlayer!!.isPlaying){
                playerPause()
            } else {
                playerPlaying()
            }
        }

        lsq_player_img.setOnClickListener {
            if (!mMediaPlayerPreparedFinish) return@setOnClickListener

            if (mMediaPlayer!!.isPlaying){
                playerPause()
            } else {
                playerPlaying()
            }
        }

        lsq_next_step.setOnClickListener {
            startActivity<AlbumSelectActivity>("model" to mCurrentModelItem)
            finish()
        }
    }

    private fun playerPlaying(){
        lsq_player_img.visibility = View.GONE
        mMediaPlayer?.start()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        playerPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mMediaPlayer?.pause()
        mMediaPlayer?.stop()
        mMediaPlayer?.release()

        mMediaPlayer = null

        mEvaThreadPool.execute{
            for (item in mEvaModel!!.listReplaceableImageAssets()){
                item.thumbnail.recycle()
            }
            for (item in mEvaModel!!.listReplaceableVideoAssets()){
                item.thumbnail.recycle()
            }

            runOnUiThread {
                lsq_video_display_area.removeAllViews()
            }
        }
    }

}