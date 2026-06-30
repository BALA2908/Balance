package com.balance.budget.data.receipt

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * On-device receipt OCR (ML Kit Latin text recognition, bundled model). Pure
 * recognition only — parsing the amount/merchant is the deterministic
 * [com.balance.budget.domain.receipt.ReceiptParser]'s job. Stays on the device.
 */
object ReceiptScanner {
    suspend fun recognizeText(context: Context, imageUri: Uri): String =
        suspendCancellableCoroutine { cont ->
            try {
                val image = InputImage.fromFilePath(context, imageUri)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                recognizer.process(image)
                    .addOnSuccessListener { cont.resume(it.text) }
                    .addOnFailureListener { cont.resumeWithException(it) }
                cont.invokeOnCancellation { recognizer.close() }
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }
}
