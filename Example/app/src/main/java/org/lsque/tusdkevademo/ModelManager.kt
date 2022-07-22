package org.lsque.tusdkevademo

import com.tusdk.pulse.eva.EvaModel
import com.tusdk.pulse.eva.EvaModelEditor
import com.tusdk.pulse.eva.EvaReplaceConfig
import java.util.*
import kotlin.collections.HashMap

/**
 * TuSDK
 * org.lsque.tusdkevademo
 * EvaDemo
 *
 * @author        H.ys
 * @Date        2022/6/28  10:38
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
object ModelManager {

    private var mModel : EvaModel? = null;

    private val mImageAndVideoList : LinkedList<EditorModelItem> = LinkedList()

    private val mItemList : LinkedList<EditorModelItem> = LinkedList()

    private val mConfigMap = HashMap<Any,Any>()

    private var mImageItemArray : Array<EvaModel.VideoReplaceItem>? = null

    private var mVideoItemArray : Array<EvaModel.VideoReplaceItem>? = null

    private var mTextItemArray : Array<EvaModel.TextReplaceItem>? = null

    private var mItemsResTypeMap = HashMap<String,AlbumItemType>()

    public fun init(path : String){
        mModel = EvaModel()
        mModel!!.create(path)

        mImageItemArray = mModel!!.listReplaceableImageAssets()
        mVideoItemArray = mModel!!.listReplaceableVideoAssets()
        mTextItemArray = mModel!!.listReplaceableTextAssets()
    }

    public fun getModel() : EvaModel?{
        return mModel
    }

    public fun getImageAndVideoList() : LinkedList<EditorModelItem>{
        if (mImageAndVideoList.isEmpty()){
            if (mImageItemArray != null){
                val imageList = mImageItemArray!!
                for (i in imageList){

                    mImageAndVideoList.add(EditorModelItem(i,EditType.Image,i.startTime,i.endTime,i.ioTimes,i.type))
                }
            }
            if (mVideoItemArray != null){
                val videoList = mVideoItemArray!!
                for (i in videoList){
                    mImageAndVideoList.add(EditorModelItem(i,EditType.Video,i.startTime,i.endTime,i.ioTimes,i.type))
                }
            }
            mImageAndVideoList.sortBy {
                it.startPos
            }
        }
        return mImageAndVideoList
    }

    public fun getImageArrays() : Array<EvaModel.VideoReplaceItem>?{
        return mImageItemArray
    }

    public fun getVideoArrays() : Array<EvaModel.VideoReplaceItem>?{
        return mVideoItemArray
    }

    public fun getTextArrays() : Array<EvaModel.TextReplaceItem>?{
        return mTextItemArray
    }

    public fun getItemsList() : LinkedList<EditorModelItem>{
        if (mItemList.isEmpty()){
            val imageVideoList = getImageAndVideoList()
            val itemsList = mItemList
            itemsList.addAll(imageVideoList)

            val textList = mTextItemArray!!
            for (i in textList){
                itemsList.add(EditorModelItem(i,EditType.Text,i.startTime,i.endTime,i.ioTimes,EvaModel.AssetType.kTEXT))
            }

            itemsList.sortBy {
                it.startPos
            }
        }
        return mItemList
    }

    public fun putResType(id : String,type : AlbumItemType){
        mItemsResTypeMap.put(id,type)
    }

    public fun getResType(id : String) : AlbumItemType?{
        return mItemsResTypeMap[id]
    }



    public fun putConfig(id : String,config : Any){
        mConfigMap.put(id,config)
    }

    public fun getConfig(id : String) : Any?{
        return mConfigMap[id]
    }

    public fun getConfigMap() : HashMap<Any,Any>{
        return mConfigMap
    }

    public fun release(){

        mImageItemArray = null
        mVideoItemArray = null
        mTextItemArray = null

        mItemList.clear()
        mItemsResTypeMap.clear()
        mImageAndVideoList.clear()
        mConfigMap.clear()

        mModel = null
    }

}