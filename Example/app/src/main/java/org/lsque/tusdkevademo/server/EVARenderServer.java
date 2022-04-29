package org.lsque.tusdkevademo.server;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaFormat;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Nullable;

import com.tusdk.pulse.DispatchQueue;
import com.tusdk.pulse.Engine;
import com.tusdk.pulse.Producer;
import com.tusdk.pulse.eva.EvaDirector;
import com.tusdk.pulse.eva.EvaModel;
import com.tusdk.pulse.eva.EvaReplaceConfig;

import org.lasque.tusdkpulse.core.struct.TuSdkSize;
import org.lasque.tusdkpulse.core.utils.TLog;
import org.lasque.tusdkpulse.core.utils.image.BitmapHelper;
import org.lsque.tusdkevademo.EVASaverCallback;
import org.lsque.tusdkevademo.IEVARenderServer;
import org.lsque.tusdkevademo.ModelEditorActivity;
import org.lsque.tusdkevademo.NetworkModelExampleActivity;
import org.lsque.tusdkevademo.R;
import org.lsque.tusdkevademo.utils.MD5Util;
import org.lsque.tusdkevademo.utils.ProduceOutputUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static android.app.PendingIntent.getActivity;

/**
 * TuSDK
 * org.lsque.tusdkevademo.server
 * EvaDemo
 *
 * @author H.ys
 * @Date 2021/12/6  14:08
 * @Copyright (c) 2020 tusdk.com. All rights reserved.
 */
public class EVARenderServer extends Service {


    private static class EVARenderTask {

        private DispatchQueue mRenderQueue;
        private EvaModel mModel;
        private EvaDirector mDirector;
        private boolean isAssets;

    }

    private static class EVATaskState {
        public double taskOutputProgress = 0.0;
        public boolean isSuccess = false;
        public String taskId = "";
    }

    private ConcurrentHashMap<String, EVARenderTask> mTaskMap;

    private ConcurrentHashMap<String, EVATaskState> mSavingTask;

    private ConcurrentHashMap<String, EVATaskState> mSavedTask;

    private Notification.Builder mCurrentNotificationBuilder;

    private static final String NOTIFICATIONID = "EVARender";

    private NotificationManager mNotificationManager;

