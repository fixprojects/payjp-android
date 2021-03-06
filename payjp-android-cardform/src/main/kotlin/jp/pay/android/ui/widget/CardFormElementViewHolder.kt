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
package jp.pay.android.ui.widget

import android.os.Build
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.autofill.AutofillId
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import jp.pay.android.R
import jp.pay.android.model.CardBrand
import jp.pay.android.model.CardComponentInput
import jp.pay.android.plugin.CardScannerPlugin
import jp.pay.android.ui.extension.addOnTextChanged
import jp.pay.android.ui.extension.setErrorOrNull

internal typealias OnCardFormElementTextChanged = (type: CardFormElementType, s: CharSequence, start: Int, before: Int, count: Int) -> Unit
internal typealias OnCardFormElementEditorAction = (type: CardFormElementType, v: TextView, actionId: Int, event: KeyEvent?) -> Boolean
internal typealias OnCardFormElementFocusChanged = (type: CardFormElementType, view: View, hasFocus: Boolean) -> Unit
internal typealias OnCardFormElementKeyDownDeleteWithEmpty = (type: CardFormElementType, view: View) -> Boolean

internal sealed class CardFormElementViewHolder(
    type: CardFormElementType,
    parent: ViewGroup,
    layoutId: Int,
    inputLayoutId: Int,
    inputEditTextId: Int,
    onTextChanged: OnCardFormElementTextChanged,
    onEditorAction: OnCardFormElementEditorAction,
    onFocusChanged: OnCardFormElementFocusChanged,
    onKeyDownDeleteWithEmpty: OnCardFormElementKeyDownDeleteWithEmpty,
    autofillId: Any?
) : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layoutId, parent, false)) {

    private val inputTextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable) {}

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            onTextChanged.invoke(type, s, start, before, count)
        }
    }
    protected val inputLayout: TextInputLayout = itemView.findViewById(inputLayoutId)
    protected val editText: TextInputEditText = itemView.findViewById(inputEditTextId)

    init {
        editText.addTextChangedListener(inputTextWatcher)
        editText.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            onFocusChanged.invoke(type, v, hasFocus)
        }
        editText.setOnEditorActionListener { v, actionId, event ->
            onEditorAction.invoke(type, v, actionId, event)
        }
        editText.setOnKeyListener { v, keyCode, event ->
            takeIf {
                editText.text.isNullOrEmpty() &&
                    keyCode == KeyEvent.KEYCODE_DEL &&
                    event.action == KeyEvent.ACTION_DOWN
            }?.run {
                onKeyDownDeleteWithEmpty(type, v)
            } ?: false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            (autofillId as? AutofillId)?.let {
                editText.autofillId = it
            }
        }
    }

    protected fun setTextDisablingInputWatcher(input: CardComponentInput<*>?) {
        if (input?.input != editText.text.toString()) {
            editText.removeTextChangedListener(inputTextWatcher)
            editText.setText(input?.input)
            editText.addTextChangedListener(inputTextWatcher)
        }
    }

    protected fun setInputError(input: CardComponentInput<*>?, showErrorImmediately: Boolean) {
        inputLayout.setErrorOrNull(
            input?.errorMessage?.take(showErrorImmediately.not())?.let {
                itemView.resources.getString(it)
            }
        )
    }

    class CardFormNumberElement(
        parent: ViewGroup,
        cardNumberFormatter: CardNumberFormatTextWatcher,
        scannerPlugin: CardScannerPlugin?,
        onClickScannerIcon: View.OnClickListener?,
        onTextChanged: OnCardFormElementTextChanged,
        onEditorAction: OnCardFormElementEditorAction,
        onFocusChanged: OnCardFormElementFocusChanged,
        onNumberInputChanged: (s: CharSequence) -> Unit,
        onKeyDownDeleteWithEmpty: OnCardFormElementKeyDownDeleteWithEmpty,
        autofillId: Any?
    ) :
        CardFormElementViewHolder(
            CardFormElementType.Number,
            parent,
            R.layout.payjp_card_form_element_number_layout,
            R.id.input_layout_number,
            R.id.input_edit_number,
            onTextChanged,
            onEditorAction,
            onFocusChanged,
            onKeyDownDeleteWithEmpty,
            autofillId
        ) {

        init {
            editText.addTextChangedListener(cardNumberFormatter)
            editText.addOnTextChanged { s, _, _, _ ->
                onNumberInputChanged(s)
            }
            scannerPlugin?.run {
                inputLayout.endIconMode = TextInputLayout.END_ICON_CUSTOM
                onClickScannerIcon?.let {
                    inputLayout.setEndIconOnClickListener(it)
                }
            }
        }

        fun bindData(
            input: CardComponentInput.CardNumberInput?,
            showErrorImmediately: Boolean
        ) {
            setTextDisablingInputWatcher(input)
            setInputError(input, showErrorImmediately)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                editText.setAutofillHints(View.AUTOFILL_HINT_CREDIT_CARD_NUMBER)
            }
        }
    }

    class CardFormExpirationElement(
        parent: ViewGroup,
        expirationFormatter: TextWatcher,
        onTextChanged: OnCardFormElementTextChanged,
        onEditorAction: OnCardFormElementEditorAction,
        onFocusChanged: OnCardFormElementFocusChanged,
        onKeyDownDeleteWithEmpty: OnCardFormElementKeyDownDeleteWithEmpty,
        autofillId: Any?
    ) :
        CardFormElementViewHolder(
            CardFormElementType.Expiration,
            parent, R.layout.payjp_card_form_element_expiration_layout,
            R.id.input_layout_expiration,
            R.id.input_edit_expiration,
            onTextChanged,
            onEditorAction,
            onFocusChanged,
            onKeyDownDeleteWithEmpty,
            autofillId
        ) {

        init {
            editText.addTextChangedListener(expirationFormatter)
        }

        fun bindData(
            input: CardComponentInput.CardExpirationInput?,
            showErrorImmediately: Boolean
        ) {
            setTextDisablingInputWatcher(input)
            setInputError(input, showErrorImmediately)
            (editText as? CardExpirationEditText)?.expiration = input?.value
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                editText.setAutofillHints(View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DAY)
            }
        }
    }

    class CardFormHolderNameElement(
        parent: ViewGroup,
        onTextChanged: OnCardFormElementTextChanged,
        onEditorAction: OnCardFormElementEditorAction,
        onFocusChanged: OnCardFormElementFocusChanged,
        onKeyDownDeleteWithEmpty: OnCardFormElementKeyDownDeleteWithEmpty,
        autofillId: Any?
    ) :
        CardFormElementViewHolder(
            CardFormElementType.HolderName,
            parent,
            R.layout.payjp_card_form_element_holder_name_layout,
            R.id.input_layout_holder_name,
            R.id.input_edit_holder_name,
            onTextChanged,
            onEditorAction,
            onFocusChanged,
            onKeyDownDeleteWithEmpty,
            autofillId
        ) {

        fun bindData(
            input: CardComponentInput.CardHolderNameInput?,
            showErrorImmediately: Boolean
        ) {
            setTextDisablingInputWatcher(input)
            setInputError(input, showErrorImmediately)
        }
    }

    class CardFormCvcElement(
        parent: ViewGroup,
        onTextChanged: OnCardFormElementTextChanged,
        onEditorAction: OnCardFormElementEditorAction,
        onFocusChanged: OnCardFormElementFocusChanged,
        onKeyDownDeleteWithEmpty: OnCardFormElementKeyDownDeleteWithEmpty,
        autofillId: Any?
    ) :
        CardFormElementViewHolder(
            CardFormElementType.Cvc,
            parent,
            R.layout.payjp_card_form_element_cvc_layout,
            R.id.input_layout_cvc,
            R.id.input_edit_cvc,
            onTextChanged,
            onEditorAction,
            onFocusChanged,
            onKeyDownDeleteWithEmpty,
            autofillId
        ) {
        private var brand: CardBrand = CardBrand.UNKNOWN
            set(value) {
                if (field != value) {
                    editText.filters =
                        arrayOf<InputFilter>(InputFilter.LengthFilter(value.cvcLength))
                }
                field = value
            }

        fun bindData(
            input: CardComponentInput.CardCvcInput?,
            brand: CardBrand,
            showErrorImmediately: Boolean
        ) {
            setTextDisablingInputWatcher(input)
            setInputError(input, showErrorImmediately)
            this.brand = brand
        }
    }
}
