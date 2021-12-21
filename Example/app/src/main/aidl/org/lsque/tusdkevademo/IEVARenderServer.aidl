// IEVARenderServer.aidl
package org.lsque.tusdkevademo;
import org.lsque.tusdkevademo.EVASaverCallback;

// Declare any non-default types here with import statements

interface IEVARenderServer {

            String initRenderTask(in String path,in boolean isAssets);

            void updateImage(in Map item,in String taskId);

            void updateVideo(in Map item,in String taskId);

            void updateAudio(in Map item,in String taskId);

            void updateText(in List item, in String taskId);

            void requestSave(in String taskId,in String savePath,in EVASaverCallback callback);

}