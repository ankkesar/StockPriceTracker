package com.mbkfx.stockpricetracker.domain.model

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for ConnectionStatus enum
 */
class ConnectionStatusTest {

  @Test
  fun `ConnectionStatus has all expected values`() {
    val values = ConnectionStatus.values()
    assertEquals(4, values.size)
    assertTrue(values.contains(ConnectionStatus.OFFLINE))
    assertTrue(values.contains(ConnectionStatus.CONNECTED))
    assertTrue(values.contains(ConnectionStatus.ERROR))
    assertTrue(values.contains(ConnectionStatus.CONNECTING))
  }

  @Test
  fun `ConnectionStatus valueOf works correctly`() {
    assertEquals(ConnectionStatus.OFFLINE, ConnectionStatus.valueOf("OFFLINE"))
    assertEquals(ConnectionStatus.CONNECTED, ConnectionStatus.valueOf("CONNECTED"))
    assertEquals(ConnectionStatus.ERROR, ConnectionStatus.valueOf("ERROR"))
    assertEquals(ConnectionStatus.CONNECTING, ConnectionStatus.valueOf("CONNECTING"))
  }
}

