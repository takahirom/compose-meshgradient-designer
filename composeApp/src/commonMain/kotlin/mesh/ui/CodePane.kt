package mesh.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A read-only, monospaced view of the generated Kotlin [code] with a "Copy" button that writes it
 * to the clipboard via [copyToClipboard]. The button label briefly changes to confirm the copy.
 */
@Composable
fun CodePane(code: String, modifier: Modifier = Modifier) {
    var copied by remember { mutableStateOf(false) }
    // Reset the confirmation label whenever the code changes.
    LaunchedEffect(code) { copied = false }
    Column(modifier.background(Color(0xFF1E1E1E))) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Generated Kotlin", color = Color(0xFFCCCCCC), fontWeight = FontWeight.SemiBold)
            Button(onClick = {
                copyToClipboard(code)
                copied = true
            }) {
                Text(if (copied) "Copied!" else "Copy")
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Text(
                text = code,
                color = Color(0xFFEAEAEA),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
            )
        }
    }
}
