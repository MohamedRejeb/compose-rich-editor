package com.mohamedrejeb.richeditor.paragraph.type

public enum class ListNestedLevel(
    internal val indentMultiplier: Float,
    internal val number: Int // the Number Must be the same as Level
) {
    LEVEL_1(indentMultiplier = 1f, number = 1),
    LEVEL_2(indentMultiplier = 2f, number = 2),
    LEVEL_3(indentMultiplier = 3f, number = 3);

    internal fun getNextOrMax(): ListNestedLevel {
        return when (this) {
            LEVEL_1 -> LEVEL_2
            LEVEL_2 -> LEVEL_3
            LEVEL_3 -> LEVEL_3
        }
    }

    internal fun getPreviousOrMin(): ListNestedLevel {
        return when (this) {
            LEVEL_1 -> LEVEL_1
            LEVEL_2 -> LEVEL_1
            LEVEL_3 -> LEVEL_2
        }
    }


    internal companion object {
        val maxNestedLevel = LEVEL_3

        fun getByNumber(number: Int): ListNestedLevel? {
            return when (number) {
                1 -> LEVEL_1
                2 -> LEVEL_2
                3 -> LEVEL_3
                else -> null
            }
        }
    }
}