package org.lsque.tusdkevademo.albumselect

import android.content.Context
import android.provider.MediaStore
import org.lasque.tusdkpulse.core.utils.ThreadHelper
import org.lasque.tusdkpulse.core.utils.sqllite.ImageSqlHelper
import org.lasque.tusdkpulse.core.utils.sqllite.ImageSqlInfo
import org.lsque.tusdkevademo.AlbumInfo
import org.lsque.tusdkevademo.AlbumItemType
import org.lsque.tusdkevademo.utils.MD5Util
import java.util.*
import kotlin.collections.ArrayList

/**
 * TuSDK
 * org.lsque.tusdkevademo.albumselect
 * EvaDemo
 *
 * @author        H.ys
 * @Date        2022/6/28  14:42
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
object AlbumManager {

    private var mContext: Context? = null

    private val mImageList: LinkedList<AlbumSelectItem> = LinkedList<AlbumSelectItem>()

    private val mVideoList: LinkedList<AlbumSelectItem> = LinkedList<AlbumSelectItem>()

    private val mMediaList: LinkedList<AlbumSelectItem> = LinkedList<AlbumSelectItem>()

    private var needWaiting = false

    public fun init(context: Context) {
        mContext = context
        refreshMediaData()
    }

    public fun getImageList(): LinkedList<AlbumSelectItem> {
        checkNeedWait()
        return mImageList
    }

    public fun getVideoList(): LinkedList<AlbumSelectItem> {
        checkNeedWait()
        return mVideoList
    }

    public fun getMediaList(): LinkedList<AlbumSelectItem> {
        checkNeedWait()
        return mMediaList
    }

    public fun refreshData(){
        refreshMediaData()
    }

    private fun checkNeedWait() {
        while (needWaiting) {

        }
    }

    private fun refreshMediaData() {
        ThreadHelper.runThread {
            needWaiting = true
            mImageList.clear()
            mVideoList.clear()
            mMediaList.clear()

            getImageList(mImageList)

            getVideoList(mVideoList)

            mMediaList.addAll(mImageList)
            mMediaList.addAll(mVideoList)

            mMediaList.sortedByDescending { it.albumInfo.createDate }
            mImageList.sortedByDescending { it.albumInfo.createDate }
            mVideoList.sortedByDescending { it.albumInfo.createDate }

            needWaiting = false
        }
    }

    /**
     * 将扫描的视频添加到集合中
     */
    private fun getVideoList(albumList: LinkedList<AlbumSelectItem>) {
        val cursor = mContext!!.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null, null, "date_added desc"
        )
        while (cursor!!.moveToNext()) {
            val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
            val duration =
                cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))
            val createDate =
                cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED))

            val albumItem = AlbumInfo(
                path, AlbumItemType.Video, duration, createDate,
                MD5Util.crypt(path)
            )

            albumList.add(
                AlbumSelectItem(albumItem, 0)
            )

        }
        cursor.close()
    }

    private fun getImageList(albumList: LinkedList<AlbumSelectItem>) {
        var imageList: ArrayList<ImageSqlInfo>? =
            ImageSqlHelper.getPhotoList(mContext!!.contentResolver, true)
        if (imageList != null)
            for (item in imageList) {
                val albumItem = AlbumInfo(
                    item.path, AlbumItemType.Image, 0, item.createDate.timeInMillis,
                    MD5Util.crypt(item.path)
                )
                albumList.add(
                    AlbumSelectItem(albumItem, 0)
                )

            }
    }


}