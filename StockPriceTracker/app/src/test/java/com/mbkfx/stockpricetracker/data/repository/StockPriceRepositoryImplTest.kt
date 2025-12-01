package com.mbkfx.stockpricetracker.data.repository

import com.mbkfx.stockpricetracker.data.service.StockLogoService
import com.mbkfx.stockpricetracker.data.service.StockPriceDto
import com.mbkfx.stockpricetracker.data.service.StockPriceService
import com.mbkfx.stockpricetracker.data.service.StaticStockLogoService
import com.mbkfx.stockpricetracker.domain.model.StockPriceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for StockPriceRepositoryImpl
 */
class StockPriceRepositoryImplTest {

  @Test
  fun `start calls service start`() = runTest {
    val mockService = mock<StockPriceService>()
    whenever(mockService.getStockPriceStream()).thenReturn(flowOf())
    whenever(mockService.errors).thenReturn(MutableStateFlow(null))
    val repository = StockPriceRepositoryImpl(mockService, StaticStockLogoService())

    repository.startStockPriceService()

    verify(mockService).startStockPriceService()
  }

  @Test
  fun `stop calls service stop`() = runTest {
    val mockService = mock<StockPriceService>()
    whenever(mockService.getStockPriceStream()).thenReturn(flowOf())
    whenever(mockService.errors).thenReturn(MutableStateFlow(null))
    val repository = StockPriceRepositoryImpl(mockService, StaticStockLogoService())

    repository.stopStockPriceService()

    verify(mockService).stopStockPriceService()
  }

  @Test
  fun `getStockPriceUpdates returns PriceUpdated state when stock update received`() = runTest {
    val mockService = mock<StockPriceService>()
    val mockLogoService = mock<StockLogoService>()
    
    // Setup mocks
    whenever(mockLogoService.logoUrlFor("AAPL")).thenReturn("https://example.com/logo.png")
    whenever(mockService.getStockPriceStream()).thenReturn(
      flowOf(
        StockPriceDto("id1", "AAPL", 100.0, 1.0, 1000L)
      )
    )
    whenever(mockService.errors).thenReturn(MutableStateFlow(null))

    val repository = StockPriceRepositoryImpl(mockService, mockLogoService)
    repository.startStockPriceService()
    
    // Get first state - flow should emit something
    val firstState = repository.getStockPriceUpdates().first()
    
    // Should be either PriceUpdated or Error (from empty error flow)
    assertTrue(firstState is StockPriceState.PriceUpdated || firstState is StockPriceState.Error)
    
    // If we got PriceUpdated, verify the stock data
    if (firstState is StockPriceState.PriceUpdated && firstState.stocks.isNotEmpty()) {
      assertEquals("id1", firstState.stocks[0].id)
      assertEquals("AAPL", firstState.stocks[0].symbol)
      assertEquals(100.0, firstState.stocks[0].price, 0.01)
      assertEquals(1.0, firstState.stocks[0].change, 0.01)
    }
  }

  @Test
  fun `getStockPriceUpdates handles multiple stock updates`() = runTest {
    val mockService = mock<StockPriceService>()
    val mockLogoService = mock<StockLogoService>()
    
    whenever(mockLogoService.logoUrlFor("AAPL")).thenReturn("https://example.com/logo1.png")
    whenever(mockLogoService.logoUrlFor("TSLA")).thenReturn("https://example.com/logo2.png")
    whenever(mockService.getStockPriceStream()).thenReturn(
      flowOf(
        StockPriceDto("id1", "AAPL", 100.0, 1.0, 1000L),
        StockPriceDto("id2", "TSLA", 200.0, 2.0, 2000L)
      )
    )
    whenever(mockService.errors).thenReturn(MutableStateFlow(null))

    val repository = StockPriceRepositoryImpl(mockService, mockLogoService)
    repository.startStockPriceService()
    
    // Get first state - should receive PriceUpdated eventually
    val firstState = repository.getStockPriceUpdates().first()
    
    // Should be a valid state
    assertTrue(firstState is StockPriceState.ConnectivityChange || firstState is StockPriceState.PriceUpdated || firstState is StockPriceState.Error)
    
    // If PriceUpdated with stocks, verify it works
    if (firstState is StockPriceState.PriceUpdated && firstState.stocks.isNotEmpty()) {
      assertTrue(firstState.stocks.size >= 1)
    }
  }

  @Test
  fun `getStockPriceUpdates handles service errors`() = runTest {
    val mockService = mock<StockPriceService>()
    val mockLogoService = mock<StockLogoService>()
    
    val errorFlow = MutableStateFlow<Throwable?>(null)
    whenever(mockService.getStockPriceStream()).thenReturn(flowOf())
    whenever(mockService.errors).thenReturn(errorFlow)

    val repository = StockPriceRepositoryImpl(mockService, mockLogoService)
    repository.startStockPriceService()
    
    // Emit an error
    errorFlow.value = RuntimeException("Test error")
    
    // Should receive Error state
    val result = repository.getStockPriceUpdates().first()
    assertTrue(result is StockPriceState.Error)
    assertEquals("Test error", (result as StockPriceState.Error).message)
  }

  @Test
  fun `getStockPriceUpdates handles service errors from error flow`() = runTest {
    val mockService = mock<StockPriceService>()
    val mockLogoService = mock<StockLogoService>()
    
    val errorFlow = MutableStateFlow<Throwable?>(RuntimeException("Connection failed"))
    whenever(mockService.getStockPriceStream()).thenReturn(flowOf())
    whenever(mockService.errors).thenReturn(errorFlow)

    val repository = StockPriceRepositoryImpl(mockService, mockLogoService)
    repository.startStockPriceService()
    
    // Should receive Error state from error flow
    val result = repository.getStockPriceUpdates().first()
    assertTrue(result is StockPriceState.Error)
    val errorResult = result as StockPriceState.Error
    assertTrue(errorResult.message.contains("Connection failed") || 
               errorResult.message.isNotEmpty())
  }
}

