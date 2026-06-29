package com.balance.budget.service.parser

/** UPI apps whose payment notifications we parse (on-device, opt-in). */
enum class UpiApp(val packageName: String, val displayName: String) {
    GPAY("com.google.android.apps.nbu.paisa.user", "Google Pay"),
    PHONEPE("com.phonepe.app", "PhonePe"),
    PAYTM("net.one97.paytm", "Paytm");

    companion object {
        fun fromPackage(pkg: String): UpiApp? = entries.firstOrNull { it.packageName == pkg }
        val packages: Set<String> = entries.map { it.packageName }.toSet()
    }
}

/** A debit parsed from a payment notification, before user confirmation. */
data class ParsedPayment(
    val amountMinor: Long,
    val merchant: String?,
    val app: UpiApp,
    val rawText: String,
    val postedAt: Long,
)
