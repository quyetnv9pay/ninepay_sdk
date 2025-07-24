package com.npsdk.demo;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.text.method.HideReturnsTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.npsdk.LibListener;
import com.npsdk.NineLibListener;
import com.npsdk.demo.databinding.ActivityHomeBinding;
import com.npsdk.demo.dialog.ServiceSheet;
import com.npsdk.demo.event.UserInfoEvent;
import com.npsdk.demo.model.User;
import com.npsdk.demo.util.AsteriskPasswordTransformationMethod;
import com.npsdk.module.NPayLibrary;
import com.npsdk.module.model.SdkConfig;
import com.npsdk.module.model.UserInfo;
import com.npsdk.module.utils.Actions;
import com.npsdk.module.utils.Flavor;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class HomeActivity extends AppCompatActivity {

    public static final String TAG = HomeActivity.class.getSimpleName();
    ActivityHomeBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Create flavor by package name test
        String flavorEnv = Flavor.setEnvTest(this);
        String mcCode = "";
        String colorCode = "";
        String secretKey = "";
        String phoneNumber = "";
        SdkConfig sdkConfig = new SdkConfig.Builder(this).merchantCode(mcCode).secretKey(secretKey).uid(null).env(flavorEnv).brandColor(colorCode).phoneNumber(phoneNumber).build();
        initSdk(sdkConfig);

        binding.btnNinepay.setOnClickListener(v -> {
            //show các dịch vụ của ví
            ServiceSheet serviceSheet = new ServiceSheet();
            serviceSheet.show(getSupportFragmentManager(), "ServiceSheet");
        });

        binding.btnLogin.setOnClickListener(v -> {
            NPayLibrary.getInstance().openSDKWithAction(Actions.LOGIN);
        });

        binding.btnEyes.setOnClickListener(v -> {
            boolean isHide = binding.txtBalance.getTransformationMethod() instanceof AsteriskPasswordTransformationMethod;
            binding.txtBalance.setTransformationMethod(isHide ? HideReturnsTransformationMethod.getInstance() : new AsteriskPasswordTransformationMethod());
            binding.btnEyes.setImageResource(isHide ? R.drawable.ic_eye_enable : R.drawable.ic_eye_hide);

        });

        binding.btnLogout.setOnClickListener(v -> NPayLibrary.getInstance().logout());

        setView();
        getUserInfo();
    }

    private void setView() {
        boolean isLogin = NPayLibrary.getInstance().isLogin();
        binding.btnLogin.setVisibility(isLogin ? View.GONE : View.VISIBLE);
        binding.btnLogout.setVisibility(isLogin ? View.VISIBLE : View.GONE);
        binding.btnEyes.setVisibility(isLogin ? View.VISIBLE : View.GONE);
        binding.txtBalance.setText(isLogin ? "******" : getString(R.string.ninepay));
    }

    private void getUserInfo() {
        boolean isLogin = NPayLibrary.getInstance().isLogin();
        if (isLogin) {
            // lấy thông tin user info
            NPayLibrary.getInstance().getUserInfo();
        }
    }

    private void initSdk(SdkConfig sdkConfig) {
        NPayLibrary.getInstance().init(HomeActivity.this, sdkConfig, new NineLibListener() {
            @SuppressLint("SetTextI18n")
            public void getInfoSuccess(String jsonData) {
                User userInfo = User.fromJson(jsonData);
                String name = userInfo.getName();
                if (name.isEmpty()) {
                    name = userInfo.getPhone();
                }

                setView();

                binding.btnEyes.setImageResource(R.drawable.ic_eye_enable);
                binding.txtBalance.setTransformationMethod(new AsteriskPasswordTransformationMethod());
                binding.txtUserName.setText(name);
                binding.txtBalance.setText(userInfo.getBalance().toString());

            }

            @Override
            public void onLogoutSuccessful() {
                binding.txtUserName.setText(getString(R.string.hello));
                setView();
                binding.txtBalance.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                binding.txtBalance.setText(getString(R.string.ninepay));
            }

            @Override
            public void sdkDidComplete(String name, Object status, @Nullable Object params) {
                getUserInfo();
            }
        });
    }

}
