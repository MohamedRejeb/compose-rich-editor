package com.mohamedrejeb.richeditor.compose.spellcheck.utils

internal fun applyCapitalizationStrategy(source: String, target: String): String {
    fun isAllUpperCase(str: String): Boolean = str.all { it.isUpperCase() || !it.isLetter() }
    fun isAllLowerCase(str: String): Boolean = str.all { it.isLowerCase() || !it.isLetter() }
    fun isTitleCase(str: String): Boolean {
        val words = str.split(" ")
        return words.size > 1 && words.all {
            it.isNotEmpty() && it[0].isUpperCase() && it.substring(1)
                .all { char -> char.isLowerCase() }
        }
    }

    fun isCollapsedTitleCase(str: String): Boolean {
        return str.length > 2 && str[0].isUpperCase() && str[1].isLowerCase() && str.substring(2)
            .any { char -> char.isUpperCase() }
    }

    fun isInitialCaps(str: String): Boolean =
        str.isNotEmpty() && str[0].isUpperCase() && str.substring(1)
            .all { it.isLowerCase() || !it.isLetter() }

    fun makeTitleCase(target: String): String {
        return target.split(" ").joinToString(" ") { word ->
            if (word.isNotEmpty()) word[0].uppercase() + word.substring(1).lowercase() else word
        }
    }

    return when {
        isAllUpperCase(source) -> target.uppercase()
        isAllLowerCase(source) -> target.lowercase()
        isTitleCase(source) -> makeTitleCase(target)
        isCollapsedTitleCase(source) -> makeTitleCase(target)
        isInitialCaps(source) -> target.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        else -> target
    }
}