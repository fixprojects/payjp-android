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
package jp.pay.android

object Payjp {

    private var tokenService: PayjpTokenService? = null
    private var configuration: PayjpConfiguration? = null

    /**
     * Initialize with publicKey.
     *
     * @param publicKey public API Key. You must **NOT** use secret key.
     */
    @JvmStatic
    fun init(publicKey: String): Payjp {
        return init(PayjpConfiguration.Builder(publicKey).build())
    }

    /**
     * Initialize with configuration.
     *
     * @param configuration configuration for [Payjp]
     */
    @JvmStatic
    fun init(configuration: PayjpConfiguration): Payjp {
        this.configuration = configuration
        val payjpToken = PayjpToken(configuration.tokenConfiguration())
        this.tokenService = payjpToken
        PayjpCardForm.configure(
            debugEnabled = configuration.debugEnabled,
            tokenService = payjpToken,
            cardScannerPlugin = configuration.cardScannerPlugin,
            handler = configuration.tokenBackgroundHandler,
            callbackExecutor = configuration.callbackExecutor
        )
        return this
    }

    @JvmStatic
    fun token() = checkNotNull(tokenService) {
        "You must call Payjp.init(publicKey)"
    }

    @JvmStatic
    fun cardForm() = PayjpCardForm
}
