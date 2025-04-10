package com.npsdk.jetpack_sdk

import PasswordBottomSheet
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.npsdk.R
import com.npsdk.jetpack_sdk.DataOrder.Companion.bankTokenSelected
import com.npsdk.jetpack_sdk.DataOrder.Companion.isProgressing
import com.npsdk.jetpack_sdk.DataOrder.Companion.isStartScreen
import com.npsdk.jetpack_sdk.DataOrder.Companion.userInfo
import com.npsdk.jetpack_sdk.base.AppUtils
import com.npsdk.jetpack_sdk.base.listener.CloseListener
import com.npsdk.jetpack_sdk.base.view.DialogNotification
import com.npsdk.jetpack_sdk.base.view.ImageFromUrl
import com.npsdk.jetpack_sdk.base.view.LoadingView
import com.npsdk.jetpack_sdk.base.view.ShimmerLoading
import com.npsdk.jetpack_sdk.base.view.TopAppBarApp
import com.npsdk.jetpack_sdk.base.view.clickableWithoutRipple
import com.npsdk.jetpack_sdk.repository.CallbackCreateOrder
import com.npsdk.jetpack_sdk.repository.CallbackOrder
import com.npsdk.jetpack_sdk.repository.CheckValidatePayment
import com.npsdk.jetpack_sdk.repository.CreateOrderWalletRepo
import com.npsdk.jetpack_sdk.repository.model.CreateOrderParamsWallet
import com.npsdk.jetpack_sdk.repository.model.ListBankModel
import com.npsdk.jetpack_sdk.repository.model.MerchantInfo
import com.npsdk.jetpack_sdk.repository.model.ValidatePaymentModel
import com.npsdk.jetpack_sdk.repository.model.validate_payment.CreditCard
import com.npsdk.jetpack_sdk.repository.model.validate_payment.Methods
import com.npsdk.jetpack_sdk.repository.openWebviewOTP
import com.npsdk.jetpack_sdk.theme.PaymentNinepayTheme
import com.npsdk.jetpack_sdk.theme.fontAppBold
import com.npsdk.jetpack_sdk.theme.fontAppDefault
import com.npsdk.jetpack_sdk.theme.initColor
import com.npsdk.jetpack_sdk.viewmodel.AppViewModel
import com.npsdk.jetpack_sdk.viewmodel.InputViewModel
import com.npsdk.module.NPayLibrary
import com.npsdk.module.PaymentMethod
import com.npsdk.module.model.Bank
import com.npsdk.module.model.UserInfo
import com.npsdk.module.utils.*
import com.npsdk.module.model.UserInfoModel
import com.npsdk.module.utils.Actions
import com.npsdk.module.utils.Flavor
import com.npsdk.module.utils.NameCallback
import java.net.URLEncoder
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.launch

class DataOrder {
    companion object {
        var urlData: String = ""
        var isShowResultScreen = true
        var dataOrderSaved by mutableStateOf<ValidatePaymentModel?>(null)
        var amount: Any? = null
        var listBankModel: ListBankModel? = null

        @SuppressLint("StaticFieldLeak")
        var activityOrder: Activity? = null
        var selectedItemMethod by mutableStateOf<String?>(null)
        var userInfo by mutableStateOf<UserInfo?>(null)
        var merchantInfo: MerchantInfo? = null

        var totalAmount by mutableStateOf<Int?>(null)
        var bankTokenSelected by mutableStateOf<Bank?>(null)
        // Có đang tiếp tục tiến trình thanh toán hay không?
        var isProgressing = false
        var isStartScreen = false

        // Limit bank (when linked)
        var isLimitItem by mutableStateOf(true)

        // Hàm này đc gọi trong khởi tạo SDK.
        // Xoá toàn bộ cache của user cũ
        @JvmStatic
        fun clearData() {
            urlData = ""
            isShowResultScreen = true
            dataOrderSaved = null
            amount = null
            listBankModel = null
            activityOrder = null
            selectedItemMethod = null
            userInfo = null
            merchantInfo = null
            totalAmount = null
            bankTokenSelected = null
            isProgressing = false
            isStartScreen = false
            isLimitItem = true
        }
    }
}

enum class CreditCardEnum {
    VISA, MASTER, JCB, AMEX
}
class OrderActivity : ComponentActivity() {
    private var methodDefault: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isProgressing = false
        isStartScreen = false
        DataOrder.amount = null
        DataOrder.totalAmount = null
        DataOrder.dataOrderSaved = null
        bankTokenSelected = null
        DataOrder.selectedItemMethod = null
        DataOrder.isLimitItem = true

