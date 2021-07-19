/**
 *  TuSDK
 *  droid-sdk-eva$
 *  org.lsque.tusdkevademo$
 *  @author  H.ys
 *  @Date    2019/7/1$ 16:42$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lsque.tusdkevademo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.jetbrains.anko.find
import org.lasque.tusdkpulse.core.view.TuSdkViewHelper

class AlbumAdapter(context: Context, albumList: List<AlbumInfo>) : RecyclerView.Adapter<AlbumAdapter.ViewHolder>() {


    private val mContext: Context = context
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private var mAlbumList = albumList
    private var mItemClickListener: OnItemClickListener? = null


    override fun getItemCount(): Int {
        return mAlbumList.size
    }

    public fun getAlbumList(): List<AlbumInfo> {
        return mAlbumList;
    }

    public fun setAlbumList(albumList: List<AlbumInfo>) {
        mAlbumList = albumList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = mInflater.inflate(R.layout.lsq_album_select_video_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val currentItem = mAlbumList[position]
        when (currentItem.type) {
            AlbumItemType.Image -> {
                viewHolder!!.textView.visibility = View.GONE
            }
            AlbumItemType.Video -> {
                viewHolder!!.textView.visibility = View.VISIBLE
                viewHolder.textView.text = String.format("%02d:%02d", currentItem.duration / 1000 / 60, currentItem.duration / 1000 % 60)
            }
        }
        Glide.with(mContext).load(currentItem.path).into(viewHolder.imageView)
        viewHolder.itemView.setOnClickListener(object : TuSdkViewHelper.OnSafeClickListener(1000) {
            override fun onSafeClick(v: View?) {
                mItemClickListener!!.onClick(viewHolder.itemView, currentItem, position)
            }
        })
    }


    public fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        mItemClickListener = onItemClickListener
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.find(R.id.lsq_video_thumb_view)
        val textView: TextView = itemView.find(R.id.lsq_movie_time)
    }

    interface OnItemClickListener {
        fun onClick(view: View, item: AlbumInfo, position: Int)
    }
}