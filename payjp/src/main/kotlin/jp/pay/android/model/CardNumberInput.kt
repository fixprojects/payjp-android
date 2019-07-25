/*
 *
 * Copyright (c) 2019 PAY, Inc.
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
package jp.pay.android.model

import jp.pay.android.R
import jp.pay.android.validator.CardNumberValidatorService
import jp.pay.android.validator.CardNumberValidator

internal data class CardNumberInput(
    val input: String?,
    val acceptedBrands: List<CardBrand>?,
    val brandDetector: CardBrandDetectable = CardBrandDetector,
    val cardNumberValidator: CardNumberValidatorService = CardNumberValidator
) : CardComponentInput<String> {

    val brand: CardBrand = input?.filter(Character::isDigit)
        ?.let(brandDetector::detectWithDigits)
        ?: CardBrand.UNKNOWN

    override val errorMessage: FormInputError?
    override val value: String?

    init {
        val digits = input?.filter(Character::isDigit)
        errorMessage = when {
            digits.isNullOrEmpty() -> FormInputError(messageId = R.string.payjp_card_form_error_no_number, lazy = true)
            !cardNumberValidator.isCardNumberLengthValid(digits) ->
                FormInputError(messageId = R.string.payjp_card_form_error_invalid_number, lazy = true)
            !cardNumberValidator.isLuhnValid(digits) ->
                FormInputError(messageId = R.string.payjp_card_form_error_invalid_number, lazy = false)
            brand != CardBrand.UNKNOWN ->
                FormInputError(messageId = R.string.payjp_card_form_error_invalid_brand, lazy = true)
            acceptedBrands?.contains(brand) == false ->
                FormInputError(messageId = R.string.payjp_card_form_error_invalid_brand, lazy = false)
            else -> null
        }
        value = digits.takeIf { errorMessage == null }
    }
}