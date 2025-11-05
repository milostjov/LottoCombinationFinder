// AdsMediationSetup.kt
package com.zoqo.lottocombinationfinder.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.ump.*

object AdsMediationSetup {
    private const val TAG = "AdsMediationSetup"

    fun initMobileAds(context: Context) {
        val config = RequestConfiguration.Builder()
            .setTestDeviceIds(listOf("EB2E150CD191D64D50A5038B152AF5EB")) // zameni realnim ID-evima
            .build()
        MobileAds.setRequestConfiguration(config)

        MobileAds.initialize(context) { status ->
            Log.d(TAG, "MobileAds initialized: $status")
        }
    }

    /**
     * Pozovi pre loadovanja oglasa.
     * Ako je forma potrebna, biće prikazana; u svakom slučaju onReady() se zove.
     */
    fun obtainConsent(
        activity: Activity,
        onReady: () -> Unit
    ) {
        val params = ConsentRequestParameters.Builder()
            // .setTagForUnderAgeOfConsent(false)
            .build()

        val consentInfo = UserMessagingPlatform.getConsentInformation(activity)

        consentInfo.requestConsentInfoUpdate(
            activity,
            params,
            {
                if (consentInfo.isConsentFormAvailable) {
                    // Učitaj formu
                    UserMessagingPlatform.loadConsentForm(
                        activity,
                        { form ->
                            if (consentInfo.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
                                form.show(activity) {
                                    // nakon zatvaranja forme (bilo koji ishod) nastavi
                                    onReady()
                                }
                            } else {
                                onReady()
                            }
                        },
                        { formError: FormError ->
                            Log.w(TAG, "UMP form load error: code=${formError.errorCode}, msg=${formError.message}")
                            onReady()
                        }
                    )
                } else {
                    onReady()
                }
            },
            { formError: FormError ->
                Log.w(TAG, "UMP request error: code=${formError.errorCode}, msg=${formError.message}")
                onReady()
            }
        )
    }
}
