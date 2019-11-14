/**
 *  TuSDK
 *  droid-sdk-eva$
 *  org.lsque.tusdkevademo$
 *  @author  H.ys
 *  @Date    2019/7/1$ 17:06$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lsque.tusdkevademo

import android.content.Intent
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.title_item_layout.*

class AlbumActivity : ScreenAdapterActivity() {

    private var mAlbumFragment: AlbumFragment = AlbumFragment()
    private var isOnlyImage = false
    private var isOnlyVideo = false
    private var currentWidth = 0
    private var currentHeight = 0
    private var videoDuration = 0f
    private var isAlpha = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.album_activity)
        isOnlyImage = intent.getBooleanExtra("onlyImage", false)
        isOnlyVideo = intent.getBooleanExtra("onlyVideo", false)
        currentWidth = intent.getIntExtra("width", 0)
        currentHeight = intent.getIntExtra("height", 0)
        videoDuration = intent.getFloatExtra("videoDuration",0f)
        isAlpha = intent.getBooleanExtra("isAlpha",false)
        var bundle = Bundle()
        bundle.putBoolean("onlyImage", isOnlyImage)
        bundle.putBoolean("onlyVideo", isOnlyVideo)
        bundle.putInt("width", currentWidth)
        bundle.putInt("height", currentHeight)
        bundle.putFloat("videoDuration",videoDuration)
        bundle.putBoolean("isAlpha",isAlpha)
        mAlbumFragment!!.arguments = bundle
        initView()
    }

    private fun initView() {
        supportFragmentManager.beginTransaction().add(R.id.lsq_album_fragment, mAlbumFragment).commit()
        lsq_back.setOnClickListener { finish() }
        lsq_next.visibility = View.GONE
        lsq_title_item_title.text = "素材选择"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
        setResult(resultCode, data)
        finish()
    }
}