        DataOrder.activityOrder = this
        val bundle = intent.extras
        if (bundle != null) {
            methodDefault = bundle.getString("method")
            DataOrder.urlData = bundle.getString("url") ?: ""
        }
        setContent {
            PaymentNinepayTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = colorResource(id = R.color.background)
                ) {
                    Body()
                }
            }
        }

        CloseListener().listener(this)
    }

    override fun onDestroy() {
        CloseListener().cancelListener(this)
        super.onDestroy()
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun Body() {
        val inputViewModel: InputViewModel = viewModel()
        val appViewModel: AppViewModel = viewModel()
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        var showDialogDeposit by remember {
            mutableStateOf(false)
        }
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(true) {
            // Get detail payment
            CheckValidatePayment().check(context, DataOrder.urlData, CallbackOrder { data ->
                DataOrder.dataOrderSaved = data
                DataOrder.amount =
                    DataOrder.dataOrderSaved!!.data.amount
                setDefaultAmount()
            })
            if (AppUtils.isLogged()) {
                // Get user info
                NPayLibrary.getInstance().getUserInfoSendToPayment(null)
            }
        }

        DisposableEffect(DataOrder.dataOrderSaved) {
            if (DataOrder.dataOrderSaved != null) {
                // Neu mac dinh chon duy nhat vi 9Pay
                if (methodDefault == PaymentMethod.WALLET) {
                    DataOrder.totalAmount = DataOrder.dataOrderSaved!!.data.feeData.wallet
                }
            }
//            // Chon mac dinh ngan hang lien ket dau tien neu co
//            userInfo?.let {
//                if (methodDefault == null && (it.banks ?: arrayListOf()).isNotEmpty()) {
//                    bankTokenSelected = it.banks.first()
//                }
//            }

            onDispose { }
        }

        if (DataOrder.dataOrderSaved == null) {
            ShimmerLoading()
            return
        }

        if (AppUtils.isLogged()) {
            if (userInfo == null) {
                ShimmerLoading()
                return
            }
        }



        val bottomSheetState = rememberModalBottomSheetState(
            initialValue = ModalBottomSheetValue.Hidden,
            skipHalfExpanded = true,
                    confirmValueChange = {
                        it !=ModalBottomSheetValue.HalfExpanded
                    }
        )

        LaunchedEffect(bottomSheetState) {
            snapshotFlow { bottomSheetState.isVisible }.collect { isVisible ->
                if (!isVisible) {
                    keyboardController?.hide()
                }
            }
        }

        ModalBottomSheetLayout(
            sheetState = bottomSheetState,
            sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            sheetContent = {
                PasswordBottomSheet(
                    keyboardController = keyboardController,
                    focusRequester = focusRequester,
                    bottomSheetState = bottomSheetState,
                )
            },
        ) {
            Column {
                TopAppBarApp(isShowBack = true, onBack = {
                    finish()
                })

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopStart) {
                    LazyColumn(
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Card giao dien thong tin don hang
                        item {
                            DataOrder.dataOrderSaved?.let {
                                Box(modifier = Modifier.padding(12.dp)) {
                                    HeaderOrder(it)
                                }
                            }

                        }

                        // Show tat ca phuong thuc thanh toan
                        item {

                            DataOrder.dataOrderSaved?.data?.methods?.let {
                                Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                                    ShowMethodPayment(it, onItemClick = { itemCallback ->
                                        DataOrder.selectedItemMethod = itemCallback.code
                                        // Tinh phi mac dinh (chưa co phi)
                                        DataOrder.totalAmount =
                                            DataOrder.dataOrderSaved!!.data.amount

                                        // Phi Vi 9Pay
                                        if (DataOrder.selectedItemMethod == PaymentMethod.WALLET) {
                                            DataOrder.totalAmount =
                                                DataOrder.dataOrderSaved!!.data.feeData.wallet
                                            return@ShowMethodPayment
                                        }

                                        // Phi the ATM
                                        if (DataOrder.selectedItemMethod == PaymentMethod.ATM_CARD) {
                                            DataOrder.totalAmount =
                                                DataOrder.dataOrderSaved!!.data.feeData.atmCard
                                            return@ShowMethodPayment
                                        }

                                        if (DataOrder.selectedItemMethod == PaymentMethod.LINK_BANK) {
                                            // Lấy phi theo giống hình thức số dư ví, bởi vì thẻ lưu trên ví.
                                            DataOrder.totalAmount =
                                                DataOrder.dataOrderSaved!!.data.feeData.wallet
                                            // Phi chuyen khoan ngan hang
                                            if (DataOrder.selectedItemMethod == PaymentMethod.TRANSFER) {
                                                DataOrder.totalAmount =
                                                    DataOrder.dataOrderSaved!!.data.feeData.collection
                                                return@ShowMethodPayment
                                            }

                                            // Phi the quoc te
                                            if (DataOrder.selectedItemMethod == PaymentMethod.CREDIT_CARD) {
                                                var creditCard =
                                                    DataOrder.dataOrderSaved!!.data.feeData.creditCard;
                                                for (card in creditCard) {
                                                    if (card.cardBrand == CreditCardEnum.VISA.name) {
                                                        DataOrder.totalAmount = card.inLand
                                                        return@ShowMethodPayment
                                                    }
                                                }
                                            }

                                            if (DataOrder.selectedItemMethod == PaymentMethod.LINK_BANK) {
                                                // Lấy phi theo giống hình thức số dư ví, bởi vì thẻ lưu trên ví.
                                                DataOrder.totalAmount =
                                                    DataOrder.dataOrderSaved!!.data.feeData.wallet

                                                // Set mac dinh item dau tien neu link bank click
                                                if (bankTokenSelected == null) {
                                                    // Chon mac dinh ngan hang lien ket dau tien neu co
                                                    userInfo?.let {
                                                        if ((it.banks
                                                                ?: arrayListOf()).isNotEmpty()
                                                        ) {
                                                            bankTokenSelected = it.banks.first()
                                                        }
                                                    }
                                                }
                                            }
                                        }})
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(30.dp))
                        }
                    }


                    // Hien thi dialog nap tien neu khong du tien
                    if (showDialogDeposit) Box(modifier = Modifier.align(Alignment.Center)) {
                        ShowDepositDialog(onDismiss = {
                            showDialogDeposit = !showDialogDeposit
                        }, onDeposit = {
                            isProgressing = true
                            NPayLibrary.getInstance().openSDKWithAction(Actions.DEPOSIT)
                        })
                    }

                    // Thong bao tao giao dich (neu co loi xay ra)
                    if (inputViewModel.showNotification.value) {
                        Box(modifier = Modifier.align(Alignment.Center)) {
                            DialogNotification(
                                contextString = inputViewModel.stringDialog.value,
                                onDismiss = {
                                    inputViewModel.showNotification.value = false
                                })
                        }
                    }

                    // Loading khi goi API
                    if (appViewModel.isShowLoading) Box(modifier = Modifier.align(Alignment.Center)) {
                        LoadingView()
                    }

//                    if (bottomSheetState.is) {
//                        Box(
//                            Modifier
//                                .fillMaxSize()
//                                .background(Color.Black.copy(alpha = 0.5f))
//                                .clickable {
//                                    scope.launch {
//                                        scaffoldState.bottomSheetState.collapse()
//                                    }
//                                }
//                        )
//                    }
                }

            Footer(modifier = Modifier.padding(horizontal = 16.dp),
                clickContinue = { ->
                    if (methodDefault == PaymentMethod.WALLET || DataOrder.selectedItemMethod == PaymentMethod.WALLET) {
                        if (userInfo == null) {

                            if (!AppUtils.isLogged()) {
                                isProgressing = true
                                isStartScreen = false
                                // Gọi sang webview login
                                NPayLibrary.getInstance().openSDKWithAction(Actions.LOGIN)
                                return@Footer
                            }
                            inputViewModel.showNotification.value = true
                            inputViewModel.stringDialog.value = "Đang lấy dữ liệu tài khoản ví!!!"
                            return@Footer
                        }
                        userInfo?.balance?.let { it1 ->
                            DataOrder.totalAmount?.let { it2 ->
                                if (it1 < it2) {
                                    showDialogDeposit = true
                                    return@Footer
                                }
                            }
                        }

                        scope.launch {
                            focusRequester.requestFocus()
                            keyboardController?.show()
                            bottomSheetState.show()
                        }

//                        val intent = Intent(context, PasswordActivity::class.java)
//                        context.startActivity(intent)

                        return@Footer
                    }
                    if (DataOrder.selectedItemMethod == PaymentMethod.LINK_BANK) {
                        if (bankTokenSelected == null) {
                            inputViewModel.showNotification.value = true
                            inputViewModel.stringDialog.value = "Vui lòng chọn ngân hàng liên kết để thanh toán!"
                            return@Footer
                        }
                        // Lấy phi theo giống hình thức số dư ví, bởi vì thẻ lưu trên ví.
                        DataOrder.totalAmount = DataOrder.dataOrderSaved!!.data.feeData.wallet

                        val typeMethod = convertTypeToMethod()
                        val paramsCreateOrder = CreateOrderParamsWallet(
                            url = DataOrder.urlData, method = typeMethod,
                            amount = DataOrder.totalAmount.toString(),
                            cardToken = bankTokenSelected?.getbToken()
                        )
                        appViewModel.isShowLoading = true
                        CreateOrderWalletRepo().create(context, paramsCreateOrder, CallbackCreateOrder {
                            appViewModel.isShowLoading = false
                            it.message?.let { it1 ->
                                if (it.errorCode == 1) {
                                    NPayLibrary.getInstance().callBackToMerchant(
                                        NameCallback.SDK_PAYMENT, false, null
                                    )
                                    appViewModel.hideLoading()
                                    inputViewModel.showNotification.value = true
                                    inputViewModel.stringDialog.value = it1
                                    NPayLibrary.getInstance().callbackError(2002, it1)
                                } else if (it.errorCode == 0) {
                                    openWebviewOTP(context, it.data!!.redirectUrl!!)
                                    finish()
                                }
                            }
                        })
                        return@Footer
                    }

                    if (DataOrder.selectedItemMethod == PaymentMethod.TRANSFER) {
                        finish()
                        val urlTransfer = "${Flavor.baseUrl}/v1/viet-qr?data=${URLEncoder.encode(DataOrder.urlData)}&from=merchant"
                        NPayLibrary.getInstance().openSDKWithAction(urlTransfer)
                        return@Footer
                    }
                    val intent = Intent(context, InputCardActivity::class.java)
                    intent.putExtra("method", DataOrder.selectedItemMethod)
                    context.startActivity(intent)
                })
            }
        }
    }

    private fun convertTypeToMethod(): String {
        when (bankTokenSelected!!.getbType()) {
            2 -> return  PaymentMethod.ATM_CARD
            3 -> return PaymentMethod.CREDIT_CARD
            else -> return PaymentMethod.WALLET
        }
    }

    private fun generateLinkWeb(orderId: String): String {
        return "${Flavor.baseUrl}/v1/mobile-payment?b_type=${bankTokenSelected!!.getbType()}&order_id=$orderId&b_code=${bankTokenSelected!!.getbCode()}&b_token=${bankTokenSelected!!.getbToken()}"
    }

    @Composable
    private fun Footer(modifier: Modifier, clickContinue: () -> Unit) {
        Column(
            modifier = modifier
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).fillMaxWidth().height(44.dp).background(
                    if (DataOrder.selectedItemMethod == null) colorResource(R.color.grey) else initColor()
                ).clickable {
                    if (DataOrder.selectedItemMethod != null) clickContinue()
                },
            ) {
                androidx.compose.material.Text(
                    "Tiếp tục", fontFamily = fontAppDefault, fontSize = 12.sp, color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

        }
    }


    @Composable
    private fun ShowMethodPayment(list: List<Methods>, onItemClick: (Methods) -> Unit) {
        val listMethodsAll = remember { mutableStateListOf<Methods>() }
        val maxItemLimit = 2

        DisposableEffect(DataOrder.isLimitItem, userInfo) {
            listMethodsAll.clear()
            // Chua login thi khong limit item phuong thuc thanh toan
            if (userInfo == null || (userInfo?.banks ?: arrayListOf()).isEmpty()) {
                listMethodsAll.addAll(ArrayList(list))
            } else {
                // Neu limit thi loc item limit
                if (DataOrder.isLimitItem && list.size > maxItemLimit) {
                    list.forEachIndexed { index, methods ->
                        if (index < maxItemLimit) {
                            listMethodsAll.add(methods)
                        }
                    }
                } else {
                    // Show tat ca neu nhan nut
                    listMethodsAll.addAll(ArrayList(list))
                }
            }

            onDispose {
                // Các tác vụ dọn dẹp, nếu có
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (methodDefault == PaymentMethod.WALLET) "Phương thức thanh toán" else "Chọn phương thức thanh toán",
                style = TextStyle(
                    fontWeight = FontWeight.W600, fontSize = 12.sp, fontFamily = fontAppBold
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier.clip(shape = RoundedCornerShape(12.dp)).background(Color.White)
                    .padding(vertical = 8.dp)
            ) {
                // Khong chon method nao.
                if (methodDefault == null || methodDefault.equals(PaymentMethod.DEFAULT)) listMethodsAll.forEachIndexed { index, item ->

                    Column {
                        ItemRow(item, item.code == DataOrder.selectedItemMethod) { ->
                            onItemClick(item)
                        }
                        if (item.code == PaymentMethod.LINK_BANK) {
                            LinkBank()
                        }
                        if (item.code != PaymentMethod.LINK_BANK && index != listMethodsAll.size - 1) Divider(
                            modifier = Modifier.padding(start = 50.dp),
                            color = Color(0XFFF1F3F4)
                        )
                    }

                } else Column {
                    // Chon 1 method nhat dinh
                    var methodFind: Methods? = null
                    listMethodsAll.forEach { item ->
                        if (item.code == methodDefault) {
                            methodFind = item
                            DataOrder.selectedItemMethod = methodFind!!.code
                            return@forEach
                        }
                    }
                    methodFind?.let { ItemRow(it, true) {} }
                }
                if (methodDefault != PaymentMethod.WALLET && (userInfo?.banks ?: arrayListOf()).isNotEmpty())
                    Column {
                        Box(
                            Modifier.padding(horizontal = 12.dp).padding(top = 12.dp).height(1.dp).fillMaxWidth()
                                .background(Color.Gray, shape = DottedShape(step = 5.dp))
                        )
                        Text(
                            if (DataOrder.isLimitItem) "Xem thêm phương thức khác" else "Đóng lại",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp)
                                .clickableWithoutRipple {
                                    DataOrder.isLimitItem = !DataOrder.isLimitItem
                                },
                            style = TextStyle(
                                color = colorResource(R.color.blue),
                                fontFamily = fontAppDefault, fontSize = 12.sp, fontWeight = FontWeight.W600
                            )
                        )
                    }
            }
        }
    }

    @Composable
    private fun LinkBank() {

        if (methodDefault != PaymentMethod.WALLET && (userInfo?.banks ?: arrayListOf()).isNotEmpty()) Column {

            // Info bank list
            userInfo?.banks?.let { banks ->
                banks.forEachIndexed { _, item ->
                    Row(
                        modifier = Modifier.background(colorResource(R.color.background))
                            .padding(horizontal = 12.dp, vertical = 10.dp).clickableWithoutRipple {
                                DataOrder.selectedItemMethod = PaymentMethod.LINK_BANK
                                // Lấy phi theo giống hình thức số dư ví, bởi vì thẻ lưu trên ví.
                                DataOrder.totalAmount = DataOrder.dataOrderSaved!!.data.feeData.wallet
                                bankTokenSelected = item
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.width(16.dp))
                        ImageFromUrl(item.getbLogo(), modifier = Modifier.size(36.dp))
                        Column(modifier = Modifier.padding(horizontal = 16.dp).weight(1f)) {
                            Text(
                                item.getbFullname(),
                                style = TextStyle(
                                    color = colorResource(R.color.black),
                                    fontWeight = FontWeight.W600,
                                    fontSize = 12.sp,
                                    fontFamily = fontAppDefault
                                )
                            )
                            Text(
                                item.getbAccount(),
                                style = TextStyle(
                                    color = colorResource(R.color.titleText),
                                    fontWeight = FontWeight.W400,
                                    fontSize = 12.sp,
                                    fontFamily = fontAppDefault
                                )
                            )
                        }
                        if (bankTokenSelected?.getbToken() == item.getbToken()) Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = initColor(),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    private fun getNameMerchant(): String {
        DataOrder.merchantInfo?.let {
            return (it.merchantName) ?: "Ví điện tử 9Pay"
        }
        return "Ví điện tử 9Pay"
    }

    @Composable
    private fun ItemRow(item: Methods, isChecked: Boolean, onItemClick: () -> Unit) {

        if (item.code == PaymentMethod.LINK_BANK && (userInfo?.banks ?: arrayListOf()).isEmpty()) {
            return
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp).clickableWithoutRipple {
                onItemClick()
            }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
        ) {
            // Image method
            // Neu la vi thi thay the sang logo thuong hieu rieng merchant. Khong thi mac dinh
            if (!(DataOrder.merchantInfo?.logo.isNullOrBlank()) && item.code.contains(PaymentMethod.WALLET)) ImageFromUrl(
                DataOrder.merchantInfo!!.logo!!, modifier = Modifier.width(36.dp).height(36.dp)
            ) else ImageFromUrl(
                item.icon, modifier = Modifier.width(36.dp).height(36.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween

            ) {
                val parseAmount: Int =
                    if (DataOrder.amount is Double) (DataOrder.amount as Double).toInt() else (DataOrder.amount as Int)
                val phone = userInfo?.phone
                Column {
                    if (item.code.equals(PaymentMethod.WALLET) && phone == null) Text(
                        getNameMerchant(),
                        style = TextStyle(fontWeight = FontWeight.W600, fontSize = 13.sp, fontFamily = fontAppBold)
                    )

                    if (item.code.equals(PaymentMethod.WALLET) && phone != null) Text(
                        "${getNameMerchant()}: $phone",
                        style = TextStyle(fontWeight = FontWeight.W600, fontSize = 13.sp, fontFamily = fontAppBold)
                    )

                    if (!item.code.equals(PaymentMethod.WALLET))
                        Text(
                            item.name,
                            style = TextStyle(
                                fontWeight = FontWeight.W600,
                                fontSize = 13.sp,
                                fontFamily = fontAppBold
                            )
                        )
                    userInfo?.balance?.let {
                        if (it < parseAmount && item.code.equals(PaymentMethod.WALLET)) Text(
                            "${AppUtils.formatMoney(it)} - Không đủ",
                            style = TextStyle(
                                fontWeight = FontWeight.W400,
                                fontSize = 12.sp,
                                fontFamily = fontAppDefault,
                                color = colorResource(R.color.yellow)
                            )
                        ) else if (item.code.equals(PaymentMethod.WALLET)) Text(
                            "Số dư ${AppUtils.formatMoney(it)}",
                            style = TextStyle(
                                fontWeight = FontWeight.W400,
                                fontSize = 12.sp,
                                fontFamily = fontAppDefault,
                                color = colorResource(R.color.grey)
                            )
                        )
                    }
                }

                if (userInfo != null && userInfo?.balance!! < parseAmount && item.code.equals(
                        PaymentMethod.WALLET
                    )
                ) Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.width(80.dp).height(28.dp).clip(RoundedCornerShape(14.dp))
                        .background(colorResource(R.color.background)).clickableWithoutRipple {
                            isProgressing = true
                            NPayLibrary.getInstance().openSDKWithAction(Actions.DEPOSIT)
                        }
                ) {
                    Text(
                        "Nạp tiền",
                        style = TextStyle(
                            color = initColor(),
                            fontSize = 12.sp,
                            fontFamily = fontAppDefault,
                            fontWeight = FontWeight.W600
                        )
                    )
                } else
                    Image(
                        painter = painterResource(if (isChecked) R.drawable.radio_checked else R.drawable.radio_no_check),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        colorFilter = ColorFilter.tint(color = if (isChecked) initColor() else colorResource(R.color.grey))
                    )
            }
        }
    }

    @Composable
    private fun ShowDepositDialog(onDismiss: () -> Unit = {}, onDeposit: () -> Unit = {}) {
        Dialog(onDismissRequest = {
            onDismiss()
        }) {
            Column(
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(color = Color.White).padding(12.dp)
                    .fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Số dư trong ví không đủ", color = Color.Black, fontSize = 14.sp, fontFamily = fontAppBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Số dư trong ví của bạn không đủ để thực hiện giao dịch, vui lòng nạp tiền để tiếp tục.",
                    color = colorResource(R.color.titleText),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    fontFamily = fontAppDefault
                )
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Quay lại",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).clickableWithoutRipple(onDismiss),
                        fontFamily = fontAppBold,
                        fontSize = 12.sp,
                        color = colorResource(R.color.black)
                    )
                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).fillMaxWidth().height(40.dp)
                            .background(
                                initColor()
                            ).clickableWithoutRipple {
                                onDismiss()
                                onDeposit()
                            }
                    ) {
                        Text(
                            "Nạp tiền",
                            textAlign = TextAlign.Center,
                            fontFamily = fontAppBold,
                            fontSize = 12.sp,
                            color = colorResource(R.color.white)
                        )
                    }
                }

            }
        }
    }
}
