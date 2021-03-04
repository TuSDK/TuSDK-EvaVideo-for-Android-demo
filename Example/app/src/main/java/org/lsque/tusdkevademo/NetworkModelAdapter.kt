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
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.SparseArray
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.transcode.TranscoderRegistry
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.target.Target.SIZE_ORIGINAL
import com.bumptech.glide.request.transition.Transition
import org.lasque.tusdk.core.TuSdkContext
import org.lasque.tusdk.core.struct.TuSdkSize
import org.lasque.tusdk.core.utils.TLog
import org.lasque.tusdk.core.utils.image.BitmapHelper
import org.lasque.tusdk.core.view.TuSdkViewHelper
import java.util.*
import java.io.IOException
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class NetworkModelAdapter(
    context: Context,
    modelList: ArrayList<ModelItem>,
    downloadMap: SparseArray<Boolean>,
    mDownloadMap: HashMap<String, Int>
) : RecyclerView.Adapter<NetworkModelAdapter.ViewHolder>() {

    private val mContext: Context = context
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private var mModelList: ArrayList<ModelItem> = modelList
    private val mDownloadMap = downloadMap
    private var mCurrentDownloadMap: HashMap<String, Int> = mDownloadMap

    private var mItemClickListener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, p1: Int): ViewHolder {
        return ViewHolder(mInflater.inflate(R.layout.network_model_list_item, parent, false))
    }

    public fun setModelList(modelList: ArrayList<ModelItem>) {
        this.mModelList = modelList
        notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long {
        return mModelList[position].hashCode().toLong()
    }

    override fun getItemCount(): Int {
        return mModelList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, p1: Int) {
        if (mDownloadMap[p1] == null) mDownloadMap.put(p1, false)
        val mModelPath = mContext.getSharedPreferences("EVA-DOWNLOAD", Context.MODE_PRIVATE)
            .getString(mModelList[p1]!!.modelDownloadUrl, "")
        val mModelVer = mContext.getSharedPreferences("EVA-DOWNLOAD", Context.MODE_PRIVATE)
            .getString("${mModelList[p1].modelId}_ver", "")
        if (!TextUtils.equals(mModelVer, mModelList[p1].modelVer)) mDownloadMap.put(p1, false)
        holder!!.mTextView.text = mModelList[p1].modelName
        holder.itemView.setOnClickListener(object : TuSdkViewHelper.OnSafeClickListener(1000) {
            override fun onSafeClick(v: View?) {
                if (mDownloadMap[p1])
                    mItemClickListener?.onClick(holder.itemView, mModelList[p1], p1)
                else mItemClickListener?.onDownloadClick(
                    holder,
                    holder.mDownloadCircleImageView,
                    mModelList[p1],
                    p1
                )

            }
        })
        if (mModelList[p1].iconName == "cover.jpg") {
            Glide.with(mContext)
                .asBitmap()
                .load("file:///android_asset/${mModelList[p1].modelDir}/cover.jpg")
                .placeholder(R.drawable.logo_set)
                .into(holder.mImageView)
        } else {
            Glide.with(mContext)
                .asBitmap()
                .load("http://files.tusdk.com/eva/${mModelList[p1].iconName}")
                .placeholder(R.drawable.logo_set).error(R.drawable.logo_set)
                .into(holder.mImageView)
        }
        if (!TextUtils.isEmpty(mModelPath) && mModelVer == mModelList[p1].modelVer) {
            mDownloadMap.put(p1, true)
            mModelList[p1]!!.modelDownloadFilePath = mModelPath
            holder.mDownloadImageView.visibility = View.GONE
            holder.mDownloadCircleImageView.visibility = View.INVISIBLE
        } else
            if (isFileExists(mModelList[p1].fileName, mModelList[p1].modelDir)) {
                mDownloadMap.put(p1, true)
                holder.mDownloadImageView.visibility = View.GONE
                holder.mDownloadCircleImageView.visibility = View.INVISIBLE
            } else {
                holder.mDownloadCircleImageView.visibility = View.INVISIBLE
                holder.mDownloadImageView.visibility = View.VISIBLE
            }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            var payload: Int = payloads[0] as Int
            if (payload == -1) {
                mDownloadMap.put(position, true)
                holder.mDownloadCircleImageView.postDelayed({
                    if (holder.mDownloadCircleImageView.animation != null) holder.mDownloadCircleImageView.animation.cancel()
                    holder.mDownloadCircleImageView.visibility = View.INVISIBLE
                    holder.mDownloadImageView.visibility = View.GONE
                    holder.mDownloadCircleImageView.postInvalidate()
                }, 500)
                TLog.d("[Debug] Download-Over")
            }
        }
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (holder.mDownloadCircleImageView.tag == "NeedAddAnimation") {
            val mRotateAnimation: Animation = RotateAnimation(
                0f,
                360.0f,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f
            )
            mRotateAnimation.fillAfter = true
            mRotateAnimation.interpolator = LinearInterpolator()
            mRotateAnimation.duration = 1200
            mRotateAnimation.repeatCount = Animation.INFINITE
            mRotateAnimation.repeatMode = Animation.RESTART
            holder.mDownloadCircleImageView.visibility = View.VISIBLE
            holder.mDownloadCircleImageView.animation = mRotateAnimation
            holder.mDownloadCircleImageView.animation.startNow()
        }
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)
    }

    public fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        mItemClickListener = onItemClickListener
    }

    private fun isFileExists(filename: String, dirName: String): Boolean {
        val assetManager = mContext.assets
        try {
            val names = assetManager.list(dirName)
            for (i in names.indices) {
                if (names[i] == filename.trim { it <= ' ' }) {
                    return true
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        return false
    }


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        public val mImageView: ImageView = itemView.findViewById(R.id.lsq_model_icon) as ImageView
        public val mTextView: TextView = itemView.findViewById(R.id.lsq_model_name) as TextView
        public val mDownloadImageView: ImageView =
            itemView.findViewById(R.id.lsq_model_download) as ImageView
        public val mDownloadCircleImageView: ImageView =
            itemView.findViewById(R.id.lsq_model_pre_loading) as ImageView
    }

    /**
     * Item点击事件
     */
    interface OnItemClickListener {
        fun onClick(view: View, item: ModelItem, position: Int)
        fun onDownloadClick(
            holder: ViewHolder,
            progressView: ImageView,
            item: ModelItem,
            position: Int
        )
    }
}