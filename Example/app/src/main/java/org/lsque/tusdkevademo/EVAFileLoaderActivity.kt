/**
 *  TuSDK
 *  EvaDemo$
 *  org.lsque.tusdkevademo$
 *  @author  H.ys
 *  @Date    2022/3/14$ 16:24$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lsque.tusdkevademo

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.loader.content.CursorLoader
import com.tusdk.pulse.Engine
import org.jetbrains.anko.startActivity
import org.lasque.tusdkpulse.core.TuSdkContext
import org.lasque.tusdkpulse.core.utils.FileHelper
import org.lasque.tusdkpulse.core.utils.TLog
import org.lsque.tusdkevademo.albumselect.AlbumSelectActivity
import java.io.File


/**
 * TuSDK
 * org.lsque.tusdkevademo
 * EvaDemo
 *
 * @author        H.ys
 * @Date        2022/3/14  16:24
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class EVAFileLoaderActivity : ScreenAdapterActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val engine = Engine.getInstance()
        engine.init(null)

        val intent = intent
        val uri = intent.data

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
            val item = ModelItem(
                file.absolutePath,
                uri!!.lastPathSegment!!,
                uri!!.lastPathSegment!!,
                file.absolutePath,
                file.name,
                file.absolutePath,
                file.name,
                "1.0.0",
                0
            )
//            startActivity<ModelDetailActivity>("model" to item)

            startActivity<AlbumSelectActivity>("model-path" to item.modelDir)
        }
        finish()

    }

    private fun getTempOutputPath(): String {
        return TuSdkContext.getAppCacheDir(
            "evacache",
            false
        ).absolutePath + "/eva_temp" + System.currentTimeMillis() + ".eva"
    }


}