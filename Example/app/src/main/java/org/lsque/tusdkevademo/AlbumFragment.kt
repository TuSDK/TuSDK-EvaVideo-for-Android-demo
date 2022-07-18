 /**
 *  TuSDK
 *  droid-sdk-eva$
 *  org.lsque.tusdkevademo$
 *  @author  H.ys
 *  @Date    2019/7/1$ 16:00$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lsque.tusdkevademo

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.movie_album_fragment.*
import org.jetbrains.anko.startActivityForResult
import org.lasque.tusdkpulse.core.utils.ThreadHelper
import org.lasque.tusdkpulse.core.utils.sqllite.ImageSqlHelper
import org.lasque.tusdkpulse.core.utils.sqllite.ImageSqlInfo
import org.lasque.tusdkpulse.impl.view.widget.TuProgressHub
import org.lsque.tusdkevademo.DynamicModelActivity.Companion.ALBUM_REQUEST_CODE_DYNAMCI
import org.lsque.tusdkevademo.ModelEditorActivity.Companion.ALBUM_REQUEST_CODE_ALPHA_VIDEO
import org.lsque.tusdkevademo.utils.MD5Util
import java.util.*
import kotlin.collections.ArrayList

class AlbumFragment : Fragment(), LoadTaskDelegate {

    /* 最小视频时长(单位：ms) */
    private val MIN_VIDEO_DURATION = 3000

    /* 最大视频时长(单位：ms) */
    private val MAX_VIDEO_DURATION = 60000 * 3

    private var isAlpha = false

    private var mAlbumAdapter: AlbumAdapter? = null

    public var mSelectList: ArrayList<AlbumInfo> = ArrayList()

    private var mMaxSize = 1

    private var mMinSize = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.movie_album_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val gridLayoutManager = GridLayoutManager(activity, 4)
        lsq_album_list.layoutManager = gridLayoutManager
        isAlpha = arguments!!.getBoolean("isAlpha")
        mMaxSize = requireArguments().getInt("maxSize", 1)
        mMinSize = requireArguments().getInt("minSize",-1)
    }

    override fun onResume() {
        super.onResume()
        var loadTask = LoadAlbumTask(this)
        loadTask.execute()
    }

    private fun getAlbumList(): LinkedList<AlbumInfo> {
        var albumList = LinkedList<AlbumInfo>()
        when {
            arguments!!.getBoolean("onlyImage") -> getImageList(albumList)
            arguments!!.getBoolean("onlyVideo") -> getVideoList(albumList)
            else -> {
                getVideoList(albumList)
                getImageList(albumList)
            }
        }
        albumList.sortedByDescending { it.createDate }
        return albumList
    }

    /**
     * 将扫描的视频添加到集合中
     */
    private fun getVideoList(albumList: LinkedList<AlbumInfo>) {
        val cursor = activity!!.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null, null, "date_added desc")
        while (cursor!!.moveToNext()) {
            val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
            val duration = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))
            val createDate = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED))
            //根据时间长短加入显示列表
            if (duration in 1 until MAX_VIDEO_DURATION) {
                albumList.add(AlbumInfo(path, AlbumItemType.Video, duration, createDate,MD5Util.crypt(path)))
            }
        }
        cursor.close()
    }

    private fun getImageList(albumList: LinkedList<AlbumInfo>) {
        var imageList:ArrayList<ImageSqlInfo>? = ImageSqlHelper.getPhotoList(activity!!.contentResolver, true)
        if (imageList != null)
            for (item in imageList) {
                albumList.add(AlbumInfo(item.path, AlbumItemType.Image, 0, item.createDate.timeInMillis,MD5Util.crypt(item.path)))
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

    }

    companion object {
        /**
         * 相册加载
         */
        internal class LoadAlbumTask(private val loadTaskDelegate: LoadTaskDelegate) : AsyncTask<Void, Int, MutableList<AlbumInfo>>() {

            override fun doInBackground(vararg voids: Void): MutableList<AlbumInfo>? {
                return loadTaskDelegate.onLoadStart()
            }

            override fun onPreExecute() {
                loadTaskDelegate.onLoading()
                super.onPreExecute()
            }

            override fun onPostExecute(imageInfos: MutableList<AlbumInfo>?) {
                var imageInfos = imageInfos
                TuProgressHub.dismiss()
                if (imageInfos == null) imageInfos = ArrayList()
                ThreadHelper.post {
                    loadTaskDelegate.onLoadFinish(imageInfos)
                }
            }
        }
    }

    override fun onLoadFinish(imageInfos: MutableList<AlbumInfo>) {
        if (mAlbumAdapter == null) {
            mAlbumAdapter = AlbumAdapter(requireActivity().baseContext, imageInfos, mSelectList)
            mAlbumAdapter!!.setMaxSize(mMaxSize)
                                     mAlbumAdapter!!.setOnItemClickListener(object : AlbumAdapter.OnItemClickListener {
                    override fun onClick(view: View, item: AlbumInfo, position: Int) {
                    if (mMaxSize == 1){
                        when (item.type) {
                            AlbumItemType.Image -> {
                                activity!!.startActivityForResult<ImageCuterActivity>(ModelEditorActivity.ALBUM_REQUEST_CODE_IMAGE, "width" to arguments!!["width"], "height" to arguments!!["height"], "imagePath" to item.path)
                            }
                            AlbumItemType.Video -> {
                                if (isAlpha) {
                                    var intent = Intent()
                                    intent.putExtra("videoPath", item?.path)
                                    activity?.setResult(ALBUM_REQUEST_CODE_ALPHA_VIDEO, intent)
                                    activity?.finish()
                                } else {
                                    activity!!.startActivityForResult<MovieCuterActivity>(ModelEditorActivity.ALBUM_REQUEST_CODE_VIDEO, "videoDuration" to arguments!!["videoDuration"], "width" to arguments!!["width"], "height" to arguments!!["height"], "videoPath" to item.path)
                                }
                            }
                        }
                    } else {
                        mAlbumAdapter!!.updateSelectedVideoPosition(position)
                    }
                }
            })
            lsq_album_list.adapter = mAlbumAdapter
        }
        if (mAlbumAdapter!!.getAlbumList().size != imageInfos.size ||
            !(MD5Util.crypt(mAlbumAdapter!!.getAlbumList().toString()).equals(MD5Util.crypt(imageInfos.toString()))) ) {
            mAlbumAdapter!!.setAlbumList(imageInfos)
        }    }

    override fun onLoadStart(): MutableList<AlbumInfo>? {
        return getAlbumList()
    }

    override fun onLoading() {
        TuProgressHub.showToast(activity, "数据加载中...")
    }
}

public interface LoadTaskDelegate{
    fun onLoadFinish(imageInfos: MutableList<AlbumInfo>)

    fun onLoadStart() : MutableList<AlbumInfo>?

    fun onLoading()
}