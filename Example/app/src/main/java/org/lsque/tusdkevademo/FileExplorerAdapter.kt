/**
 *  TuSDK
 *  EvaDemo$
 *  org.lsque.tusdkevademo$
 *  @author  H.ys
 *  @Date    2022/2/9$ 14:53$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lsque.tusdkevademo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkpulse.core.view.TuSdkViewHelper
import java.io.File
import java.io.FilenameFilter

/**
 * TuSDK
 * org.lsque.tusdkevademo
 * EvaDemo
 *
 * @author        H.ys
 * @Date        2022/2/9  14:53
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class FileExplorerAdapter(
    context: Context,
    parentFile : File
) : RecyclerView.Adapter<FileExplorerAdapter.ViewHolder>() {

    private val mContext : Context = context
    private var mParentFile : File = parentFile
    private var mItemClickListener : OnItemClickListener? = null
    private val mInflater : LayoutInflater = LayoutInflater.from(mContext)

    private var mFileList : MutableList<File> = if (parentFile.isHidden || parentFile.list() == null){
        ArrayList<File>()} else {
        val list = ArrayList<File>(parentFile.listFiles().size)
        list.addAll(parentFile.listFiles(object : FilenameFilter{
            override fun accept(dir: File?, name: String?): Boolean {
                if (name == null) return false;
                return if (name.endsWith(".eva")){
                    true
                } else{
                    false
                }
            }
        }))
        list
        }


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        public val mFileNameView : TextView = itemView.findViewById(R.id.lsq_file_explorer_item)

    }

    interface OnItemClickListener{

        fun onFileClick(file : File)
        fun onFolderClick(file : File)
        fun onFolderReturn(file: File);

    }

    public fun setOnItemClickListener(listener: OnItemClickListener){
        mItemClickListener = listener
    }

    public fun updateParentFile(parentFile: File){
        mParentFile = parentFile
        mFileList = if (parentFile.isHidden || parentFile.list() == null){
            ArrayList<File>()} else {
            val list = ArrayList<File>(parentFile.list().size)
            list.addAll(parentFile.listFiles()!!)
            list
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(mInflater.inflate(R.layout.file_explorer_item,parent,false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val itemFile = mFileList[holder.adapterPosition]
        if (itemFile.path.equals("..")){
            holder.mFileNameView.setText("..")
        } else {
            holder.mFileNameView.setText(itemFile.name)
        }


        if (!holder.itemView.hasOnClickListeners()){
            holder.itemView.setOnClickListener(object : TuSdkViewHelper.OnSafeClickListener(1000){
                override fun onSafeClick(v: View?) {
                    if (itemFile.path.equals("..")){
                        mItemClickListener?.onFolderReturn(mParentFile.parentFile)
                    } else if (itemFile.isFile){
                        mItemClickListener?.onFileClick(itemFile)
                    } else {
                        mItemClickListener?.onFolderClick(itemFile)
                    }
                }

            })
        }
    }

    override fun getItemCount(): Int {
        return mFileList.size
    }
}