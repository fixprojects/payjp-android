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
package jp.pay.android.network

import jp.pay.android.exception.PayjpRequiredTdsException
import jp.pay.android.model.ThreeDSecureToken
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Intercept 3DS redirect
 *
 * It will throw [PayjpRequiredTdsException] with capturing id from location.
 */
internal class TdsRedirectionInterceptor : Interceptor {

    companion object {
        private val REGEX_TDS_PATH = """\A/v1/tds/([\w\d_]+)/.*\z""".toRegex()
    }

    override fun intercept(c: Interceptor.Chain): Response {
        val request = c.request()
        val response = c.proceed(request)
        if (response.isRedirect) {
            response.header("location")?.let { location ->
                REGEX_TDS_PATH.find(location)?.destructured
            }?.let { (id) ->
                throw PayjpRequiredTdsException(ThreeDSecureToken(id = id))
            }
        }
        return response
    }
}
