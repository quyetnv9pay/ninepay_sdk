package com.npsdk.jetpack_sdk

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.window.OnBackInvokedDispatcher
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.npsdk.R
import com.npsdk.jetpack_sdk.base.AppUtils
import com.npsdk.jetpack_sdk.base.Validator
import com.npsdk.jetpack_sdk.base.listener.CloseListener
import com.npsdk.jetpack_sdk.base.view.*
import com.npsdk.jetpack_sdk.repository.*
import com.npsdk.jetpack_sdk.repository.model.CreateOrderParamsInland
import com.npsdk.jetpack_sdk.repository.model.CreateOrderParamsInter
import com.npsdk.jetpack_sdk.theme.PaymentNinepayTheme
import com.npsdk.jetpack_sdk.theme.initColor
import com.npsdk.jetpack_sdk.viewmodel.AppViewModel
import com.npsdk.jetpack_sdk.viewmodel.InputViewModel
import com.npsdk.module.NPayLibrary
import com.npsdk.module.PaymentMethod
import com.npsdk.module.utils.Actions
import com.npsdk.module.utils.NameCallback

class InputCardActivity : ComponentActivity() {

    private var method: String? = null

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataOrder.isStartScreen = false
        val intent = intent.extras
        if (intent != null) {
            method = intent.getString("method")
        }
        DataOrder.dataOrderSaved = null
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                setDefaultAmount()
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                setDefaultAmount()
                finish()
            }
        }
        setContent {
            PaymentNinepayTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(topBar = {
                        TopAppBarApp(onBack = {
                            setDefaultAmount()
                            finish()

                        })
                    }, content = { paddingValues ->
                        Body(paddingValues = paddingValues)
                    })
                }
            }
        }

        CloseListener().listener(this)
    }

    override fun onDestroy() {
        CloseListener().cancelListener(this)
        super.onDestroy()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun Body(paddingValues: PaddingValues?) {
        val inputViewModel: InputViewModel = viewModel()
        val appViewModel: AppViewModel = viewModel()
        val context = LocalContext.current
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current
        var isSaveToken = false

        LaunchedEffect(true) {
            if (DataOrder.listBankModel == null) {
                appViewModel.showLoading()
                GetListBank().get(context) { response ->
                    appViewModel.hideLoading()
                    DataOrder.listBankModel = response
                }
            }
            CheckValidatePayment().check(context, DataOrder.urlData, CallbackOrder { data ->
                DataOrder.dataOrderSaved = data
                // Default wallet
                DataOrder.amount = data!!.data.amount
                setDefaultAmount()
                if (method == PaymentMethod.ATM_CARD) {
                    DataOrder.totalAmount = data.data.feeData.atmCard
                }

                if (method == PaymentMethod.CREDIT_CARD) {
                    var creditCard = DataOrder.dataOrderSaved!!.data.feeData.creditCard;
                    for (card in creditCard){
                        if (card.cardBrand == CreditCardEnum.VISA.name){
                            DataOrder.totalAmount = card.inLand
                        }
                    }
                }

            })

            if (AppUtils.isLogged()) {
                NPayLibrary.getInstance().getUserInfoSendToPayment(null)
            }
        }

        if (DataOrder.dataOrderSaved == null) {
            Box(modifier = Modifier.padding(top = 40.dp)) {
                ShimmerLoading()
            }
            return
        }
        Box(
            modifier = Modifier.fillMaxSize().background(colorResource(id = R.color.background))
                .padding(top = paddingValues!!.calculateTopPadding()).clickableWithoutRipple {
                    context.let {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }

                }, contentAlignment = Alignment.TopStart
        ) {
            LazyColumn(
                modifier = Modifier.padding(
                    start = 16.dp, end = 16.dp, top = 16.dp, bottom = 60.dp
                )
            ) {
                item {
                    if (DataOrder.dataOrderSaved != null) HeaderOrder(DataOrder.dataOrderSaved!!)
                }

                item {
                    if (method == PaymentMethod.ATM_CARD) CardInland(viewModel = inputViewModel) else if (method == PaymentMethod.CREDIT_CARD) CardInternational(
                        viewModel = inputViewModel
                    )
                }

//                item {
//                    SaveCardView(callBack = { isSaveToken = it })
//                }

                item {
                    if (inputViewModel.showNotification.value) {
                        DialogNotification(contextString = inputViewModel.stringDialog.value, onDismiss = {
                            inputViewModel.showNotification.value = false
                        })
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(50.dp))
                }

            }

            if (appViewModel.isShowLoading) Box(Modifier.align(Alignment.Center)) {
                LoadingView()
            }
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (method == PaymentMethod.CREDIT_CARD && DataOrder.dataOrderSaved != null) PolicyView()
                FooterButton(onClickBack = {
                    setDefaultAmount()
                    (context as Activity).finish()
                }, onClickContinue = {
                    DataOrder.activityOrder?.finish() // Close order activity

                    when (method) {
                        PaymentMethod.CREDIT_CARD -> createOrderInternational(
                            inputViewModel,
                            context,
                            appViewModel,
                            isSaveToken
                        )

                        PaymentMethod.ATM_CARD -> createOrderInland(
                            inputViewModel, context, appViewModel,
                            isSaveToken
                        )
                    }

                }, modifier = Modifier.background(colorResource(R.color.background)).fillMaxWidth().padding(12.dp)
                )
            }

        }
    }
}

