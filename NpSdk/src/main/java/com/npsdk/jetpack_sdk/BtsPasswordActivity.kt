import androidx.compose.material.ExperimentalMaterialApi
import com.npsdk.jetpack_sdk.theme.PaymentNinepayTheme

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.npsdk.R
import com.npsdk.jetpack_sdk.DataOrder
import com.npsdk.jetpack_sdk.ResultPayment
import com.npsdk.jetpack_sdk.base.view.*
import com.npsdk.jetpack_sdk.repository.*
import com.npsdk.jetpack_sdk.repository.model.CreateOrderParamsWallet
import com.npsdk.jetpack_sdk.theme.fontAppDefault
import com.npsdk.jetpack_sdk.theme.initColor
import com.npsdk.jetpack_sdk.viewmodel.AppViewModel
import com.npsdk.jetpack_sdk.viewmodel.InputViewModel
import com.npsdk.module.NPayLibrary
import com.npsdk.module.PaymentMethod
import com.npsdk.module.utils.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PasswordBottomSheet(
    focusRequester: FocusRequester = FocusRequester(),
    keyboardController: SoftwareKeyboardController? = LocalSoftwareKeyboardController.current,
    bottomSheetState: ModalBottomSheetState
) {
    val context = LocalContext.current
    val appViewModel: AppViewModel = viewModel()
    val inputViewModel: InputViewModel = viewModel()
    var passwordSaved by remember { mutableStateOf("") }
    var errTextPassword by remember { mutableStateOf("") }

    DisposableEffect(bottomSheetState.isVisible) {
        if (!bottomSheetState.isVisible) {
            passwordSaved = ""
            errTextPassword = ""

        }
        onDispose { }
    }

    PaymentNinepayTheme {
        Box(
            modifier = Modifier
                .background(colorResource(id = R.color.background))
        ) {
            LazyColumn(horizontalAlignment = Alignment.CenterHorizontally) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White) // White background for the app bar text
                                .padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = "Nhập mật khẩu",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.Black,
                                fontFamily = fontAppDefault,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        Divider()
                        Spacer(modifier = Modifier.height(20.dp))
                        Image(
                            painter = painterResource(id = R.drawable.bts_password),
                            contentDescription = "logo",
                            modifier = Modifier
                                .size(85.dp)
                        )
                        FieldPassword(onTextChanged = { password ->
                            passwordSaved = password
                            if (passwordSaved.length == 6) {
                                errTextPassword = ""
                                // Verify password and create order, payment.
                                appViewModel.showLoading()
                                VerifyPassword().check(context, passwordSaved, CallbackVerifyPassword {
                                    // Message khac null tuc la co loi xay ra, show loi ra man hinh
                                    if (it != null) {
                                        appViewModel.hideLoading()
                                        errTextPassword = it
                                        return@CallbackVerifyPassword
                                    } else {
                                        // Khong co loi gi, tao order
                                        createOrderWallet(context, inputViewModel, appViewModel, passwordSaved,
                                            messageError = { callback ->
                                                inputViewModel.showNotification.value = true
                                                inputViewModel.stringDialog.value = callback
                                            })
                                    }
                                })
                            }
                        }, errTextPassword, focusRequester, keyboardController, bottomSheetState)
                        Footer1(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            clickContinue = {
                                if (appViewModel.isShowLoading) {
                                    return@Footer1
                                }
                                if (passwordSaved.length < 6) {
                                    errTextPassword = "Vui lòng nhập mật khẩu để tiếp tục!"
                                    return@Footer1
                                }
                                // Verify password and create order, payment.
                                appViewModel.showLoading()
                                VerifyPassword().check(context, passwordSaved, CallbackVerifyPassword {
                                    // Message khac null tuc la co loi xay ra, show loi ra man hinh
                                    if (it != null) {
                                        appViewModel.hideLoading()
                                        errTextPassword = it
                                        return@CallbackVerifyPassword
                                    } else {
                                        // Khong co loi gi, tao order
                                        createOrderWallet(context, inputViewModel, appViewModel, passwordSaved,
                                            messageError = { callback ->
                                                inputViewModel.showNotification.value = true
                                                inputViewModel.stringDialog.value = callback
                                            })
                                    }
                                })

                            })
                    }
                }
                item {
                    if (inputViewModel.showNotification.value) {
                        DialogNotification(contextString = inputViewModel.stringDialog.value, onDismiss = {
                            inputViewModel.showNotification.value = false
                        })
                    }
                }
                item {
                    if (appViewModel.isShowLoading) {
                        LaunchedEffect(Unit) {
                            delay(30000)
                            appViewModel.isShowLoading = false
                        }
                        Dialog(
                            onDismissRequest = { appViewModel.isShowLoading = false },
                            DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(White, shape = RoundedCornerShape(8.dp))
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun FieldPassword(
    onTextChanged: (String) -> Unit = {},
    errTextPassword: String,
    focusRequester: FocusRequester = FocusRequester(),
    keyboardController: SoftwareKeyboardController? = LocalSoftwareKeyboardController.current,
    bottomSheetState: ModalBottomSheetState
) {
    val context = LocalContext.current
    var passwordStr by remember { mutableStateOf("") }

    LaunchedEffect(errTextPassword) {
        if (errTextPassword.isNotBlank()) {
            if (errTextPassword != "Vui lòng nhập mật khẩu để tiếp tục!") {
                passwordStr = ""
            }
        }
    }

    DisposableEffect(bottomSheetState.isVisible) {
        if (!bottomSheetState.isVisible) {
            passwordStr = ""
        }
        onDispose { }
    }

    Box(
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 50.dp, vertical = 16.dp)
        ) {
            Text(
                "Vui lòng nhập mật khẩu",
                textAlign = TextAlign.Center,
                style = TextStyle(
                    color = colorResource(R.color.black),
                    fontSize = 12.sp,
                    fontFamily = fontAppDefault
                )
            )
            Spacer(modifier = Modifier.height(20.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.clickableWithoutRipple {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }) {
                // Hide copy, cut, paste
                CompositionLocalProvider(LocalTextToolbar provides EmptyTextToolbar) {
                    // PIN Field Password
                    BasicTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .focusRequester(focusRequester),
                        maxLines = 1,
                        minLines = 1,
                        singleLine = true,
                        cursorBrush = SolidColor(Color.White),
                        textStyle = TextStyle(colorResource(R.color.white)),
                        keyboardOptions = KeyboardOptions(
                            autoCorrect = false,
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.NumberPassword
                        ),
                        value = passwordStr,
                        onValueChange = {
                            if (it.length <= 6) {
                                passwordStr = it
                                onTextChanged(passwordStr)
                            }
                        })
                }
                LineDot(passwordStr.length)
            }


            Text(
                errTextPassword, style = TextStyle(
                    color = colorResource(R.color.red),
                    fontSize = 11.sp,
                    fontFamily = fontAppDefault
                )
            )

            Text(
                modifier = Modifier.clickableWithoutRipple {
                    DataOrder.isProgressing = true
                    // Gọi sang webview login
                    val phone = Preference.getString(context, Flavor.prefKey + Constants.PHONE, "")
                    NPayLibrary.getInstance().openSDKWithAction(Actions.forgotPassword(phone))
                },
                text = "Quên mật khẩu?",
                style = TextStyle(
                    color = initColor(),
                    fontSize = 14.sp,
                    fontFamily = fontAppDefault
                )
            )

        }
    }
}

@Composable
private fun LineDot(lengthPin: Int) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(6) { index ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(
                        if (lengthPin <= index) colorResource(R.color.grey) else initColor(),
                        shape = CircleShape
                    ),
            ) {
            }
        }

    }
}

