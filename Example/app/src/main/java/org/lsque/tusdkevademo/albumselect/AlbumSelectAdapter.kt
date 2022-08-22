/**
 *  TuSDK
 *  EvaDemo$
 *  org.lsque.tusdkevademo.albumselect$
 *  @author  H.ys
 *  @Date    2022/6/27$ 11:18$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lsque.tusdkevademo.albumselect

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.album_select_item.view.*
import org.lsque.tusdkevademo.AlbumInfo
import org.lsque.tusdkevademo.AlbumItemType
import org.lsque.tusdkevademo.R

/**
 * TuSDK
 * org.lsque.tusdkevademo.albumselect
 * EvaDemo
 *
 * @author        H.ys
 * @Date        2022/6/27  11:18
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class AlbumSelectAdapter(context: Context,albumList : MutableList<AlbumSelectItem>) :
    RecyclerView.Adapter<AlbumSelectAdapter.ViewHolder>() {

    private val mContext = context
    private val mInflater = LayoutInflater.from(mContext)
    private var mAlbumList = albumList
    private var mItemClickListener : OnItemClickListener? = null

    inner class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView){
        val imageView = itemView.lsq_album_item_image
        val selectTab = itemView.lsq_album_item_select_tab
        val selectView = itemView.lsq_album_item_select
        val durationView = itemView.lsq_album_item_duration_or_type
    }

    interface OnItemClickListener {
        fun onClick(view: View, item: AlbumSelectItem, position: Int)

        fun onSelect(view : View,item:AlbumSelectItem,position: Int)
    }

    public fun setItemClickListener(itemClickListener: OnItemClickListener){
        mItemClickListener = itemClickListener
    }

    public fun setAlbumList(albumList : MutableList<AlbumSelectItem>){
        mAlbumList = albumList
        notifyDataSetChanged()
    }

    public fun getAlbumList(): MutableList<AlbumSelectItem> {
        return mAlbumList;
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = mInflater.inflate(R.layout.album_select_item,parent,false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = mAlbumList[position]
        Glide.with(mContext).load(currentItem.albumInfo.path).into(holder.imageView)
        if (currentItem.count > 0){
            holder.selectTab.visibility = View.VISIBLE
        } else {
            holder.selectTab.visibility = View.GONE
        }

        when(currentItem.albumInfo.type){
            AlbumItemType.Image -> {
                holder.durationView.text = "图片"
            }
            AlbumItemType.Video -> {
                holder.durationView.text = String.format("%02d:%02d", currentItem.albumInfo.duration / 1000 / 60, currentItem.albumInfo.duration / 1000 % 60)
            }
        }

        holder.imageView.setOnClickListener {
            mItemClickListener?.onClick(holder.imageView,currentItem,position)
        }

        holder.selectView.setOnClickListener {
            mItemClickListener?.onSelect(holder.selectView,currentItem,position)
        }


    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()){
            super.onBindViewHolder(holder, position, payloads)
        } else {
            for (p in payloads){
                if (p is Int){
                    val payload : Int = p
                    if (payload == -1){
                        val currentItem = mAlbumList[position]
                        if (currentItem.count > 0){
                            holder.selectTab.visibility = View.VISIBLE
                        } else {
                            holder.selectTab.visibility = View.GONE
                        }
                    }
                }
            }

        }

    }

    override fun getItemCount(): Int {
        return mAlbumList.size
    }
}