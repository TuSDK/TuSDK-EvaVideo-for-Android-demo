/**
 *  TuSDK
 *  EvaDemo$
 *  org.lsque.tusdkevademo$
 *  @author  H.ys
 *  @Date    2022/6/27$ 10:31$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lsque.tusdkevademo.albumselect

import android.content.Intent
import android.graphics.RectF
import android.os.Bundle
import android.util.SparseArray
import android.view.View
import android.widget.Toast
import androidx.core.util.valueIterator
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.tusdk.pulse.eva.EvaModel
import com.tusdk.pulse.eva.EvaReplaceConfig
import kotlinx.android.synthetic.main.album_select_activity.*
import kotlinx.android.synthetic.main.title_item_layout.*
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.toast
import org.lasque.tusdkpulse.core.utils.TLog
import org.lsque.tusdkevademo.*
import java.util.*

/**
 * TuSDK
 * org.lsque.tusdkevademo
 * EvaDemo
 *
 * @author        H.ys
 * @Date        2022/6/27  10:31
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class AlbumSelectActivity : ScreenAdapterActivity() , AlbumSelectFragment.OnAlbumSelectListener{

    companion object{
        public const val IMAGE_CUTER_REQUEST_CODE = 1
        public const val VIDEO_CUTER_REQUEST_CODE = 2
    }

    private var mImageAndVideoList = LinkedList<EditorModelItem>()

    private val modelManager = ModelManager

    private var mModelItemAdapter : AlbumBottomSelectAdapter? = null

    private var mCurrentModelItem : EditorModelItem? = null

    private var mCurrentToast : Toast? = null


    inner class AlbumFragmentAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
        FragmentStateAdapter(fragmentManager, lifecycle) {

        private val fragmentArray : SparseArray<AlbumSelectFragment> = SparseArray<AlbumSelectFragment>()

        public fun getFragmentArray() : SparseArray<AlbumSelectFragment>{
            return fragmentArray
        }

        override fun getItemCount(): Int {
            return 3;
        }

        override fun createFragment(position: Int): Fragment {
            var bundle = Bundle()
             when(position){
                0->{
                    bundle.putBoolean("onlyImage",false)
                    bundle.putBoolean("onlyVideo",false)
                }
                1 ->{
                    bundle.putBoolean("onlyImage",false)
                    bundle.putBoolean("onlyVideo",true)
                }
                2->{
                    bundle.putBoolean("onlyImage",true)
                    bundle.putBoolean("onlyVideo",false)
                }
                else ->{
                    bundle.putBoolean("onlyImage",false)
                    bundle.putBoolean("onlyVideo",false)
                }
            }
            val albumFragment = AlbumSelectFragment()
            albumFragment.arguments = bundle
            albumFragment.setOnAlbumSelectListener(this@AlbumSelectActivity)
            if (fragmentArray[position] == null){
                fragmentArray.put(position,albumFragment)
            }
            return albumFragment;
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data == null) return

        val modelItem = mCurrentModelItem!!.modelItem as EvaModel.VideoReplaceItem

        when(requestCode){
            IMAGE_CUTER_REQUEST_CODE->{
                val rectArray = data!!.extras!!.getFloatArray("zoom")!!

                val config = EvaReplaceConfig.ImageOrVideo()
                config.crop = RectF(rectArray[0], rectArray[1], rectArray[2], rectArray[3])
                config.audioMixWeight = 0F

                modelManager.putConfig(modelItem.id,config)

                mModelItemAdapter?.notifyItemChanged(mImageAndVideoList.indexOf(mCurrentModelItem!!))
            }

            VIDEO_CUTER_REQUEST_CODE->{
                val rectArray = data!!.getFloatArrayExtra("zoom")!!
                val start = data!!.extras!!.getLong("start",0L)
                val duration = data!!.extras!!.getLong("duration",-1L)

                val config = EvaReplaceConfig.ImageOrVideo()
                config.crop = RectF(rectArray[0], rectArray[1], rectArray[2], rectArray[3])
                config.repeat = 2
                config.audioMixWeight = 0F
                config.start = start
                config.duration = duration

                modelManager.putConfig(modelItem.id,config)

                mModelItemAdapter?.notifyItemChanged(mImageAndVideoList.indexOf(mCurrentModelItem!!))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.album_select_activity)

        AlbumManager.init(this)

        val modelItem = intent.getParcelableExtra<ModelItem>("model")!!

        val modelPath = modelItem.modelDownloadFilePath

        modelManager.init(modelPath)

        mImageAndVideoList = modelManager.getImageAndVideoList()

        TLog.e("item list size ${mImageAndVideoList.size} model path ${modelPath}")

        initViews()

    }

    private fun initViews() {

        lsq_next.visibility = View.GONE
        lsq_back.setOnClickListener {
            finish()
        }

        val tabLayout = lsq_album_tab_layout
        val viewpager = lsq_album_view_pager
        val viewPagerAdapter = AlbumFragmentAdapter(supportFragmentManager,lifecycle)

        viewpager.adapter = viewPagerAdapter

        val tabMediator = TabLayoutMediator(tabLayout,viewpager,false,object : TabLayoutMediator.TabConfigurationStrategy{
            override fun onConfigureTab(tab: TabLayout.Tab, position: Int) {
                when(position){
                    1->{
                        tab.text = "视频"
                    }
                    2->{
                        tab.text = "图片"
                    }
                    else ->{
                        tab.text = "全部"
                    }
                }
            }
        })

        tabMediator.attach()

        val modelItemAdapter = AlbumBottomSelectAdapter(this,mImageAndVideoList,modelManager.getConfigMap())

        modelItemAdapter.setOnItemClickListener(object : AlbumBottomSelectAdapter.OnItemClickListener{
            override fun onClick(
                modelItem: EditorModelItem,
                albumItem: AlbumSelectItem,
                position: Int
            ) {
                mCurrentModelItem = modelItem
                val mitem = modelItem.modelItem as EvaModel.VideoReplaceItem
                when(albumItem.albumInfo.type){
                    AlbumItemType.Image -> {
                        startActivityForResult<ImageCuterActivity>(IMAGE_CUTER_REQUEST_CODE,"width" to mitem.width,"height" to mitem.height,"imagePath" to albumItem.albumInfo.path)
                    }
                    AlbumItemType.Video -> {
                        val videoDuration = mitem.endTime - mitem.startTime
                        startActivityForResult<MovieCuterActivity>(VIDEO_CUTER_REQUEST_CODE,"videoDuration" to videoDuration * 1000,"width" to mitem.width,"height" to mitem.height,"videoPath" to albumItem.albumInfo.path)
                    }
                }
            }

            override fun onUnselected(
                modelItem: EditorModelItem,
                albumItem: AlbumSelectItem,
                position: Int
            ) {
                albumItem.count -- ;

                modelItemAdapter.unbindAlbumItem(modelItem)

                val fragmentArray = viewPagerAdapter.getFragmentArray()
                for (i in fragmentArray.valueIterator()){
                    i.notifyAlbumItemChanged(albumItem)
                }
            }

            override fun onSelected(modelItem: EditorModelItem, position: Int) {
                modelItemAdapter.setSelectPos(position)
            }

        })

        val linearLayoutManager = LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false)
        lsq_album_select_view.layoutManager = linearLayoutManager
        lsq_album_select_view.adapter = modelItemAdapter

        mModelItemAdapter = modelItemAdapter

        lsq_album_bottom_slogan.text = "需要${mImageAndVideoList.size}个素材"

        lsq_album_bottom_commit.setOnClickListener {
            if (!mModelItemAdapter!!.isFill()){
                toastText("素材不足,需要继续选择")
            } else {
                for (item in mModelItemAdapter!!.getModelMap()){
                    val first = item.key
                    val second = item.value

                    when(second.albumInfo.type){
                        AlbumItemType.Image -> {
                            val item = first.modelItem as EvaModel.VideoReplaceItem
                            item.resPath = second.albumInfo.path
                            ModelManager.putResType(item.id,AlbumItemType.Image)
                        }
                        AlbumItemType.Video -> {
                            val item = first.modelItem as EvaModel.VideoReplaceItem
                            item.resPath = second.albumInfo.path
                            ModelManager.putResType(item.id,AlbumItemType.Video)
                        }
                        else -> {

                        }
                    }
                }
                val modelItem = intent.getParcelableExtra<ModelItem>("model")!!
                startActivity<ModelEditorActivity>("isFromModelManager" to true,"model" to modelItem)
                finish()
            }
        }


    }

    override fun onSelect(item: AlbumSelectItem, position: Int) : Boolean{
        if (mModelItemAdapter!!.isFill()){
            toastText("素材已经填满了")
            return false
        }
        val currentPos = mModelItemAdapter!!.getSelectPos()

        val currentItem = mImageAndVideoList[currentPos]

        if (item.albumInfo.type == AlbumItemType.Video){
            if (!(currentItem.assetsType == EvaModel.AssetType.kVIDEO_ONLY || currentItem.assetsType == EvaModel.AssetType.kIMAGE_VIDEO)){
                return false
            }

        } else {
            if (!(currentItem.assetsType == EvaModel.AssetType.kIMAGE_ONLY || currentItem.assetsType == EvaModel.AssetType.kIMAGE_VIDEO)){
                return false
            }
        }

        mModelItemAdapter!!.bindAlbumItem(currentItem,item)

        val pos = mModelItemAdapter!!.getSelectPos()
        if (pos >= 0){
            lsq_album_select_view.smoothScrollToPosition(pos)
        }

        return true

    }

    override fun enableSelect(): Boolean {
        if (mModelItemAdapter == null) return false
        else return !mModelItemAdapter!!.isFill()
    }

    override fun onPause() {
        super.onPause()

        if (mCurrentToast != null){
            mCurrentToast?.cancel()
        }
    }

    private fun toastText(text : String){
        if (mCurrentToast == null){
            mCurrentToast = Toast.makeText(this,text,Toast.LENGTH_SHORT)
        } else {
            mCurrentToast!!.setText(text)
        }
        mCurrentToast?.show()
    }
}