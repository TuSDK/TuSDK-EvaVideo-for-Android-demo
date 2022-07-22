/**
 *  TuSDK
 *  EvaDemo$
 *  org.lsque.tusdkevademo.albumselect$
 *  @author  H.ys
 *  @Date    2022/6/27$ 14:26$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lsque.tusdkevademo.albumselect

import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.movie_album_fragment.*
import org.jetbrains.anko.support.v4.runOnUiThread
import org.jetbrains.anko.support.v4.toast
import org.lasque.tusdkpulse.core.utils.ThreadHelper
import org.lasque.tusdkpulse.core.utils.sqllite.ImageSqlHelper
import org.lasque.tusdkpulse.core.utils.sqllite.ImageSqlInfo
import org.lsque.tusdkevademo.AlbumInfo
import org.lsque.tusdkevademo.AlbumItemType
import org.lsque.tusdkevademo.R
import org.lsque.tusdkevademo.utils.MD5Util
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.max

/**
 * TuSDK
 * org.lsque.tusdkevademo.albumselect
 * EvaDemo
 *
 * @author        H.ys
 * @Date        2022/6/27  14:26
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class AlbumSelectFragment : Fragment() {

    public interface OnAlbumSelectListener{
        fun onSelect(item : AlbumSelectItem,position : Int) : Boolean

        fun enableSelect() : Boolean
    }

    private var mAlbumAdapter: AlbumSelectAdapter? = null

    private var mOnAlbumSelectListener : OnAlbumSelectListener? = null

    private var mAlbumList : LinkedList<AlbumSelectItem>? = null

    public fun setOnAlbumSelectListener(albumSelectListener: OnAlbumSelectListener){
        mOnAlbumSelectListener = albumSelectListener
    }

    public fun notifyAlbumItemChanged(item : AlbumSelectItem){
        if(mAlbumList != null &&mAlbumList!!.contains(item))
            mAlbumAdapter?.notifyItemChanged(mAlbumList!!.indexOf(item))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.movie_album_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val gridLayoutManager = GridLayoutManager(activity, 4)
        lsq_album_list.layoutManager = gridLayoutManager
    }

    override fun onResume() {
        super.onResume()

        ThreadHelper.runThread {
            val albumList = getAlbumList()
            mAlbumList = albumList

            runOnUiThread {
                if (mAlbumAdapter == null){
                    mAlbumAdapter = AlbumSelectAdapter(requireActivity().baseContext,albumList)
                    mAlbumAdapter!!.setItemClickListener(object : AlbumSelectAdapter.OnItemClickListener{
                        override fun onClick(view: View, item: AlbumSelectItem, position: Int) {

                        }

                        override fun onSelect(view: View, item: AlbumSelectItem, position: Int) {
                           if (mOnAlbumSelectListener == null){
                               return
                           }

                            if (mOnAlbumSelectListener!!.enableSelect()){

                                if (mOnAlbumSelectListener != null && mOnAlbumSelectListener!!.onSelect(item,position)){
                                    item.count++;
                                    mAlbumAdapter!!.notifyItemChanged(position)
                                } else {
                                    runOnUiThread {
                                        toast("当前坑位无法选择这个素材")
                                    }
                                }


                            } else {
                                runOnUiThread {
                                    toast("素材已经填满了")
                                }
                            }
                        }

                    })

                    lsq_album_list.adapter = mAlbumAdapter
                }
                if (mAlbumAdapter?.itemCount != albumList.size ||
                    !(MD5Util.crypt(mAlbumAdapter!!.getAlbumList().toString()).equals(MD5Util.crypt(mAlbumList.toString()))) ){
                    mAlbumAdapter!!.setAlbumList(albumList)
                }
            }
        }
    }

    private fun getAlbumList(): LinkedList<AlbumSelectItem> {
        var albumList =
        when {
            arguments!!.getBoolean("onlyImage") -> AlbumManager.getImageList()
            arguments!!.getBoolean("onlyVideo") -> AlbumManager.getVideoList()
            else -> {
                AlbumManager.getMediaList()
            }
        }
        return albumList
    }

}