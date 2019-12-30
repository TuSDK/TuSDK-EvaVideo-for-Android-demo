/**
 *  TuSDK
 *  droid-sdk-eva$
 *  org.lsque.tusdkevademo$
 *  @author  H.ys
 *  @Date    2019/7/1$ 10:30$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lsque.tusdkevademo

import android.os.Parcel
import android.os.Parcelable
import org.lsque.tusdkevademo.utils.EVAItem


object ModelItemFactory {

    public fun getModelItem(dirArray:ArrayList<String>,nameArray : ArrayList<String>,templateNameArray:java.util.ArrayList<String>,idArray : java.util.ArrayList<Int>) : java.util.ArrayList<ModelItem>{
        var modelList:ArrayList<ModelItem> = ArrayList<ModelItem>()
        for (s in 0 until dirArray.size){
            modelList.add(createModelItem(dirArray[s],nameArray[s],templateNameArray[s],idArray[s]))
        }
        return modelList
    }

    public fun getModelItem(item : ArrayList<EVAItem>) : java.util.ArrayList<ModelItem>{
        val modelList:ArrayList<ModelItem> = ArrayList<ModelItem>()
        for (s in item){
            modelList.add(createModelItem("",s.nm,"lsq_eva_${s.id}.eva","${s.id}.jpg",s.vid,s.id))
        }
        return modelList
    }

    private fun createModelItem(modelDir: String,modelName : String,templateName: String,id : Int) : ModelItem{
        return createModelItem(modelDir, modelName, templateName,"${id}.jpg","1.0.0",id)
    }

    private fun createModelItem(modelDir: String,modelName : String,templateName: String,iconName : String,ver : String,id : Int) : ModelItem{
        return ModelItem(modelDir,modelName,
            templateName,"http://files.tusdk.com/eva/$templateName",templateName,"",iconName,ver,id)
    }

}

data class ModelItem(var modelDir:String,var modelName : String,var templateName:String,var modelDownloadUrl : String,var fileName:String,var modelDownloadFilePath:String , var iconName : String,var modelVer : String,var modelId : Int) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readInt()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(modelDir)
        parcel.writeString(modelName)
        parcel.writeString(templateName)
        parcel.writeString(modelDownloadUrl)
        parcel.writeString(fileName)
        parcel.writeString(modelDownloadFilePath)
        parcel.writeString(iconName)
        parcel.writeString(modelVer)
        parcel.writeInt(modelId)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ModelItem> {
        override fun createFromParcel(parcel: Parcel): ModelItem {
            return ModelItem(parcel)
        }

        override fun newArray(size: Int): Array<ModelItem?> {
            return arrayOfNulls(size)
        }
    }
}