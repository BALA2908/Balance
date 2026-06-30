package com.balance.budget.feature.receipt

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.balance.budget.core.util.Money
import com.balance.budget.data.receipt.ReceiptScanner
import com.balance.budget.domain.receipt.ReceiptParser
import kotlinx.coroutines.launch
import java.io.File

/**
 * "Scan a receipt": capture a photo with the system camera, OCR it on-device, and
 * open Quick Add **pre-filled** via the existing deep link — so it flows through
 * the one save path (the user just confirms). No auto-save, no second writer.
 */
@Composable
fun ScanReceiptButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var working by remember { mutableStateOf(false) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingUri
        if (success && uri != null) {
            working = true
            scope.launch {
                val parsed = runCatching {
                    ReceiptParser.parse(ReceiptScanner.recognizeText(context, uri))
                }.getOrNull()
                working = false
                val link = Uri.parse("balance://quickadd").buildUpon().apply {
                    parsed?.amountMinor?.let { appendQueryParameter("amount", Money.formatPlain(it).replace(",", "")) }
                    parsed?.merchant?.let { appendQueryParameter("merchant", it) }
                }.build()
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, link).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            }
        }
    }

    OutlinedButton(
        onClick = {
            val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
            val file = File(dir, "receipt_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            pendingUri = uri
            launcher.launch(uri)
        },
        enabled = !working,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (working) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
                Icon(Icons.Outlined.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Text(
                text = if (working) "  Reading receipt…" else "  Scan a receipt",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}
