package com.npsdk.demo.dialog;


import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.gson.JsonObject;
import com.npsdk.demo.R;
import com.npsdk.demo.adapter.ServiceAdapter2;
import com.npsdk.demo.databinding.LayoutSheetServiceBinding;
import com.npsdk.demo.util.GridSpacingItemDecoration;
import com.npsdk.demo.util.ServiceEnum;
import com.npsdk.demo.util.ServiceModel;
import com.npsdk.demo.util.Utils;
import com.npsdk.jetpack_sdk.DataOrder;
import com.npsdk.module.NPayLibrary;
import com.npsdk.module.PaymentMethod;
import com.npsdk.module.utils.Actions;

import java.util.List;

public class ServiceSheet extends BottomSheetDialogFragment implements ServiceAdapter2.IServiceClicked {

    private LayoutSheetServiceBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = LayoutSheetServiceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public int getTheme() {
        return R.style.BottomSheetDialog;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        List<ServiceModel> services = Utils.getServices();

        binding.rcService.setHasFixedSize(true);
        binding.rcService.setLayoutManager(new GridLayoutManager(getContext(), 4));

        int spanCount = 4;
        int spacing = 50;
        boolean includeEdge = true;
        binding.rcService.addItemDecoration(new GridSpacingItemDecoration(spanCount, spacing, includeEdge));

        ServiceAdapter2 serviceAdapter2 = new ServiceAdapter2(getContext(), services, this);
        binding.rcService.setAdapter(serviceAdapter2);

    }

    @Override
    public void onItemServiceClicked(ServiceEnum type) {
        NPayLibrary lib = NPayLibrary.getInstance();
        switch (type) {
            case Transfer:
                lib.openSDKWithAction(Actions.TRANSFER);
                break;
            case Payment:
                lib.openSDKWithAction(Actions.QR);
                break;
            case Deposit:
                lib.openSDKWithAction(Actions.DEPOSIT);
                break;
            case Withdrawal:
                lib.openSDKWithAction(Actions.WITHDRAW);
                break;
            case TopUpPhone:
                lib.openSDKWithAction(Actions.TOPUP);
                break;
            case BillPayment:
                NPayLibrary.getInstance().openSDKWithAction(Actions.BILLING);
                break;
            case PaymentGate:
                showDialog();
                break;
        }

    }

    private void showDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(requireContext());
        final AppCompatEditText edittext = new AppCompatEditText(getContext());
        edittext.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(com.npsdk.R.color.green)));
        alert.setMessage(getString(R.string.payment_gate));
        alert.setTitle("Nhập url thanh toán của bạn");
        alert.setView(edittext);
        alert.setPositiveButton("Next", (dialog, whichButton) -> {
            String url = edittext.getText().toString();
            if (url.isEmpty() && NPayLibrary.getInstance().sdkConfig.getEnv().contains("staging")) {
                url = "https://stg-api.pgw.9pay.mobi/portal?baseEncode=eyJtZXJjaGFudEtleSI6IlZNNzE0RyIsInRpbWUiOjE2NzcxMjM3ODcsImludm9pY2Vfbm8iOiJCb29raW5nTHZ5cmpScnQiLCJhbW91bnQiOjIwMDAwLCJkZXNjcmlwdGlvbiI6IlRoYW5oIHRvYW4gZG9uIGhhbmcgQm9va2luZ0x2eXJqUnJ0IiwicmV0dXJuX3VybCI6Imh0dHBzOi8vcXAuc3Bob3Rvbi5jb20vYXBpL3YxL3BheW1lbnQvY29tcGxldGUtdHJhbnNhY3Rpb24iLCJiYWNrX3VybCI6Imh0dHA6Ly9xcC50ZXN0L2FwaS92My9jdXN0b21lci9ib29raW5nIiwibGFuZyI6ImVuIiwic2F2ZV90b2tlbiI6MCwiaXNfY3VzdG9tZXJfcGF5X2ZlZSI6MX0%3D&signature=eUtKetwGRFgoIJ5zwADzU7KjuIwlPK4RKq9IO5fL6so%3D";
            }
            NPayLibrary.getInstance().openPaymentOnSDK(url, PaymentMethod.DEFAULT, DataOrder.Companion.isShowResultScreen());
        });

        alert.setNegativeButton("Cancel", (dialog, whichButton) -> {
            dialog.dismiss();
        });
        alert.show();
    }
}
