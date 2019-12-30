package org.lsque.tusdkevademo.utils

import android.app.DownloadManager
import android.app.DownloadManager.STATUS_FAILED
import android.app.DownloadManager.STATUS_SUCCESSFUL
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import org.lasque.tusdk.core.utils.FileHelper
import org.lasque.tusdk.core.utils.ThreadHelper
import java.io.File
import java.lang.Exception
import java.util.*
import kotlin.collections.HashMap

/**
 * TuSDK
 * $desc$
 *
 * @author        H.ys
 * @Date        $data$ $time$
 * @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 */
class DownloadManagerUtil (context: Context,listener : DownloadStateListener){

    val mContext = context
    val mDownloadManager : DownloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    val mRequestLinkList : LinkedList<Long> = LinkedList()

    var mDownloadStateListener = listener

    var mDownloadReceiver : DownloadReceiver? = null

    var mJsonReceiver : EVAJsonDownloadReceiver? = null

    var mEVAJsonRequestId = -1L




    public fun createRuquest(url : String,title:String,fileName:String){
        if (mDownloadReceiver == null) {
            mDownloadReceiver = DownloadReceiver(this)
            val intentFilter = IntentFilter()
            intentFilter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            intentFilter.addAction(DownloadManager.ACTION_NOTIFICATION_CLICKED)
            mContext.registerReceiver(mDownloadReceiver,intentFilter)
        }
        /** 注册广播接收者接受下载完成与通知栏进度条点击事件 */
        /** 创建DownloadManger使用的Request对象 */
        val request = DownloadManager.Request(Uri.parse(url))
        /** 1. 设置Request的下载环境 */
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
        /** 2. 设置在下载过程中 是否在通知栏显示进度 */
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        /** 3. 设置通知栏显示标题 */
        request.setTitle(title)
        /** 4. 设置下载提示信息 */
        request.setDescription("${mContext.applicationInfo.name}正在下载")
        /** 5. 设置下载保存路径 */
        request.setDestinationInExternalFilesDir(mContext, Environment.DIRECTORY_DOWNLOADS, fileName)
        /** 6. 使用DownloadManger链接Request对象 并保存任务Id */
        val requestId = mDownloadManager.enqueue(request)
        mRequestLinkList.add(requestId)
        mDownloadReceiver!!.requestUrlMap[requestId] = url
    }

    public fun downLoadEVAJson(){

        if (mJsonReceiver == null){
            mJsonReceiver = EVAJsonDownloadReceiver(this)

            val intentFilter = IntentFilter()
            intentFilter.addAction(DownloadManager.ACTION_NOTIFICATION_CLICKED)

            intentFilter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            mContext.registerReceiver(mJsonReceiver,intentFilter)
        }


        val jsonRequest = DownloadManager.Request(Uri.parse("http://files.tusdk.com/eva/eva.json"))

        jsonRequest.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE)

