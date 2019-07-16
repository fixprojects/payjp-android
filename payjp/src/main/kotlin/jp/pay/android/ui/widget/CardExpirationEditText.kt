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
package jp.pay.android.ui.widget

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import com.google.android.material.textfield.TextInputEditText

// card number max digits size
private const val TOTAL_MAX_DIGITS = 4
// digits + 3 delimiters
private const val TOTAL_MAX_SYMBOLS = 5
private const val DELIMITER_INDEX = 2
private const val DELIMITER_CHAR = '/'

class CardExpirationEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : TextInputEditText(context, attrs) {

    init {
        addTextChangedListenerForFormat()
    }

    private fun addTextChangedListenerForFormat() {
        addTextChangedListener(object : TextWatcher {
            var latestChangeStart: Int = 0
            var latestInsertionSize: Int = 0

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                latestChangeStart = start
                latestInsertionSize = after
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                if (!isInputCorrect(s)) {
                    s.replace(0, s.length, buildCorrectString(createDigitArray(s)))
                }
            }

            private fun isInputCorrect(s: Editable): Boolean = when {
                s.length > TOTAL_MAX_SYMBOLS -> false
                // NOTE: When we delete 1 character from `11/`, input should be `11`.
                // But on the other hand, we expect `11/` when we input `11`.
                // So we allow two digit without `/` only after deletion.
                s.length == DELIMITER_INDEX -> latestChangeStart == 2 && latestInsertionSize == 0
                else -> (0 until s.length).all { i ->
                    val c = s[i]
                    when (i) {
                        DELIMITER_INDEX -> DELIMITER_CHAR == c
                        // index 0 should be 0 or 1 (month in 01 ~ 12).
                        0 -> c == '0' || c == '1'
                        else -> Character.isDigit(c)
                    }
                }
            }

            private fun buildCorrectString(digits: CharArray): String {
                val formatted = StringBuilder()

                for (i in digits.indices) {
                    val c = digits[i]
                    var index = i
                    if (i == 0 && c != '0' && c != '1') {
                        formatted.append('0')
                        index++
                    }
                    if (Character.isDigit(c)) {
                        formatted.append(c)
                        if (index < digits.size - 1 && index + 1 == DELIMITER_INDEX) {
                            formatted.append(DELIMITER_CHAR)
                        }
                    }
                }

                return formatted.toString()
            }

            private fun createDigitArray(s: Editable): CharArray {
                val digits = CharArray(TOTAL_MAX_DIGITS)
                var index = 0
                var i = 0
                while (i < s.length && index < TOTAL_MAX_DIGITS) {
                    val current = s[i]
                    if (Character.isDigit(current)) {
                        digits[index] = current
                        index++
                    }
                    i++
                }
                return digits
            }
        })
    }
}