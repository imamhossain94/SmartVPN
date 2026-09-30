package com.newagedevs.smartvpn

import com.newagedevs.smartvpn.extensions.formatSize
import com.newagedevs.smartvpn.extensions.round
import com.newagedevs.smartvpn.model.VpnServer
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the pure-JVM logic that used to throw or return garbage.
 *
 * The XML parsers are deliberately not covered here: they go through
 * `org.xmlpull.v1`, which is an Android runtime class and is only stubbed in
 * local unit tests, so asserting on them would require an instrumented test.
 */
class VpnServerParsingTest {

    @Test
    fun `round returns zero instead of throwing on NaN`() {
        // A zero elapsed time used to produce NaN, and BigDecimal(NaN) threw a
        // NumberFormatException that escaped the speed test.
        val nan = 0.0 / 0.0
        assertTrue(nan.isNaN())
        assertEquals(0.0, nan.round(2), 0.0)
    }

    @Test
    fun `round returns zero instead of throwing on infinity`() {
        val inf = 1.0 / 0.0
        assertTrue(inf.isInfinite())
        assertEquals(0.0, inf.round(2), 0.0)
    }

    @Test
    fun `round clamps a normal value`() {
        assertEquals(12.35, 12.3456.round(2), 0.0001)
    }

    @Test
    fun `int round is a no-op for non finite input`() {
        // `Int.round` is the "keep at least this many decimals" form: the
        // receiver is the scale. NaN and Infinity must not reach BigDecimal.
        assertEquals(0.0, 2.round(0.0 / 0.0), 0.0)
        assertEquals(0.0, 2.round(1.0 / 0.0), 0.0)
        assertEquals(1.23, 2.round(1.2345), 0.0001)
    }

    @Test
    fun `formatSize handles zero and sub kilobyte values`() {
        assertEquals("0 B/s", formatSize(0))
        assertEquals("512 B/s", formatSize(512))
    }

    @Test
    fun `formatSize scales up through the units`() {
        assertTrue(formatSize(1024).endsWith("KB"))
        assertTrue(formatSize(1024L * 1024).endsWith("MB"))
    }

    @Test
    fun `gson round trips a server list`() {
        val servers = listOf(VpnServer(hostname = "a", ip = "1.2.3.4", ping = "10", speed = "1 MB/s"))
        val json = Gson().toJson(servers)
        val type = object : TypeToken<List<VpnServer>>() {}.type
        val restored = Gson().fromJson<List<VpnServer>>(json, type)
        assertEquals(1, restored!!.size)
        assertEquals("1.2.3.4", restored.first().ip)
        assertEquals("a", restored.first().hostname)
    }

    @Test
    fun `gson returns null for the literal null rather than crashing`() {
        // Gson hands back null for this, and the repository used to pass that
        // straight to sortedBy(), which threw on the first access.
        val type = object : TypeToken<List<VpnServer>>() {}.type
        assertNull(Gson().fromJson<List<VpnServer>>("null", type))
    }

    @Test
    fun `servers sort by ping with unparseable values last`() {
        // The old code sorted nulls first, which promoted a broken server to
        // the auto-selected "best" one.
        val servers = listOf(
            VpnServer(hostname = "no-ping", ping = ""),
            VpnServer(hostname = "slow", ping = "300"),
            VpnServer(hostname = "fast", ping = "12"),
        )
        val sorted = servers.sortedBy { it.ping.toIntOrNull() ?: Int.MAX_VALUE }
        assertEquals(listOf("fast", "slow", "no-ping"), sorted.map { it.hostname })
    }
}
