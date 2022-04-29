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
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.TextUtils
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.tusdk.pulse.eva.EvaModel
import com.tusdk.pulse.eva.EvaReplaceConfig
import org.jetbrains.anko.find
import org.jetbrains.anko.textColor
import org.lasque.tusdkpulse.core.struct.TuSdkSize
import org.lasque.tusdkpulse.core.utils.AssetsHelper
import org.lasque.tusdkpulse.core.utils.StringHelper
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkpulse.core.utils.image.BitmapHelper
import org.lasque.tusdkpulse.core.utils.image.ImageOrientation
import java.io.File
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.util.*


class ModelEditorAdapter(context: Context, modelList: LinkedList<EditorModelItem>,configMap : HashMap<Any,Any>) : RecyclerView.Adapter<ModelEditorAdapter.ViewHolder>() {

    protected val STORAGE = "/storage/"

    private val mContext = context
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private var mModelList: LinkedList<EditorModelItem> = modelList
    private var mConfigMap = configMap

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
                var showText = ""
                val currentItem = item.modelItem as EvaModel.VideoReplaceItem
                if (currentItem.type == EvaModel.AssetType.kIMAGE_VIDEO) {
                    showText =mContext.getString(R.string.lsq_editor_item_video_image)
                } else if (currentItem.type == EvaModel.AssetType.kVIDEO_ONLY) {
                    showText = mContext.getString(R.string.lsq_editor_item_video)
                } else if (currentItem.type == EvaModel.AssetType.kMASK){
                    showText = "MASK"
                } else if (currentItem.type == EvaModel.AssetType.kIMAGE_ONLY){
                    showText = mContext.getString(R.string.lsq_editor_item_image)
                }

                (holder as ImageViewHolder).textView.text = showText
                holder!!.itemView.setOnClickListener { mItemClickListener!!.onImageItemClick(holder.itemView, mModelList[position].modelItem as EvaModel.VideoReplaceItem, position, EditType.Image) }

                var imageEntriy = (item.modelItem as EvaModel.VideoReplaceItem)
                TLog.e("current image path ${imageEntriy.resPath}")
                Glide.with(mContext).asBitmap().load(imageEntriy.resPath).into(object : CustomViewTarget<ImageView,Bitmap>(holder.imageView){
                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        TLog.e("image load failed")
                    }

                    override fun onResourceCleared(placeholder: Drawable?) {
                    }

                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        var result = resource
                        if (mConfigMap[imageEntriy.id] != null){
                            val size = TuSdkSize.create(result)
                            val config = mConfigMap[imageEntriy.id] as EvaReplaceConfig.ImageOrVideo
                            val targetCrop : Rect = Rect(
                                config.crop.left.times(size.width).toInt(),config.crop.top.times(size.height).toInt(),config.crop.right.times(size.width).toInt(),config.crop.bottom.times(size.height).toInt()
                            )
                            result = BitmapHelper.imgCorp(result,targetCrop,0f,ImageOrientation.Up)
                        }
                        holder.imageView.setImageBitmap(result)
                    }

                })
            }

            EditType.Text -> {
                val item = mModelList[position]
                val textEntity = item.modelItem as EvaModel.TextReplaceItem
                val itemTextView = (holder as TextViewHolder).textView
                itemTextView.text = textEntity.text
                itemTextView.textColor = if(TextUtils.isEmpty(textEntity.text)) Color.parseColor("#555555") else Color.parseColor("#ffffff")
                holder!!.itemView.setOnClickListener { mItemClickListener!!.onTextItemClick(holder.itemView, mModelList[position].modelItem as EvaModel.TextReplaceItem, position, EditType.Text) }
            }


            EditType.Video,EditType.Alpha-> {
                holder!!.itemView.setOnClickListener { mItemClickListener!!.onVideoItemClick(holder.itemView, mModelList[position].modelItem as EvaModel.VideoReplaceItem, position, EditType.Video) }
                val item = mModelList[position]
                val currentItem = item.modelItem as EvaModel.VideoReplaceItem

                var showText = ""
                if (currentItem.type == EvaModel.AssetType.kIMAGE_VIDEO) {
                    showText =mContext.getString(R.string.lsq_editor_item_video_image)
                } else if (currentItem.type == EvaModel.AssetType.kVIDEO_ONLY) {
                    showText = mContext.getString(R.string.lsq_editor_item_video)
                } else if (currentItem.type == EvaModel.AssetType.kMASK){
                    showText = "MASK"
                } else if (currentItem.type == EvaModel.AssetType.kIMAGE_ONLY){
                    showText = mContext.getString(R.string.lsq_editor_item_image)
                }

                (holder as ImageViewHolder).textView.text = showText

                if (currentItem.resPath.startsWith(STORAGE)) {
                    val orientation = BitmapHelper.getImageOrientation(currentItem.resPath)
                    Glide.with(mContext).asBitmap().load(currentItem.resPath)
                        .into(object : CustomViewTarget<ImageView,Bitmap>(holder.imageView){
                            override fun onLoadFailed(errorDrawable: Drawable?) {
                            }

                            override fun onResourceCleared(placeholder: Drawable?) {
                            }

                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                var result = resource
                                if (mConfigMap[currentItem.id] != null){
                                    val size = TuSdkSize.create(resource)
                                    val config = mConfigMap[currentItem.id] as EvaReplaceConfig.ImageOrVideo
                                    val targetCrop : Rect = Rect(
                                        config.crop.left.times(size.width).toInt(),config.crop.top.times(size.height).toInt(),config.crop.right.times(size.width).toInt(),config.crop.bottom.times(size.height).toInt()
                                    )
                                    result = BitmapHelper.imgCorp(resource,targetCrop,0f,ImageOrientation.Up)
                                }
                                view.setImageBitmap(result)
                            }

                        })
                } else if (AssetsHelper.hasAssets(mContext,currentItem.resPath)){
                    Glide.with(mContext).asBitmap().load("file:///android_asset/${currentItem.resPath}").into((holder.imageView))
                } else{
                    val loadImage = currentItem.thumbnail
                    if (loadImage != null){
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            loadImage.isPremultiplied = true
                        }
                        var result = loadImage
                        if (mConfigMap[currentItem.id] != null){
                            val size = TuSdkSize.create(loadImage)
                            val config = mConfigMap[currentItem.id] as EvaReplaceConfig.ImageOrVideo
                            val targetCrop : Rect = Rect(
                                config.crop.left.times(size.width).toInt(),config.crop.top.times(size.height).toInt(),config.crop.right.times(size.width).toInt(),config.crop.bottom.times(size.height).toInt()
                            )
                            result = BitmapHelper.imgCorp(loadImage,targetCrop,0f,ImageOrientation.Up)
                        }
                        holder.imageView.setImageBitmap(result)
                    }
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
        fun onImageItemClick(view: View, item: EvaModel.VideoReplaceItem, position: Int, type: EditType)
        fun onVideoItemClick(view: View, item: EvaModel.VideoReplaceItem, position: Int, type: EditType)
        fun onTextItemClick(view: View, item: EvaModel.TextReplaceItem, position: Int, type: EditType)
    }


}