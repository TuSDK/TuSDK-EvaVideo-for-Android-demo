package org.lsque.tusdkevademo;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import org.lasque.tusdkpulse.impl.TuSpecialScreenHelper;
import org.lsque.tusdkevademo.utils.AppManager;
import org.lsque.tusdkevademo.utils.DisplayUtils;

/**
 * @author xujie
 * @Date 2018/10/29
 */

public class ScreenAdapterActivity extends FragmentActivity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        AppManager.getInstance().addActivity(this); //添加到栈中
        if(TuSpecialScreenHelper.isNotchScreen())
        {
            setTheme(android.R.style.Theme_NoTitleBar);
        }
        DisplayUtils.INSTANCE.setCustomDensity(this,getApplication());
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppManager.getInstance().finishActivity(this); //从栈中移除
    }
}
