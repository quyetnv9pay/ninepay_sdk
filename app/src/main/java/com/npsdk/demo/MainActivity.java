package com.npsdk.demo;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.npsdk.LibListener;
import com.npsdk.demo.databinding.ActivityMainBinding;
import com.npsdk.demo.util.AsteriskPasswordTransformationMethod;
import com.npsdk.jetpack_sdk.DataOrder;
import com.npsdk.module.NPayLibrary;
import com.npsdk.module.PaymentMethod;
import com.npsdk.module.model.SdkConfig;
import com.npsdk.module.utils.Actions;
import com.npsdk.module.utils.Flavor;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = "MainActivityLOG";
    boolean isShow = false;

    ActivityMainBinding binding;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.txtMoney.setTransformationMethod(new AsteriskPasswordTransformationMethod());

        listener();
        // Create flavor by package name test
        String flavorEnv = Flavor.setEnvTest(this);
        Bundle bundle = getIntent().getExtras();
        String mcCode = "";
        String colorCode = "";
        String secretKey = "";
        String phoneNumber = "";

        if (bundle != null) {
            colorCode = bundle.getString("color_code");
            mcCode = bundle.getString("merchant_code");
            secretKey = bundle.getString("secret_key");
            phoneNumber = bundle.getString("phone_number");
        }
        SdkConfig sdkConfig = new SdkConfig.Builder(this).merchantCode(mcCode).secretKey(secretKey).uid(null).env(flavorEnv).brandColor(colorCode).phoneNumber(phoneNumber).build();
        initSdk(sdkConfig);
    }

    private void listener() {
        binding.llQuetMa.setOnClickListener(this);
        binding.llNapTien.setOnClickListener(this);
        binding.llChuyenTien.setOnClickListener(this);
        binding.llLichSu.setOnClickListener(this);
        binding.llMuaTheDt.setOnClickListener(this);
        binding.llNapTienDt.setOnClickListener(this);
        binding.llNapData.setOnClickListener(this);
        binding.llMuaTheDichVu.setOnClickListener(this);
        binding.llMuaTheDt.setOnClickListener(this);
        binding.llNapData.setOnClickListener(this);
        binding.btnBankLinkManage.setOnClickListener(this);
        binding.btnBankLinkAdd.setOnClickListener(this);
        binding.layoutSdv.setOnClickListener(this);
        binding.btnEyes.setOnClickListener(this);
        binding.rlInfo.setOnClickListener(this);
        binding.btnClose.setOnClickListener(this);
        binding.btnThanhToan.setOnClickListener(this);
        binding.btnThanhToan2.setOnClickListener(this);
        binding.btnThanhToan3.setOnClickListener(this);
        binding.btnThanhToan4.setOnClickListener(this);
        binding.testClick.setOnClickListener(this);
    }


    private void initSdk(SdkConfig sdkConfig) {
        NPayLibrary.getInstance().init(MainActivity.this, sdkConfig, new LibListener() {

            @Override
            public void getInfoSuccess(String jsonData) {
                System.out.println(jsonData);
            }

            @Override
            public void onError(int errorCode, String message) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onLogoutSuccessful() {
                Toast.makeText(MainActivity.this, "Logout success!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCloseSDK() {
                Toast.makeText(MainActivity.this, "onCloseSDK", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void sdkDidComplete(String name, Object status, @Nullable Object params) {
                Toast.makeText(MainActivity.this, name + " " + status, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void backToAppFrom(String screen) {
                System.out.println(screen);
            }
        });

    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        String url = binding.edtUrlPaygate.getText().toString();
        if (url.isEmpty() && NPayLibrary.getInstance().sdkConfig.getEnv().contains("staging"))
            url = "https://stg-api.pgw.9pay.mobi/portal?baseEncode=eyJtZXJjaGFudEtleSI6IlZNNzE0RyIsInRpbWUiOjE2NzcxMjM3ODcsImludm9pY2Vfbm8iOiJCb29raW5nTHZ5cmpScnQiLCJhbW91bnQiOjIwMDAwLCJkZXNjcmlwdGlvbiI6IlRoYW5oIHRvYW4gZG9uIGhhbmcgQm9va2luZ0x2eXJqUnJ0IiwicmV0dXJuX3VybCI6Imh0dHBzOi8vcXAuc3Bob3Rvbi5jb20vYXBpL3YxL3BheW1lbnQvY29tcGxldGUtdHJhbnNhY3Rpb24iLCJiYWNrX3VybCI6Imh0dHA6Ly9xcC50ZXN0L2FwaS92My9jdXN0b21lci9ib29raW5nIiwibGFuZyI6ImVuIiwic2F2ZV90b2tlbiI6MCwiaXNfY3VzdG9tZXJfcGF5X2ZlZSI6MX0%3D&signature=eUtKetwGRFgoIJ5zwADzU7KjuIwlPK4RKq9IO5fL6so%3D";
        switch (v.getId()) {
            case R.id.ll_quet_ma:
                Log.d(TAG, "onClick: ll_rut_tien");
                NPayLibrary.getInstance().openSDKWithAction(Actions.QR);
                break;
            case R.id.ll_nap_tien:
                Log.d(TAG, "onClick: ll_nap_tien");
                NPayLibrary.getInstance().openSDKWithAction(Actions.DEPOSIT);
                break;
            case R.id.ll_chuyen_tien:
                NPayLibrary.getInstance().openSDKWithAction(Actions.TRANSFER);
                Log.d(TAG, "onClick: ll_chuyen_tien");
                break;
            case R.id.ll_lich_su:
                Log.d(TAG, "onClick: ll_lich_su");
                NPayLibrary.getInstance().openSDKWithAction(Actions.HISTORY);
                break;
            case R.id.ll_thanh_toan_hoa_don:
                Log.d(TAG, "onClick: ll_thanh_toan_hoa_don");
                NPayLibrary.getInstance().openSDKWithAction(Actions.BILLING);
                break;
            case R.id.ll_nap_tien_dt:
                Log.d(TAG, "onClick: ll_nap_tien_dt");
                NPayLibrary.getInstance().openSDKWithAction(Actions.TOPUP);
                break;
            case R.id.ll_mua_the_game:
                Log.d(TAG, "onClick: ll_mua_the_game");
                NPayLibrary.getInstance().openSDKWithAction(Actions.GAME_CARD);
                break;
            case R.id.ll_mua_the_dich_vu:
                Log.d(TAG, "onClick: ll_mua_the_dich_vu");
                NPayLibrary.getInstance().openSDKWithAction(Actions.SERVICE_CARD);
                break;
            case R.id.ll_mua_the_dt:
                Log.d(TAG, "onClick: ll_mua_the_dt");
                NPayLibrary.getInstance().openSDKWithAction(Actions.PHONE_CARD);
                break;
            case R.id.ll_nap_data:
                Log.d(TAG, "onClick: ll_nap_data");
                NPayLibrary.getInstance().openSDKWithAction(Actions.DATA_CARD);
                break;
            case R.id.btn_bank_link_manage:
                Log.d(TAG, "onClick: btn_bank_link_manage");
//                NPayLibrary.getInstance().openWallet(Actions.BANK_LINK_MANAGE);
                break;
            case R.id.btn_bank_link_add:
                Log.d(TAG, "onClick: btn_bank_link_add");
//                NPayLibrary.getInstance().openWallet(Actions.ADD_LINK_BANK);
                break;
            case R.id.btn_eyes:
                Log.d(TAG, "onClick: btn_eyes");
                binding.txtMoney.setTransformationMethod(isShow ? HideReturnsTransformationMethod.getInstance() : new AsteriskPasswordTransformationMethod());
                isShow = !isShow;
                break;
            case R.id.layout_sdv:
            case R.id.rl_info:
                Log.d(TAG, "onClick: layout_sdv");
                NPayLibrary.getInstance().openSDKWithAction(Actions.OPEN_WALLET);
                break;
            case R.id.btnClose:
                binding.layoutWebGate.setVisibility(View.GONE);
                break;
            case R.id.btn_thanh_toan:
                //paste url thanh toán vào hàm pay

                NPayLibrary.getInstance().openPaymentOnSDK(url, PaymentMethod.WALLET, DataOrder.Companion.isShowResultScreen());
                binding.edtUrlPaygate.setText("");
                break;
            case R.id.btn_thanh_toan2:
                NPayLibrary.getInstance().openPaymentOnSDK(url, PaymentMethod.ATM_CARD, DataOrder.Companion.isShowResultScreen());
                binding.edtUrlPaygate.setText("");
                break;
            case R.id.btn_thanh_toan3:
                //paste url thanh toán vào hàm pay
                NPayLibrary.getInstance().openPaymentOnSDK(url, PaymentMethod.CREDIT_CARD, DataOrder.Companion.isShowResultScreen());
                break;
            case R.id.btn_thanh_toan4:
                //paste url thanh toán vào hàm pay
                NPayLibrary.getInstance().openPaymentOnSDK(url, PaymentMethod.DEFAULT, DataOrder.Companion.isShowResultScreen());
                break;
            case R.id.test_click:
//                NPayLibrary.getInstance().getUserInfo();
//                NPayLibrary.getInstance().openWallet(url);
//                String old = Preference.getString(this, Flavor.prefKey + Constants.ACCESS_TOKEN, "");
//                Preference.save(this, NPayLibrary.getInstance().sdkConfig.getEnv() + Constants.ACCESS_TOKEN, old + "a");
//                Intent i = new Intent(this, WebviewComposeActivity.class);
//                i.putExtra("url", "https://zing.vn");
//                startActivity(i);

//                Intent i = new Intent(this, TestWebviewActivity.class);
//                startActivity(i);

//                new JsHandler(this).getClipboardData();
//                NPayLibrary.getInstance().openSDKWithAction("https://stg-sdk.9pay.mobi/v1/viet-qr");
                NPayLibrary.getInstance().openSDKWithAction(Actions.WITHDRAW);
                break;
        }
        binding.edtUrlPaygate.setText("");

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

}

