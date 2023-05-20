package com.mohamedrejeb.richeditor.utils

import kotlin.test.Test
import kotlin.test.assertEquals

internal class FloatUtilsTest {

    @Test
    fun testMaxDecimals() {
        val float = 12.123457f
        val float2 = 0.49999f
        val float3 = 0.5f
        val float4 = 0.50001f
        val float5 = 0.0000000f
        val float6 = 5f

        assertEquals(
            12.123f,
            float.maxDecimals(3)
        )
        assertEquals(
            0.5f,
            float2.maxDecimals(1)
        )
        assertEquals(
            0.5f,
            float3.maxDecimals(1)
        )
        assertEquals(
            0.5f,
            float4.maxDecimals(1)
        )
        assertEquals(
            0f,
            float5.maxDecimals(1)
        )
        assertEquals(
            5f,
            float6.maxDecimals(1)
        )
    }

}