object EmptyTextToolbar : TextToolbar {
    // Hide copy, cut, paste
    override val status: TextToolbarStatus = TextToolbarStatus.Hidden
    override fun hide() {}
    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) {
    }
}

@Composable
private fun Footer1(
    modifier: Modifier,
    clickContinue: () -> Unit
) {
    Column(
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .fillMaxWidth()
                .height(44.dp)
                .background(initColor())
                .clickable {
                    clickContinue()
                },
        ) {
            Text(
                "Tiếp tục", fontFamily = fontAppDefault, fontSize = 12.sp, color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

private fun createOrderWallet(
    context: Context,
    inputView: InputViewModel,
    appViewModel: AppViewModel,
    password: String,
    messageError: (String) -> Unit
) {
    appViewModel.showLoading()
    val params = CreateOrderParamsWallet(
        url = DataOrder.urlData, method = PaymentMethod.WALLET,
        amount = DataOrder.totalAmount.toString()
    )
    CreateOrderWalletRepo().create(context, params, CallbackCreateOrder {

        it.message?.let { it1 ->
            if (it.errorCode == 1) {
                NPayLibrary.getInstance().callBackToMerchant(
                    NameCallback.SDK_PAYMENT, false, null
                )
                NPayLibrary.getInstance().callbackError(2002, it1)
                appViewModel.hideLoading()
                inputView.showNotification.value = true
                inputView.stringDialog.value = it1
            } else if (it.errorCode == 0) {
                val orderId = Utils.convertUrlToOrderId(it.data!!.redirectUrl!!)
                CreatePayment().create(context, orderId, CallbackCreatePayment { paymentId, message ->
                    run {
                        if (paymentId == null) {
                            NPayLibrary.getInstance().callBackToMerchant(
                                NameCallback.SDK_PAYMENT, false, null
                            )
                            appViewModel.hideLoading()
                            inputView.showNotification.value = true
                            inputView.stringDialog.value = message
                            NPayLibrary.getInstance().callbackError(2002, it1)
                            return@run
                        } else {
                            // Tạo payment ví.
                            VerifyPayment().create(
                                context,
                                paymentId,
                                password,
                                CallbackVerifyPayment { message: String? ->
                                    appViewModel.hideLoading()
                                    if (message != null) {
                                        NPayLibrary.getInstance().callBackToMerchant(
                                            NameCallback.SDK_PAYMENT, false, null
                                        )
                                        messageError(message)
                                    } else {
                                        (context as Activity).finish() // Close screen
                                        DataOrder.activityOrder?.finish()
                                        if (DataOrder.isShowResultScreen) {
                                            // Move to result screen
                                            val intent = Intent(context, ResultPayment::class.java)
                                            intent.putExtra("status", Constants.SUCCESS)
                                            context.startActivity(intent)
                                        } else {
                                            // Done
                                            NPayLibrary.getInstance().callBackToMerchant(
                                                NameCallback.SDK_PAYMENT, true, null
                                            )
                                            NPayLibrary.getInstance().listener.onCloseSDK()
                                        }
                                    }
                                })
                        }
                    }
                })
            }
        }
    })
}