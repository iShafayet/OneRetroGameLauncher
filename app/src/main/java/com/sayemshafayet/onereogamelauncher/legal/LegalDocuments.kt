package com.sayemshafayet.onereogamelauncher.legal

object LegalDocuments {
    /** Bump when Terms or Privacy change materially — users must re-accept. */
    const val VERSION = 1

    const val PRIVACY_URL = "https://oneretrogamelauncher.com/privacy"
    const val TERMS_URL = "https://oneretrogamelauncher.com/terms"

    const val ASSET_PRIVACY = "legal/privacy-policy.md"
    const val ASSET_TERMS = "legal/terms-of-service.md"
}

enum class LegalDocumentKind(val title: String, val assetPath: String, val webUrl: String) {
    Privacy(
        title = "Privacy Policy",
        assetPath = LegalDocuments.ASSET_PRIVACY,
        webUrl = LegalDocuments.PRIVACY_URL,
    ),
    Terms(
        title = "Terms of Service",
        assetPath = LegalDocuments.ASSET_TERMS,
        webUrl = LegalDocuments.TERMS_URL,
    ),
}

fun isLegalAccepted(acceptedVersion: Int): Boolean =
    acceptedVersion >= LegalDocuments.VERSION
