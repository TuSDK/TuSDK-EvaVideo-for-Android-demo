/**
 *  TuSDK
 *  droid-sdk-eva$
 *  org.lsque.tusdkevademo$
 *  @author  H.ys
 *  @Date    2019/7/1$ 10:05$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lsque.tusdkevademo

import android.Manifest
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.View
import kotlinx.android.synthetic.main.demo_entry_activity.*
import org.jetbrains.anko.startActivity
import org.lsque.tusdkevademo.utils.PermissionUtils
import java.util.*


class DemoEntryActivity : ScreenAdapterActivity() {

    val LAYOUT_ID: Int = R.layout.demo_entry_activity

    val MODEL_LIST: ArrayList<String> = arrayListOf(
            "09-tutujieshao",
            "06-xinhunkuaile-fen",
           "10-shishagnqianyan")


    val NAME_LIST : ArrayList<String> = arrayListOf("" +
            "涂图视频融合介绍",
            "HappyWedding",
            "时尚潮流")


    val TEMPLATES_LIST : ArrayList<String> = arrayListOf(
            "lsq_eva_23.eva",
            "lsq_eva_28.eva",
            "lsq_eva_30.eva")

    var modelAdapter: ModelAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LAYOUT_ID)
        initView()
    }

    private fun initView() {
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_WIFI_STATE)
        PermissionUtils.requestRequiredPermissions(this, permissions)
        setModelView()
    }

    private fun setModelView() {
        modelAdapter = ModelAdapter(this, ModelItemFactory.getModelItem(MODEL_LIST,NAME_LIST,TEMPLATES_LIST))
        modelAdapter!!.setOnItemClickListener(object : ModelAdapter.OnItemClickListener {
            override fun onClick(view: View, item: ModelItem, position: Int) {
                startActivity<ModelDetailActivity>("model" to item)
            }
        })
        lsq_model_list.layoutManager = StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL)
        lsq_model_list.adapter = modelAdapter
    }
}