package com.mohamedrejeb.richeditor.sample.common.spellcheck

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.darkrockstudios.symspellkt.api.SpellChecker
import com.darkrockstudios.symspellkt.common.SpellCheckSettings
import com.darkrockstudios.symspellkt.impl.SymSpell
import com.darkrockstudios.symspellkt.impl.loadUniGramLine
import com.mohamedrejeb.richeditor.common.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
@Composable
fun rememberSampleSpellChecker(): MutableState<SpellChecker?> {
    val scope = rememberCoroutineScope()
    val spellChecker = remember { mutableStateOf<SpellChecker?>(null) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.Default) {
            val checker = SymSpell(spellCheckSettings = SpellCheckSettings(topK = 5))

            Res.readBytes("files/en-80k.txt")
                .decodeToString()
                .lineSequence()
                .forEach { line ->
                    checker.dictionary.loadUniGramLine(line)
                    yield()
                }

            spellChecker.value = checker
        }
    }

    return spellChecker
}
