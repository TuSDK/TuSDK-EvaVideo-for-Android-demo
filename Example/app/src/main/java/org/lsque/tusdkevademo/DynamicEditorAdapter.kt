/**
 *  TuSDK
 *  EvaDemo$
 *  org.lsque.tusdkevademo$
 *  @author  H.ys
 *  @Date    2022/4/8$ 10:09$
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
import com.tusdk.pulse.eva.EvaModel
import org.jetbrains.anko.find
import java.util.*

/**
 * TuSDK
 * org.lsque.tusdkevademo
 * EvaDemo
 *
 * @author        H.ys
 * @Date        2022/4/8  10:08
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class DynamicEditorAdapter(context : Context,itemList : LinkedList<EvaModel.DynReplaceItem>) :
    RecyclerView.Adapter<DynamicEditorAdapter.ViewHolder>() {

    protected val STORAGE = "/storage/"

    private val mContext = context

    private val mInflater: LayoutInflater = LayoutInflater.from(context)

    private val mItemList : LinkedList<EvaModel.DynReplaceItem> = itemList

    inner class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView){
        val imageView = itemView.find<ImageView>(R.id.lsq_editor_icon)
        val textView = itemView.find<TextView>(R.id.lsq_editor_icon_num)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val viewHolder = ViewHolder(mInflater.inflate(R.layout.item_editor_image,parent,false))
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(mContext).load(mItemList[position].resPath).into(holder.imageView)

        holder.textView.setText(position.toString())
    }

    override fun getItemCount(): Int {
        return mItemList.size
    }

}