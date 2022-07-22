/**
 *  TuSDK
 *  droid-sdk-eva$
 *  org.lsque.tusdkevademo$
 *  @author  H.ys
 *  @Date    2019/7/1$ 13:33$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lsque.tusdkevademo

import android.os.Parcelable
import com.tusdk.pulse.eva.EvaModel

data class EditorModelItem(var modelItem: Any, var modelType:EditType,var startPos : Long,var endPos : Long,var ioTimes : ArrayList<EvaModel.ItemIOTime>,var assetsType : EvaModel.AssetType)