/*
 *
 * Copyright (c) 2020 PAY, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package jp.pay.android.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.google.android.material.snackbar.Snackbar
import jp.pay.android.PayjpCardForm
import jp.pay.android.R
import jp.pay.android.model.CardBrand
import jp.pay.android.model.TenantId
import jp.pay.android.model.ThreeDSecureToken
import jp.pay.android.model.Token
import jp.pay.android.ui.extension.showWith
import jp.pay.android.ui.widget.PayjpAcceptedBrandsView
import jp.pay.android.ui.widget.PayjpCardFormAbstractFragment
import jp.pay.android.ui.widget.PayjpCardFormView
import jp.pay.android.util.nonNull
import jp.pay.android.verifier.PayjpVerifier
import jp.pay.android.verifier.ui.PayjpThreeDSecureResultCallback

/**
 * PayjpCardFormActivity show card form.
 *
 */
internal class PayjpCardFormActivity : AppCompatActivity(R.layout.payjp_card_form_activity),
    PayjpCardFormView.OnValidateInputListener,
    PayjpCardFormView.CardFormEditorListener {

    internal companion object {
        const val DEFAULT_CARD_FORM_REQUEST_CODE = 1
        private const val FRAGMENT_CARD_FORM = "FRAGMENT_CARD_FORM"
        private const val EXTRA_KEY_TENANT = "EXTRA_KEY_TENANT"
        private const val CARD_FORM_EXTRA_KEY_TOKEN = "DATA"

        fun start(activity: Activity, requestCode: Int?, tenant: TenantId?) {
            activity.startActivityForResult(
                Intent(activity, PayjpCardFormActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .apply {
                        if (tenant != null) {
                            putExtra(EXTRA_KEY_TENANT, tenant.id)
                        }
                    },
                requestCode ?: DEFAULT_CARD_FORM_REQUEST_CODE
            )
        }

        fun start(fragment: Fragment, requestCode: Int?, tenant: TenantId?) {
            fragment.startActivityForResult(
                Intent(fragment.requireActivity(), PayjpCardFormActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .apply {
                        if (tenant != null) {
                            putExtra(EXTRA_KEY_TENANT, tenant.id)
                        }
                    },
                requestCode ?: DEFAULT_CARD_FORM_REQUEST_CODE
            )
        }

        fun onActivityResult(data: Intent?, callback: PayjpCardFormResultCallback) {
            val token = data?.getParcelableExtra<Token>(CARD_FORM_EXTRA_KEY_TOKEN)
            val result = if (token != null) {
                PayjpCardFormResult.Success(token = token)
            } else {
                PayjpCardFormResult.Canceled
            }
            callback.onResult(result)
        }
    }

    private val tenantId: TenantId? by lazy {
        intent?.getStringExtra(EXTRA_KEY_TENANT)?.let { TenantId(it) }
    }
    private val inputMethodManager: InputMethodManager by lazy {
        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }
    private var cardFormFragment: PayjpCardFormAbstractFragment? = null
    private var viewModel: CardFormScreenViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.payjp_card_form_screen_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setUpUI()
        cardFormFragment = findCardFormFragment()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        PayjpVerifier.handleThreeDSecureResult(requestCode,
            PayjpThreeDSecureResultCallback { result ->
                viewModel?.onCompleteCardVerify(result)
            })
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }

    override fun onValidateInput(view: PayjpCardFormView, isValid: Boolean) {
        viewModel?.onValidateInput(isValid)
    }

    override fun onLastFormEditorActionDone(
        view: PayjpCardFormView,
        textView: TextView,
        event: KeyEvent?
    ): Boolean {
        performSubmitButton(textView.windowToken)
        return true
    }

    private fun setUpUI() {
        val acceptedBrandsView = findViewById<PayjpAcceptedBrandsView>(R.id.accepted_brands)
        val submitButton = findViewById<Button>(R.id.card_form_button)
        val submitButtonProgress = findViewById<ProgressBar>(R.id.card_form_button_progress)
        submitButton.setOnClickListener {
            performSubmitButton(it.windowToken)
        }
        val loadingView = findViewById<ViewGroup>(R.id.loading_view)
        val errorView = findViewById<ViewGroup>(R.id.error_view)
        val reloadContentButton = findViewById<Button>(R.id.reload_content_button)
        reloadContentButton.setOnClickListener {
            viewModel?.onClickReload()
        }
        val errorMessageView = findViewById<TextView>(R.id.error_message)
        val contentView = findViewById<ViewGroup>(R.id.content_view)

        val vmFactory = CardFormScreenViewModel.Factory(
            owner = this,
            tokenService = checkNotNull(PayjpCardForm.tokenService()) {
                "You must initialize Payjp first"
            },
            tenantId = tenantId,
            errorTranslator = ContextErrorTranslator(this),
            tokenHandlerExecutor = PayjpCardForm.tokenHandlerExecutor()
        )
        viewModel = ViewModelProvider(this, vmFactory).get(CardFormScreenViewModel::class.java)
            .also { vm ->
                lifecycle.addObserver(vm)
                vm.acceptedBrands.nonNull().observe(this, acceptedBrandsView::setAcceptedBrands)
                vm.contentViewVisibility.observe(this, contentView::setVisibility)
                vm.errorViewVisibility.observe(this, errorView::setVisibility)
                vm.loadingViewVisibility.observe(this, loadingView::setVisibility)
                vm.reloadContentButtonVisibility.observe(this, reloadContentButton::setVisibility)
                vm.submitButtonVisibility.observe(this, submitButton::setVisibility)
                vm.submitButtonProgressVisibility.observe(this, submitButtonProgress::setVisibility)
                vm.submitButtonIsEnabled.observe(this, submitButton::setEnabled)
                vm.errorViewText.observe(this, errorMessageView::setText)
                vm.addCardFormCommand.nonNull().observe(this) { brands ->
                    cardFormFragment = addCardFormFragment(brands.toTypedArray())
                    viewModel?.onAddedCardForm()
                }
                vm.errorDialogMessage.nonNull().observe(this, this::showErrorMessage)
                vm.success.nonNull().observe(this, this::finishWithSuccess)
                vm.startVerifyCommand.nonNull().observe(this, this::startVerify)
                vm.snackBarMessage.nonNull().observe(this, this::showSnackBarMessage)
            }
    }

    private fun addCardFormFragment(acceptedBrands: Array<CardBrand>): PayjpCardFormAbstractFragment? {
        return supportFragmentManager.let { manager ->
            PayjpCardForm.newFragment2(
                holderNameEnabled = true,
                tenantId = tenantId,
                acceptedBrands = acceptedBrands
            ).also { fragment ->
                manager
                    .beginTransaction().apply {
                        replace(R.id.card_form_view, fragment, FRAGMENT_CARD_FORM)
                    }
                    .commit()
            }
        }
    }

    private fun findCardFormFragment(): PayjpCardFormAbstractFragment? {
        return supportFragmentManager.let { manager ->
            (manager.findFragmentByTag(FRAGMENT_CARD_FORM) as? PayjpCardFormAbstractFragment)?.also { f ->
                if (!f.isAdded) {
                    manager
                        .beginTransaction().apply {
                            replace(R.id.card_form_view, f, FRAGMENT_CARD_FORM)
                        }
                        .commit()
                }
            }
        }
    }

    private fun performSubmitButton(windowToken: IBinder) {
        inputMethodManager.hideSoftInputFromWindow(windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        cardFormFragment?.let { cardForm ->
            if (cardForm.isValid) {
                viewModel?.onCreateToken(cardForm.createToken())
            }
        }
    }

    private fun showSnackBarMessage(@StringRes message: Int) {
        Snackbar.make(findViewById(R.id.content_view), message, Snackbar.LENGTH_SHORT).show()
        viewModel?.onDisplaySnackBarMessage()
    }

    private fun showErrorMessage(message: CharSequence) {
        AlertDialog.Builder(this)
            .setTitle(R.string.payjp_card_form_dialog_title_error)
            .setMessage(message)
            .setNegativeButton(R.string.payjp_card_form_dialog_ok, null)
            .create()
            .showWith(this)
        viewModel?.onDisplayedErrorMessage()
    }

    private fun finishWithSuccess(token: Token) {
        val data = Intent().putExtra(CARD_FORM_EXTRA_KEY_TOKEN, token)
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    private fun startVerify(tdsToken: ThreeDSecureToken) {
        PayjpVerifier.startThreeDSecureFlow(tdsToken, this)
        viewModel?.onStartedVerify()
    }
}
