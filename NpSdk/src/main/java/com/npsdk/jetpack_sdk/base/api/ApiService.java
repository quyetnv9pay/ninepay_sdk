package com.npsdk.jetpack_sdk.base.api;

import com.npsdk.jetpack_sdk.repository.model.*;
import org.jetbrains.annotations.Nullable;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    @FormUrlEncoded
    @POST("/sdk/v1/paygate-whitelabel/validate-order")
    Call<ValidatePaymentModel> check(@Field("data") String url);

    @FormUrlEncoded
    @POST("/sdk/v2/paygate-whitelabel/create-order")
    Call<String> createOrderCardInter(
            @Header("Device-Id") String deviceId,
            @Field("data_encrypt") String data
    );

    @FormUrlEncoded
    @POST("/sdk/v2/paygate-whitelabel/create-order")
    Call<String> createOrderCardInland(
            @Header("Device-Id") String deviceId,
            @Field("data_encrypt") String data
    );

    @FormUrlEncoded
    @POST("/sdk/v1/paygate-whitelabel/create-order")
    Call<CreateOrderCardModel> createOrderCardWallet(
            @Field("data") String url,
            @Field("method") String method,
            @Field("amount") String amount,
            @Nullable @Field("card_token") String cardToken);

    @GET("/sdk/v1/paygate-whitelabel/banks-support")
    Call<ListBankModel> getListBanks();

    @FormUrlEncoded
    @POST("/sdk/v1/wallet/payment")
    Call<String> createPayment(@Field("type") String type, @Field("order_id") String orderId);


    @FormUrlEncoded
    @POST("/sdk/v1/payment/verifyOtp")
    Call<String> verifyPayment(@Field("payment_id") String paymentId, @Field("otp") String otp);

    @FormUrlEncoded
    @POST("/sdk/v1/reset-password/check-info")
    Call<String> verifyPassword(@Field("password") String password);

    @GET("/sdk/v1/func/list")
    Call<MerchantModel> getMerchant();

    @POST("/sdk/api/v1/user/list-banks")
    Call<String> getListPaymentMethod();

    @FormUrlEncoded
    @POST("/sdk/api/v1/merchant/create-order")
    Call<String> createOrder(
            @Header("Device-Id") String deviceId,
            @Field("data_encrypt") String data
    );
}
