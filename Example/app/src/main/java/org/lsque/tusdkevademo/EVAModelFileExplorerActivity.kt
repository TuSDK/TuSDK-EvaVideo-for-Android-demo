/**
 *  TuSDK
 *  EvaDemo$
 *  org.lsque.tusdkevademo$
 *  @author  H.ys
 *  @Date    2022/2/9$ 14:29$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lsque.tusdkevademo

import android.Manifest
import android.os.Bundle
import android.os.Environment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.eva_file_explorer_activity.*
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast
import org.lasque.tusdkpulse.core.utils.TLog
import org.lsque.tusdkevademo.utils.PermissionUtils
import java.io.File

/**
 * TuSDK
 * org.lsque.tusdkevademo
 * EvaDemo
 *
 * @author        H.ys
 * @Date        2022/2/9  14:29
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class EVAModelFileExplorerActivity : ScreenAdapterActivity() {

    val LAYOUT_ID : Int = R.layout.eva_file_explorer_activity

    var mRootFile : File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LAYOUT_ID)
        mRootFile =  File(String.format("%s/TuSDK/EVAMODELS",getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)))
        initView()
    }

    private fun initView(){
        if (!mRootFile!!.exists()){
            mRootFile!!.mkdirs()
        }

        TLog.e("[Debug] %s path %s %s", TAG,mRootFile!!.absolutePath,mRootFile!!.exists())

        val fileExplorerAdapter = FileExplorerAdapter(this,mRootFile!!)
        fileExplorerAdapter.setOnItemClickListener(object : FileExplorerAdapter.OnItemClickListener{
            override fun onFileClick(file: File) {
                val item = ModelItem(file.absolutePath,file.name,file.name,file.absolutePath,file.name,file.absolutePath,file.name,"1.0.0",0)
                startActivity<ModelDetailActivity>("model" to item)
            }

            override fun onFolderClick(file: File) {
                fileExplorerAdapter.updateParentFile(file)
            }

            override fun onFolderReturn(file: File) {
                if (file.path.equals(mRootFile!!.parentFile.path)){
                    toast("已经到达根目录了!!")
                } else {
                    fileExplorerAdapter.updateParentFile(file)
                }
            }

        })

        lsq_file_explorer_list.layoutManager = LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false)
        lsq_file_explorer_list.adapter = fileExplorerAdapter

    }

    companion object {
        public const val TAG = "EVAModelFileExplorerActivity"
    }

}