    private IEVARenderServer.Stub mBinder = new IEVARenderServer.Stub() {

        @Override
        public String initRenderTask(final String path, final boolean isAssets) throws RemoteException {
            if (mSavingTask.isEmpty()){
                long taskTime = System.currentTimeMillis();
                String key = path + taskTime;
                final String taskId = MD5Util.crypt(key);

                final EVARenderTask task = new EVARenderTask();
                task.isAssets = isAssets;
                task.mRenderQueue = new DispatchQueue();
                task.mRenderQueue.runSync(new Runnable() {
                    @Override
                    public void run() {
                        task.mModel = new EvaModel();
                        if (isAssets) {
                            task.mModel.createFromAsset(getApplicationContext(), path);
                        } else {
                            task.mModel.create(path);
                        }
                        task.mDirector = new EvaDirector();
                        task.mDirector.open(task.mModel);
                        mTaskMap.put(taskId, task);
                    }
                });
                return taskId;
            }
            return "";

        }

        @Override
        public void updateImage(final Map item, final String taskId) throws RemoteException {
            final EVARenderTask currentTask = mTaskMap.get(taskId);
            final HashMap<EvaModel.VideoReplaceItem, EvaReplaceConfig.ImageOrVideo> realMap = (HashMap<EvaModel.VideoReplaceItem, EvaReplaceConfig.ImageOrVideo>) item;
            if (currentTask == null) return;

            currentTask.mRenderQueue.runAsync(new Runnable() {
                @Override
                public void run() {
                    for (Map.Entry<EvaModel.VideoReplaceItem, EvaReplaceConfig.ImageOrVideo> i : realMap.entrySet()) {
                        currentTask.mDirector.updateImage(i.getKey(), i.getValue());
                    }
                }
            });
        }

        @Override
        public void updateVideo(Map item, String taskId) throws RemoteException {
            final EVARenderTask currentTask = mTaskMap.get(taskId);
            final HashMap<EvaModel.VideoReplaceItem, EvaReplaceConfig.ImageOrVideo> realMap = (HashMap<EvaModel.VideoReplaceItem, EvaReplaceConfig.ImageOrVideo>) item;
            if (currentTask == null) return;

            currentTask.mRenderQueue.runAsync(new Runnable() {
                @Override
                public void run() {
                    for (Map.Entry<EvaModel.VideoReplaceItem, EvaReplaceConfig.ImageOrVideo> i : realMap.entrySet()) {
                        currentTask.mDirector.updateVideo(i.getKey(), i.getValue());
                    }
                }
            });
        }

        @Override
        public void updateAudio(Map item, String taskId) throws RemoteException {
            final EVARenderTask currentTask = mTaskMap.get(taskId);
            final HashMap<EvaModel.AudioReplaceItem, EvaReplaceConfig.Audio> realMap = (HashMap<EvaModel.AudioReplaceItem, EvaReplaceConfig.Audio>) item;
            if (currentTask == null) return;
            currentTask.mRenderQueue.runAsync(new Runnable() {
                @Override
                public void run() {
                    for (Map.Entry<EvaModel.AudioReplaceItem, EvaReplaceConfig.Audio> i : realMap.entrySet()) {
                        currentTask.mDirector.updateAudio(i.getKey(), i.getValue());
                    }
                }
            });
        }

        @Override
        public void updateText(List item, String taskId) throws RemoteException {
            final EVARenderTask currentTask = mTaskMap.get(taskId);
            final ArrayList<EvaModel.TextReplaceItem> realList = (ArrayList<EvaModel.TextReplaceItem>) item;
            if (currentTask == null) return;
            currentTask.mRenderQueue.runAsync(new Runnable() {
                @Override
                public void run() {
                    for (EvaModel.TextReplaceItem i : realList) {
                        currentTask.mDirector.updateText(i);
                    }
                }
            });
        }

        @Override
        public void requestSave(final String taskId, final String savePath, final EVASaverCallback callback) throws RemoteException {
            final EVARenderTask currentTask = mTaskMap.get(taskId);
            if (currentTask == null) return;

            final EVATaskState state = new EVATaskState();
            state.taskId = taskId;
            currentTask.mRenderQueue.runAsync(new Runnable() {
                @Override
                public void run() {
                    TLog.e("request save start - 1");
                    final EvaDirector.Producer producer = currentTask.mDirector.newProducer();
                    Producer.OutputConfig config = new Producer.OutputConfig();
                    config.watermarkPosition = 1;
                    config.watermark = BitmapHelper.getRawBitmap(getApplicationContext(), R.raw.sample_watermark);
                    TuSdkSize supportSize = ProduceOutputUtils.getSupportSize(MediaFormat.MIMETYPE_VIDEO_AVC);
                    TuSdkSize outputSize = TuSdkSize.create(currentTask.mModel.getWidth(), currentTask.mModel.getHeight());
                    if (supportSize.maxSide() < outputSize.maxSide()) {
                        config.scale = ((double) supportSize.maxSide()) / outputSize.maxSide();
                    }else {
                        config.scale = 1.0;
                    }
                    producer.setOutputConfig(config);

                    producer.setListener(new Producer.Listener() {
                        @Override
                        public void onEvent(Producer.State s, long ts) {
                            TLog.e("current state : " + s + "current ts : " + ts);

                            if (s == Producer.State.kEND) {
                                state.isSuccess = true;
                                mSavedTask.put(taskId, state);
                                mSavingTask.remove(taskId);
                                currentTask.mRenderQueue.runAsync(new Runnable() {
                                    @Override
                                    public void run() {
                                        producer.cancel();
                                        producer.release();
                                        currentTask.mDirector.resetProducer();
                                        currentTask.mDirector.close();
                                        stopForeground(true);
                                        try {
                                            callback.saveSuccess(taskId);
                                        } catch (RemoteException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });

                            } else if (s == Producer.State.kWRITING) {
                                state.taskOutputProgress = (ts / ((double) producer.getDuration())) * 100;
                                mCurrentNotificationBuilder.setContentText("渲染中... " + (int)state.taskOutputProgress + "%");
                                mCurrentNotificationBuilder.setProgress(100, ((int) state.taskOutputProgress),false);
                                mNotificationManager.notify(200,mCurrentNotificationBuilder.build());
                                try {
                                    callback.progress(state.taskOutputProgress,state.taskId);
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                    TLog.e("request save start - 2");
                    if (!producer.init(savePath)) {
                        return;
                    }

                    state.taskOutputProgress = 0.0;
                    state.isSuccess = false;
                    mSavingTask.put(taskId, state);
                    TLog.e("request save start - 3");
                    if (!producer.start()) {
                        TLog.e("request save start - 4");
                        mSavingTask.remove(taskId);
                    }


                }
            });
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mTaskMap = new ConcurrentHashMap<>();
        mSavingTask = new ConcurrentHashMap<>();
        mSavedTask = new ConcurrentHashMap<>();
        Engine.getInstance().init(null);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATIONID, "EVARenderService", NotificationManager.IMPORTANCE_HIGH);
            channel.setLightColor(Color.BLUE);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
//            channel.setSound(null,null);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
             notificationManager.createNotificationChannel(channel);

            Notification.Builder builder = new Notification.Builder(getApplicationContext(),"EVARender");
            Intent nIntent = new Intent(this, NetworkModelExampleActivity.class);
            builder.setContentIntent(PendingIntent.getActivity(this,0,nIntent,0))
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher))
                    .setContentTitle("后台渲染进行中...")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentText("渲染中...")
                    .setOngoing(true)
                    .setProgress(100,0,false)
                    .setWhen(System.currentTimeMillis());

            Notification notification = builder.build();
            notification.defaults = Notification.DEFAULT_VIBRATE;

            startForeground(200,notification);

            mCurrentNotificationBuilder = builder;

            mNotificationManager = notificationManager;
        }


        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        Engine.getInstance().release();
    }

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
