/**
 *  TuSDK
 *  droid-sdk-eva
 *  org.lsque.tusdkevademo
 *  @author  H.ys
 *  @Date    2019/7/1 13:20
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lsque.tusdkevademo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ColorSpace
import android.os.Build
import android.text.TextUtils
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import org.jetbrains.anko.find
import org.jetbrains.anko.textColor
import org.lasque.tusdk.core.utils.StringHelper
import org.lasque.tusdk.eva.EvaAsset
import org.lasque.tusdk.eva.TuSdkEvaImageEntity
import org.lasque.tusdk.eva.TuSdkEvaTextEntity
import org.lasque.tusdk.eva.TuSdkEvaVideoEntity
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.util.*


class ModelEditorAdapter(context: Context, modelList: LinkedList<EditorModelItem>) : RecyclerView.Adapter<ModelEditorAdapter.ViewHolder>() {

    protected val STORAGE = "/storage/"

    private val mContext = context
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private var mModelList: LinkedList<EditorModelItem> = modelList

    private var mItemClickListener: OnItemClickListener? = null

    private val IMAGE_TYPE = 1
    private val TEXT_TYPE = 2
    private val VIDEO_TYPE = 3
    private val ALPHA_VIDEO_TYPE = 4

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val viewHolder = when (viewType) {
            IMAGE_TYPE -> {
                ImageViewHolder(mInflater.inflate(R.layout.item_editor_image, parent, false))
            }
            TEXT_TYPE -> {
                TextViewHolder(mInflater.inflate(R.layout.item_editor_text, parent, false))
            }
            VIDEO_TYPE -> {
                ImageViewHolder(mInflater.inflate(R.layout.item_editor_image, parent, false))
            }
            else -> {
                null
            }
        }
        return viewHolder!!
    }

    override fun getItemCount(): Int {
        return mModelList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        when (mModelList[position].modelType) {
            EditType.Image -> {
                val item = mModelList[position]
                (holder as ImageViewHolder).textView.text = mContext.getString(R.string.lsq_editor_item_image)
                holder!!.itemView.setOnClickListener { mItemClickListener!!.onClick(holder.itemView, mModelList[position].modelItem as TuSdkEvaImageEntity, position, EditType.Image) }

                var imageEntriy = (item.modelItem as TuSdkEvaImageEntity)
                val loadImage = imageEntriy.loadImage()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    loadImage.isPremultiplied = true
                }
                holder.imageView.setImageBitmap(loadImage)
            }

            EditType.Text -> {
                val item = mModelList[position]
                val textEntity = item.modelItem as TuSdkEvaTextEntity
                val itemTextView = (holder as TextViewHolder).textView
                itemTextView.text = (item.modelItem as TuSdkEvaTextEntity).displayText
                itemTextView.textColor = if(TextUtils.isEmpty(textEntity.replaceText)) Color.parseColor("#555555") else Color.parseColor("#ffffff")
                holder!!.itemView.setOnClickListener { mItemClickListener!!.onClick(holder.itemView, mModelList[position].modelItem as TuSdkEvaTextEntity, position, EditType.Text) }
            }


            EditType.Video,EditType.Alpha-> {
                holder!!.itemView.setOnClickListener { mItemClickListener!!.onClick(holder.itemView, mModelList[position].modelItem as TuSdkEvaVideoEntity, position, EditType.Video) }
                val item = mModelList[position]
                val currentItem = item.modelItem as TuSdkEvaVideoEntity

                var showText = ""
                if (currentItem.assetType == EvaAsset.TuSdkEvaAssetType.EvaVideoImage) {
                    showText =mContext.getString(R.string.lsq_editor_item_video_image)
                } else if (currentItem.assetType == EvaAsset.TuSdkEvaAssetType.EvaOnlyVideo) {
                    showText = mContext.getString(R.string.lsq_editor_item_video)
                } else if (currentItem.assetType == EvaAsset.TuSdkEvaAssetType.EvaAlphaVideo){
                    showText = "Alpha"
                } else if (currentItem.assetType == EvaAsset.TuSdkEvaAssetType.EvaOnlyImage){
                    showText = mContext.getString(R.string.lsq_editor_item_image)
                }

                (holder as ImageViewHolder).textView.text = showText

                if (StringHelper.isBlank(currentItem.imagePath)) {

                    if (StringHelper.isBlank(currentItem.videoPath)){
                        val loadImage = currentItem.loadImageAssetBitmap()
                        if (loadImage != null){
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                loadImage.isPremultiplied = true
                            }
                            holder.imageView.setImageBitmap(loadImage)
                        }
                    } else if (currentItem.videoPath.startsWith(STORAGE)) {
                        Glide.with(mContext).asBitmap().load(currentItem.videoPath).into((holder.imageView))
                    } else {
                        Glide.with(mContext).asBitmap().load("file:///android_asset/${currentItem.videoPath}").into((holder.imageView))
                    }

                } else {
                    val loadImage = currentItem.loadImageAssetBitmap()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        loadImage.isPremultiplied = true
                    }
                    holder.imageView.setImageBitmap(loadImage)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (mModelList[position].modelType) {
            EditType.Image -> {
                IMAGE_TYPE
            }
            EditType.Text -> {
                TEXT_TYPE
            }
            EditType.Video -> {
                VIDEO_TYPE
            }
            EditType.Alpha ->{
                VIDEO_TYPE
            }
        }
    }

    public fun setEditorModelList(modelList: LinkedList<EditorModelItem>) {
        mModelList = modelList
        notifyDataSetChanged()
    }

    public fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        mItemClickListener = onItemClickListener
    }

    abstract inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    inner class ImageViewHolder(itemView: View) : ViewHolder(itemView) {
        val imageView = itemView.find<ImageView>(R.id.lsq_editor_icon)
        val textView = itemView.find<TextView>(R.id.lsq_editor_icon_num)
    }

    inner class TextViewHolder(itemView: View) : ViewHolder(itemView) {
        val textView = itemView.find<TextView>(R.id.lsq_edior_text)
    }


    interface OnItemClickListener {
        fun onClick(view: View, item: TuSdkEvaImageEntity, position: Int, type: EditType)
        fun onClick(view: View, item: TuSdkEvaVideoEntity, position: Int, type: EditType)
        fun onClick(view: View, item: TuSdkEvaTextEntity, position: Int, type: EditType)
    }


}