// IDYNEVARenderServer.aidl
package org.lsque.tusdkevademo;
import org.lsque.tusdkevademo.EVASaverCallback;


// Declare any non-default types here with import statements

interface IDYNEVARenderServer {

String initRenderTask(in String path,in boolean isAssets);

void updateResource(in String taskId, in List item,in String audioPath);

void requestSave(in String taskId,in String savePath, in EVASaverCallback callback);

}