@Composable
fun SaveCardView(callBack: (Boolean) -> Unit) {

    var isChecked by remember {
        mutableStateOf(false)
    }

    var showDialog by remember {
        mutableStateOf(false)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        // Nut check
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)).border(
            width = 1.dp,
            color = if (isChecked) initColor() else colorResource(R.color.grey),
            shape = RoundedCornerShape(4.dp)
        ).background(if (!isChecked) Color.White else initColor()).clickable {
            if (DataOrder.userInfo != null) {
                isChecked = !isChecked
                callBack(isChecked)
            } else showDialog = true
        }) {
            if (isChecked) Icon(
                Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp)
            )
        }
        Spacer(modifier = Modifier.width(13.dp))
        // Mo ta nut bam
        DescriptionSaveCard()
    }

    if (showDialog) ShowConfirmDialog(
        onDismiss = {
            showDialog = false
        },
        onLeft = {
            showDialog = false
//            isChecked = false
        }, onRight = {
            showDialog = false
            DataOrder.isProgressing = true
            NPayLibrary.getInstance().openSDKWithAction(Actions.LOGIN)
//            isChecked = !isChecked
//            callBack(isChecked)
        }, title = "Bạn sẽ được chuyển đến trang đăng nhập",
        content = "Bạn cần đăng nhập bằng số điện thoại để lưu lại liên kết thẻ. Vui lòng hoàn tất đăng nhập để tiến hành lưu lại liên kết thẻ.",
        titleLeft = "Bỏ qua",
        titleRight = "Đăng nhập"
    )
}

