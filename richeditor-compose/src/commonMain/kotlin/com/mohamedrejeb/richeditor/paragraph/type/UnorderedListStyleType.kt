package com.mohamedrejeb.richeditor.paragraph.type

@ConsistentCopyVisibility
public data class UnorderedListStyleType private constructor(
    internal val prefixes: List<String>,
) {
    public companion object {
        public fun from(vararg prefix: String): UnorderedListStyleType {
            return UnorderedListStyleType(prefix.toList())
        }

        public fun from(prefixes: List<String>): UnorderedListStyleType {
            return UnorderedListStyleType(prefixes)
        }

        public val Disc: UnorderedListStyleType = UnorderedListStyleType(
            prefixes = listOf("•")
        )

        public val Circle: UnorderedListStyleType = UnorderedListStyleType(
            prefixes = listOf("◦")
        )

        public val Square: UnorderedListStyleType = UnorderedListStyleType(
            prefixes = listOf("▪")
        )
    }
}