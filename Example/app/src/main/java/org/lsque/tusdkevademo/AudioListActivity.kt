/**
 *  TuSDK
 *  droid-sdk-eva$
 *  org.lsque.tusdkevademo$
 *  @author  H.ys
 *  @Date    2019/7/2$ 12:42$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lsque.tusdkevademo

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_audio_list.*
import org.lasque.tusdk.core.utils.TLog
import java.lang.Exception


class AudioListActivity : ScreenAdapterActivity() {

    private var mMediaPlayer : MediaPlayer = MediaPlayer()

    private var mAudioPath : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_list)
        initView()
    }

    private fun initView() {
        lsq_audio_list.layoutManager = LinearLayoutManager(this)
        val audioAdapter = AudioListAdapter(this,AudioItemFactory.getAudioItemList(this))
        audioAdapter.setOnItemClickListener(object : AudioListAdapter.OnItemClickListener{
            override fun onSelect(view: View, item: AudioItem, position: Int) {
                try {
                    mAudioPath = item.audioPath
                    mMediaPlayer.release()
                    mMediaPlayer = MediaPlayer()
                    val audioFd = assets.openFd(item.audioPath)
                    mMediaPlayer.setDataSource(audioFd.fileDescriptor,audioFd.startOffset,audioFd.length)
                    mMediaPlayer.isLooping = true
                    mMediaPlayer.prepare()
                    mMediaPlayer.start()
                } catch (e: Exception){
                    TLog.e(e)
                }
            }

            override fun onClick(view: View, item: AudioItem, position: Int) {
                mMediaPlayer.stop()
                val intent = intent
                val bundle = Bundle()
                intent.setClass(this@AudioListActivity,ModelEditorActivity.javaClass)
                bundle.putString("audioPath",item.audioPath)
                intent.putExtras(bundle)
                setResult(33,intent)
                finish()
            }
        })
        lsq_audio_list.adapter = audioAdapter
        lsq_close.setOnClickListener { finish() }
    }

    override fun finish() {
        if (mAudioPath != null){
            val intent = intent
            val bundle = Bundle()
            intent.setClass(this@AudioListActivity,ModelEditorActivity.javaClass)
            bundle.putString("audioPath",mAudioPath)
            intent.putExtras(bundle)
            setResult(33,intent)
        }
        overridePendingTransition(0,R.anim.activity_close_from_top_to_bottom)
        super.finish()
        mMediaPlayer.release()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
    }
}