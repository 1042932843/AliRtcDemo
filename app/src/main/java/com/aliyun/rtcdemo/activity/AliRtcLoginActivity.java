package com.aliyun.rtcdemo.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.alivc.rtc.AliRtcEngine;
import com.aliyun.rtcdemo.R;
import com.aliyun.rtcdemo.base.BaseActivity;
import com.aliyun.rtcdemo.bean.RTCAuthInfo;
import com.aliyun.rtcdemo.network.AliRtcWebUtils;
import com.aliyun.rtcdemo.utils.AliRtcConstants;
import com.aliyun.rtcdemo.utils.DensityUtils;
import com.aliyun.rtcdemo.utils.ParserJsonUtils;
import com.aliyun.rtcdemo.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * 登录activity
 */
public class AliRtcLoginActivity extends BaseActivity implements View.OnClickListener {

    private EditText mEtChannelId;
    private AliRtcEngine mAliRtcEngine;
    private TextView mSdkVersion;
    private Button mLoginChannel;
    private ProgressDialog mProgressDialog;
    private String mUserName;
    private String mChannelId;
    /**
     * 频道id最小长度
     */
    private static final int CHANNELID_MIN_SIZE = 3;
    /**
     * 频道id最大长度
     */
    private static final int CHANNELID_MAX_SIZE = 12;
    /**
     * 防止抖动
     */
    public static final int MIN_CLICK_DELAY_TIME = 1500;
    private long mLastClickTime = 0;
    private View mParent;
    /**
     * 开启音频采集
     */
    private SwitchCompat mStartAudioCapture;
    /**
     * 开启音频播放
     */
    private SwitchCompat mStartAudioPlay;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alirtc_activity_login);

        if (!isTaskRoot()) {
            finish();
            return;
        }

        setUpSplash();

        initEngine();

        initView();

        if (AliRtcConstants.BRAND_OPPO.equalsIgnoreCase(Build.BRAND) && AliRtcConstants.MODEL_OPPO_R17.equalsIgnoreCase(Build.MODEL)) {
            mParent.setPadding(0, DensityUtils.dip2px(this, 20), 0, 0);
        }

        initData();
    }

    private void requestData() {
        HashMap<String, String> hashMap = new HashMap<>();
        String base = AliRtcConstants.GSLB_TEST;
        String url="https://api.dteacher-test.readboy.com/alirtc/app/v1/login?room="+mChannelId+"&user="+mUserName;
        showProgressDialog(true);
        AliRtcWebUtils.getInstance().doGet(url, null, new AliRtcWebUtils.HttpCallBack() {
            @Override
            public void onError(String error) {
                ThreadUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showProgressDialog(false);
                    }
                });
            }

            @Override
            public void onSuccess(String result) {
                showProgressDialog(false);
                //{"code":0,"data":{"gslb":["https://rgslb.rtc.aliyuncs.com"],"appid":"0qs634p6","turn":{"password":"0dcd55fd943d540a27308603c0f3a9d7d6a16712b141481916574b66ee3996d9","username":"89ae7abd2854962a?appid=0qs634p6&channel=123&nonce=AK-da4df4b5-a3ca-413e-8914-ae81c9db40e8&timestamp=1583543964"},"userid":"89ae7abd2854962a","nonce":"AK-da4df4b5-a3ca-413e-8914-ae81c9db40e8","token":"0dcd55fd943d540a27308603c0f3a9d7d6a16712b141481916574b66ee3996d9","timestamp":1583543964}}
                RTCAuthInfo rtcAuthInfo = ParserJsonUtils.parserLoginJson(result);
                if (rtcAuthInfo != null) {
                    showAuthInfo(rtcAuthInfo);
                }
            }

        });

    }


    private void initEngine() {
        mAliRtcEngine = AliRtcEngine.getInstance(this);

    }

    private void initView() {
        mEtChannelId = findViewById(R.id.et_channel);
        mLoginChannel = findViewById(R.id.bt_AuthInfo);
        mStartAudioCapture = findViewById(R.id.start_audio_capture);
        mStartAudioPlay = findViewById(R.id.start_audio_play);
        mSdkVersion = findViewById(R.id.tv_sdk_version);
        mParent = findViewById(R.id.login_parent);
        mLoginChannel.setOnClickListener(this);
    }

    private void initData() {
        if (null != mAliRtcEngine) {
            mSdkVersion.setText(mAliRtcEngine.getSdkVersion());
        }

    }

    /**
     * 跳转频道activity
     */
    private void doCreateChannel() {
        mChannelId = mEtChannelId.getText().toString().trim();
        if (mChannelId.isEmpty()) {
            Toast.makeText(AliRtcLoginActivity.this, getString(R.string.alirtc_attention), Toast.LENGTH_SHORT).show();
            return;
        }

        if (mChannelId.length() < CHANNELID_MIN_SIZE || mChannelId.length() > CHANNELID_MAX_SIZE) {
            Toast.makeText(AliRtcLoginActivity.this, getString(R.string.alirtc_channel_error), Toast.LENGTH_SHORT).show();
            return;
        }

        //用户名
        mUserName = randomName();

        requestData();
    }

    /**
     * 随机生成用户名
     *
     * @return
     */
    private String randomName() {
        Random rd = new Random();
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            // 你想生成几个字符
            str.append((char) (Math.random() * 26 + 'a'));
        }
        return str.toString();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bt_AuthInfo:
                long currentTime = System.currentTimeMillis();
                if (currentTime - mLastClickTime > MIN_CLICK_DELAY_TIME) {
                    mLastClickTime = currentTime;
                    doCreateChannel();

                }
                break;
            default:
                break;
        }
    }

    /**
     * 网络获取加入频道信息
     *
     * @param rtcAuthInfo
     */
    public void showAuthInfo(RTCAuthInfo rtcAuthInfo) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("alirtcsample://chat"));
        Bundle b = new Bundle();
        //用户名
        b.putString("username", mUserName);
        //频道号
        String channel = mEtChannelId.getText().toString();
        b.putString("channel", channel);
        //音频采集
        b.putBoolean("audioCapture", mStartAudioCapture.isChecked());
        //音频播放
        b.putBoolean("audioPlay", mStartAudioPlay.isChecked());
        b.putSerializable("rtcAuthInfo", rtcAuthInfo);
        intent.putExtras(b);
        startActivity(intent);
    }

    /**
     * 进入房间过程中的加载动画
     *
     * @param isShow
     */
    public void showProgressDialog(boolean isShow) {
        if (isShow && mProgressDialog != null && !mProgressDialog.isShowing()) {
            mProgressDialog.show();
        } else if (isShow && mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setMessage("登陆中...");
            mProgressDialog.show();
        } else if (!isShow && mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //退出时释放AliRtcEngine
        if (mAliRtcEngine != null) {
            mAliRtcEngine.destroy();
        }
    }


}
