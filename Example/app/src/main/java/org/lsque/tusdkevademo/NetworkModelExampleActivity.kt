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
import android.content.Context
import android.os.Bundle
import android.util.SparseArray
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import kotlinx.android.synthetic.main.demo_entry_activity.lsq_model_list
import kotlinx.android.synthetic.main.network_model_example_activity.*
import org.jetbrains.anko.startActivity
import org.lasque.tusdk.core.utils.TLog
import org.lasque.tusdk.core.utils.ThreadHelper
import org.lsque.tusdkevademo.utils.DownloadManagerUtil
import org.lsque.tusdkevademo.utils.PermissionUtils
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


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

    override fun onComplete(path: String, requestId: Long, mRequestUrl: String) {
        if (mDownloadItemMap.get(mRequestUrl) == null) return
        applicationContext.getSharedPreferences("EVA-DOWNLOAD", Context.MODE_PRIVATE).edit().putString(mDownloadItemMap[mRequestUrl]!!.modelDownloadUrl,path).commit()
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
            modelAdapter!!.notifyItemChanged(mDownloadMap[mRequestUrl]!!, -1)
            mDownloadMap.remove(mRequestUrl)
            mDownloadItemMap.remove(mRequestUrl)
            TLog.d("[Debug] Path : $path")
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
            "11-shuoaini","12-zimushan","13-hufupin",
            "14-shipmv","15-shipmv","16-tongqu",
            "17-jiandanship","18-renyuanjieshao","19-qichejieshao",
             "02-jiugongge","04-quweixiatian","03-shilitaohua",
            "05-xinhunkuaile-lan","06-xinhunkuaile-fen","08-weddingday",
           "10-shishagnqianyan","11-dianyingjiaopian","09-tutujieshao"
            )


    val NAME_LIST : ArrayList<String> = arrayListOf("" +
            "说爱你","字幕快闪","产品推广",
            "视频MV","照片展示","童趣",
            "简单视频展示","人员介绍","汽车介绍",
            "九宫格","趣味夏天","十里桃花",
            "HappyWedding","HappyWedding","婚礼纪念日",
            "时尚潮流","电影胶片","涂图视频融合介绍"
            )


    val TEMPLATES_LIST : ArrayList<String> = arrayListOf(
            "lsq_eva_42.eva","lsq_eva_43.eva","lsq_eva_44.eva",
            "lsq_eva_45.eva","lsq_eva_46.eva","lsq_eva_47.eva",
            "lsq_eva_48.eva","lsq_eva_49.eva","lsq_eva_50.eva",
            "lsq_eva_24.eva","lsq_eva_26.eva","lsq_eva_25.eva",
            "lsq_eva_27.eva","lsq_eva_28.eva","lsq_eva_29.eva",
            "lsq_eva_30.eva","lsq_eva_31.eva","lsq_eva_23.eva"
            )

    var modelAdapter: NetworkModelAdapter? = null

    val downloadMap : SparseArray<Boolean> = SparseArray()

    var mCurrentModel : ModelItem? = null

    var mCurrentItemHolder : NetworkModelAdapter.ViewHolder? =null

    var mCurrentPosition = -1

    var downloadItemList = LinkedList<Int>()

    var mDownloadMap : HashMap<String,Int> = HashMap()
    var mDownloadItemMap : HashMap<String,ModelItem> = HashMap()
    var mDownLoadItemHolderMap : HashMap<String,NetworkModelAdapter.ViewHolder> = HashMap()

    private var isDownloadComplete = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LAYOUT_ID)
        mDownloadUtil = DownloadManagerUtil(this,this)
        initView()
    }

    private fun initView() {
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_WIFI_STATE)
        PermissionUtils.requestRequiredPermissions(this, permissions)
        setModelView()
        lsq_back.setOnClickListener { finish() }
        lsq_model_list.visibility= View.VISIBLE
    }

    private fun setModelView() {
        modelAdapter = NetworkModelAdapter(this, ModelItemFactory.getModelItem(MODEL_LIST,NAME_LIST,TEMPLATES_LIST),downloadMap,mDownloadMap)
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
                /** 模拟下载操作 */
                ThreadHelper.runThread {
                    if(!downloadMap[position]){
                        mCurrentModel = item
                        mDownloadMap[mCurrentModel!!.modelDownloadUrl] = position
                        mDownloadItemMap[item.modelDownloadUrl] = item
                        mDownLoadItemHolderMap[item.modelDownloadUrl] = holder
                        mDownloadUtil!!.createRuquest(item.modelDownloadUrl,"${item.modelName}模板",item.templateName)
                    }
                }
                /**
                 *              Demo内基于DownloadManger实现的下载功能模拟 将下载地址传入即可
                 *              mDownloadUtil.createRuquest("模板下载地址")
                 * */
            }

            override fun onClick(view: View, item: ModelItem, position: Int) {
                if (downloadMap[position]){
                    startActivity<ModelDetailActivity>("model" to item)
                } else {
                    Toast.makeText(this@NetworkModelExampleActivity,"模板未下载,请先下载模板",Toast.LENGTH_SHORT).show()
                }
            }
        })
        val layoutManger = StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL)
        layoutManger.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
        lsq_model_list.layoutManager = layoutManger
        lsq_model_list.animation = null
        lsq_model_list.setHasFixedSize(true)
        (lsq_model_list.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        lsq_model_list.adapter = modelAdapter
    }
}