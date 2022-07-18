/**
 *  TuSDK
 *  EvaDemo$
 *  org.lsque.tusdkevademo$
 *  @author  H.ys
 *  @Date    2022/4/7$ 15:36$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lsque.tusdkevademo

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.Rect
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.tusdk.pulse.DispatchQueue
import com.tusdk.pulse.Engine
import com.tusdk.pulse.Player
import com.tusdk.pulse.Producer
import com.tusdk.pulse.eva.EvaModel
import com.tusdk.pulse.eva.EvaReplaceConfig
import com.tusdk.pulse.eva.dynamic.DynEvaDirector
import com.tusdk.pulse.utils.AssetsMapper
import kotlinx.android.synthetic.main.activity_dynamic_model_editor.*
import kotlinx.android.synthetic.main.activity_dynamic_model_editor.lsq_editor_change_bgm
import kotlinx.android.synthetic.main.activity_dynamic_model_editor.lsq_editor_cut_load
import kotlinx.android.synthetic.main.activity_dynamic_model_editor.lsq_editor_cut_load_parogress
import kotlinx.android.synthetic.main.activity_dynamic_model_editor.lsq_editor_item_list
import kotlinx.android.synthetic.main.activity_dynamic_model_editor.lsq_model_seles
import kotlinx.android.synthetic.main.activity_dynamic_model_editor.lsq_player_img
import kotlinx.android.synthetic.main.activity_dynamic_model_editor.lsq_seek
import kotlinx.android.synthetic.main.title_item_layout.*
import org.jetbrains.anko.longToast
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.textColor
import org.jetbrains.anko.toast
import org.lasque.tusdkpulse.core.TuSdk
import org.lasque.tusdkpulse.core.struct.TuSdkSize
import org.lasque.tusdkpulse.core.utils.AssetsHelper
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkpulse.core.utils.image.BitmapHelper
import org.lsque.tusdkevademo.ModelEditorActivity.Companion.AUDIO_REQUEST_CODE
import org.lsque.tusdkevademo.server.DYNEVARenderServer
import org.lsque.tusdkevademo.utils.ProduceOutputUtils
import java.io.File
import java.util.*
import java.util.concurrent.Semaphore
import kotlin.collections.ArrayList
import kotlin.math.min

/**
 * TuSDK
 * org.lsque.tusdkevademo
 * EvaDemo
 *
 * @author        H.ys
 * @Date        2022/4/7  15:36
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class DynamicModelEditorActivity : ScreenAdapterActivity(){

    private var mEvaModel : EvaModel? = null

    private var mDirector : DynEvaDirector? = null

    private var mPlayer : DynEvaDirector.Player? = null

    private var mQueue = DispatchQueue()

    private var mPlayerState : Player.State = Player.State.kREADY

    private var mCurrentTs = 0L

    private var isRelease = false

    private var isEnable = true

    private var mAudioPath = ""

    private var mConfigList = ArrayList<EvaReplaceConfig.ImageOrVideo>()

    private var mPlayerListener = Player.Listener { state, ts ->

        mPlayerState = state
        if (state == Player.State.kEOS){
            mQueue.runSync {
                mPlayer?.pause()
                mPlayer?.seekTo(0)
            }
            runOnUiThread {
                runOnUiThread {
                    lsq_player_img.visibility = View.VISIBLE
                }
            }
        } else if (state == Player.State.kPLAYING || state == Player.State.kDO_PREVIEW){
            runOnUiThread {
                mCurrentTs = ts
                lsq_seek.progress = ts.toInt()
            }
        }

    }

    private var mDynItem : EvaModel.DynReplaceItem? = null

    private val mServiceLock = Semaphore(0)

    private var mIDynEvaRenderServer : IDYNEVARenderServer? = null

    private var mSavedMap = HashMap<String,String>()

    private val mDynEvaServerRunnable : Runnable = Runnable {
        if (mIDynEvaRenderServer == null) return@Runnable

        val renderServer = mIDynEvaRenderServer!!

        val modelPath = intent.getStringExtra("modelPath")

        var taskId = ""

        taskId = renderServer.initRenderTask(modelPath,false)

        if (TextUtils.isEmpty(taskId)){
            runOnUiThread {
                longToast("当前已经有渲染任务在进行了,请等待当前任务结束后再试")
            }
            return@Runnable
        }

        val outputFilePath =
            "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).path}/eva_output${System.currentTimeMillis()}.mp4"

        val itemList = intent.getSerializableExtra("itemPaths") as ArrayList<AlbumInfo>

        val configList = ArrayList<EvaReplaceConfig.ImageOrVideo>()

        for (item in itemList){
            val config = EvaReplaceConfig.ImageOrVideo()
            config.path = item.path
            configList.add(config)
        }

        renderServer.updateResource(taskId,configList,mAudioPath)

        mSavedMap.put(taskId,outputFilePath)

        renderServer.requestSave(taskId,outputFilePath,object : EVASaverCallback.Stub() {
            override fun progress(p: Double, taskId: String?) {
                runOnUiThread {
                    TLog.e("当前进度 : "+ p + "当前任务ID : " + taskId)
                }
            }

            override fun saveSuccess(taskId: String?) {
                sendBroadcast(
                    Intent(
                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        Uri.fromFile(File(outputFilePath))
                    )
                )
                this@DynamicModelEditorActivity.serviceUnbind()

                runOnUiThread {
                    toast("渲染完成")
                }
            }

            override fun saveFailure(taskId: String?) {

            }

        })


    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mIDynEvaRenderServer = IDYNEVARenderServer.Stub.asInterface(service)
            TLog.e("服务连接成功")
            mServiceLock.release()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mIDynEvaRenderServer = null
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data == null) return
        when(requestCode){

            AUDIO_REQUEST_CODE -> {
                val audioPath = AssetsMapper(this).mapAsset(data!!.extras!!.getString("audioPath"))!!
                mAudioPath = audioPath
                TLog.e("")
                mQueue.runSync {
                    mDirector!!.updateAudio(mAudioPath)
                }
            }

        }
        playerReplay()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dynamic_model_editor)
        initView()
    }

    private fun initView(){
        lsq_title_item_title.text = "动态模板预览"
        lsq_back.setOnClickListener {
            mQueue.runSync {
                mPlayer!!.pause()
                finish()
            }
        }

        val modelPath = intent.getStringExtra("modelPath")
        lsq_model_seles.init(Engine.getInstance().mainGLContext)

        val itemList = intent.getSerializableExtra("itemPaths") as ArrayList<AlbumInfo>

        mQueue.runAsync {

            mDirector = DynEvaDirector()

            mEvaModel = EvaModel()

            mEvaModel!!.create(modelPath,EvaModel.ModelType.DYNAMIC)

            val ret = mDirector!!.open(mEvaModel)

            if (!ret){
                mEvaModel!!.debugDump()
                TLog.e("[Error] DynEvaDirector Open Failed")
                return@runAsync
            }

            mDirector!!.create()

            val configList = ArrayList<EvaReplaceConfig.ImageOrVideo>()

            for (item in itemList){
                val config = EvaReplaceConfig.ImageOrVideo()
                config.path = item.path
                configList.add(config)
            }

            mConfigList.addAll(configList)

            mDirector!!.updateResource(configList)


            mPlayer = mDirector!!.newPlayer()
            mPlayer!!.setListener(mPlayerListener)
            if (!mPlayer!!.open()){
                return@runAsync
            }

            val seekMax = mPlayer!!.duration.toInt()

            val list = mDirector!!.replaceItems
            val itemList = LinkedList<EvaModel.DynReplaceItem>()
            itemList.addAll(list)

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


                val adapter = DynamicEditorAdapter(this,itemList)
                val linearLayoutManager = LinearLayoutManager(this)
                linearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
                lsq_editor_item_list.layoutManager = linearLayoutManager
                lsq_editor_item_list.adapter = adapter

                lsq_seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        if (!fromUser) return
                        mQueue.runAsync {
                            mPlayer?.previewFrame(progress.toLong())
                        }
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                        playerPause()
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {

                    }

                })

                lsq_model_seles.setOnClickListener {
                    if (mPlayerState != Player.State.kPLAYING){
                        playerPlaying()
                    } else {
                        playerPause()
                    }
                }

                lsq_player_img.setOnClickListener {
                    if (mPlayerState == Player.State.kEOS){
                        mQueue.runAsync {
                            mPlayer?.seekTo(0)
                        }
                        playerPlaying()
                    } else if (mPlayerState != Player.State.kPLAYING){
                        playerPlaying()
                    } else {
                        playerPause()
                    }
                }

                lsq_editor_change_bgm.setOnClickListener {
                    startActivityForResult<AudioListActivity>(ModelEditorActivity.AUDIO_REQUEST_CODE)
                    this.overridePendingTransition(R.anim.activity_open_from_bottom_to_top, R.anim.activity_keep_status)
                }

                lsq_next.text = "保存"
                lsq_next.textColor = Color.parseColor("#007aff")
                lsq_next.setOnClickListener {
                    mPlayer!!.pause()
                    lsq_player_img.visibility = View.VISIBLE
                    MaterialDialog(this).show {
                        title(text = "保存方式")
                        message(text = "请选择保存方式,前台保存或后台保存")

                        positiveButton(R.string.lsq_foreground_save){ dialog ->
                            dialog.dismiss()
                            saveVideoSync()
                        }

                        negativeButton(R.string.lsq_background_save){dialog->
                            dialog.dismiss()
                            saveVideoAsync()
                        }

                        cancelOnTouchOutside(true)
                    }
                }


            }

            lsq_model_seles.attachPlayer(mPlayer)

            mPlayer!!.play()
            mPlayer!!.pause()
            mPlayer!!.previewFrame(1)
            runOnUiThread {
                lsq_seek.max = seekMax
            }
        }
    }

    private fun serviceUnbind(){
        try {
            applicationContext.unbindService(serviceConnection)
        } catch (e : Exception){

        }
    }

    private fun saveVideoAsync() {
        val intent = Intent(applicationContext,DYNEVARenderServer::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(intent)
        } else {
            applicationContext.startService(intent)
        }
        bindService(intent,serviceConnection,Context.BIND_AUTO_CREATE)
        mQueue.runAsync {
            if (mIDynEvaRenderServer == null){
                mServiceLock.acquire()
            }
            mDynEvaServerRunnable.run()
        }
    }

    private fun saveVideoSync() {
        lsq_editor_cut_load.visibility = View.VISIBLE
        lsq_editor_cut_load_parogress.setValue(0f)
        setEnable(false)
        mQueue.runSync {

            val producer = mDirector!!.newProducer()

            val outputFilePath =
                "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).path}/eva_output${System.currentTimeMillis()}.mp4"

            val config: Producer.OutputConfig = Producer.OutputConfig()
            config.watermarkPosition = 1
            config.watermark = BitmapHelper.getRawBitmap(applicationContext, R.raw.sample_watermark)
            val supportSize = ProduceOutputUtils.getSupportSize(MediaFormat.MIMETYPE_VIDEO_AVC)
            val outputSize = TuSdkSize.create(mEvaModel!!.width, mEvaModel!!.height)
            if (supportSize.maxSide() < outputSize.maxSide()) {
                config.scale = supportSize.maxSide().toDouble() / outputSize.maxSide().toDouble()
            }
            producer.setOutputConfig(config)

            producer.setListener { state, ts ->
                if (state == Producer.State.kEND){
                    mQueue.runAsync {
                        producer.cancel()
                        mDirector!!.resetProducer()
                        mPlayer!!.seekTo(mCurrentTs)
                    }
                    sendBroadcast(
                        Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                            Uri.fromFile(File(outputFilePath))
                        )
                    )
                    runOnUiThread {
                        setEnable(true)
                        lsq_editor_cut_load.setVisibility(View.GONE)
                        lsq_editor_cut_load_parogress.setValue(0f)
                        Toast.makeText(applicationContext, "保存成功", Toast.LENGTH_SHORT).show()
                    }
                } else if (state == Producer.State.kWRITING){
                    runOnUiThread {
                        lsq_editor_cut_load.visibility = View.VISIBLE
                        lsq_editor_cut_load_parogress.setValue((ts / producer.duration.toFloat()) * 100f)
                    }
                }

            }

            if (!producer.init(outputFilePath)){
                return@runSync
            }

            if (!producer.start()){
                TLog.e("[Error] EvaProducer Start failed")
            }

        }
    }

    private fun playerPause() {
        mQueue.runSync {
            if (mPlayerState == Player.State.kPLAYING){
                mPlayer?.pause()
                runOnUiThread {
                    lsq_player_img.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun playerPlaying() {
        mQueue.runSync {
            mPlayer?.play()
            runOnUiThread {
                lsq_player_img.visibility = View.GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        playerPause()
    }

    override fun onPause() {
        super.onPause()
        playerPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        lsq_model_seles.release()
        lsq_edit_content.removeView(lsq_model_seles)
        mQueue.runSync {
            if(!isRelease) {
                mPlayer!!.close()
                mDirector!!.resetPlayer()
                mDirector!!.close()
            }
        }
        TuSdk.getAppTempPath().deleteOnExit()
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

    private fun playerReplay() {
        mQueue.runSync {
            mPlayer!!.seekTo(0)
            playerPlaying()
        }
    }





}