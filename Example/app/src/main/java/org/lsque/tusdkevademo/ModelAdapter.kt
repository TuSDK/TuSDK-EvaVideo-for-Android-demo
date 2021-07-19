/**
 *  TuSDK
 *  droid-sdk-eva$
 *  org.lsque.tusdkevademo$
 *  @author  H.ys
 *  @Date    2019/7/1$ 10:42$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lsque.tusdkevademo

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import org.lasque.tusdkpulse.core.view.TuSdkViewHelper
import java.util.*


class ModelAdapter(context:Context,modelList: ArrayList<ModelItem>) : RecyclerView.Adapter<ModelAdapter.ViewHolder>() {

    private val mContext:Context = context
    private val mInflater : LayoutInflater = LayoutInflater.from(context)
    private val mModelList : ArrayList<ModelItem> = modelList

    private var mItemClickListener : OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, p1: Int): ViewHolder {
        return ViewHolder(mInflater.inflate(R.layout.model_list_item,parent,false))
    }

    override fun getItemCount(): Int {
        return mModelList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, p1: Int) {
        holder!!.mTextView.text = mModelList[p1].modelName
        holder.itemView.setOnClickListener ( object : TuSdkViewHelper.OnSafeClickListener(1000){
            override fun onSafeClick(v: View?) {
                mItemClickListener?.onClick(holder.itemView,mModelList[p1],p1)
            }
        } )
        Glide.with(mContext)
                .load("file:///android_asset/${mModelList[p1].modelDir}/cover.jpg")
                .into(holder.mImageView)
    }

    public fun setOnItemClickListener(onItemClickListener: OnItemClickListener){
        mItemClickListener = onItemClickListener
    }


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        public val mImageView: ImageView = itemView.findViewById(R.id.lsq_model_icon) as ImageView
        public val mTextView : TextView = itemView.findViewById(R.id.lsq_model_name) as TextView
    }

    /**
     * Item点击事件
     */
    interface OnItemClickListener {
        fun onClick(view: View, item: ModelItem, position: Int)
    }
}