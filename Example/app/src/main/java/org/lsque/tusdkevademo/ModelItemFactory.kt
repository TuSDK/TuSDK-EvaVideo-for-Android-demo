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


object ModelItemFactory {

    public fun getModelItem(dirArray:ArrayList<String>,nameArray : ArrayList<String>,templateNameArray:java.util.ArrayList<String>) : java.util.ArrayList<ModelItem>{
        var modelList:ArrayList<ModelItem> = ArrayList<ModelItem>()
        for (s in 0 until dirArray.size){
            modelList.add(createModelItem(dirArray[s],nameArray[s],templateNameArray[s]))
        }
        return modelList
    }

    private fun createModelItem(modelDir: String,modelName : String,templateName: String) : ModelItem{
        return ModelItem(modelDir,modelName,"$modelDir/$templateName")
    }

}

data class ModelItem(var modelDir:String,var modelName : String,var templateName:String) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readString()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(modelDir)
        parcel.writeString(modelName)
        parcel.writeString(templateName)
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