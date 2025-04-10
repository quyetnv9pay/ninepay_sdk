package com.npsdk.demo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.npsdk.jetpack_sdk.DataOrder;

public class SetupActivity extends AppCompatActivity {

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        EditText colorCode = findViewById(R.id.colorCode);
        EditText merchantCode = findViewById(R.id.merchantCode);
        EditText secretKey = findViewById(R.id.secretKey);
        Button btn_open_sdk = findViewById(R.id.btn_open_sdk);
        EditText phoneNumber = findViewById(R.id.phoneNumber);
        Switch switchButton = findViewById(R.id.switchButton);


        btn_open_sdk.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), MainActivity.class);
            intent.putExtra("color_code", colorCode.getText().toString());
            intent.putExtra("merchant_code", merchantCode.getText().toString());
            intent.putExtra("secret_key", secretKey.getText().toString());
            intent.putExtra("phone_number", phoneNumber.getText().toString());
            v.getContext().startActivity(intent);
        });

        DataOrder.Companion.setShowResultScreen(true);
        switchButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            DataOrder.Companion.setShowResultScreen(isChecked);

            Boolean show = DataOrder.Companion.isShowResultScreen();
            System.out.println("Show is: "+show);
        });

    }
}