        jsonRequest.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)


        jsonRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)

        jsonRequest.setDestinationInExternalFilesDir(mContext,Environment.DIRECTORY_DOWNLOADS,"eva.json")

        jsonRequest.setVisibleInDownloadsUi(false)

        mEVAJsonRequestId = mDownloadManager.enqueue(jsonRequest)
    }

    public interface DownloadStateListener{
        /**
         * 下载完成回调
         */
        fun onComplete(path: String, requestId: Long, mRequestUrl: String)

        fun onFile(requestId: Long, mRequestUrl: String)

        fun onEVAJsonDownloadFile()

        fun onEVAJsonDownloadComplete()

        /**
         * 通知栏进度条点击回调
         */
        fun onNotificationClicked()
    }

    class DownloadReceiver(downloadManagerUtil: DownloadManagerUtil) : BroadcastReceiver(){
        val mDownloadManagerUtil = downloadManagerUtil
        var mRequestId = -1L
        val requestUrlMap : HashMap<Long,String> = HashMap()
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent!!.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                mRequestId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID,-1)
                if (mDownloadManagerUtil.mRequestLinkList.isNotEmpty() && mDownloadManagerUtil.mRequestLinkList.contains(mRequestId)) {
                    /** BroadcastReceiver内不允许进行耗时操作 */
                    ThreadHelper.post {
                        var downloadFileUri = mDownloadManagerUtil.mDownloadManager.getUriForDownloadedFile(mRequestId)
                        if (downloadFileUri == null) return@post
                        mDownloadManagerUtil.mRequestLinkList.remove(mRequestId)
                        val query = DownloadManager.Query().setFilterById(mRequestId)
                        val cursor= mDownloadManagerUtil.mDownloadManager.query(query)
                        try {
                            if (cursor != null && cursor.moveToFirst()){

                                if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))== STATUS_SUCCESSFUL){
                                    mDownloadManagerUtil.mDownloadStateListener?.onComplete(getPath(context,downloadFileUri),mRequestId,requestUrlMap[mRequestId]!!)
                                } else if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))== STATUS_FAILED) {
                                    mDownloadManagerUtil.mDownloadStateListener?.onFile(mRequestId,requestUrlMap[mRequestId]!!)
                                }
                            }
                        } catch (e : Exception){

                        } finally {

                        }

                    }
                }
            } else if (intent.getAction().equals(DownloadManager.ACTION_NOTIFICATION_CLICKED)) {
                /** BroadcastReceiver内不允许进行耗时操作 */
                ThreadHelper.post {
                    mDownloadManagerUtil.mDownloadStateListener?.onNotificationClicked()
                }

            }
            context?.unregisterReceiver(this)
        }

        fun getPath(context: Context?,uri: Uri) : String{
            var path = ""
            var fileName = ""
            val filePathColumn = arrayOf(MediaStore.MediaColumns.DATA,MediaStore.MediaColumns.DISPLAY_NAME)
            val cursor = context?.contentResolver!!.query(uri,filePathColumn,null,null,null,null)
            if (cursor == null) return path
            if (cursor.moveToFirst()){
                try {
                    path = cursor.getString(cursor.getColumnIndex(filePathColumn[0]))
                } catch (e:Exception){
                }
                fileName = cursor.getString(cursor.getColumnIndex(filePathColumn[1]))
            }
            return path
        }

    }

    class EVAJsonDownloadReceiver(downloadManagerUtil: DownloadManagerUtil) : BroadcastReceiver(){
        val mDownloadManagerUtil = downloadManagerUtil
        var mRequestId = -1L
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent!!.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                mRequestId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID,-1)
                if (mRequestId == -1L) return
                ThreadHelper.post {
                    if (mDownloadManagerUtil.mEVAJsonRequestId == mRequestId){
                        var downloadFileUri = mDownloadManagerUtil.mDownloadManager.getUriForDownloadedFile(mRequestId)
                        if (downloadFileUri == null) return@post
                        val query = DownloadManager.Query().setFilterById(mRequestId)
                        val cursor= mDownloadManagerUtil.mDownloadManager.query(query)
                        try {
                            if (cursor != null && cursor.moveToFirst()){
                                if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))== STATUS_SUCCESSFUL){
                                    context?.getSharedPreferences("EVA-DOWNLOAD", Context.MODE_PRIVATE)!!.edit().putString("evaJson",getPath(context,downloadFileUri)).apply()
                                    mDownloadManagerUtil.mDownloadStateListener.onEVAJsonDownloadComplete()
                                }else if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))== STATUS_FAILED){
                                    mDownloadManagerUtil.mDownloadStateListener.onEVAJsonDownloadFile()
                                }
                            }
                        } catch (e :Exception){

                        }
                    }
                }

            }
            context?.unregisterReceiver(this)
        }

        fun getPath(context: Context?,uri: Uri) : String{
            var path = ""
            var fileName = ""
            val filePathColumn = arrayOf(MediaStore.MediaColumns.DATA,MediaStore.MediaColumns.DISPLAY_NAME)
            val cursor = context?.contentResolver!!.query(uri,filePathColumn,null,null,null,null)
            if (cursor == null) return path
            if (cursor.moveToFirst()){
                try {
                    path = cursor.getString(cursor.getColumnIndex(filePathColumn[0]))
                } catch (e:Exception){
                }
                fileName = cursor.getString(cursor.getColumnIndex(filePathColumn[1]))
            }
            return path
        }

    }




}