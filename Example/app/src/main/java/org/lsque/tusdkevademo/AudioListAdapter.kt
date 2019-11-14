/**
 *  TuSDK
 *  droid-sdk-eva$
 *  org.lsque.tusdkevademo$
 *  @author  H.ys
 *  @Date    2019/7/2$ 13:27$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lsque.tusdkevademo

import android.content.Context
import android.graphics.Color
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.jetbrains.anko.find
import org.jetbrains.anko.textColor


class AudioListAdapter(context: Context, audioList: List<AudioItem>) : RecyclerView.Adapter<AudioListAdapter.ViewHolder>(){

    private var mCurrentSelectItem = -1

    var mAudioList = audioList
    var mContext = context
    var mInflater : LayoutInflater = LayoutInflater.from(mContext)
    var mItemClickListener : OnItemClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, p1: Int): ViewHolder {
        return ViewHolder(mInflater.inflate(R.layout.item_audio_list,parent,false))
    }

    override fun getItemCount(): Int {
        return mAudioList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var currentItem = mAudioList[position]
        holder!!.textView.text = currentItem.audioName
        holder!!.textView.textColor = if (position == mCurrentSelectItem){Color.parseColor("#007aff")} else {Color.parseColor("#ffffff")}
        holder!!.itemView.setOnClickListener {
            if (mCurrentSelectItem != position){
                var preItemIndex = mCurrentSelectItem
                mCurrentSelectItem = position
                notifyItemChanged(preItemIndex)
                notifyItemChanged(mCurrentSelectItem)
                mItemClickListener!!.onSelect(holder.itemView,currentItem,position)
            } else {
                mItemClickListener!!.onClick(holder.itemView,currentItem,position)
            }
        }
    }


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.find(R.id.lsq_audio_name)
    }

    public fun setOnItemClickListener(itemClickListener: OnItemClickListener){
        this.mItemClickListener = itemClickListener
    }

    interface OnItemClickListener {
        fun onClick(view: View, item: AudioItem, position: Int)
        fun onSelect(view:View,item:AudioItem,position: Int)
    }

}