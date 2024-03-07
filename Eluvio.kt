package app.eluvio.deeplinks

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import java.net.URLEncoder

object Eluvio {
    enum class DeeplinkResult {
        /**
         * Media Wallet is installed and the deeplink was opened.
         */
        MEDIA_WALLET_LAUNCHED,

        /**
         * Media Wallet is not installed, relevant store has been opened.
         */
        STORE_LAUNCHED,

        /**
         * Media Wallet is not installed and no known app store was found.
         */
        STORE_NOT_FOUND,

        /**
         * The given link is not supported, or not an eluvio link.
         */
        UNSUPPORTED_LINK;
    }

    private const val MEDIA_WALLET_PACKAGE_NAME = "app.eluvio.wallet"
    private const val PLAY_STORE_PACKAGE_NAME = "com.android.vending"
    private const val AMAZON_STORE_PACKAGE_NAME = "com.amazon.venezia"

    /**
     * Creates a deep link to a specific SKU in Media Wallet.
     */
    @JvmStatic
    fun createSkuDeeplink(
        marketplace: String,
        sku: String,
    ): String = "elvwallet://items/$marketplace/ictr/$sku"

    /**
     * Launches Media Wallet with the given marketplace and SKU.
     * @see launchDeeplink for details on how Media Wallet is launched.
     */
    @JvmStatic
    @JvmOverloads
    fun launchSkuDeeplink(
        context: Context,
        marketplace: String,
        sku: String,
        jwt: String? = null
    ): DeeplinkResult {
        val url = createSkuDeeplink(marketplace, sku)
        return launchDeeplink(context, url, jwt)
    }

    /**
     * Checks if Media Wallet is installed on the device and launches it with the given URL.
     * If Media Wallet is not installed, the relevant app store (Google Play or Amazon App Store)
     * is opened and the URL is passed as an install referrer.
     */
    @JvmStatic
    @JvmOverloads
    fun launchDeeplink(context: Context, url: String, jwt: String? = null): DeeplinkResult {
        val urlWithJwt = if (jwt == null) url else
            Uri.parse(url).buildUpon().appendQueryParameter("jwt", jwt).build().toString()
        return when {
            !isLinkSupported(urlWithJwt) -> {
                DeeplinkResult.UNSUPPORTED_LINK
            }

            context.isPackageInstalled(MEDIA_WALLET_PACKAGE_NAME) -> {
                launchMediaWallet(context, urlWithJwt)
                DeeplinkResult.MEDIA_WALLET_LAUNCHED
            }

            else -> {
                launchStore(context, urlWithJwt)
            }
        }
    }

    /**
     * Launches Media Wallet with the given URL.
     */
    private fun launchMediaWallet(context: Context, url: String) {
        openUrl(context, url, MEDIA_WALLET_PACKAGE_NAME)
    }

    /**
     * Launches the relevant app store (Google Play or Amazon App Store) to download Media Wallet.
     * Forwards the URL as an install referrer for Google Play only (Amazon doesn't support this).
     */
    private fun launchStore(context: Context, url: String): DeeplinkResult {
        return when {
            context.isPackageInstalled(PLAY_STORE_PACKAGE_NAME) -> {
                val encodedUrl = URLEncoder.encode(url, "UTF-8")
                openUrl(
                    context,
                    "market://details?id=${MEDIA_WALLET_PACKAGE_NAME}&referrer=url%3D${encodedUrl}"
                )
                DeeplinkResult.STORE_LAUNCHED
            }

            context.isPackageInstalled(AMAZON_STORE_PACKAGE_NAME) -> {
                openUrl(context, "amzn://apps/android?p=${MEDIA_WALLET_PACKAGE_NAME}")
                DeeplinkResult.STORE_LAUNCHED
            }

            else -> DeeplinkResult.STORE_NOT_FOUND
        }
    }

    private fun isLinkSupported(url: String): Boolean {
        return url.startsWith("elvwallet://")
    }

    private fun openUrl(context: Context, url: String, packageName: String? = null) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        intent.setPackage(packageName)
        context.startActivity(intent)
    }

    private fun Context.isPackageInstalled(packageName: String): Boolean {
        return try {
            val flags = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(flags.toLong())
                )
            } else {
                @Suppress("DEPRECATION") packageManager.getPackageInfo(packageName, flags)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
