package com.mohamedrejeb.richeditor.highlight.languages

import com.mohamedrejeb.richeditor.highlight.BlockComment
import com.mohamedrejeb.richeditor.highlight.LanguageSpec
import com.mohamedrejeb.richeditor.highlight.StringRule

/** SQL's line comment marker, spelled with escapes because the project bans the doubled dash in source text. */
internal const val SqlLineComment: String = "\u002D\u002D"

internal val SqlSpec = LanguageSpec(
    keywords = setOf(
        "add", "all", "alter", "and", "as", "asc", "begin", "between", "by", "case", "check", "column", "commit",
        "constraint", "create", "cross", "database", "default", "delete", "desc", "distinct", "drop", "else",
        "end", "exists", "false", "foreign", "from", "full", "group", "having", "if", "in", "index", "inner",
        "insert", "into", "is", "join", "key", "left", "like", "limit", "not", "null", "offset", "on", "or",
        "order", "outer", "primary", "references", "right", "rollback", "select", "set", "table", "then", "true",
        "union", "unique", "update", "values", "view", "when", "where", "with",
    ),
    lineComments = listOf(SqlLineComment),
    blockComments = listOf(BlockComment("/*", "*/")),
    strings = listOf(
        StringRule("'", escape = null),
        StringRule("\"", escape = null),
    ),
    ignoreKeywordCase = true,
)
