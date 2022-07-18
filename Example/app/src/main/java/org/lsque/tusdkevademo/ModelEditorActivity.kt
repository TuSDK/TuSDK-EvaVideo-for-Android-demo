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
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Environment.DIRECTORY_DCIM
import android.os.IBinder
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.PopupWindow
import android.widget.SeekBar
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.tusdk.pulse.Engine
import com.tusdk.pulse.MediaInspector
import com.tusdk.pulse.Player
import com.tusdk.pulse.Producer
import com.tusdk.pulse.eva.EvaDirector
import com.tusdk.pulse.eva.EvaModel
import com.tusdk.pulse.eva.EvaReplaceConfig
import com.tusdk.pulse.utils.AssetsMapper
import kotlinx.android.synthetic.main.model_editor_activity.*
import kotlinx.android.synthetic.main.popup_edit_layout.view.*
import kotlinx.android.synthetic.main.title_item_layout.*
import org.jetbrains.anko.*
import org.lasque.tusdkpulse.core.TuSdk
import org.lasque.tusdkpulse.core.TuSdkContext
import org.lasque.tusdkpulse.core.struct.TuSdkSize
import org.lasque.tusdkpulse.core.utils.AssetsHelper
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkpulse.core.utils.ThreadHelper
import org.lasque.tusdkpulse.core.utils.image.BitmapHelper
import org.lsque.tusdkevademo.server.EVARenderServer
import org.lsque.tusdkevademo.utils.ProduceOutputUtils
import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.max
import kotlin.math.min


class ModelEditorActivity : ScreenAdapterActivity() {

    companion object {
        public const val ALBUM_REQUEST_CODE_IMAGE = 1
        public const val ALBUM_REQUEST_CODE_VIDEO = 2
        public const val AUDIO_REQUEST_CODE = 3
        public const val ALBUM_REQUEST_CODE_ALPHA_VIDEO = 4
    }

    private var mEvaDirector: EvaDirector? = null

    /**  Eva播放器 */
    private var mEvaPlayer: EvaDirector.Player? = null

    /**  Eva Model */
    private var mEvaModel: EvaModel? = null

    /** 是否已经销毁 **/
    private var isRelease = false

    private var mEvaPlayerCurrentState: Player.State? = null

    private var mEvaThreadPool: ExecutorService = Executors.newSingleThreadScheduledExecutor()

    private var mCurrentTs = 0L

    private var mCurrentFrameRate = 0

    private var mDurationFrames = 0

    private var mCurrentFrame = 0;

    private var mCurrentLine: LineDataSet? = null

    private var mCurrentItemsPos = -1

    private var mNextItemStartPos = 0L


    /**  Eva播放器进度监听 */
    private var mPlayerProcessListener: Player.Listener = Player.Listener { state, ts ->
        mEvaPlayerCurrentState = state
        if (state == Player.State.kEOS) {
            mEvaPlayer!!.pause()
            mEvaPlayer!!.seekTo(0)
            runOnUiThread {
                playerPause()
            }

            mCurrentItemsPos = -1
            mNextItemStartPos = 0

        } else if (state == Player.State.kPLAYING || state == Player.State.kDO_PREVIEW) {

            runOnUiThread {
                mCurrentTs = ts
                lsq_seek.progress = ts.toInt()
//                debug_time.text = "总时长(TS) : ${mEvaPlayer!!.duration} \n" +
//                        "当前视频播放时间(TS) : ${ts} \n"+cv
//                        "剩余时长 : ${mEvaPlayer!!.duration - ts}"


                if (ts > mNextItemStartPos){
                    mCurrentItemsPos = min(mCurrentItemsPos + 1,mEditorList.size - 1);

                    val prePos = mNextItemStartPos
                    mNextItemStartPos = mEditorList[mCurrentItemsPos].startPos;

                    TLog.e("current item pos %s",mCurrentItemsPos)

                    val delay = if (mNextItemStartPos - prePos < 50){100L} else {500L}

                    TLog.e("current delay %s",delay)

                    val targetPos = mCurrentItemsPos

                    ThreadHelper.postDelayed({
                        mEditorAdapter?.setHighLightPos(targetPos)
                        lsq_editor_item_list.smoothScrollToPosition(targetPos)
                    },delay)
                }
            }
        }

        if (state == Player.State.kDO_SEEK || state == Player.State.kDO_PREVIEW) {
            mCurrentFrame = (ts.toFloat() * mCurrentFrameRate / 1000f).toInt()

            mCurrentFrame = min(mCurrentFrame, mDurationFrames)

            TLog.e("current frame ${mCurrentFrame} max frame ${mDurationFrames} current ts ${ts} framerate ${mCurrentFrameRate} duration ${mEvaPlayer!!.duration}")

            for (i in 0 until mEditorList.size){
                if (ts > mEditorList[i].startPos){
                    mCurrentItemsPos = i
                    mNextItemStartPos = mEditorList[min(mEditorList.size - 1,i + 1)].startPos

                    runOnUiThread {
                        mEditorAdapter?.setHighLightPos(mCurrentItemsPos)
                        lsq_editor_item_list.smoothScrollToPosition(mCurrentItemsPos)
                    }
                    break;
                }
            }


        }

        if (state == Player.State.kDO_PLAY){
            runOnUiThread {
                mEditorAdapter?.setCurrentClickPos(-1)
            }
        }
    }

