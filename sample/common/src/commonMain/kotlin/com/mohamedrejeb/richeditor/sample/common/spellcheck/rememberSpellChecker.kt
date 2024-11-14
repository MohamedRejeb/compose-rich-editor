package com.mohamedrejeb.richeditor.sample.common.spellcheck

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.darkrockstudios.symspellkt.api.SpellChecker
import com.darkrockstudios.symspellkt.impl.SymSpell
import com.darkrockstudios.symspellkt.impl.loadUniGramLine
import com.mohamedrejeb.richeditor.common.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
@Composable
fun rememberSpellChecker(): SpellChecker? {
    val scope = rememberCoroutineScope()
    var spellChecker by remember { mutableStateOf<SpellChecker?>(null) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.Default) {
            val checker = SymSpell()

            Res.readBytes("files/en-80k.txt")
                .decodeToString()
                .lineSequence()
                .forEachAsync { line ->
                    checker.dictionary.loadUniGramLine(line)
                    yield()
                }

            spellChecker = checker
        }
    }

    return spellChecker
}

private suspend fun <T> Sequence<T>.forEachAsync(
    action: suspend (T) -> Unit
) {
    for (item in this) {
        action(item)
    }
}