fun createOrderInternational(
    viewModel: InputViewModel,
    context: Context,
    appViewModel: AppViewModel,
    saveToken: Boolean
) {

    val numberCard = viewModel.numberOfCardInter.value
    val nameCard = viewModel.nameOfCardInter.value
    val exp = viewModel.expirationDateCardInter.value
    val ccv = viewModel.cvvCardInter.value

    viewModel.numberOfCardErrorInter.value = Validator.validateNumberCardInter(numberCard, viewModel)
    viewModel.nameOfCardErrorInter.value = Validator.validateNameCard(nameCard)
    viewModel.expirationDateCardErrorInter.value = Validator.validateExpirationCard(
        exp, viewModel.monthNonParseInter, viewModel.yearNonParseInter
    )
    viewModel.cvvCardErrorInter.value = Validator.validateCCVCard(ccv)

    if (viewModel.numberOfCardErrorInter.value.isNotBlank()) {
        return
    }
    if (viewModel.numberOfCardErrorInter.value.isNotBlank()) {
        return
    }

    if (viewModel.expirationDateCardErrorInter.value.isNotBlank()) {
        return
    }

    if (viewModel.cvvCardErrorInter.value.isNotBlank()) {
        return
    }

    val expCardStr = viewModel.expirationDateCardInter.value.split("/")
    val month: String = expCardStr.first()
    val year: String = expCardStr[1]
    var amount: Any? = DataOrder.totalAmount
    if (amount is Double) amount = amount.toInt()
    val params = CreateOrderParamsInter(
        url = DataOrder.urlData,
        cardNumber = viewModel.numberOfCardInter.value,
        cardName = viewModel.nameOfCardInter.value,
        expireMonth = month,
        expireYear = year,
        cvc = viewModel.cvvCardInter.value,
        amount = amount.toString(),
        method = PaymentMethod.CREDIT_CARD,
        saveToken = if (saveToken) 1 else 0
    )

    appViewModel.showLoading()
    CreateOrderInterRepo().create(context, params, CallbackCreateOrder {
        appViewModel.hideLoading()
        it.message?.let { it1 ->
            if (it.errorCode == 1) {
                viewModel.showNotification.value = true
                viewModel.stringDialog.value = it1
                NPayLibrary.getInstance().callBackToMerchant(
                    NameCallback.SDK_PAYMENT, false, null
                )
                NPayLibrary.getInstance().callbackError(2002, it1)
            } else if (it.errorCode == 0) {
                (context as Activity).finish() // Close input card
                openWebviewOTP(context, it.data!!.redirectUrl!!)
            }
        }

    })

}

fun createOrderInland(
    viewModel: InputViewModel, context: Context, appViewModel: AppViewModel,
    isSaveToken: Boolean
) {

    val numberCard = viewModel.numberCardInLand.value
    val nameCard = viewModel.nameCardInLand.value
    val dateCard = viewModel.dateCardInLand.value

    viewModel.numberCardErrorInLand.value = Validator.validateNumberCardATM(numberCard, inputViewModel = viewModel)
    viewModel.nameCardErrorInLand.value = Validator.validateNameCard(nameCard)
    viewModel.dateCardErrorInLand.value = Validator.validateDateCardInland(
        viewModel.dateCardInLand.value, viewModel.monthNonParseInLand, viewModel.yearNonParseInLand, viewModel
    )

    if (viewModel.numberCardErrorInLand.value.isNotBlank()) {
        return
    }
    if (viewModel.nameCardErrorInLand.value.isNotBlank()) {
        return
    }


    if (viewModel.dateCardErrorInLand.value.isNotBlank()) {
        return
    }

    val expCardStr = dateCard.split("/")
    val month: String = expCardStr.first()
    val year: String = expCardStr[1]
    var amount: Any? = DataOrder.totalAmount
    if (amount is Double) amount = amount.toInt()
    val params = CreateOrderParamsInland(
        url = DataOrder.urlData,
        cardNumber = numberCard,
        cardName = nameCard,
        expireMonth = month,
        expireYear = year,
        amount = amount.toString(),
        method = PaymentMethod.ATM_CARD,
        saveToken = if (isSaveToken) 1 else 0
    )

    appViewModel.showLoading()
    CreateOrderInlandRepo().create(context, params, CallbackCreateOrder {
        appViewModel.hideLoading()
        it.message?.let { it1 ->
            if (it.errorCode == 1) {
                viewModel.showNotification.value = true
                viewModel.stringDialog.value = it1
                NPayLibrary.getInstance().callBackToMerchant(
                    NameCallback.SDK_PAYMENT, false, null
                )
                NPayLibrary.getInstance().callbackError(2002, it1)
            } else if (it.errorCode == 0) {
                (context as Activity).finish() // Close input card
                openWebviewOTP(context, it.data!!.redirectUrl!!)
            }
        }
    })

}

fun setDefaultAmount() {
    try {
        DataOrder.amount?.let {
            DataOrder.totalAmount = it.toString().toInt()
        }

    } catch (e: Exception) {
        println("Error parse data")
    }
}