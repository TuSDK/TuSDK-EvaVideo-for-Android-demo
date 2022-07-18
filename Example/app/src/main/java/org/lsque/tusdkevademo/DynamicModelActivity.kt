/**
 *  TuSDK
 *  EvaDemo$
 *  org.lsque.tusdkevademo$
 *  @author  H.ys
 *  @Date    2022/4/7$ 10:31$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lsque.tusdkevademo

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.tusdk.pulse.Engine
import com.tusdk.pulse.utils.AssetsMapper
import kotlinx.android.synthetic.main.activity_dynamic_model.*
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.startActivityForResult
import org.lasque.tusdkpulse.core.TuSdkContext
import org.lasque.tusdkpulse.core.utils.FileHelper
import org.lasque.tusdkpulse.core.utils.TLog
import org.lsque.tusdkevademo.utils.PermissionUtils
import java.io.File

/**
 * TuSDK
 * org.lsque.tusdkevademo
 * EvaDemo
 *
 * @author        H.ys
 * @Date        2022/4/7  10:31
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class DynamicModelActivity : ScreenAdapterActivity(){

    private var mCurrentModelPath = ""

    companion object{

        public const val ALBUM_REQUEST_CODE_DYNAMCI = 5;

    }

    private val CONTENT_RESULT_CODE = 100

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data == null) return
        if (requestCode == CONTENT_RESULT_CODE && resultCode == Activity.RESULT_OK){
            val uri = data!!.getData()
            var file : File? = null

            val sp = getSharedPreferences("fileLoader",Context.MODE_PRIVATE)
            if (sp.contains(uri.toString())){
                file = File(sp.getString(uri.toString(),""))
            } else {
                if (uri != null){
                    val input = contentResolver.openInputStream(uri)
                    val path = getTempOutputPath()
                    file = File(path)
                    val output = file.outputStream()
                    FileHelper.copy(input,output)
                    FileHelper.safeClose(input)
                    FileHelper.safeClose(output)
                    sp.edit().putString(uri.toString(),path).apply()
                }
            }

            if (file != null) {
                mCurrentModelPath = file.absolutePath

                startActivityForResult<AlbumActivity>(ALBUM_REQUEST_CODE_DYNAMCI,"maxSize" to 99)
            }
        } else {
            when(requestCode){
                ALBUM_REQUEST_CODE_DYNAMCI->{
                    val items = data!!.extras!!.getSerializable("itemPaths")
                    if (items!= null){
                        startActivity<DynamicModelEditorActivity>("itemPaths" to items,"modelPath" to mCurrentModelPath)
                    }
                }

            }
        }


    }

    private fun getTempOutputPath(): String {
        return TuSdkContext.getAppCacheDir(
            "evacache",
            false
        ).absolutePath + "/eva_temp" + System.currentTimeMillis() + ".eva"
    }

    val LAYOUT_ID : Int = R.layout.activity_dynamic_model

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LAYOUT_ID)
        val mEngine = Engine.getInstance()
        mEngine.init(null)

        initView()

    }

    private fun initView(){
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_WIFI_STATE)
        PermissionUtils.requestRequiredPermissions(this, permissions)

        val sp = getSharedPreferences("dyn",Context.MODE_PRIVATE)
        if (!sp.contains("dynamic-model-path")){
            val mapper = AssetsMapper(this)
            val path = mapper.mapAsset("dyn_test.eva")
            sp.edit().putString("dynamic-model-path",path).apply()
        }

        val model_path = sp.getString("dynamic-model-path","")!!
        if (model_path.isEmpty()){
            TLog.e("动态模板不存在")
            finish()
        }
        mCurrentModelPath = model_path
        lsq_dyn_test_model.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent,CONTENT_RESULT_CODE)
        }

    }

}