package com.mocoding.richeditor.android

import com.mocoding.richeditor.App
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                App()
            }
        }
    }
}

data class Difference(val addedChars: List<Pair<Char, Int>>, val removedChars: List<Pair<Char, Int>>)

fun getDifference(originalText: String, modifiedText: String): Difference {
    val addedChars = mutableListOf<Pair<Char, Int>>()
    val removedChars = mutableListOf<Pair<Char, Int>>()

    // Find added and removed characters
    var i = 0
    var j = 0
    while (i < originalText.length && j < modifiedText.length) {
        if (originalText[i] == modifiedText[j]) {
            i++
            j++
        } else {
            // Check if a character was removed
            var k = i + 1
            while (k < originalText.length && originalText[k] != modifiedText[j]) {
                removedChars.add(originalText[k] to k)
                k++
            }
            if (k == originalText.length) {
                removedChars.add(originalText[i] to i)
                i++
            } else {
                addedChars.add(modifiedText[j] to k)
                j++
                i = k + 1
            }
        }
    }

    // Check if any remaining characters were removed
    while (i < originalText.length) {
        removedChars.add(originalText[i] to i)
        i++
    }

    // Check if any remaining characters were added
    while (j < modifiedText.length) {
        addedChars.add(modifiedText[j] to j)
        j++
    }

    return Difference(addedChars, removedChars)
}

fun main() {
    val originalText = "The quick brown fox"
    val modifiedText = "The brown fox"

    val difference = getDifference(originalText, modifiedText)
    println(difference)
}