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
import android.media.TimedText
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.TimeUtils
import android.view.View
import android.widget.SeekBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import kotlinx.android.synthetic.main.include_seek_bar.*
import kotlinx.android.synthetic.main.model_detail_activity.*
import kotlinx.android.synthetic.main.model_detail_activity.lsq_editor_cut_load_parogress
import kotlinx.android.synthetic.main.model_detail_activity.lsq_model_name
import kotlinx.android.synthetic.main.model_detail_activity.lsq_model_seles
import kotlinx.android.synthetic.main.model_detail_activity.lsq_player_img
import kotlinx.android.synthetic.main.title_item_layout.*
import org.jetbrains.anko.startActivity
import org.lasque.tusdk.core.TuSdk
import org.lasque.tusdk.core.TuSdkContext
import org.lasque.tusdk.core.seles.output.SelesView
import org.lasque.tusdk.core.utils.AssetsHelper
import org.lasque.tusdk.core.utils.TLog
import org.lasque.tusdk.core.utils.ThreadHelper
import org.lasque.tusdk.eva.TuSdkEvaAssetManager
import org.lasque.tusdk.eva.TuSdkEvaPlayerImpl
import kotlin.math.min


class ModelDetailActivity : ScreenAdapterActivity() {

    private var mEvaPlayer:TuSdkEvaPlayerImpl? = null
    private var mEvaAssetManager: TuSdkEvaAssetManager? = null
    private var isRelease = false
    private var mPlayerProcessListener: TuSdkEvaPlayerImpl.TuSdkEvaPlayerProgressListener = TuSdkEvaPlayerImpl.TuSdkEvaPlayerProgressListener {
        progress, currentTimeNN, durationNN ->

        lsq_seek.progress = (progress * 100).toInt()
        val currentVideoHour = currentTimeNN /3600000000000
        val currentVideoMinute = (currentTimeNN % 3600000000000) /60000000000

        val currentVideoSecond = (currentTimeNN %60000000000 /1000000000)

        val durationVideoHour = durationNN /3600000000000

        val durationVideoMinute = (durationNN %3600000000000) /60000000000

        val durationVideoSecond = (durationNN %60000000000 / 1000000000)

        lsq_video_playing_time.post {
            lsq_video_playing_time.text = "$currentVideoHour:$currentVideoMinute:$currentVideoSecond/$durationVideoHour:$durationVideoMinute:$durationVideoSecond"
        }

        if (currentTimeNN == durationNN){
            ThreadHelper.post({
                playerPause()
            })
        }
    }

