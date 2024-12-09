package com.mohamedrejeb.richeditor.sample.common.spellcheck

import androidx.compose.runtime.*
import com.darkrockstudios.symspell.fdic.loadFdicFile
import com.darkrockstudios.symspellkt.api.SpellChecker
import com.darkrockstudios.symspellkt.common.SpellCheckSettings
import com.darkrockstudios.symspellkt.impl.SymSpell
import com.mohamedrejeb.richeditor.common.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
@Composable
fun rememberSampleSpellChecker(): MutableState<SpellChecker?> {
    val scope = rememberCoroutineScope()
    val spellChecker = remember { mutableStateOf<SpellChecker?>(null) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.Default) {
            val checker = SymSpell(spellCheckSettings = SpellCheckSettings(topK = 5))
            checker.dictionary.loadFdicFile(Res.readBytes("files/en-80k.fdic"))
            spellChecker.value = checker
        }
    }

    return spellChecker
}
