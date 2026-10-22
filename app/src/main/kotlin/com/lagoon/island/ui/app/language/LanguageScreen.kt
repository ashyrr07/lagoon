package com.lagoon.island.ui.app.language

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.lagoon.island.R
import com.lagoon.island.ui.app.SettingsViewModel
import com.lagoon.island.ui.app.components.Note
import com.lagoon.island.ui.app.components.ScreenScaffold
import com.lagoon.island.ui.app.components.SettingGroup
import com.lagoon.island.util.SystemScreens

/** A language Lagoon ships, named in its own language so anyone can find theirs. */
data class AppLanguage(val tag: String, val nativeName: String)

val SupportedLanguages = listOf(
    AppLanguage("en", "English"),
    AppLanguage("ru", "Русский"),
    AppLanguage("de", "Deutsch"),
    AppLanguage("fr", "Français"),
    AppLanguage("es", "Español"),
)

/** The in-app language tag currently applied, "" when following the phone. */
fun currentLanguageTag(): String =
    AppCompatDelegate.getApplicationLocales().get(0)?.language.orEmpty()

@Composable
fun LanguageScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(currentLanguageTag()) }

    fun choose(tag: String) {
        if (tag == selected) return
        selected = tag
        // Keep a copy for the overlay on Android 12, then let AppCompat apply it (recreates this screen).
        viewModel.update { it.copy(language = tag) }
        AppCompatDelegate.setApplicationLocales(
            if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
        )
    }

    ScreenScaffold(
        title = stringResource(R.string.language_title),
        subtitle = stringResource(R.string.language_subtitle),
        onBack = onBack,
    ) {
        SettingGroup(null, modifier = Modifier.selectableGroup()) {
            row {
                LanguageRow(
                    title = stringResource(R.string.language_system),
                    detail = stringResource(R.string.language_system_detail),
                    selected = selected.isEmpty(),
                    onClick = { choose("") },
                )
            }
            SupportedLanguages.forEach { language ->
                row {
                    LanguageRow(
                        title = language.nativeName,
                        detail = null,
                        selected = selected == language.tag,
                        onClick = { choose(language.tag) },
                    )
                }
            }
        }
        Note(stringResource(R.string.language_note))
        if (Build.VERSION.SDK_INT >= 33) {
            TextButton(onClick = { SystemScreens.appLanguage(context) }) {
                Text(stringResource(R.string.language_open_system))
            }
        }
    }
}

@Composable
private fun LanguageRow(title: String, detail: String?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .heightIn(min = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