    /**  可修改项列表 */
    private var mEditorList = LinkedList<EditorModelItem>()

    private var mSavedMap = HashMap<String, String>()

    /**  当前图片修改项 */
    private var mCurrentImageItem: EvaModel.VideoReplaceItem? = null

    /**  当前视频修改项 */
    private var mCurrentVideoItem: EvaModel.VideoReplaceItem? = null

    /**  当前文字修改项 */
    private var mCurrentTextItem: EvaModel.TextReplaceItem? = null

    private var mCurrentAudiItem: EvaModel.AudioReplaceItem? = null

    private var mCurrentImageItems: Array<EvaModel.VideoReplaceItem>? = null
    private var mCurrentVideoItems: Array<EvaModel.VideoReplaceItem>? = null
    private var mCurrentTextItems: Array<EvaModel.TextReplaceItem>? = null
    private var mCurrentAudioItems: Array<EvaModel.AudioReplaceItem>? = null

    private var mDiffMap: HashMap<Any, Boolean> = HashMap()
    private var mConfigMap: HashMap<Any, Any> = HashMap()

    /**  当前修改位置 */
    private var mCurrentEditPostion = 0

    /**  修改项列表Adapter */
    private var mEditorAdapter: ModelEditorAdapter? = null

    private var isEnable = true

    private var isCanChangeAudio = true

    private var mFrameDurationNN = 0L

    private var mTargetProgress = 0L

    private val mServiceLock = Semaphore(0)

    private var mIEvaRenderServer: IEVARenderServer? = null

