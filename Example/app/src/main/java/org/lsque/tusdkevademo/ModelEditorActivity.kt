/**
 *  TuSDK
 *  droid-sdk-eva$
 *  org.lsque.tusdkevademo$
 *  @author  H.ys
 *  @Date    2019/7/1$ 13:19$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lsque.tusdkevademo

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import android.media.AudioManager
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Environment.DIRECTORY_DCIM
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.SeekBar
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.tusdk.pulse.Engine
import com.tusdk.pulse.Player
import com.tusdk.pulse.Producer
import com.tusdk.pulse.eva.EvaDirector
import com.tusdk.pulse.eva.EvaModel
import com.tusdk.pulse.eva.EvaReplaceConfig
import com.tusdk.pulse.utils.AssetsMapper
import kotlinx.android.synthetic.main.model_editor_activity.*
import kotlinx.android.synthetic.main.title_item_layout.*
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.textColor
import org.lasque.tusdk.core.TuSdk
import org.lasque.tusdk.core.struct.TuSdkSize
import org.lasque.tusdk.core.utils.AssetsHelper
import org.lasque.tusdk.core.utils.TLog
import org.lasque.tusdk.core.utils.image.BitmapHelper
import org.lsque.tusdkevademo.utils.ProduceOutputUtils
import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.min


class ModelEditorActivity : ScreenAdapterActivity() {

    companion object {
        public const val ALBUM_REQUEST_CODE_IMAGE = 1
        public const val ALBUM_REQUEST_CODE_VIDEO = 2
        public const val AUDIO_REQUEST_CODE = 3
        public const val ALBUM_REQUEST_CODE_ALPHA_VIDEO = 4
    }

    private var mEvaDirector : EvaDirector? = null
    /**  Eva播放器 */
    private var mEvaPlayer: EvaDirector.Player? = null
    /**  Eva Model */
    private var mEvaModel: EvaModel? = null
    /** 是否已经销毁 **/
    private var isRelease = false

    private var mEvaPlayerCurrentState : Player.State? = null

    private var mEvaThreadPool : ExecutorService = Executors.newSingleThreadScheduledExecutor()

    private var mCurrentTs = 0L


    /**  Eva播放器进度监听 */
    private var mPlayerProcessListener: Player.Listener = Player.Listener { state, ts ->
        mEvaPlayerCurrentState = state
        if (state == Player.State.kEOS){
            mEvaPlayer!!.seekTo(0)
            runOnUiThread {
                playerPause()
            }
        } else if (state == Player.State.kPLAYING || state == Player.State.kDO_PREVIEW){
            runOnUiThread {
                mCurrentTs = ts
                lsq_seek.progress = ts.toInt()
//                debug_time.text = "总时长(TS) : ${mEvaPlayer!!.duration} \n" +
//                        "当前视频播放时间(TS) : ${ts} \n"+
//                        "剩余时长 : ${mEvaPlayer!!.duration - ts}"
            }
        }
    }

    /**  可修改项列表 */
    private var mEditorList = LinkedList<EditorModelItem>()

    /**  当前图片修改项 */
    private var mCurrentImageItem: EvaModel.VideoReplaceItem? = null
    /**  当前视频修改项 */
    private var mCurrentVideoItem: EvaModel.VideoReplaceItem? = null
    /**  当前文字修改项 */
    private var mCurrentTextItem: EvaModel.TextReplaceItem? = null

    private var mCurrentAudiItem: EvaModel.AudioReplaceItem? = null

    private var mCurrentImageItems : Array<EvaModel.VideoReplaceItem>? = null
    private var mCurrentVideoItems : Array<EvaModel.VideoReplaceItem>? = null
    private var mCurrentTextItems : Array<EvaModel.TextReplaceItem>? = null
    private var mCurrentAudioItems : Array<EvaModel.AudioReplaceItem>? = null

    private var mDiffMap : HashMap<Any,Boolean> = HashMap()
    private var mConfigMap : HashMap<Any,Any> = HashMap()

    /**  当前修改位置 */
    private var mCurrentEditPostion = 0

    /**  修改项列表Adapter */
    private var mEditorAdapter: ModelEditorAdapter? = null

    private var isEnable = true

    private var isCanChangeAudio = true

    private var mFrameDurationNN = 0L

    private var mTargetProgress = 0L

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data == null) return
        when (requestCode) {
            /**  图片裁剪回调*/
            ALBUM_REQUEST_CODE_IMAGE -> {
                val rectArray = data!!.extras.getFloatArray("zoom")
                mCurrentImageItem!!.resPath = data!!.getStringExtra("imagePath")
                val config = EvaReplaceConfig.ImageOrVideo()
                config.crop = RectF(rectArray[0], rectArray[1], rectArray[2], rectArray[3])
                config.audioMixWeight = 1F
                mEvaThreadPool.execute {
                    mDiffMap[mCurrentImageItem!!.id] = true
                    mConfigMap[mCurrentImageItem!!.id] = config
                    mEvaDirector!!.updateImage(mCurrentImageItem,config)
                }
                mTargetProgress = mCurrentImageItem!!.startTime
            }
            /** 视频裁剪回调 */
            ALBUM_REQUEST_CODE_VIDEO -> {
                when (resultCode) {
                    ALBUM_REQUEST_CODE_VIDEO -> {
                        val rectArray = data!!.getFloatArrayExtra("zoom")
                        mCurrentVideoItem!!.resPath = data!!.getStringExtra("videoPath")
                        val config = EvaReplaceConfig.ImageOrVideo()
                        config.crop = RectF(rectArray[0], rectArray[1], rectArray[2], rectArray[3])
                        config.repeat = 2
                        config.audioMixWeight = 0.5F
                        mEvaThreadPool.execute {
                            mDiffMap[mCurrentVideoItem!!.id] = true
                            mConfigMap[mCurrentVideoItem!!.id] = config
                            if (mCurrentVideoItem!!.isVideo){
                                if (!mEvaDirector!!.updateVideo(mCurrentVideoItem,config)){
                                    TLog.e("updateVideo error")
                                }
                            } else {
                                if (!mEvaDirector!!.updateImage(mCurrentVideoItem,config)){
                                    TLog.e("updateVideo error")
                                }
                            }

                        }
                    }

                    ALBUM_REQUEST_CODE_IMAGE -> {
                        var rectArray = data!!.getFloatArrayExtra("zoom")
                        mCurrentVideoItem!!.resPath = data!!.getStringExtra("imagePath")
                        val config = EvaReplaceConfig.ImageOrVideo()
                        config.crop = RectF(rectArray[0], rectArray[1], rectArray[2], rectArray[3])
                        config.audioMixWeight = 1F
                        mEvaThreadPool.execute {
                            mDiffMap[mCurrentVideoItem!!.id] = true
                            mConfigMap[mCurrentVideoItem!!.id] = config
                            if (mCurrentVideoItem!!.isVideo){
                                if (!mEvaDirector!!.updateVideo(mCurrentVideoItem,config)){
                                    TLog.e("updateVideo error")
                                }
                            } else {
                                if (!mEvaDirector!!.updateImage(mCurrentVideoItem,config)){
                                    TLog.e("updateImage error")
                                }
                            }
                        }
                    }

                    ALBUM_REQUEST_CODE_ALPHA_VIDEO -> {
                        // Mask 坑位正常不应该允许替换
                    }
                }
                mTargetProgress = mCurrentVideoItem!!.startTime
            }
            /** 音频选择回调 */
            AUDIO_REQUEST_CODE -> {
                if (mCurrentAudiItem != null){
                    val audpath = AssetsMapper(this).mapAsset(data!!.extras.getString("audioPath"))
                    mCurrentAudiItem!!.resPath = audpath
                    val config = EvaReplaceConfig.Audio()
                    config.audioMixWeight = 1f
                    config.repeat = 2
                    mEvaThreadPool.execute {
                        mDiffMap[mCurrentAudiItem!!.id] = true
                        mConfigMap[mCurrentAudiItem!!.id] = config
                        mEvaDirector!!.updateAudio(mCurrentAudiItem,config)
                    }
                    mTargetProgress = mCurrentAudiItem!!.startTime
                }
            }
        }
        mEditorAdapter?.notifyItemChanged(mCurrentEditPostion)
        /** 资源替换之后,需重新播放 */
        playerReplay()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.model_editor_activity)
        initView()
