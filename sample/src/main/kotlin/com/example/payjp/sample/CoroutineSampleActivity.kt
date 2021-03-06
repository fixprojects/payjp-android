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
package com.example.payjp.sample

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import jp.pay.android.Payjp
import jp.pay.android.coroutine.createTokenSuspend
import jp.pay.android.coroutine.getTokenSuspend
import jp.pay.android.exception.PayjpThreeDSecureRequiredException
import jp.pay.android.model.ThreeDSecureToken
import jp.pay.android.model.Token
import jp.pay.android.ui.widget.PayjpCardFormAbstractFragment
import jp.pay.android.verifier.ui.PayjpThreeDSecureResultCallback
import kotlinx.android.synthetic.main.activity_card_form_view_sample.button_create_token
import kotlinx.android.synthetic.main.activity_card_form_view_sample.button_create_token_with_validate
import kotlinx.android.synthetic.main.activity_card_form_view_sample.button_get_token
import kotlinx.android.synthetic.main.activity_card_form_view_sample.layout_buttons
import kotlinx.android.synthetic.main.activity_card_form_view_sample.progress_bar
import kotlinx.android.synthetic.main.activity_card_form_view_sample.switch_card_holder_name
import kotlinx.android.synthetic.main.activity_card_form_view_sample.text_token_content
import kotlinx.android.synthetic.main.activity_card_form_view_sample.text_token_id
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val FRAGMENT_CARD_FORM = "FRAGMENT_CARD_FORM"

class CoroutineSampleActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    private lateinit var cardFormFragment: PayjpCardFormAbstractFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_form_view_sample)
        findCardFormFragment()
        button_create_token.setOnClickListener {
            createToken()
        }
        button_create_token_with_validate.setOnClickListener {
            if (cardFormFragment.validateCardForm()) {
                createToken()
            }
        }

        button_get_token.setOnClickListener {
            getToken(text_token_id.text.toString())
        }

        switch_card_holder_name.setOnCheckedChangeListener { _, isChecked ->
            cardFormFragment.setCardHolderNameInputEnabled(isChecked)
        }
    }

    override fun onDestroy() {
        cancel()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Payjp.verifier().handleThreeDSecureResult(
            requestCode,
            PayjpThreeDSecureResultCallback {
                if (it.isSuccess()) {
                    createToken(tdsToken = it.retrieveThreeDSecureToken())
                } else {
                    Toast.makeText(this, "3-D Secure canceled.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun createToken(tdsToken: ThreeDSecureToken? = null) = launch {
        layout_buttons.visibility = View.INVISIBLE
        progress_bar.visibility = View.VISIBLE
        text_token_content.visibility = View.INVISIBLE
        try {
            val token = withContext(Dispatchers.IO) {
                if (tdsToken == null) {
                    cardFormFragment.createTokenSuspend()
                } else {
                    Payjp.token().createTokenSuspend(tdsToken)
                }
            }
            updateSuccessUI(token)
        } catch (e: PayjpThreeDSecureRequiredException) {
            Payjp.verifier().startThreeDSecureFlow(e.token, this@CoroutineSampleActivity)
        } catch (t: Throwable) {
            updateErrorUI(t, "failure creating token")
        }
    }

    private fun getToken(id: String) = launch {
        layout_buttons.visibility = View.INVISIBLE
        progress_bar.visibility = View.VISIBLE
        text_token_content.visibility = View.INVISIBLE
        try {
            val token = withContext(Dispatchers.IO) { Payjp.token().getTokenSuspend(id) }
            updateSuccessUI(token)
        } catch (t: Throwable) {
            updateErrorUI(t, "failure retrieving token")
        }
    }

    private fun updateSuccessUI(token: Token) {
        Log.i("CardFormViewSample", "token => $token")
        text_token_id.setText(token.id)
        text_token_content.text = token.toString()
        progress_bar.visibility = View.GONE
        layout_buttons.visibility = View.VISIBLE
        text_token_content.visibility = View.VISIBLE
    }

    private fun updateErrorUI(t: Throwable, message: String) {
        Log.e("CardFormViewSample", message, t)
        text_token_content.text = t.toString()
        progress_bar.visibility = View.GONE
        layout_buttons.visibility = View.VISIBLE
        text_token_content.visibility = View.VISIBLE
    }

    private fun findCardFormFragment() {
        supportFragmentManager.let { manager ->
            val f = manager.findFragmentByTag(FRAGMENT_CARD_FORM) as? PayjpCardFormAbstractFragment
            cardFormFragment = f ?: Payjp.cardForm().newCardFormFragment()
            if (!cardFormFragment.isAdded) {
                manager
                    .beginTransaction().apply {
                        replace(R.id.card_form_view, cardFormFragment, FRAGMENT_CARD_FORM)
                    }
                    .commit()
            }
        }
    }
}