    private fun playerPause() {
        mEvaPlayer!!.pause()
        lsq_player_img.visibility = View.VISIBLE
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

        mEvaPlayer = TuSdkEvaPlayerImpl(
                if(AssetsHelper.getAssetPath(TuSdkContext.context(),modelItem.templateName) == null){modelItem.modelDownloadFilePath} else {AssetsHelper.getAssetPath(TuSdkContext.context(),modelItem.templateName)})
        /** 资源加载进度回调 */
        mEvaPlayer!!.assetManager.setAssetLoadCallback(object : TuSdkEvaAssetManager.TuSdkEvaAssetLoadCallback {

            override fun onLoadProgerssChanged(progress: Float) {
                ThreadHelper.post {
                    lsq_editor_cut_load_parogress.setVisibility(View.VISIBLE)
                    lsq_editor_cut_load_parogress.setValue(progress * 100)
                }
            }

            override fun onLoaded() {
                ThreadHelper.post {
                    lsq_editor_cut_load_parogress.setVisibility(View.GONE)
                    lsq_editor_cut_load_parogress.setValue(0f)
                }
            }
            override fun onPrepareLoad() {
                ThreadHelper.post {
                    TuSdk.messageHub().dismiss()
                    mEvaAssetManager = mEvaPlayer!!.assetManager
                    lsq_model_replace_info.text =
                            "文字 ${mEvaPlayer!!.assetManager.replaceTextList.size()}段\n" +
                            "图片/视频 ${mEvaPlayer!!.assetManager.replaceImageList.size() + mEvaPlayer!!.assetManager.replaceVideoList.size()}个\n" +
                            "音频 ${mEvaPlayer!!.assetManager.replaceAudioList.size()}个"
                    var metrics = DisplayMetrics()
                    windowManager.defaultDisplay.getRealMetrics(metrics)
                    val displayArea = Rect()
                    lsq_model_seles.getGlobalVisibleRect(displayArea)
                    val videoHeight = mEvaPlayer!!.assetManager.inputSize.height.toFloat()
                    val videoWidth = mEvaPlayer!!.assetManager.inputSize.width.toFloat()
                    val whp = videoHeight / videoWidth
                    val hwp = videoWidth/videoHeight
                    if (videoWidth > videoHeight) {
                        (lsq_model_seles.layoutParams as ConstraintLayout.LayoutParams).height = (metrics.widthPixels * whp).toInt()
                    } else {
                        if (videoHeight < lsq_model_seles.layoutParams.height) {
                            lsq_model_seles.layoutParams.height = videoHeight.toInt()
                            lsq_model_seles.layoutParams.width = min(metrics.widthPixels, videoWidth.toInt())
                        } else {
                            if (videoWidth > metrics.widthPixels) {
                                lsq_model_seles.layoutParams.height = (metrics.widthPixels * whp * 0.5).toInt()
                                lsq_model_seles.layoutParams.width = (metrics.widthPixels * 0.5).toInt()
                            } else {
                                lsq_model_seles.layoutParams.width = (displayArea.height() * hwp).toInt()
                            }
                        }
                    }
                    (lsq_model_seles.layoutParams as ConstraintLayout.LayoutParams).topToBottom = R.id.lsq_title
                    mEvaPlayer!!.setDisplayContent(lsq_model_seles)
                    lsq_seek.visibility = View.VISIBLE
                    mEvaPlayer!!.selesView.fillMode = SelesView.SelesFillModeType.PreserveAspectRatioAndFill
                    mEvaPlayer!!.play()
                }
            }

        })
        mEvaPlayer!!.load()
        lsq_model_name.text = modelItem.modelName
        lsq_seek.progress = 0
        lsq_seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                mEvaPlayer!!.seek(progress.toFloat() / 100f)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                playerPause()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                playerPlaying()
            }

        })

        lsq_model_seles.setOnClickListener {
            if (mEvaPlayer!!.isPause) {
                lsq_player_img.visibility = View.GONE; mEvaPlayer!!.play()
            } else {
                lsq_player_img.visibility = View.VISIBLE; mEvaPlayer!!.pause()
            }
        }
        lsq_player_img.setOnClickListener {
            if (mEvaPlayer!!.isPause) {
                playerPlaying()
            } else {
                playerPause()
            }
        }

        lsq_next_step.setOnClickListener {
            if(!isRelease) {
                mEvaPlayer!!.release()
                isRelease = true
            }
            startActivity<ModelEditorActivity>("model" to modelItem)
            finish()
        }
        lsq_next.visibility = View.GONE

        lsq_video_to_first_frame.setOnClickListener {
            mEvaPlayer?.seek(0f)
        }

        lsq_video_to_last_frame.setOnClickListener {
            mEvaPlayer?.seek(1f)
        }

        TuSdk.messageHub().setStatus(this@ModelDetailActivity,R.string.lsq_assets_loading)
    }

    private fun playerPlaying() {
        lsq_player_img.visibility = View.GONE
        mEvaPlayer!!.play()
    }

    override fun onResume() {
        super.onResume()
        mEvaPlayer!!.setProgressListener(mPlayerProcessListener)
        if (!mEvaPlayer!!.isPause && !isFirst) {
            playerPlaying()
        }
    }

    override fun onPause() {
        super.onPause()
        playerPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        if(!isRelease) mEvaPlayer!!.release()
    }

}