//        setListenerToRootView()
        lsq_edit_content.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (lsq_editor_replace_text.visibility == View.VISIBLE) {
                    lsq_text_editor_layout.visibility = View.GONE
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(lsq_editor_replace_text.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
                }
                return false
            }

        })
    }

    private fun playerReplay() {
        mEvaThreadPool.execute {
            mEvaPlayer!!.seekTo(mTargetProgress.toLong())
            playerPlaying()
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private fun initView() {
        lsq_title_item_title.text = "融合预览"
        lsq_back.setOnClickListener {
            mEvaThreadPool.execute {
                mEvaPlayer!!.pause()
                finish()
            }
        }
        val modelItem = intent.getParcelableExtra<ModelItem>("model")

        lsq_model_seles.init(Engine.getInstance().mainGLContext)

        var editorAdapter = ModelEditorAdapter(this, mEditorList,mConfigMap)
        editorAdapter.setOnItemClickListener(object : ModelEditorAdapter.OnItemClickListener {

            override fun onImageItemClick(view: View, item: EvaModel.VideoReplaceItem, position: Int, type: EditType) {
                if (!isEnable) return
                var isOnlyImage = false
                mCurrentEditPostion = position
                when (item.type) {
                    EvaModel.AssetType.kIMAGE_ONLY -> isOnlyImage = true
                    EvaModel.AssetType.kIMAGE_VIDEO -> isOnlyImage = false
                }
                val videoDuration = (item.endTime - item.startTime) * 1000
                mCurrentImageItem = item
                startActivityForResult<AlbumActivity>(ALBUM_REQUEST_CODE_IMAGE, "videoDuration" to videoDuration, "onlyImage" to isOnlyImage, "onlyVideo" to false, "width" to item.width, "height" to item.height)
            }

            override fun onVideoItemClick(view: View, item: EvaModel.VideoReplaceItem, position: Int, type: EditType) {
                if (!isEnable) return
                var isOnlyVideo = false
                var isOnlyImage = false
                mCurrentEditPostion = position
                when (item.type) {
                    EvaModel.AssetType.kIMAGE_VIDEO -> isOnlyVideo = false
                    EvaModel.AssetType.kVIDEO_ONLY -> isOnlyVideo = true
                    EvaModel.AssetType.kIMAGE_ONLY-> isOnlyImage = true
                }
                val videoDuration = (item.endTime - item.startTime) * 1000f
                mCurrentVideoItem = item
                startActivityForResult<AlbumActivity>(ALBUM_REQUEST_CODE_VIDEO, "videoDuration" to videoDuration, "onlyImage" to isOnlyImage, "onlyVideo" to isOnlyVideo, "width" to item.width, "height" to item.height , "isAlpha" to (item.type == EvaModel.AssetType.kMASK))
            }

            override fun onTextItemClick(view: View, item: EvaModel.TextReplaceItem, position: Int, type: EditType) {
                if (!isEnable) return
                mCurrentTextItem = item
                lsq_editor_replace_text.setText(item.text)
                lsq_text_editor_layout.visibility = View.VISIBLE
                lsq_editor_replace_text.requestFocus()
                mCurrentEditPostion = position
                val inputManager: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputManager.showSoftInput(lsq_editor_replace_text, 0)
            }

        })
        mEditorAdapter = editorAdapter
        var linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        lsq_editor_item_list.layoutManager = linearLayoutManager
        lsq_editor_item_list.adapter = mEditorAdapter
        lsq_seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                /** seek 到指定位置 播放器内范围(视频时长 ms) */
                mEvaThreadPool.execute {
                    mEvaPlayer!!.previewFrame(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                playerPause()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
//                playerPlaying()
            }

        })
        lsq_model_seles.setOnClickListener {
            if (mEvaPlayerCurrentState == null) return@setOnClickListener
            if (mEvaPlayerCurrentState != Player.State.kPLAYING) {
                lsq_player_img.visibility = View.GONE;
                mEvaThreadPool.execute {
                    mEvaPlayer!!.play()
                }
            } else {
                lsq_player_img.visibility = View.VISIBLE
                mEvaThreadPool.execute {
                    mEvaPlayer!!.pause()
                }
            }
        }
        lsq_player_img.setOnClickListener {
            if (mEvaPlayerCurrentState == null) return@setOnClickListener
            if (mEvaPlayerCurrentState == Player.State.kEOS){
                mEvaThreadPool.execute {
                    mEvaPlayer!!.seekTo(0)
                    playerPlaying()
                }
            } else if (mEvaPlayerCurrentState != Player.State.kPLAYING) {
                playerPlaying()
            } else {
                playerPause()
            }
        }
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val volume = audioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC)
        lsq_voice_seek.progress = (volume * 10)
        lsq_voice_seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
//                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,progress / 10,AudioManager.FLAG_PLAY_SOUND)
                mEvaDirector!!.updateAudioMixWeightSeparate("", (progress / 10.0).toDouble())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })
        lsq_editor_change_bgm.setOnClickListener {
            if (!isCanChangeAudio) {
                Toast.makeText(this, "此资源不支持替换背景音乐", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            mCurrentAudiItem = mCurrentAudioItems!![0]
            startActivityForResult<AudioListActivity>(AUDIO_REQUEST_CODE)
            this.overridePendingTransition(R.anim.activity_open_from_bottom_to_top, R.anim.activity_keep_status)
        }

        lsq_reset_assets.setOnClickListener {
            if (mDiffMap.isEmpty()) return@setOnClickListener
            mEvaThreadPool.execute {
                val b = mEvaPlayer!!.pause()
                mEvaPlayer!!.previewFrame(0)
                runOnUiThread {
                    lsq_player_img.visibility = View.VISIBLE
                    lsq_seek.progress = 0
                }
                setEditorList(mEvaModel!!.listReplaceableImageAssets(),mEvaModel!!.listReplaceableVideoAssets(),mEvaModel!!.listReplaceableTextAssets())
                mCurrentAudioItems = mEvaModel!!.listReplaceableAudioAssets()
                mCurrentImageItem = null
                mCurrentVideoItem = null
                mCurrentTextItem = null
                mCurrentAudiItem = null
                for (item in mCurrentImageItems!!){
                    if (mDiffMap[item.id] != null){
                        if (item.isVideo){
                            mEvaDirector!!.updateVideo(item,EvaReplaceConfig.ImageOrVideo())
                        } else {
                            mEvaDirector!!.updateImage(item,EvaReplaceConfig.ImageOrVideo())
                        }
                    }
                }
                for (item in mCurrentVideoItems!!){
                    if (mDiffMap[item.id] != null){
                        if (item.isVideo){
                            mEvaDirector!!.updateVideo(item,mConfigMap[item.id] as EvaReplaceConfig.ImageOrVideo)
                        } else {
                            mEvaDirector!!.updateImage(item,mConfigMap[item.id] as EvaReplaceConfig.ImageOrVideo)
                        }
                    }
                }
                for (item in mCurrentTextItems!!){
                    if (mDiffMap[item.id] != null)
                        mEvaDirector!!.updateText(item)
                }
                for (item in mCurrentAudioItems!!){
                    if (mDiffMap[item.id] != null){
                        val config = mConfigMap[item.id] as EvaReplaceConfig.Audio
                        mEvaDirector!!.updateAudio(item,config)
                    }
                }
                mDiffMap.clear()
                mConfigMap.clear()

                runOnUiThread {
                    editorAdapter.notifyDataSetChanged()
                }
                mEvaPlayer!!.previewFrame(0)
            }
            //todo 坑位重置

        }

        lsq_editor_text_commit.setOnClickListener {
            mCurrentTextItem!!.text = lsq_editor_replace_text.text.toString()
            mEvaThreadPool.execute {
                mDiffMap[mCurrentTextItem!!] = true
                mEvaDirector!!.updateText(mCurrentTextItem)
            }
            lsq_text_editor_layout.visibility = View.GONE
            editorAdapter.notifyItemChanged(mCurrentEditPostion)
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(lsq_editor_replace_text.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
            playerReplay()
        }

        lsq_next.text = "保存"
        lsq_next.textColor = Color.parseColor("#007aff")

        lsq_next.setOnClickListener {
            lsq_editor_cut_load.visibility = View.VISIBLE
            lsq_editor_cut_load_parogress.setValue(0f)
            setEnable(false)
            /** 保存时必须把播放停止 */
            mEvaPlayer!!.pause()
            lsq_player_img.visibility = View.VISIBLE

            mEvaThreadPool.execute {
                val producer = mEvaDirector!!.newProducer()

                val outputFilePath = "${Environment.getExternalStoragePublicDirectory(DIRECTORY_DCIM).path}/eva_output${System.currentTimeMillis()}.mp4"

                val config : Producer.OutputConfig = Producer.OutputConfig()
                config.watermarkPosition = 1
                config.watermark = BitmapHelper.getRawBitmap(applicationContext,R.raw.sample_watermark)
                val supportSize = ProduceOutputUtils.getSupportSize(MediaFormat.MIMETYPE_VIDEO_AVC)
                val outputSize = TuSdkSize.create(mEvaModel!!.width,mEvaModel!!.height)
                if (supportSize.maxSide() < outputSize.maxSide()){
                    config.scale = supportSize.maxSide().toDouble() / outputSize.maxSide().toDouble()
                }
                producer.setOutputConfig(config)

                producer.setListener { state, ts ->
                    if (state == Producer.State.kEND){
                        mEvaThreadPool.execute {
                            mEvaDirector?.producer?.release()
                            mEvaDirector!!.resetProducer()
                            mEvaPlayer!!.seekTo(mCurrentTs)
                            TLog.e("ts : $mCurrentTs")
                        }
                        sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(File(outputFilePath))))
                        runOnUiThread {
                            setEnable(true)
                            lsq_editor_cut_load.setVisibility(View.GONE)
                            lsq_editor_cut_load_parogress.setValue(0f)
                            Toast.makeText(applicationContext, "保存成功", Toast.LENGTH_SHORT).show()
                        }
                    } else if (state == Producer.State.kWRITING){
                        runOnUiThread {
                            lsq_editor_cut_load.setVisibility(View.VISIBLE)
                            lsq_editor_cut_load_parogress.setValue((ts / producer.duration.toFloat()) * 100f)
                        }
                    }
                }

                if (!producer.init(outputFilePath)){
                    TLog.e("producer init error")
                    return@execute
                }

                if (!producer.start()){
                    TLog.e("[Error] EvaProducer Start failed")
                }
            }


        }

        mEvaThreadPool.execute {
            mEvaDirector = EvaDirector()

            mEvaModel = EvaModel()
            if (AssetsHelper.hasAssets(this,modelItem.templateName)){
                mEvaModel!!.createFromAsset(this,modelItem.templateName)
            } else {
                mEvaModel!!.create(modelItem.modelDownloadFilePath)
            }
            val ret = mEvaDirector!!.open(mEvaModel)
            if (!ret){
                mEvaModel!!.debugDump()
                TLog.e("[Error] EvaPlayer Open Failed")
                //todo 失败提醒
                return@execute
            }

            mEvaDirector!!.updateAudioMixWeight(1.0)

            mEvaPlayer = mEvaDirector!!.newPlayer()
            mEvaPlayer!!.setListener(mPlayerProcessListener)
            if (!mEvaPlayer!!.open()){
                return@execute
            }
            val seekMax = mEvaPlayer!!.duration.toInt()

            setEditorList(mEvaModel!!.listReplaceableImageAssets(),mEvaModel!!.listReplaceableVideoAssets(),mEvaModel!!.listReplaceableTextAssets())
            runOnUiThread {
                var metrics = DisplayMetrics()
                windowManager.defaultDisplay.getRealMetrics(metrics)
                val displayArea = Rect()
                lsq_model_seles.getGlobalVisibleRect(displayArea)
                val videoHeight = mEvaModel!!.height.toFloat()
                val videoWidth = mEvaModel!!.width.toFloat()
                val hwp = videoWidth / videoHeight
                if (videoHeight < lsq_model_seles.layoutParams.height) {
                    lsq_seek.layoutParams.width = min(metrics.widthPixels, videoWidth.toInt())
                } else {
                    if (videoWidth > metrics.widthPixels) {
                        lsq_seek.layoutParams.width = (metrics.widthPixels * 0.5).toInt()
                    } else {
                        lsq_seek.layoutParams.width = (displayArea.height() * hwp).toInt()
                    }
                }
                lsq_seek.visibility = View.VISIBLE
            }
            lsq_model_seles.attachPlayer(mEvaPlayer)
            mCurrentAudioItems = mEvaModel!!.listReplaceableAudioAssets()
            if (mCurrentAudioItems.isNullOrEmpty()){
                isCanChangeAudio = false
            }

            mEvaPlayer!!.play()
            mEvaPlayer!!.pause()
            mEvaPlayer!!.previewFrame(1)
            runOnUiThread {
                lsq_seek.max = seekMax
            }


        }

        TuSdk.messageHub().setStatus(this, R.string.lsq_assets_loading)
    }

    private fun setEditorList(replaceImageList: Array<EvaModel.VideoReplaceItem> , replaceVideoList: Array<EvaModel.VideoReplaceItem>, replaceTextList: Array<EvaModel.TextReplaceItem>) {
        mEditorList.clear()
        mCurrentImageItems = replaceImageList
        mCurrentVideoItems = replaceVideoList
        mCurrentTextItems = replaceTextList
        val editorList = ArrayList<Any>()
        /**  遍历可替换图片项列表 */
        for (compari in replaceImageList) {
            editorList.add(compari)
        }
        /**  遍历可替换视频项列表 */
        for (compari in replaceVideoList) {
            editorList.add(compari)
        }
        /**  遍历可替换文字项列表 */
        for (compari in replaceTextList) {
            editorList.add(compari)
        }
        for (compari in editorList) {
            if (compari is EvaModel.VideoReplaceItem) {
                if (compari.isVideo)
                    mEditorList.add(EditorModelItem(compari, EditType.Image,compari.startTime))
                else
                    mEditorList.add(EditorModelItem(compari,EditType.Video,compari.startTime))
            }  else if (compari is EvaModel.TextReplaceItem) {
                mEditorList.add(EditorModelItem(compari, EditType.Text,compari.startTime))
            }
        }
        mEditorList.sortBy {
            it.startPos
        }
        runOnUiThread {
            mEditorAdapter!!.setEditorModelList(mEditorList)
        }
    }

    private fun setEnable(b: Boolean) {
        lsq_seek.isEnabled = b
        lsq_editor_item_list.isEnabled = b
        lsq_editor_change_bgm.isEnabled = b
        lsq_next.isEnabled = b
        lsq_back.isEnabled = b
        lsq_model_seles.isEnabled = b
        lsq_player_img.isEnabled = b
        isEnable = b
    }

    override fun onResume() {
        super.onResume()
        mEvaThreadPool.execute {
            mEvaPlayer!!.pause()
        }
        lsq_player_img.visibility = View.VISIBLE
    }

    override fun onPause() {
        super.onPause()
        mEvaThreadPool.execute {
            mEvaPlayer!!.pause()
        }
        lsq_player_img.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        mEvaThreadPool.execute {
            if(!isRelease) {
                mEvaPlayer!!.close()
                mEvaDirector!!.resetPlayer()
                mEvaDirector!!.close()
            }
            runOnUiThread {
                lsq_model_seles.release()
                lsq_edit_content.removeView(lsq_model_seles)
            }

            for (item in mCurrentVideoItems!!){
                item.thumbnail.recycle()
            }
            for (item in mCurrentImageItems!!){
                item.thumbnail.recycle()
            }
        }
        mEvaThreadPool.shutdown()
        TuSdk.getAppTempPath().deleteOnExit()
    }

    private fun playerPause() {
        mEvaThreadPool.execute {
            if (mEvaPlayerCurrentState == Player.State.kPLAYING)
                mEvaPlayer!!.pause()
        }
        runOnUiThread {
            lsq_player_img.visibility = View.VISIBLE
        }
    }

    private fun playerPlaying() {
        mEvaThreadPool.execute {
            mEvaPlayer!!.play()
        }
        runOnUiThread {
            lsq_player_img.visibility = View.GONE
        }
    }

    override fun onBackPressed() {
        if (mEvaDirector?.producer != null){
            mEvaThreadPool.execute{
                mEvaDirector?.producer?.cancel()
            }
        } else {
            super.onBackPressed()
        }
    }
}