    private val mEVAServerRunnable: Runnable = Runnable {
        if (mIEvaRenderServer == null) return@Runnable
        val evaRenderServer = mIEvaRenderServer!!

        val modelItem = getIntent().getParcelableExtra<ModelItem>("model")!!
        var taskId = ""
        if (AssetsHelper.hasAssets(applicationContext, modelItem.templateName)) {
            taskId = evaRenderServer.initRenderTask(modelItem.templateName, true)
        } else {
            taskId = evaRenderServer.initRenderTask(modelItem.modelDownloadFilePath, false)
        }

        TLog.e("current render task id : ${taskId}")
        if (TextUtils.isEmpty(taskId)) {
            runOnUiThread {
                longToast("当前已经有渲染任务在进行了,请等待当前任务结束后再试")
            }
            return@Runnable
        }

        val outputFilePath =
            "${Environment.getExternalStoragePublicDirectory(DIRECTORY_DCIM).path}/eva_output${System.currentTimeMillis()}.mp4"

        val imageMap = java.util.HashMap<EvaModel.VideoReplaceItem, EvaReplaceConfig.ImageOrVideo>()
        val videoMap = java.util.HashMap<EvaModel.VideoReplaceItem, EvaReplaceConfig.ImageOrVideo>()
        val textMap = java.util.ArrayList<EvaModel.TextReplaceItem>()
        val audioMap = java.util.HashMap<EvaModel.AudioReplaceItem, EvaReplaceConfig.Audio>()

        for (item in mCurrentImageItems!!) {
            if (mDiffMap[item.id] == true) {
                if (item.isVideo) {
                    videoMap[item] = mConfigMap[item.id] as EvaReplaceConfig.ImageOrVideo
                } else {
                    imageMap[item] = mConfigMap[item.id] as EvaReplaceConfig.ImageOrVideo
                }
            }
        }

        for (item in mCurrentVideoItems!!) {
            if (mDiffMap[item.id] == true) {
                if (item.isVideo) {
                    videoMap[item] = mConfigMap[item.id] as EvaReplaceConfig.ImageOrVideo
                } else {
                    imageMap[item] = mConfigMap[item.id] as EvaReplaceConfig.ImageOrVideo
                }
            }
        }

        for (item in mCurrentAudioItems!!) {
            if (mDiffMap[item.id] == true) {
                audioMap[item] = mConfigMap[item.id] as EvaReplaceConfig.Audio
            }
        }

        for (item in mCurrentTextItems!!) {
            if (mDiffMap[item.id] == true) {
                textMap.add(item)
            }
        }

        evaRenderServer.updateImage(imageMap, taskId)
        evaRenderServer.updateVideo(videoMap, taskId)
        evaRenderServer.updateAudio(audioMap, taskId)
        evaRenderServer.updateText(textMap, taskId)

        mSavedMap.put(taskId, outputFilePath)

        evaRenderServer.requestSave(taskId, outputFilePath, object : EVASaverCallback.Stub() {
            override fun progress(p: Double, taskId: String) {
                // applicationContext.getSharedPreferences("evaRenderService",Context.MODE_PRIVATE).edit().putFloat(taskId,p.toFloat()).apply()
                runOnUiThread {
                    TLog.e("当前进度 : " + p + "当前任务ID : " + taskId)
                }
            }

            override fun saveSuccess(taskId: String) {
                sendBroadcast(
                    Intent(
                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        Uri.fromFile(File(outputFilePath))
                    )
                )
                this@ModelEditorActivity.serviceUnbind()

                runOnUiThread {
                    toast("渲染完成")


                }
            }

            override fun saveFailure(taskId: String) {

            }

        })
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mIEvaRenderServer = IEVARenderServer.Stub.asInterface(service)
            TLog.e("服务连接成功")
            mServiceLock.release()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mIEvaRenderServer = null
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data == null) return
        when (requestCode) {
            /**  图片裁剪回调*/
            ALBUM_REQUEST_CODE_IMAGE -> {
                val rectArray = data!!.extras!!.getFloatArray("zoom")!!
                mCurrentImageItem!!.resPath = data!!.getStringExtra("imagePath")
                if (TextUtils.isEmpty(mCurrentImageItem!!.resPath)) {
                    mCurrentImageItem!!.resPath = data!!.getStringExtra("videoPath")
                }


                val config = EvaReplaceConfig.ImageOrVideo()
                config.crop = RectF(rectArray[0], rectArray[1], rectArray[2], rectArray[3])
                config.audioMixWeight = 1F


                mEvaThreadPool.execute {
                    mDiffMap[mCurrentImageItem!!.id] = true
                    mConfigMap[mCurrentImageItem!!.id] = config
                    val updateRes = mEvaDirector!!.updateImage(mCurrentImageItem, config)
                    TLog.e("update image res ${updateRes} path = ${mCurrentImageItem!!.resPath}")

                    ModelManager.putResType(mCurrentImageItem!!.id, AlbumItemType.Image)
                }
                mTargetProgress = mCurrentImageItem!!.startTime
            }
            /** 视频裁剪回调 */
            ALBUM_REQUEST_CODE_VIDEO -> {
                when (resultCode) {
                    ALBUM_REQUEST_CODE_VIDEO -> {
                        val rectArray = data!!.getFloatArrayExtra("zoom")!!
                        mCurrentVideoItem!!.resPath = data!!.getStringExtra("videoPath")
                        val start = data!!.extras!!.getLong("start", 0L)
                        val duration = data!!.extras!!.getLong("duration", -1L)
                        val config = EvaReplaceConfig.ImageOrVideo()
                        config.crop = RectF(rectArray[0], rectArray[1], rectArray[2], rectArray[3])
                        config.repeat = 2
                        config.audioMixWeight = 0.5F
                        config.start = start
                        config.duration = duration

                        TLog.e("config ${config}")

                        mEvaThreadPool.execute {
                            mDiffMap[mCurrentVideoItem!!.id] = true
                            mConfigMap[mCurrentVideoItem!!.id] = config
                            if (mCurrentVideoItem!!.isVideo) {
                                if (!mEvaDirector!!.updateVideo(mCurrentVideoItem, config)) {
                                    TLog.e("updateVideo error")
                                }
                            } else {
                                if (!mEvaDirector!!.updateImage(mCurrentVideoItem, config)) {
                                    TLog.e("updateVideo error")
                                }
                            }

                            ModelManager.putResType(mCurrentVideoItem!!.id, AlbumItemType.Video)

                        }
                    }

                    ALBUM_REQUEST_CODE_IMAGE -> {
                        var rectArray = data!!.getFloatArrayExtra("zoom")!!
                        mCurrentVideoItem!!.resPath = data!!.getStringExtra("imagePath")
                        val config = EvaReplaceConfig.ImageOrVideo()
                        config.crop = RectF(rectArray[0], rectArray[1], rectArray[2], rectArray[3])
                        config.audioMixWeight = 1F
                        mEvaThreadPool.execute {
                            mDiffMap[mCurrentVideoItem!!.id] = true
                            mConfigMap[mCurrentVideoItem!!.id] = config
                            if (mCurrentVideoItem!!.isVideo) {
                                if (!mEvaDirector!!.updateVideo(mCurrentVideoItem, config)) {
                                    TLog.e("updateVideo error")
                                }
                            } else {
                                if (!mEvaDirector!!.updateImage(mCurrentVideoItem, config)) {
                                    TLog.e("updateImage error")
                                }
                            }

                            ModelManager.putResType(mCurrentVideoItem!!.id, AlbumItemType.Image)
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
                if (mCurrentAudiItem != null) {
                    val audpath =
                        AssetsMapper(this).mapAsset(data!!.extras!!.getString("audioPath"))!!
                    mCurrentAudiItem!!.resPath = audpath
                    val config = EvaReplaceConfig.Audio()
                    config.audioMixWeight = 1f
                    config.repeat = 2
                    mEvaThreadPool.execute {
                        mDiffMap[mCurrentAudiItem!!.id] = true
                        mConfigMap[mCurrentAudiItem!!.id] = config
                        mEvaDirector!!.updateAudio(mCurrentAudiItem, config)
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
                    imm.hideSoftInputFromWindow(
                        lsq_editor_replace_text.windowToken,
                        InputMethodManager.HIDE_NOT_ALWAYS
                    )
                }
                return false
            }

        })
    }

    private fun initRenderTestView() {
        mDurationFrames = mEvaModel!!.durationFrames

        val entries = ArrayList<Entry>(mDurationFrames + 1)
        repeat(mDurationFrames + 1) { i -> entries.add(Entry(i.toFloat(), 0f)) }
        val lineDataSet = LineDataSet(entries, "Render Times")
        lineDataSet.apply {
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.3f
            setDrawCircles(false)
            lineWidth = 0.5f
            color = Color.WHITE
        }

        mCurrentLine = lineDataSet

        lsq_renderTimesGraph.apply {
            setTouchEnabled(false)
            axisRight.isEnabled = false
            xAxis.isEnabled = false
            legend.isEnabled = false
            description = null
            data = LineData(lineDataSet)
            axisLeft.setDrawGridLines(false)
            axisLeft.labelCount = 4
            axisLeft.textColor = Color.WHITE
            val ll1 = LimitLine(16f, "60fps")
            ll1.lineColor = Color.RED
            ll1.lineWidth = 1.2f
            ll1.textColor = Color.WHITE
            ll1.textSize = 8f
            axisLeft.addLimitLine(ll1)

            val ll2 = LimitLine(32f, "30fps")
            ll2.lineColor = Color.RED
            ll2.lineWidth = 1.2f
            ll2.textColor = Color.WHITE
            ll2.textSize = 8f
            axisLeft.addLimitLine(ll2)
        }

        lsq_back_to_list.setOnClickListener {
            lsq_editor_render_layer.visibility = View.GONE
            lsq_editor_layer.visibility = View.VISIBLE
        }


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


        lsq_model_seles.init(Engine.getInstance().mainGLContext)

        var editorAdapter = ModelEditorAdapter(this, mEditorList, mConfigMap)
        editorAdapter.setOnItemClickListener(object : ModelEditorAdapter.OnItemClickListener {

            override fun onImageItemClick(
                view: View,
                item: EvaModel.VideoReplaceItem,
                position: Int,
                type: EditType
            ) {
                if (editorAdapter.getCurrentClickPos() != position) {
                    editorAdapter.setCurrentClickPos(position)

                    val targetPos = (item.endTime - item.startTime) / 2 + item.startTime

                    mEvaPlayer!!.previewFrame(targetPos.toLong())

                } else {
                    if (!isEnable) return
                    var isOnlyImage = false
                    mCurrentEditPostion = position
                    when (item.type) {
                        EvaModel.AssetType.kIMAGE_ONLY -> isOnlyImage = true
                        EvaModel.AssetType.kIMAGE_VIDEO -> isOnlyImage = false
                    }
                    val videoDuration = (item.endTime - item.startTime) * 1000f
                    mCurrentImageItem = item

                    showEditPopupWindow(
                        lsq_editor_item_list,
                        0,
                        lsq_editor_item_list.layoutManager!!.getChildAt(position)!!.x.toInt(),
                        {
                            startActivityForResult<AlbumActivity>(
                                ALBUM_REQUEST_CODE_IMAGE,
                                "videoDuration" to videoDuration,
                                "onlyImage" to isOnlyImage,
                                "onlyVideo" to false,
                                "width" to item.width,
                                "height" to item.height
                            )
                        },
                        {
                            var resType = ModelManager.getResType(item.id)
                            if (resType != null) {
                                when (resType!!) {
                                    AlbumItemType.Image -> {
                                        startActivityForResult<ImageCuterActivity>(
                                            ALBUM_REQUEST_CODE_IMAGE,
                                            "width" to item.width,
                                            "height" to item.height,
                                            "imagePath" to item.resPath
                                        )
                                    }
                                    AlbumItemType.Video -> {
                                        startActivityForResult<MovieCuterActivity>(
                                            ALBUM_REQUEST_CODE_VIDEO,
                                            "videoDuration" to videoDuration,
                                            "width" to item.width,
                                            "height" to item.height,
                                            "videoPath" to item.resPath
                                        )

                                    }
                                }
                            }

                        })
                }


            }

            override fun onVideoItemClick(
                view: View,
                item: EvaModel.VideoReplaceItem,
                position: Int,
                type: EditType
            ) {
                if (editorAdapter.getCurrentClickPos() != position) {
                    editorAdapter.setCurrentClickPos(position)

                    val targetPos = (item.endTime - item.startTime) / 2 + item.startTime

                    mEvaPlayer!!.previewFrame(targetPos.toLong())

                } else {
                    if (!isEnable) return
                    var isOnlyVideo = false
                    var isOnlyImage = false
                    mCurrentEditPostion = position
                    when (item.type) {
                        EvaModel.AssetType.kIMAGE_VIDEO -> isOnlyVideo = false
                        EvaModel.AssetType.kVIDEO_ONLY -> isOnlyVideo = true
                        EvaModel.AssetType.kIMAGE_ONLY -> isOnlyImage = true
                    }
                    val videoDuration = (item.endTime - item.startTime) * 1000f
                    mCurrentVideoItem = item

                    showEditPopupWindow(
                        lsq_editor_item_list,
                        0,
                        lsq_editor_item_list.layoutManager!!.getChildAt(position)!!.x.toInt(),
                        {
                            startActivityForResult<AlbumActivity>(
                                ALBUM_REQUEST_CODE_IMAGE,
                                "videoDuration" to videoDuration,
                                "onlyImage" to isOnlyImage,
                                "onlyVideo" to false,
                                "width" to item.width,
                                "height" to item.height
                            )
                        },
                        {
                            var resType = ModelManager.getResType(item.id)
                            if (resType != null) {
                                when (resType!!) {
                                    AlbumItemType.Image -> {
                                        startActivityForResult<ImageCuterActivity>(
                                            ALBUM_REQUEST_CODE_IMAGE,
                                            "width" to item.width,
                                            "height" to item.height,
                                            "imagePath" to item.resPath
                                        )
                                    }
                                    AlbumItemType.Video -> {
                                        startActivityForResult<MovieCuterActivity>(
                                            ALBUM_REQUEST_CODE_VIDEO,
                                            "videoDuration" to videoDuration,
                                            "width" to item.width,
                                            "height" to item.height,
                                            "videoPath" to item.resPath
                                        )

                                    }
                                }
                            }

                        })

//                    startActivityForResult<AlbumActivity>(ALBUM_REQUEST_CODE_VIDEO, "videoDuration" to videoDuration, "onlyImage" to isOnlyImage, "onlyVideo" to isOnlyVideo, "width" to item.width, "height" to item.height , "isAlpha" to (item.type == EvaModel.AssetType.kMASK))
                }


            }

            override fun onTextItemClick(
                view: View,
                item: EvaModel.TextReplaceItem,
                position: Int,
                type: EditType
            ) {
                if (!isEnable) return
                mCurrentTextItem = item
                lsq_editor_replace_text.setText(item.text)
                lsq_text_editor_layout.visibility = View.VISIBLE
                lsq_editor_replace_text.requestFocus()
                mCurrentEditPostion = position
                val inputManager: InputMethodManager =
                    getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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
            if (mEvaPlayerCurrentState == Player.State.kEOS) {
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
                //mEvaDirector!!.updateAudioMixWeightSeparate("", (progress / 10.0).toDouble())
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
            this.overridePendingTransition(
                R.anim.activity_open_from_bottom_to_top,
                R.anim.activity_keep_status
            )
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
                setEditorList(
                    mEvaModel!!.listReplaceableImageAssets(),
                    mEvaModel!!.listReplaceableVideoAssets(),
                    mEvaModel!!.listReplaceableTextAssets()
                )
                mCurrentAudioItems = mEvaModel!!.listReplaceableAudioAssets()
                mCurrentImageItem = null
                mCurrentVideoItem = null
                mCurrentTextItem = null
                mCurrentAudiItem = null
                for (item in mCurrentImageItems!!) {
                    if (mDiffMap[item.id] != null) {
                        if (item.isVideo) {
                            mEvaDirector!!.updateVideo(item, EvaReplaceConfig.ImageOrVideo())
                        } else {
                            mEvaDirector!!.updateImage(item, EvaReplaceConfig.ImageOrVideo())
                        }
                    }
                }
                for (item in mCurrentVideoItems!!) {
                    if (mDiffMap[item.id] != null) {
                        if (item.isVideo) {
                            mEvaDirector!!.updateVideo(
                                item,
                                mConfigMap[item.id] as EvaReplaceConfig.ImageOrVideo
                            )
                        } else {
                            mEvaDirector!!.updateImage(
                                item,
                                mConfigMap[item.id] as EvaReplaceConfig.ImageOrVideo
                            )
                        }
                    }
                }
                for (item in mCurrentTextItems!!) {
                    if (mDiffMap[item.id] != null)
                        mEvaDirector!!.updateText(item)
                }
                for (item in mCurrentAudioItems!!) {
                    if (mDiffMap[item.id] != null) {
                        val config = mConfigMap[item.id] as EvaReplaceConfig.Audio
                        mEvaDirector!!.updateAudio(item, config)
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
                mDiffMap[mCurrentTextItem!!.id] = true
                mEvaDirector!!.updateText(mCurrentTextItem)
            }
            lsq_text_editor_layout.visibility = View.GONE
            editorAdapter.notifyItemChanged(mCurrentEditPostion)
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(
                lsq_editor_replace_text.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )
            playerReplay()
        }

        lsq_next.text = "保存"
        lsq_next.textColor = Color.parseColor("#007aff")

        lsq_next.setOnClickListener {
            mEvaPlayer!!.pause()
            /** 保存时必须把播放停止 */
            lsq_player_img.visibility = View.VISIBLE
            MaterialDialog(this).show {
                title(text = "保存方式")
                message(text = "请选择保存方式,前台保存或后台保存")

                positiveButton(R.string.lsq_foreground_save) { dialog ->
                    dialog.dismiss()
                    saveVideoSync()
                }

                negativeButton(R.string.lsq_background_save) { dialog ->
                    dialog.dismiss()
                    saveVideoAsync()
                }

                cancelOnTouchOutside(true)
            }


        }

        lsq_to_render_layer.setOnClickListener {
            lsq_editor_layer.visibility = View.GONE
            lsq_editor_render_layer.visibility = View.VISIBLE
        }

        val isFromModelManager = intent.getBooleanExtra("isFromModelManager", false)

        mEvaThreadPool.execute {
            mEvaDirector = EvaDirector()

            if (!isFromModelManager) {
                val modelItem = intent.getParcelableExtra<ModelItem>("model")!!

                mEvaModel = EvaModel()
                if (AssetsHelper.hasAssets(this, modelItem.templateName)) {
                    mEvaModel!!.createFromAsset(this, modelItem.templateName)
                } else {
                    mEvaModel!!.create(modelItem.modelDownloadFilePath)
                }

                setEditorList(
                    mEvaModel!!.listReplaceableImageAssets(),
                    mEvaModel!!.listReplaceableVideoAssets(),
                    mEvaModel!!.listReplaceableTextAssets()
                )
            } else {
                val modelManager = ModelManager
                mEvaModel = modelManager.getModel()

                setEditorList(modelManager)

            }


            val ret = mEvaDirector!!.open(mEvaModel)
            if (!ret) {
                mEvaModel!!.debugDump()
                TLog.e("[Error] EvaPlayer Open Failed")
                //todo 失败提醒
                return@execute
            }

            mCurrentFrameRate = mEvaModel!!.frameRate
            mDurationFrames = mEvaModel!!.durationFrames


            var renderTimeGraphRange = 4f
            mEvaDirector!!.addFrameListener { ms ->
                if (mCurrentLine == null) return@addFrameListener

                mCurrentLine!!.getEntryForIndex(mCurrentFrame).y = ms
                renderTimeGraphRange = renderTimeGraphRange.coerceAtLeast(ms * 1.2f)

                if (mEvaPlayerCurrentState == Player.State.kDO_PREVIEW || mEvaPlayerCurrentState == Player.State.kDO_SEEK) {

                } else {

                    mCurrentFrame++;
                    mCurrentFrame = min(mCurrentFrame, mDurationFrames)
                }


                runOnUiThread {
                    lsq_renderTimesGraph.setVisibleYRange(
                        0.2f,
                        renderTimeGraphRange,
                        YAxis.AxisDependency.LEFT
                    )
                    lsq_renderTimesGraph.invalidate()
                }
            }

            mEvaDirector!!.setPerformanceTrackingEnable(true)

            mEvaDirector!!.updateAudioMixWeight(1.0)

            mEvaPlayer = mEvaDirector!!.newPlayer()
            mEvaPlayer!!.setListener(mPlayerProcessListener)
            if (!mEvaPlayer!!.open()) {
                return@execute
            }
            val seekMax = mEvaPlayer!!.duration.toInt()


            runOnUiThread {
                var metrics = DisplayMetrics()
                windowManager.defaultDisplay.getRealMetrics(metrics)
                val displayArea = Rect()
                lsq_model_seles.getGlobalVisibleRect(displayArea)
                val videoHeight = mEvaModel!!.height.toFloat()
                val videoWidth = mEvaModel!!.width.toFloat()
                val hwp = videoWidth / videoHeight

//                val mH = lsq_model_seles.measuredHeight
//                val mW = lsq_model_seles.measuredWidth
//
//                var w = 0
//                var h = 0
//
//                if (videoHeight >= videoWidth) {
//                    h = mH
//                    w = (mH.toFloat() * videoWidth.toFloat() / videoHeight).toInt()
//                } else {
//                    w = mW
//                    h = (mW * (videoHeight.toFloat() / videoWidth)).toInt()
//                }
//
//                lsq_model_seles.layoutParams.width = w
//                lsq_model_seles.layoutParams.height = h

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
            if (mCurrentAudioItems.isNullOrEmpty()) {
                isCanChangeAudio = false
            }


            if (isFromModelManager) {
                for (item in mCurrentImageItems!!) {
                    val id = item.id
                    var config = ModelManager.getConfig(id)
                    if (config == null) {
                        config = EvaReplaceConfig.ImageOrVideo()
                    } else {
                        config = config as EvaReplaceConfig.ImageOrVideo
                    }
                    mDiffMap.put(id, true)
                    mConfigMap.put(id, config)
                    if (item.isVideo) {
                        mEvaDirector!!.updateVideo(item, config)
                    } else {
                        mEvaDirector!!.updateImage(item, config)
                    }
                }

                for (item in mCurrentVideoItems!!) {
                    val id = item.id
                    var config = ModelManager.getConfig(id)
                    if (config == null) {
                        config = EvaReplaceConfig.ImageOrVideo()
                    } else {
                        config = config as EvaReplaceConfig.ImageOrVideo
                    }
                    mDiffMap.put(id, true)
                    mConfigMap.put(id, config)
                    if (item.isVideo) {
                        mEvaDirector!!.updateVideo(item, config)
                    } else {
                        mEvaDirector!!.updateImage(item, config)
                    }

                }
            }

            mEvaPlayer!!.play()
            mEvaPlayer!!.pause()
            mEvaPlayer!!.previewFrame(1)
            runOnUiThread {
                lsq_seek.max = seekMax

                initRenderTestView()

            }


        }

        TuSdk.messageHub().setStatus(this, R.string.lsq_assets_loading)
    }

    private var mEditPopupWindow: PopupWindow? = null

    private fun showEditPopupWindow(
        targetView: View,
        offsetY: Int,
        offsetX: Int,
        changedFun: () -> Unit,
        editFun: () -> Unit
    ) {
        if (mEditPopupWindow == null) {
            val popView = LayoutInflater.from(this).inflate(R.layout.popup_edit_layout, null, false)
            val popupWindow =
                PopupWindow(popView, TuSdkContext.dip2px(120f), TuSdkContext.dip2px(50f), true)
            popupWindow.isTouchable = true
            popupWindow.isOutsideTouchable = true

            popupWindow.setBackgroundDrawable(ColorDrawable(0x00000000))
            mEditPopupWindow = popupWindow
        }

        mEditPopupWindow!!.contentView.lsq_popup_changed.setOnClickListener {
            changedFun()
        }

        mEditPopupWindow!!.contentView.lsq_popup_edit.setOnClickListener {
            editFun()
        }

//        mEditPopupWindow!!.showAsDropDown(targetView)
//        mEditPopupWindow!!.showAtLocation(targetView,Gravity.NO_GRAVITY,targetView.x.toInt(),targetView.y.toInt())
        mEditPopupWindow!!.showAsDropDown(
            targetView,
            offsetX,
            offsetY - (targetView.measuredHeight + TuSdkContext.dip2px(50f)),
            Gravity.TOP or Gravity.LEFT
        )


    }

    private fun saveVideoAsync() {
        val intent = Intent(applicationContext, EVARenderServer::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(intent)
        } else {
            applicationContext.startService(intent)
        }
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        mEvaThreadPool.submit {
            if (mIEvaRenderServer == null) {
                mServiceLock.acquire()
            }
            mEVAServerRunnable.run()
        }

    }

    private fun serviceUnbind() {
        try {
            applicationContext.unbindService(serviceConnection)
        } catch (e: Exception) {

        }
    }

    private fun saveVideoSync() {
        lsq_editor_cut_load.visibility = View.VISIBLE
        lsq_editor_cut_load_parogress.setValue(0f)
        setEnable(false)
        mEvaThreadPool.execute {
            val producer = mEvaDirector!!.newProducer()

            val outputFilePath =
                "${Environment.getExternalStoragePublicDirectory(DIRECTORY_DCIM).path}/eva_output${System.currentTimeMillis()}.mp4"

            val config: Producer.OutputConfig = Producer.OutputConfig()
            config.watermarkPosition = 1
            config.watermark = BitmapHelper.getRawBitmap(applicationContext, R.raw.sample_watermark)
            val supportSize = ProduceOutputUtils.getSupportSize(MediaFormat.MIMETYPE_VIDEO_AVC)
            val outputSize = TuSdkSize.create(mEvaModel!!.width, mEvaModel!!.height)

            config.scale = 0.5
//            if (supportSize.maxSide() < outputSize.maxSide()) {
//                config.scale = supportSize.maxSide().toDouble() / outputSize.maxSide().toDouble()
//            }
            producer.setOutputConfig(config)

            producer.setListener { state, ts ->
                if (state == Producer.State.kEND) {
                    mEvaThreadPool.execute {
                        producer.cancel()
                        //producer.waitComplete()
                        producer.release()
                        mEvaDirector!!.resetProducer()
                        mEvaPlayer!!.seekTo(mCurrentTs)
                        TLog.e("ts : $mCurrentTs")
                    }
                    sendBroadcast(
                        Intent(
                            Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                            Uri.fromFile(File(outputFilePath))
                        )
                    )
                    runOnUiThread {
                        setEnable(true)
                        lsq_editor_cut_load.setVisibility(View.GONE)
                        lsq_editor_cut_load_parogress.setValue(0f)
                        Toast.makeText(applicationContext, "保存成功", Toast.LENGTH_SHORT).show()
                    }
                } else if (state == Producer.State.kWRITING) {
                    runOnUiThread {
                        lsq_editor_cut_load.setVisibility(View.VISIBLE)
                        lsq_editor_cut_load_parogress.setValue((ts / producer.duration.toFloat()) * 100f)
                    }
                }
            }

            if (!producer.init(outputFilePath)) {
                TLog.e("producer init error")
                return@execute
            }

            if (!producer.start()) {
                TLog.e("[Error] EvaProducer Start failed")
            }
        }
    }

    private fun setEditorList(
        replaceImageList: Array<EvaModel.VideoReplaceItem>,
        replaceVideoList: Array<EvaModel.VideoReplaceItem>,
        replaceTextList: Array<EvaModel.TextReplaceItem>
    ) {
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
                if (!compari.isVideo)
                    mEditorList.add(EditorModelItem(compari, EditType.Image, compari.startTime))
                else
                    mEditorList.add(EditorModelItem(compari, EditType.Video, compari.startTime))
            } else if (compari is EvaModel.TextReplaceItem) {
                mEditorList.add(EditorModelItem(compari, EditType.Text, compari.startTime))
            }
        }
        mEditorList.sortBy {
            it.startPos
        }
        runOnUiThread {
            mEditorAdapter!!.setEditorModelList(mEditorList)
        }
    }

    private fun setEditorList(modelManager: ModelManager) {
        mEditorList.clear()

        mCurrentImageItems = modelManager.getImageArrays()
        mCurrentVideoItems = modelManager.getVideoArrays()
        mCurrentTextItems = modelManager.getTextArrays()

        mEditorList.addAll(modelManager.getItemsList())

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
        lsq_model_seles.release()
        lsq_edit_content.removeView(lsq_model_seles)
        mEvaThreadPool.execute {
            if (!isRelease) {
                mEvaPlayer!!.close()
                mEvaDirector!!.resetPlayer()
                mEvaDirector!!.close()
            }

            for (item in mCurrentVideoItems!!) {
                item.thumbnail.recycle()
            }
            for (item in mCurrentImageItems!!) {
                item.thumbnail.recycle()
            }


        }
        mEvaThreadPool.shutdown()

        ModelManager.release()
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
}