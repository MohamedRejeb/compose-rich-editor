package com.mohamedrejeb.richeditor.paragraph.type

/**
 * The CSS `list-style-type` keywords of the predefined style types. A style type without a
 * keyword (a custom implementation, a [OrderedListStyleType.Multiple], custom bullet
 * prefixes) is not serializable, and a keyword without a style type is ignored on import.
 */
internal object ListStyleTypeKeywords {

    private val orderedByKeyword: Map<String, OrderedListStyleType> = mapOf(
        "decimal" to OrderedListStyleType.Decimal,
        "lower-alpha" to OrderedListStyleType.LowerAlpha,
        "lower-latin" to OrderedListStyleType.LowerAlpha,
        "upper-alpha" to OrderedListStyleType.UpperAlpha,
        "upper-latin" to OrderedListStyleType.UpperAlpha,
        "lower-roman" to OrderedListStyleType.LowerRoman,
        "upper-roman" to OrderedListStyleType.UpperRoman,
        "arabic-indic" to OrderedListStyleType.ArabicIndic,
        "arabic-abjad" to OrderedListStyleType.Arabic,
    )

    private val unorderedByKeyword: Map<String, UnorderedListStyleType> = mapOf(
        "disc" to UnorderedListStyleType.Disc,
        "circle" to UnorderedListStyleType.Circle,
        "square" to UnorderedListStyleType.Square,
    )

    fun orderedFromKeyword(keyword: String?): OrderedListStyleType? =
        keyword?.let { orderedByKeyword[it.trim().lowercase()] }

    fun unorderedFromKeyword(keyword: String?): UnorderedListStyleType? =
        keyword?.let { unorderedByKeyword[it.trim().lowercase()] }

    fun keywordOf(styleType: OrderedListStyleType?): String? =
        styleType?.let { type -> orderedByKeyword.entries.firstOrNull { it.value === type }?.key }

    fun keywordOf(styleType: UnorderedListStyleType?): String? =
        styleType?.let { type -> unorderedByKeyword.entries.firstOrNull { it.value == type }?.key }

    /** The keyword of the style type [paragraphType] carries, or null when it follows the config. */
    fun keywordOf(paragraphType: ParagraphType): String? =
        when (paragraphType) {
            is OrderedList -> keywordOf(paragraphType.styleTypeOverride)
            is UnorderedList -> keywordOf(paragraphType.styleTypeOverride)
            else -> null
        }
}
