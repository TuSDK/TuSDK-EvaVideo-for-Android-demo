/**
 *  TuSDK
 *  droid-sdk-eva$
 *  org.lsque.tusdkevademo$
 *  @author  H.ys
 *  @Date    2019/7/1$ 11:58$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lsque.tusdkevademo

import android.annotation.TargetApi
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.widget.SeekBar
import androidx.constraintlayout.widget.ConstraintLayout
import com.tusdk.pulse.Engine
import com.tusdk.pulse.Player
import com.tusdk.pulse.eva.EvaDirector
import com.tusdk.pulse.eva.EvaModel
import kotlinx.android.synthetic.main.include_seek_bar.*
import kotlinx.android.synthetic.main.model_detail_activity.*
import kotlinx.android.synthetic.main.model_detail_activity.lsq_editor_cut_load_parogress
import kotlinx.android.synthetic.main.model_detail_activity.lsq_model_name
import kotlinx.android.synthetic.main.model_detail_activity.lsq_model_seles
import kotlinx.android.synthetic.main.model_detail_activity.lsq_player_img
import kotlinx.android.synthetic.main.title_item_layout.*
import org.jetbrains.anko.startActivity
import org.lasque.tusdkpulse.core.utils.AssetsHelper
import org.lasque.tusdkpulse.core.utils.TLog
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.min


class ModelDetailActivity : ScreenAdapterActivity() {

    private var mEvaPlayer:EvaDirector.Player? = null
    private var mEvaModel: EvaModel? = null
    private var mEvaDirector : EvaDirector? = null
    private var isRelease = false
    private var mEvaPlayerCurrentState : Player.State? = null
    private var isSeekbarTouch = false
    private var mPlayerProcessListener: Player.Listener = Player.Listener { state, ts ->
        mEvaPlayerCurrentState = state
        if (state == Player.State.kPLAYING || state == Player.State.kDO_PREVIEW){
            if (!isSeekbarTouch){
                lsq_seek.progress = ts.toInt()
            }
            val currentVideoHour = ts /3600000
            val currentVideoMinute = (ts % 3600000) /60000

            val currentVideoSecond = (ts %60000 /1000)

            val durationMS = mEvaPlayer!!.duration
            val durationVideoHour = durationMS /3600000

            val durationVideoMinute = (durationMS %3600000) /60000

            val durationVideoSecond = (durationMS %3600000 / 1000)

            runOnUiThread {
                lsq_video_playing_time.text = "$currentVideoHour:$currentVideoMinute:$currentVideoSecond/$durationVideoHour:$durationVideoMinute:$durationVideoSecond"
            }
        }

        if (state == Player.State.kEOS){
            runOnUiThread {
                playerPause()
            }
            mEvaThreadPool.execute {
                mEvaPlayer!!.seekTo(0)
            }
        }
    }

    private var mEvaThreadPool : ExecutorService = Executors.newSingleThreadScheduledExecutor()

    private fun playerPause() {
        lsq_player_img.visibility = View.VISIBLE
        mEvaThreadPool.execute {
            if (mEvaPlayerCurrentState == Player.State.kPLAYING)
                mEvaPlayer!!.pause()
        }
    }

    private var isFirst = true

    private var mCurrentModelItem: ModelItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.model_detail_activity)
        initView()
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private fun initView() {
        lsq_title_item_title.text = "模板详情"
        lsq_back.setOnClickListener { finish() }
        val modelItem = intent.getParcelableExtra<ModelItem>("model")
        mCurrentModelItem = modelItem
        lsq_model_seles.init(Engine.getInstance().mainGLContext)

        mEvaThreadPool.execute {
            mEvaDirector = EvaDirector()

            mEvaModel = EvaModel()
            if (AssetsHelper.hasAssets(this,modelItem.templateName)){
               if (mEvaModel!!.createFromAsset(this,modelItem.templateName)){
                   TLog.e("[Error] Assets File not fount")
               }
            } else {
               if (mEvaModel!!.create(modelItem.modelDownloadFilePath)){
                   TLog.e("[Error]File not fount")

               }
            }

            val ret = mEvaDirector!!.open(mEvaModel)
            if (!ret){
                mEvaModel!!.debugDump()
                TLog.e("[Error] EvaPlayer Open Failed")
                //todo 失败提醒
                return@execute
            }

            mEvaPlayer = mEvaDirector!!.newPlayer()
            mEvaPlayer!!.setListener(mPlayerProcessListener)
            if (!mEvaPlayer!!.open()){
                return@execute
            }
            lsq_model_seles.attachPlayer(mEvaPlayer)

            mEvaPlayer!!.play()
            var seekMax = mEvaPlayer!!.duration
            var textCount = mEvaModel!!.listReplaceableTextAssets().size
            var imageVideoCount = mEvaModel!!.listReplaceableImageAssets().size + mEvaModel!!.listReplaceableVideoAssets().size
            var audioCount = mEvaModel!!.listReplaceableAudioAssets().size

            runOnUiThread {
                lsq_seek.max = seekMax.toInt()
                lsq_model_replace_info.text =
                        "文字 ${textCount}段\n" +
                                "图片/视频 ${imageVideoCount}个\n" +
                                "音频 ${audioCount}个"
            }

        }
        lsq_model_name.text = modelItem.modelName
        lsq_seek.progress = 0
        lsq_seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                mEvaThreadPool.execute {
                    mEvaPlayer!!.previewFrame(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isSeekbarTouch = true
                playerPause()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isSeekbarTouch = false
//                playerPlaying()
            }

        })

        lsq_model_seles.setOnClickListener {
            if (mEvaPlayerCurrentState == null) return@setOnClickListener
            when (mEvaPlayerCurrentState) {
                Player.State.kEOS -> {
                    mEvaThreadPool.execute {
                        mEvaPlayer!!.seekTo(0)
                        mEvaPlayer!!.play()
                    }
                }
                Player.State.kPLAYING -> {
                    playerPause()
                }
                else -> {
                    playerPlaying()
                }
            }
        }
        lsq_player_img.setOnClickListener {
            if (mEvaPlayerCurrentState == null) return@setOnClickListener
            if (mEvaPlayerCurrentState == Player.State.kPLAYING) {
                playerPause()
            } else {
                playerPlaying()
            }
        }

        lsq_next_step.setOnClickListener {
            mEvaThreadPool.execute {
                mEvaPlayer!!.pause()
            }
            startActivity<ModelEditorActivity>("model" to modelItem)
            finish()
        }
        lsq_next.visibility = View.GONE

        lsq_video_to_first_frame.setOnClickListener {
            mEvaThreadPool.execute {
                mEvaPlayer!!.pause()
                mEvaPlayer?.seekTo(0)
                mEvaPlayer!!.play()

            }
        }

        lsq_video_to_last_frame.setOnClickListener {
            mEvaThreadPool.execute {
                mEvaPlayer!!.pause()
                mEvaPlayer?.seekTo(mEvaPlayer!!.duration)
                mEvaPlayer!!.play()
            }
        }

    }

    private fun playerPlaying() {
        lsq_player_img.visibility = View.GONE
        mEvaThreadPool.execute {
            mEvaPlayer!!.play()
        }
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
        mEvaThreadPool.execute {
            if(!isRelease) {
                mEvaPlayer!!.close()
                mEvaDirector!!.close()
            }

            for (item in mEvaModel!!.listReplaceableImageAssets()){
                item.thumbnail.recycle()
            }
            for (item in mEvaModel!!.listReplaceableVideoAssets()){
                item.thumbnail.recycle()
            }
            runOnUiThread {
                lsq_model_seles.release()
                lsq_video_display_area.removeAllViews()
            }
        }
        mEvaThreadPool.shutdown()
    }

}