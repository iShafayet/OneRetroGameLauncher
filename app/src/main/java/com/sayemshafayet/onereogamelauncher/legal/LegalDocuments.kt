package com.sayemshafayet.onereogamelauncher.legal

object LegalDocuments {
    /** Bump when Terms or Privacy change materially — users must re-accept. */
    const val VERSION = 1

    const val PRIVACY_URL = "https://oneretrogamelauncher.com/privacy"
    const val TERMS_URL = "https://oneretrogamelauncher.com/terms"
}

enum class LegalDocumentKind(val title: String, val webUrl: String) {
    Privacy(
        title = "Privacy Policy",
        webUrl = LegalDocuments.PRIVACY_URL,
    ),
    Terms(
        title = "Terms of Service",
        webUrl = LegalDocuments.TERMS_URL,
    ),
}

fun isLegalAccepted(acceptedVersion: Int): Boolean =
    acceptedVersion >= LegalDocuments.VERSION
