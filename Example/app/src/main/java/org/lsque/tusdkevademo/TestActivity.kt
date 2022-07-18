/**
 *  TuSDK
 *  EvaDemo$
 *  org.lsque.tusdkevademo$
 *  @author  H.ys
 *  @Date    2022/4/15$ 14:08$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lsque.tusdkevademo

import android.os.Bundle
import android.os.Environment
import com.tusdk.pulse.DispatchQueue
import com.tusdk.pulse.Engine
import com.tusdk.pulse.Producer
import com.tusdk.pulse.eva.EvaModel
import com.tusdk.pulse.eva.EvaProducer
import org.jetbrains.anko.toast
import org.lasque.tusdkpulse.core.utils.TLog
import java.io.File
import java.io.FilenameFilter
import java.util.concurrent.Semaphore

/**
 * TuSDK
 * org.lsque.tusdkevademo
 * EvaDemo
 *
 * @author        H.ys
 * @Date        2022/4/15  14:08
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class TestActivity : ScreenAdapterActivity(){

    var mRootFile : File? = null

    var mOutputRootFile : File? = null

    val queue = DispatchQueue()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mEngine = Engine.getInstance()
        mEngine.init(null)

        initModels()
    }

    private fun initModels() {

        val testRunnable = Runnable {
            mRootFile = File((String.format("%s/TuSDK/TestModels",getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS))))

            if (!mRootFile!!.exists()) mRootFile!!.mkdirs()

            mOutputRootFile = File((String.format("%s/TuSDK/TestModelsOutput",getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS))))

            if (!mOutputRootFile!!.exists()) mOutputRootFile!!.mkdirs()

            TLog.e("[Debug] %s path %s %s",mRootFile!!.absolutePath,mOutputRootFile!!.absolutePath,"")


            val testModels = mRootFile!!.listFiles(object : FilenameFilter{
                override fun accept(dir: File?, name: String?): Boolean {
                    return name!!.endsWith(".eva")
                }

            })

            val lock = Semaphore(0)

            for (item in testModels){
                val outputPath = File(mOutputRootFile,"${item.name}_eva_output${System.currentTimeMillis()}.mp4").absolutePath

                val model = EvaModel()
                model.create(item.absolutePath)

                val producer = EvaProducer()
                producer.init(outputPath, model)

                val config = Producer.OutputConfig()
                producer.setOutputConfig(config)

                producer.setListener { state, ts ->
                    runOnUiThread {
                        toast("${item.name} 当前进度 ${ts}")
                    }

                    if (state == Producer.State.kEND){
                        lock.release()
                    }
                }

                producer.start()

                lock.acquire()

                producer.cancel()
                producer.release()

                runOnUiThread {
                    toast("${item.name} 导出结束")
                }
            }

            runOnUiThread {
                toast("全部文件输出完成")
            }
        }


        queue.runAsync { testRunnable.run() }



    }
}