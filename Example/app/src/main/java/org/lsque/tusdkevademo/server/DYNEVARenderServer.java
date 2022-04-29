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
import com.tusdk.pulse.eva.EvaModel;
import com.tusdk.pulse.eva.EvaReplaceConfig;
import com.tusdk.pulse.eva.dynamic.DynEvaDirector;

import org.lasque.tusdkpulse.core.struct.TuSdkSize;
import org.lasque.tusdkpulse.core.utils.TLog;
import org.lasque.tusdkpulse.core.utils.image.BitmapHelper;
import org.lsque.tusdkevademo.EVASaverCallback;
import org.lsque.tusdkevademo.IDYNEVARenderServer;
import org.lsque.tusdkevademo.NetworkModelExampleActivity;
import org.lsque.tusdkevademo.R;
import org.lsque.tusdkevademo.utils.MD5Util;
import org.lsque.tusdkevademo.utils.ProduceOutputUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TuSDK
 * org.lsque.tusdkevademo.server
 * EvaDemo
 *
 * @author H.ys
 * @Date 2022/4/12  10:42
 * @Copyright (c) 2020 tusdk.com. All rights reserved.
 */
public class DYNEVARenderServer extends Service {

    private static class RenderTask{
        private DispatchQueue mRenderQueue;
        private EvaModel mModel;
        private DynEvaDirector mDirector;
        private boolean isAssets;
    }

    private static class EVATaskState{
        public double taskOutputProgress = 0.0;
        public boolean isSuccess = false;
        public String taskId = "";
    }

    private static final String NOTIFICATIONID = "DYNEVARender";

    private ConcurrentHashMap<String,RenderTask> mTaskMap;

    private ConcurrentHashMap<String, EVATaskState> mSavingTask;

    private ConcurrentHashMap<String, EVATaskState> mSavedTask;

    private Notification.Builder mCurrentNotificationBuilder;

    private NotificationManager mNotificationManager;

    private IDYNEVARenderServer.Stub mBinder = new IDYNEVARenderServer.Stub() {
        @Override
        public String initRenderTask(final String path, final boolean isAssets) throws RemoteException {
            if (mTaskMap.isEmpty()){
                long taskTime = System.currentTimeMillis();
                String key = path + taskTime;
                final String taskId = MD5Util.crypt(key);

                final RenderTask task = new RenderTask();
                task.isAssets = isAssets;
                task.mRenderQueue = new DispatchQueue();
                task.mRenderQueue.runSync(new Runnable() {
                    @Override
                    public void run() {
                        task.mModel = new EvaModel();
                        if (isAssets){
                            task.mModel.createFromAsset(getApplicationContext(),path, EvaModel.ModelType.DYNAMIC);
                        } else {
                            task.mModel.create(path, EvaModel.ModelType.DYNAMIC);
                        }

                        task.mDirector = new DynEvaDirector();
                        task.mDirector.open(task.mModel);
                        task.mDirector.create();
                        mTaskMap.put(taskId,task);
                    }
                });
                return taskId;
            }
            return "";
        }

        @Override
        public void updateResource(String taskId, List item, final String audioPath) throws RemoteException {
            final RenderTask task = mTaskMap.get(taskId);
            final ArrayList<EvaReplaceConfig.ImageOrVideo> realList = (ArrayList<EvaReplaceConfig.ImageOrVideo>) item;
            if (task == null || realList.isEmpty()) return;

            task.mRenderQueue.runAsync(new Runnable() {
                @Override
                public void run() {
                    task.mDirector.updateResource(realList);
                    task.mDirector.updateAudio(audioPath);
                }
            });
        }

        @Override
        public void requestSave(final String taskId, final String savePath, final EVASaverCallback callback) throws RemoteException {
            final RenderTask task = mTaskMap.get(taskId);
            if (task == null) return;

            final EVATaskState state = new EVATaskState();
            state.taskId = taskId;

            task.mRenderQueue.runAsync(new Runnable() {
                @Override
                public void run() {

                    final DynEvaDirector.Producer producer = task.mDirector.newProducer();
                    Producer.OutputConfig config = new Producer.OutputConfig();
                    config.watermarkPosition = 1;
                    config.watermark = BitmapHelper.getRawBitmap(getApplicationContext(), R.raw.sample_watermark);
                    TuSdkSize supportSize = ProduceOutputUtils.getSupportSize(MediaFormat.MIMETYPE_VIDEO_AVC);
                    TuSdkSize outputSize = TuSdkSize.create(task.mModel.getWidth(), task.mModel.getHeight());
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
                                task.mRenderQueue.runAsync(new Runnable() {
                                    @Override
                                    public void run() {
                                        producer.cancel();
//                                        producer.release();
                                        task.mDirector.resetProducer();
                                        task.mDirector.close();
                                        try {
                                            callback.saveSuccess(taskId);
                                            stopForeground(true);
                                        } catch (RemoteException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });

                            } else if (s == Producer.State.kWRITING) {
                                state.taskOutputProgress = (ts / ((double) producer.getDuration())) * 100;
                                mCurrentNotificationBuilder.setContentText("渲染中... " + (int)state.taskOutputProgress + "%");
                                mCurrentNotificationBuilder.setProgress(100, ((int) state.taskOutputProgress),false);
                                mNotificationManager.notify(201,mCurrentNotificationBuilder.build());
                                try {
                                    callback.progress(state.taskOutputProgress,state.taskId);
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });

                    if (!producer.init(savePath)){
                        return;
                    }

                    state.taskOutputProgress = 0.0;
                    state.isSuccess = false;
                    mSavingTask.put(taskId,state);
                    if (!producer.start()){
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
            NotificationChannel channel = new NotificationChannel(NOTIFICATIONID, "DYNEVARenderService", NotificationManager.IMPORTANCE_HIGH);
            channel.setLightColor(Color.GRAY);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
//            channel.setSound(null,null);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);

            Notification.Builder builder = new Notification.Builder(getApplicationContext(),NOTIFICATIONID);
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

            startForeground(201,notification);

            mCurrentNotificationBuilder = builder;

            mNotificationManager = notificationManager;
        }
        return super.onStartCommand(intent, flags, startId);

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopForeground(true);
        Engine.getInstance().release();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }
}
