// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import helium314.keyboard.settings.ui.PocketScreen
import helium314.keyboard.settings.ui.RowDivider
import helium314.keyboard.settings.ui.SectionLabel
import helium314.keyboard.settings.ui.SettingsCard
import helium314.keyboard.settings.ui.SettingsRow

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import helium314.keyboard.latin.dictionary.Dictionary
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.Links
import helium314.keyboard.latin.common.LocaleUtils
import helium314.keyboard.latin.common.LocaleUtils.localizedDisplayName
import helium314.keyboard.latin.common.splitOnWhitespace
import helium314.keyboard.latin.dictionary.DictionaryInfoUtils
import helium314.keyboard.latin.dictionary.getDictionaryLocales
import helium314.keyboard.latin.utils.IntentUtils
import helium314.keyboard.latin.utils.SubtypeSettings
import helium314.keyboard.latin.utils.htmlToAnnotated
import helium314.keyboard.latin.utils.locale
import helium314.keyboard.latin.utils.withHtmlLink
import helium314.keyboard.settings.SearchScreen
import helium314.keyboard.latin.utils.Theme
import helium314.keyboard.settings.dialogs.ConfirmationDialog
import helium314.keyboard.settings.dialogs.DictionaryDialog
import helium314.keyboard.settings.dictionaryFilePicker
import helium314.keyboard.settings.initPreview
import helium314.keyboard.latin.utils.previewDark
import java.io.File
import java.util.Locale

@Composable
fun DictionaryScreen(
    onClickBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val enabledLanguages = SubtypeSettings.getEnabledSubtypes(true).map { it.locale().language }
    val cachedDictFolders = DictionaryInfoUtils.getCacheDirectories(ctx).map { it.name }
    val comparer = compareBy<Locale>({ it.language !in enabledLanguages }, { it.toLanguageTag() !in cachedDictFolders }, { it.displayName })
    val dictionaryLocales = remember { getDictionaryLocales(ctx).sortedWith(comparer) }
    var selectedLocale: Locale? by remember { mutableStateOf(null) }
    var showAddDictDialog by remember { mutableStateOf(false) }
    val dictPicker = dictionaryFilePicker(selectedLocale)
    PocketScreen(stringResource(R.string.pp_dictionaries), onClickBack) {
        SettingsCard {
            SettingsRow(
                stringResource(R.string.sc_add_dictionary),
                stringResource(R.string.sc_add_dictionary_sub),
                R.drawable.ic_pp_add,
                { showAddDictDialog = true }
            ) {}
        }
        SectionLabel(stringResource(R.string.sc_dictionaries))
        SettingsCard {
            dictionaryLocales.forEachIndexed { index, locale ->
                if (index > 0) RowDivider()
                val (dicts, hasInternal) = getUserAndInternalDictionaries(ctx, locale)
                val types = dicts.mapTo(mutableListOf()) { it.name.substringBefore("_${DictionaryInfoUtils.USER_DICTIONARY_SUFFIX}") }
                if (hasInternal && !types.contains(Dictionary.TYPE_MAIN))
                    types.add(0, stringResource(R.string.internal_dictionary_summary))
                SettingsRow(
                    locale.localizedDisplayName(LocalResources.current),
                    types.joinToString(", ").ifBlank { null },
                    R.drawable.ic_settings_languages,
                    { selectedLocale = locale }
                ) {}
            }
        }
    }
    if (showAddDictDialog) {
        ConfirmationDialog(
            onDismissRequest = { showAddDictDialog = false },
            onConfirmed = {
                val intent = IntentUtils.getResolvableTypeIntent(ctx, "*/*")
                    .setAction(Intent.ACTION_OPEN_DOCUMENT)
                dictPicker.launch(intent)
            },
            title = { Text(stringResource(R.string.add_new_dictionary_title)) },
            content = {
                val link = stringResource(R.string.dictionary_link_text).withHtmlLink(Links.DICTIONARY_URL)
                val addDictString = stringResource(R.string.add_dictionary, link)
                Text(addDictString.htmlToAnnotated())
            }
        )
    }
    if (selectedLocale != null) {
        DictionaryDialog(
            onDismissRequest = { selectedLocale = null },
            locale = selectedLocale!!
        )
    }
}

/** @return list of user dictionary files and whether an internal dictionary exists */
fun getUserAndInternalDictionaries(context: Context, locale: Locale): Pair<List<File>, Boolean> {
    val userDicts = mutableListOf<File>()
    var hasInternalDict = false
    val userLocaleDir = DictionaryInfoUtils.getCacheDirectoryForLocale(locale, context)?.let { File(it) }
    if (userLocaleDir?.exists() == true && userLocaleDir.isDirectory) {
        userLocaleDir.listFiles()?.forEach {
            if (it.name.endsWith(DictionaryInfoUtils.USER_DICTIONARY_SUFFIX))
                userDicts.add(it)
            else if (it.name.startsWith(DictionaryInfoUtils.MAIN_DICT_PREFIX))
                hasInternalDict = true
        }
    }
    if (hasInternalDict)
        return userDicts to true
    val internalDicts = DictionaryInfoUtils.getAssetsDictionaryList(context) ?: return userDicts to false
    val best = LocaleUtils.getBestMatch(locale, internalDicts.toList()) {
        DictionaryInfoUtils.extractLocaleFromAssetsDictionaryFile(it)
    }
    return userDicts to (best != null)
}

@Preview
@Composable
private fun Preview() {
    initPreview(LocalContext.current)
    Theme(previewDark) {
        Surface {
            DictionaryScreen { }
        }
    }
}
