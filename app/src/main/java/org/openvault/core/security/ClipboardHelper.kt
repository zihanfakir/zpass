package org.openvault.core.security

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object ClipboardHelper {

    private var clearJob: Job? = null

    /**
     * Copies text to system clipboard, marks it sensitive on Android 13+,
     * and schedules an automatic clear after timeoutSeconds.
     */
    fun copyToClipboard(
        context: Context,
        label: String,
        text: String,
        isSensitive: Boolean = true,
        timeoutSeconds: Long = 30
    ) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val clip = ClipData.newPlainText(label, text)

        if (isSensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }

        clipboard.setPrimaryClip(clip)

        // Cancel previous timer
        clearJob?.cancel()

        if (timeoutSeconds > 0) {
            clearJob = CoroutineScope(Dispatchers.Main).launch {
                delay(timeoutSeconds * 1000L)
                try {
                    // Check if current clip still matches before clearing
                    val currentClip = clipboard.primaryClip
                    if (currentClip != null && currentClip.itemCount > 0) {
                        val currentText = currentClip.getItemAt(0).text?.toString()
                        if (currentText == text) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                clipboard.clearPrimaryClip()
                            } else {
                                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Safely ignore clipboard clearance error
                }
            }
        }
    }
}
