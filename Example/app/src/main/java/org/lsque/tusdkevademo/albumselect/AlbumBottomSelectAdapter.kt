/**
 *  TuSDK
 *  EvaDemo$
 *  org.lsque.tusdkevademo.albumselect$
 *  @author  H.ys
 *  @Date    2022/6/27$ 15:29$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lsque.tusdkevademo.albumselect

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.tusdk.pulse.eva.EvaModel
import com.tusdk.pulse.eva.EvaReplaceConfig
import kotlinx.android.synthetic.main.album_bottom_select_item.view.*
import org.lasque.tusdkpulse.core.TuSdkContext
import org.lasque.tusdkpulse.core.struct.TuSdkSize
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkpulse.core.utils.image.BitmapHelper
import org.lasque.tusdkpulse.core.utils.image.ImageOrientation
import org.lsque.tusdkevademo.EditorModelItem
import org.lsque.tusdkevademo.R
import java.text.DecimalFormat

/**
 * TuSDK
 * org.lsque.tusdkevademo.albumselect
 * EvaDemo
 *
 * @author        H.ys
 * @Date        2022/6/27  15:29
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class AlbumBottomSelectAdapter(context : Context,selectList : MutableList<EditorModelItem>,configMap : HashMap<Any, Any>) : RecyclerView.Adapter<AlbumBottomSelectAdapter.ViewHolder>() {

    interface OnItemClickListener{
        fun onClick(modelItem : EditorModelItem,albumItem : AlbumSelectItem,position: Int)
        fun onUnselected(modelItem : EditorModelItem,albumItem : AlbumSelectItem,position: Int)
        fun onSelected(modelItem: EditorModelItem,position: Int)
    }

    private var mModelMap = HashMap<EditorModelItem,AlbumSelectItem>()
    private var mModelList = selectList
    private var mContext = context
    private var mInflater = LayoutInflater.from(mContext)
    private var mConfigMap = configMap

    private var mInsertArray : Array<Boolean> = Array(mModelList.size) { _ -> false }

    private var mCurrentSelectPos = 0

    private var mOnItemClickListener : OnItemClickListener? = null

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView = itemView.lsq_select_image
        val unselectedView = itemView.lsq_select_unselected
        val sloganView = itemView.lsq_select_item_slogan
        val typeView = itemView.lsq_select_type
        val durationView = itemView.lsq_select_duration

        val realUnselectedView = itemView.lsq_select_unselected_real_click
    }

    public fun setOnItemClickListener(itemClickListener: OnItemClickListener){
        mOnItemClickListener = itemClickListener
    }

    public fun nextSelectPos(){
        if (mCurrentSelectPos == mInsertArray.size - 1){
            var findSuccess = false
            for (i in mInsertArray.indices){
                if (!mInsertArray[i]){
                    mCurrentSelectPos = i
                    findSuccess = true
                    break
                }
            }
            if (!findSuccess){
                mCurrentSelectPos = -1
            }
        } else {
            for (i in mCurrentSelectPos until mInsertArray.size){
                if (!mInsertArray[i]){
                    mCurrentSelectPos = i
                    return
                }
            }

            for (i in 0 until mCurrentSelectPos){
                if (!mInsertArray[i]){
                    mCurrentSelectPos = i
                    return
                }
            }
        }
    }

    public fun setSelectPos(pos : Int){
        val preSelectPos = mCurrentSelectPos
        mCurrentSelectPos = pos
        notifyItemChanged(preSelectPos)
        notifyItemChanged(mCurrentSelectPos)
    }

    public fun getSelectPos() : Int{
        return mCurrentSelectPos
    }

    public fun isFill() : Boolean{
        for (i in mInsertArray.indices){
            if (!mInsertArray[i]) return false
        }
        return true
    }

    public fun getModelMap() : HashMap<EditorModelItem,AlbumSelectItem>{
        return mModelMap
    }

    public fun bindAlbumItem(modelItem : EditorModelItem,albumItem : AlbumSelectItem){
        mModelMap.put(modelItem,albumItem)
        val index = mModelList.indexOf(modelItem)
        mInsertArray[index] = true
        val prePos = mCurrentSelectPos
        nextSelectPos()
        notifyItemChanged(prePos)
        notifyItemChanged(mCurrentSelectPos)
    }

    public fun unbindAlbumItem(modelItem : EditorModelItem){
        mModelMap.remove(modelItem)
        val prePos = mCurrentSelectPos
        mCurrentSelectPos = mModelList.indexOf(modelItem)
        mInsertArray[mCurrentSelectPos] = false
        notifyItemChanged(prePos)
        notifyItemChanged(mCurrentSelectPos)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = mInflater.inflate(R.layout.album_bottom_select_item,parent,false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = mModelList[position]
        if (mModelMap[currentItem] == null){

            if (position == mCurrentSelectPos){
                holder.itemView.setBackgroundResource(R.drawable.red_stroke_bg)
            } else {
                holder.itemView.setBackgroundResource(R.drawable.item_select_stroke_bg)
            }

            val modelItem = currentItem.modelItem as EvaModel.VideoReplaceItem
            val itemDuration = (modelItem.endTime - modelItem.startTime).toDouble() / 1000f
            holder.durationView.text = String.format("%.2fs",itemDuration);

            var showText = ""
            if (modelItem.type == EvaModel.AssetType.kIMAGE_VIDEO) {
                showText = mContext.getString(R.string.lsq_editor_item_video_image)
            } else if (modelItem.type == EvaModel.AssetType.kVIDEO_ONLY) {
                showText = mContext.getString(R.string.lsq_editor_item_video)
            } else if (modelItem.type == EvaModel.AssetType.kMASK){
                showText = "MASK"
            } else if (modelItem.type == EvaModel.AssetType.kIMAGE_ONLY){
                showText = mContext.getString(R.string.lsq_editor_item_image)
            }

            holder.typeView.text = showText

            holder.durationView.visibility = View.VISIBLE
            holder.sloganView.visibility = View.GONE
            holder.typeView.visibility = View.VISIBLE
            holder.unselectedView.visibility = View.GONE
            holder.realUnselectedView.visibility = View.GONE
            holder.imageView.visibility = View.GONE


            holder.itemView.setOnClickListener {
                mOnItemClickListener?.onSelected(currentItem,position)
            }
        } else {
            holder.durationView.visibility = View.GONE
            holder.sloganView.visibility = View.VISIBLE
            holder.typeView.visibility = View.GONE
            holder.unselectedView.visibility = View.VISIBLE
            holder.realUnselectedView.visibility = View.VISIBLE
            holder.imageView.visibility = View.VISIBLE

            holder.itemView.setBackgroundResource(0)


            val currentAlbumItem = mModelMap[currentItem]
            val modelItem = currentItem.modelItem as EvaModel.VideoReplaceItem

            val roundedCorners = RoundedCorners(TuSdkContext.dip2px(10f))
            val requestOption = RequestOptions.bitmapTransform(roundedCorners)

            Glide.with(mContext).asBitmap().load(currentAlbumItem!!.albumInfo.path)
                .apply(requestOption)
                .into(object : CustomViewTarget<ImageView,Bitmap>(holder.imageView){
                override fun onLoadFailed(errorDrawable: Drawable?) {
                    TLog.e("image load failed")
                }

                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    var result = resource
                    if (mConfigMap[modelItem.id] != null){
                        val size = TuSdkSize.create(result)
                        val config = mConfigMap[modelItem.id] as EvaReplaceConfig.ImageOrVideo
                        val targetCrop : Rect = Rect(
                            config.crop.left.times(size.width).toInt(),config.crop.top.times(size.height).toInt(),config.crop.right.times(size.width).toInt(),config.crop.bottom.times(size.height).toInt()
                        )
                        result = BitmapHelper.imgCorp(result,targetCrop,0f,ImageOrientation.Up)
                    }
                    holder.imageView.setImageBitmap(result)
                }

                override fun onResourceCleared(placeholder: Drawable?) {

                }

            })

            holder.realUnselectedView.setOnClickListener {
                mOnItemClickListener?.onUnselected(currentItem,currentAlbumItem,position)
            }

            holder.imageView.setOnClickListener {
                mOnItemClickListener?.onClick(currentItem,currentAlbumItem,position)
            }



        }
    }

    override fun getItemCount(): Int {
        return mModelList.size
    }
}