package com.mohamedrejeb.richeditor.json

import kotlin.test.Test
import kotlin.test.assertEquals

class RichTextJsonSchemaTest {
    @Test
    fun `current schema version is 1`() {
        assertEquals(1, CURRENT_JSON_SCHEMA_VERSION)
    }
}
