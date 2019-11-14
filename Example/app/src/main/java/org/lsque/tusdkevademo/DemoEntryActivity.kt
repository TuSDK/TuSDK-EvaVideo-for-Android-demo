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
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.View
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import kotlinx.android.synthetic.main.demo_entry_activity.*
import org.jetbrains.anko.startActivity
import org.lasque.tusdk.core.view.TuSdkViewHelper
import org.lsque.tusdkevademo.utils.PermissionUtils
import java.util.*


class DemoEntryActivity : ScreenAdapterActivity() {

    val LAYOUT_ID: Int = R.layout.demo_entry_activity

    val MODEL_LIST: ArrayList<String> = arrayListOf(
            "09-tutujieshao", "02-jiugongge","03-shilitaohua","04-quweixiatian",
            "05-xinhunkuaile-lan","06-xinhunkuaile-fen","08-weddingday",
           "10-shishagnqianyan","11-dianyingjiaopian")


    val NAME_LIST : ArrayList<String> = arrayListOf("" +
            "涂图视频融合介绍",  "九宫格","十里桃花","趣味夏天",
            "HappyWedding","HappyWedding","良缘结",
            "时尚潮流","电影胶片")


    val TEMPLATES_LIST : ArrayList<String> = arrayListOf(
            "lsq_eva_23.eva","lsq_eva_24.eva","lsq_eva_25.eva","lsq_eva_26.eva",
            "lsq_eva_27.eva","lsq_eva_28.eva","lsq_eva_29.eva",
            "lsq_eva_30.eva","lsq_eva_31.eva")

    var modelAdapter: ModelAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LAYOUT_ID)
        initView()
    }

    private fun initView() {
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_WIFI_STATE)
        PermissionUtils.requestRequiredPermissions(this, permissions)
        lsq_to_network_model.setOnClickListener (
            object : TuSdkViewHelper.OnSafeClickListener(){
                override fun onSafeClick(v: View?) {
                    startActivity<NetworkModelExampleActivity>()
                }
            }
        )
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