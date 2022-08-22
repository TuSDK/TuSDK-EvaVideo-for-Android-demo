/**
 *  TuSDK
 *  droid-sdk-eva$
 *  org.lsque.tusdkevademo$
 *  @author  H.ys
 *  @Date    2019/7/1$ 10:05$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lsque.tusdkevademo

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.SparseArray
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.tusdk.pulse.Engine
import com.tusdk.pulse.eva.EvaModel
import com.tusdk.pulse.eva.TuSDKEva
import com.tusdk.pulse.utils.AssetsMapper
import kotlinx.android.synthetic.main.demo_entry_activity.lsq_model_list
import kotlinx.android.synthetic.main.network_model_example_activity.*
import org.jetbrains.anko.startActivity
import org.json.JSONArray
import org.lasque.tusdkpulse.core.TuSdkContext
import org.lasque.tusdkpulse.core.utils.AssetsHelper
import org.lasque.tusdkpulse.core.utils.FileHelper
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkpulse.core.utils.ThreadHelper
import org.lsque.tusdkevademo.albumselect.AlbumSelectActivity
import org.lsque.tusdkevademo.utils.DownloadManagerUtil
import org.lsque.tusdkevademo.utils.EVAItem
import org.lsque.tusdkevademo.utils.PermissionUtils
import java.io.*
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile


class NetworkModelExampleActivity : ScreenAdapterActivity(), DownloadManagerUtil.DownloadStateListener {
    override fun onFile(requestId: Long, mRequestUrl: String) {
        if (mDownloadItemMap.get(mRequestUrl) == null) return
        mDownloadMap[mRequestUrl]?.let { downloadMap.put(it,false) }
        ThreadHelper.postDelayed({
            if (mDownLoadItemHolderMap[mRequestUrl] == null) return@postDelayed
            mDownLoadItemHolderMap[mRequestUrl]?.mDownloadCircleImageView?.animation?.cancel()
            mDownLoadItemHolderMap[mRequestUrl]?.mDownloadCircleImageView?.tag = ""
            mDownLoadItemHolderMap[mRequestUrl]?.mDownloadCircleImageView!!.animation = null
            mDownLoadItemHolderMap[mRequestUrl]?.mDownloadCircleImageView!!.visibility = View.INVISIBLE
            mDownLoadItemHolderMap[mRequestUrl]?.mDownloadImageView!!.visibility = View.VISIBLE
            mDownLoadItemHolderMap.remove(mRequestUrl)
        },500)
    }

    override fun onEVAJsonDownloadFile() {
        Toast.makeText(this,"列表更新失败,请稍后重试",Toast.LENGTH_SHORT).show()
        lsq_refresh_layout.isRefreshing = false
    }

    override fun onEVAJsonDownloadComplete() {
        TLog.e("[Debug] onEVAJsonDownloadComplete")
        mEVAItemList.clear()
        getList()
        lsq_refresh_layout.isRefreshing = false
    }

    private fun getList() {
        try {
            val jsonReader: FileReader = FileReader(File(applicationContext.getSharedPreferences("EVA-DOWNLOAD", Context.MODE_PRIVATE).getString("evaJson", "")))
            var jsonArray = JSONArray(jsonReader.readText())
            for (i in 0 until jsonArray.length()) {
                val nm = jsonArray.getJSONObject(i).getString("nm")
                val id = jsonArray.getJSONObject(i).getInt("id")
                val vid = jsonArray.getJSONObject(i).getString("v")
                mEVAItemList.add(EVAItem(nm, id,vid))
            }

            refreshAdapterData()
        } catch (e : FileNotFoundException){
            e.printStackTrace()
        }

    }

    private fun refreshAdapterData() {
        val modelList = ModelItemFactory.getModelItem(mEVAItemList)
        modelAdapter?.setModelList(modelList)
    }

    override fun onComplete(path: String, requestId: Long, mRequestUrl: String) {
        if (mDownloadItemMap.get(mRequestUrl) == null) return
        applicationContext.getSharedPreferences("EVA-DOWNLOAD", Context.MODE_PRIVATE).edit().putString(mDownloadItemMap[mRequestUrl]!!.modelDownloadUrl,path).apply()
        isDownloadComplete = true
        mDownloadItemMap[mRequestUrl]?.modelDownloadFilePath = path
        ThreadHelper.postDelayed({
            if (mDownLoadItemHolderMap[mRequestUrl] == null) return@postDelayed
            mDownLoadItemHolderMap[mRequestUrl]?.mDownloadCircleImageView?.animation?.cancel()
            mDownLoadItemHolderMap[mRequestUrl]?.mDownloadCircleImageView?.tag = ""
            mDownLoadItemHolderMap[mRequestUrl]?.mDownloadCircleImageView!!.animation = null
            mDownLoadItemHolderMap[mRequestUrl]?.mDownloadCircleImageView!!.visibility = View.INVISIBLE
            mDownLoadItemHolderMap.remove(mRequestUrl)
        },500)
        mDownloadMap[mRequestUrl]?.let { downloadMap.put(it,true) }
        if (mDownloadMap[mRequestUrl] !=null) {
            modelAdapter!!.notifyItemChanged(mDownloadMap[mRequestUrl]!!, -2)
            mDownloadMap.remove(mRequestUrl)
            mDownloadItemMap.remove(mRequestUrl)
        }
    }

    override fun onNotificationClicked() {
        /**
         * 这里是通知栏进度点击事件的回调
         */
    }

    val LAYOUT_ID: Int = R.layout.network_model_example_activity

    var mDownloadUtil : DownloadManagerUtil? = null

    val MODEL_LIST: ArrayList<String> = arrayListOf(
            "32-meihaoshike"
            )


    val NAME_LIST : ArrayList<String> = arrayListOf(
            "美好时刻"
            )


    val TEMPLATES_LIST : ArrayList<String> = arrayListOf(
            ""
            )

    val ID_LIST : ArrayList<Int> = arrayListOf(142)

    var modelAdapter: NetworkModelAdapter? = null

    val downloadMap : SparseArray<Boolean> = SparseArray()

    var mCurrentModel : ModelItem? = null

    var mCurrentItemHolder : NetworkModelAdapter.ViewHolder? =null

    var mCurrentPosition = -1

    var downloadItemList = LinkedList<Int>()

    var mDownloadMap : HashMap<String,Int> = HashMap()
    var mDownloadItemMap : HashMap<String,ModelItem> = HashMap()
    var mDownLoadItemHolderMap : HashMap<String,NetworkModelAdapter.ViewHolder> = HashMap()

    var mEVAItemList  = java.util.ArrayList<EVAItem>()

    private var isDownloadComplete = true

    private val CONTENT_RESULT_CODE = 100

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CONTENT_RESULT_CODE && resultCode == Activity.RESULT_OK){
            val uri = data!!.getData()
            var file : File? = null

            val sp = getSharedPreferences("fileLoader",Context.MODE_PRIVATE)
            if (sp.contains(uri.toString())){
                file = File(sp.getString(uri.toString(),""))
            } else {
                if (uri != null){
                    val input = contentResolver.openInputStream(uri)
                    val path = getTempOutputPath()
                    file = File(path)
                    val output = file.outputStream()
                    FileHelper.copy(input,output)
                    FileHelper.safeClose(input)
                    FileHelper.safeClose(output)
                    sp.edit().putString(uri.toString(),path).apply()
                }
            }

            if (file != null) {
                val item = ModelItem(
                    file.absolutePath,
                    uri!!.lastPathSegment!!,
                    uri!!.lastPathSegment!!,
                    file.absolutePath,
                    file.name,
                    file.absolutePath,
                    file.name,
                    "1.0.0",
                    0
                )
//                startActivity<ModelDetailActivity>("model" to item)

                startActivity<AlbumSelectActivity>("model" to item)
            }
        }
    }

    private fun getTempOutputPath(): String {
        return TuSdkContext.getAppCacheDir(
            "evacache",
            false
        ).absolutePath + "/eva_temp" + System.currentTimeMillis() + ".eva"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LAYOUT_ID)
        val mEngine = Engine.getInstance()
        mEngine.init(null)
        mDownloadUtil = DownloadManagerUtil(this,this)
        mDownloadUtil!!.downLoadEVAJson()

        lsq_version_code.setText("TuSDK EVA SDK ${TuSDKEva.SDK_VERSION}-${TuSDKEva.TOOLS_VERSION} \n" +
                " © 2022 TUTUCLOUD.COM")

        initView()

        //需要模板字体外置时可参考
        // initFonts()
    }

    override fun onPause() {
        super.onPause()
        ModelManager.release()
    }

    private fun initFonts() {
        mapZipFile("fontsDir","fonts.zip")
    }

    private fun mapZipFile(key: String, fileName: String) {
        val sp = TuSdkContext.context().getSharedPreferences("TU-TTF", MODE_PRIVATE)
        val assetsMapper = AssetsMapper(TuSdkContext.context())
        if (!sp.contains(key)) {
            try {
                val zipFilePath = assetsMapper.mapAsset(fileName)
                val index = zipFilePath.lastIndexOf(".")
                val saveDir = zipFilePath.substring(0, index)
                val saveFileDir = File(saveDir)
                if (!saveFileDir.exists()) saveFileDir.mkdirs()
                var entry: ZipEntry? = null
                var entryFilePath: String? = null
                var count = 0
                val bufferSize = 1024
                val buffer = ByteArray(bufferSize)
                var bis: BufferedInputStream? = null
                var bos: BufferedOutputStream? = null
                val zip = ZipFile(zipFilePath)
                val entries = zip.entries() as Enumeration<ZipEntry>
                //循环对压缩包里的每一个文件进行解压
                while (entries.hasMoreElements()) {
                    entry = entries.nextElement()
                    /**这里提示如果当前元素是文件夹时，在目录中创建对应文件夹
                     * ，如果是文件，得出路径交给下一步处理 */
                    entryFilePath = saveDir + File.separator + entry.name
                    TLog.e("entry file path %s", entryFilePath)
                    val file = File(entryFilePath)
                    if (entryFilePath.endsWith("/")) {
                        if (!file.exists()) {
                            file.mkdirs()
                        }
                        continue
                    }
                    /***这里即是上一步所说的下一步，负责文件的写入✧ */
                    bos = BufferedOutputStream(FileOutputStream(entryFilePath))
                    bis = BufferedInputStream(zip.getInputStream(entry))
                    while (bis.read(buffer, 0, bufferSize).also { count = it } != -1) {
                        bos.write(buffer, 0, count)
                    }
                    bos.flush()
                    bos.close()
                }
                sp.edit().putString(key, saveDir).apply()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                TLog.e(e)
            } catch (e: IOException) {
                e.printStackTrace()
                TLog.e(e)
            }
        }
    }

    private fun initView() {
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,  Manifest.permission.RECORD_AUDIO)
        PermissionUtils.requestRequiredPermissions(this, permissions)
        setModelView()
        lsq_refresh_layout.setOnRefreshListener {
            mDownloadUtil!!.downLoadEVAJson()
        }
        lsq_back.setOnClickListener { finish() }
        lsq_model_list.visibility= View.VISIBLE
        lsq_eva_file_explorer.setOnClickListener {
//            startActivity<EVAModelFileExplorerActivity>()

            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent,CONTENT_RESULT_CODE)
        }
    }

    private fun setModelView() {
        modelAdapter = NetworkModelAdapter(this, ModelItemFactory.getModelItem(MODEL_LIST,NAME_LIST,TEMPLATES_LIST,ID_LIST),downloadMap,mDownloadMap)
        modelAdapter!!.setHasStableIds(false)
        modelAdapter!!.setOnItemClickListener(object : ModelAdapter.OnItemClickListener, NetworkModelAdapter.OnItemClickListener {
            override fun onDownloadClick(holder: NetworkModelAdapter.ViewHolder, progressView: ImageView, item: ModelItem, position: Int) {
                holder.mDownloadImageView.visibility = View.GONE
                holder.mDownloadCircleImageView.visibility = View.VISIBLE
                val mRotateAnimation : Animation = RotateAnimation(0f, 360.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
                mRotateAnimation.fillAfter = true
                mRotateAnimation.interpolator = LinearInterpolator()
                mRotateAnimation.duration = 1200
                mRotateAnimation.repeatCount = Animation.INFINITE
                mRotateAnimation.repeatMode = Animation.RESTART
                holder.mDownloadCircleImageView.animation = mRotateAnimation
                holder.mDownloadCircleImageView.tag = "NeedAddAnimation"
                holder.mDownloadCircleImageView.animation.startNow()
                modelAdapter!!.notifyItemChanged(position,-3)
                /** 模拟下载操作 */
                ThreadHelper.runThread {
                    if(!downloadMap[position] && (!mDownloadMap.containsValue(position))){
                        mCurrentModel = item
                        mDownloadMap[mCurrentModel!!.modelDownloadUrl] = position
                        mDownloadItemMap[item.modelDownloadUrl] = item
                        mDownLoadItemHolderMap[item.modelDownloadUrl] = holder
                        val filePath = applicationContext.getSharedPreferences("EVA-DOWNLOAD", Context.MODE_PRIVATE).getString(
                            mCurrentModel!!.modelDownloadUrl,"")
                        val locVer = applicationContext.getSharedPreferences("EVA-DOWNLOAD", Context.MODE_PRIVATE).getString("${mCurrentModel!!.modelId}_ver","")
                        if (!TextUtils.isEmpty(filePath) && !TextUtils.isEmpty(locVer) && !TextUtils.equals(mCurrentModel!!.modelVer,locVer)){
                            FileHelper.delete(File(filePath))
                            EvaModel.ClearTemplatePath()
                        }
                        mDownloadUtil!!.createRuquest(item.modelDownloadUrl,"${item.modelName}模板",item.templateName,object : DownloadManagerUtil.DownloadingProgressListener{
                            override fun onProgress(progress: Float, currentBytes: Long, totalBytes: Long) {
                                val currentPosition = position
                                runOnUiThread {
                                    modelAdapter?.notifyItemChanged(position,NetworkModelAdapter.ProgressData(currentBytes, totalBytes, progress))
                                }
                            }

                            override fun onCompleted() {
                                val currentPosition = position
                                runOnUiThread {
                                    modelAdapter!!.notifyItemChanged(currentPosition,-2)
                                }
                            }

                        })
                        applicationContext.getSharedPreferences("EVA-DOWNLOAD", Context.MODE_PRIVATE).edit().putString("${item.modelId}_ver",item.modelVer).apply()
                    }
                }
                /**
                 *              Demo内基于DownloadManger实现的下载功能模拟 将下载地址传入即可
                 *              mDownloadUtil.createRuquest("模板下载地址")
                 * */
            }

            override fun onCheckModelVer(
                holder: NetworkModelAdapter.ViewHolder,
                progressView: ImageView,
                item: ModelItem,
                position: Int
            ) : Boolean {
                val locVer = applicationContext.getSharedPreferences("EVA-DOWNLOAD", Context.MODE_PRIVATE).getString("${mCurrentModel!!.modelId}_ver","")

                val currentVer = item.modelVer

                return TextUtils.equals(locVer,currentVer)
            }

            override fun onClick(view: View, item: ModelItem, position: Int) {
                if (AssetsHelper.getAssetPath(TuSdkContext.context(),item.templateName) == null && TextUtils.isEmpty(item.modelDownloadFilePath)){
                    Toast.makeText(this@NetworkModelExampleActivity,"模板正在下载中,请稍后",Toast.LENGTH_SHORT).show()
                    return
                }
                if (downloadMap[position]){
                    if (TextUtils.isEmpty(item.videoUrl)){
                        startActivity<ModelDetailActivity>("model" to item)
                    } else {
                        startActivity<ModelVideoActivity>("model" to item)
                    }

                } else {
                    Toast.makeText(this@NetworkModelExampleActivity,"模板未下载,请先下载模板",Toast.LENGTH_SHORT).show()
                }
            }
        })
        val layoutManger = StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL)
        layoutManger.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
        layoutManger.reverseLayout = false
        lsq_model_list.layoutManager = layoutManger
        lsq_model_list.animation = null
        lsq_model_list.setHasFixedSize(true)
        (lsq_model_list.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        lsq_model_list.adapter = modelAdapter
        getList()
    }

    override fun onDestroy() {
        super.onDestroy()
        val mEngine = Engine.getInstance()
        mEngine.release()
    }
}