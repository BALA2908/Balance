package com.balance.budget.data.local.crypto

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides the SQLCipher passphrase for the encrypted Room database.
 *
 * The passphrase is a 32-byte random value generated **once** on first launch
 * and stored in [EncryptedSharedPreferences], which is itself sealed by a
 * Keystore-backed master key (hardware-backed on the S24). The raw key never
 * leaves the device and is never logged. If you ever wipe app data, a new key
 * is generated and the old encrypted DB becomes unreadable (expected).
 */
@Singleton
class DatabaseKeyProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    /** Returns the DB passphrase bytes, creating and persisting one if needed. */
    fun getOrCreatePassphrase(): ByteArray {
        prefs.getString(KEY_PASSPHRASE, null)?.let {
            return Base64.decode(it, Base64.NO_WRAP)
        }
        val fresh = ByteArray(32).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_PASSPHRASE, Base64.encodeToString(fresh, Base64.NO_WRAP))
            .apply()
        return fresh
    }

    private companion object {
        const val PREFS_NAME = "balance_secrets"
        const val KEY_PASSPHRASE = "db_passphrase"
    }
}
