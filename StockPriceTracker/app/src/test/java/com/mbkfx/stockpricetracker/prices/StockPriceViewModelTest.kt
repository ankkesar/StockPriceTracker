package com.mbkfx.stockpricetracker.prices

import com.mbkfx.stockpricetracker.domain.model.ConnectionStatus
import com.mbkfx.stockpricetracker.domain.model.StockPriceState
import com.mbkfx.stockpricetracker.domain.repository.StockPriceRepository
import com.mbkfx.stockpricetracker.prices.model.StockUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for StockPriceViewModel
 * 
 * These tests use test dispatchers to simulate Android's main thread.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StockPriceViewModelTest {

  private lateinit var mockRepository: StockPriceRepository
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockRepository = mock()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `retry calls repository stop and start`() = runTest(testDispatcher) {
    whenever(mockRepository.getStockPriceUpdates()).thenReturn(flowOf())

    val viewModel = StockPriceViewModel(mockRepository)
    advanceUntilIdle()

    viewModel.retry()

    verify(mockRepository).stopStockPriceService()
    verify(mockRepository).startStockPriceService()
  }

  @Test
  fun `start calls repository start`() = runTest(testDispatcher) {
    whenever(mockRepository.getStockPriceUpdates()).thenReturn(flowOf())

    val viewModel = StockPriceViewModel(mockRepository)
    advanceUntilIdle()

    viewModel.start()

    verify(mockRepository).startStockPriceService()
  }

  @Test
  fun `stop calls repository stop`() = runTest(testDispatcher) {
    whenever(mockRepository.getStockPriceUpdates()).thenReturn(flowOf())

    val viewModel = StockPriceViewModel(mockRepository)
    advanceUntilIdle()

    viewModel.stop()

    verify(mockRepository).stopStockPriceService()
  }

  @Test
  fun `ConnectivityChange updates status`() = runTest(testDispatcher) {
    val stateFlow = MutableStateFlow<StockPriceState>(
      StockPriceState.ConnectivityChange(ConnectionStatus.CONNECTED)
    )

    whenever(mockRepository.getStockPriceUpdates()).thenReturn(stateFlow)

    val viewModel = StockPriceViewModel(mockRepository)
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals(ConnectionStatus.CONNECTED, uiState.status)
  }

  @Test
  fun `PriceUpdated updates stocks status and timestamp with sorting`() = runTest(testDispatcher) {
    val stocks = listOf(
      StockUiModel("id1", "AAPL", 100.0, 1.0, 1000L, null),
      StockUiModel("id2", "TSLA", 250.0, 5.0, 2000L, null),
      StockUiModel("id3", "GOOG", 150.0, 2.0, 1500L, null)
    )
    val updatedAt = 5000L
    val stateFlow = MutableStateFlow<StockPriceState>(
      StockPriceState.PriceUpdated(stocks, updatedAt)
    )

    whenever(mockRepository.getStockPriceUpdates()).thenReturn(stateFlow)

    val viewModel = StockPriceViewModel(mockRepository)
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals(ConnectionStatus.CONNECTED, uiState.status)
    assertEquals(3, uiState.stocks.size)
    assertEquals(updatedAt, uiState.lastServerMessageAt)
  }

  @Test
  fun `PriceUpdated uses event updatedAt timestamp`() = runTest(testDispatcher) {
    val stocks = listOf(
      StockUiModel("id1", "AAPL", 100.0, 1.0, 1000L, null)
    )
    val expectedTimestamp = 12345L
    val stateFlow = MutableStateFlow<StockPriceState>(
      StockPriceState.PriceUpdated(stocks, expectedTimestamp)
    )

    whenever(mockRepository.getStockPriceUpdates()).thenReturn(stateFlow)

    val viewModel = StockPriceViewModel(mockRepository)
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals(expectedTimestamp, uiState.lastServerMessageAt)
  }

  @Test
  fun `Error updates errorMessage and status`() = runTest(testDispatcher) {
    val stateFlow = MutableStateFlow<StockPriceState>(
      StockPriceState.Error("Test error message")
    )

    whenever(mockRepository.getStockPriceUpdates()).thenReturn(stateFlow)

    val viewModel = StockPriceViewModel(mockRepository)
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals(ConnectionStatus.ERROR, uiState.status)
    assertEquals("Test error message", uiState.errorMessage)
  }

  @Test
  fun `Shutdown updates status to OFFLINE`() = runTest(testDispatcher) {
    val stateFlow = MutableStateFlow<StockPriceState>(
      StockPriceState.Shutdown
    )

    whenever(mockRepository.getStockPriceUpdates()).thenReturn(stateFlow)

    val viewModel = StockPriceViewModel(mockRepository)
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals(ConnectionStatus.OFFLINE, uiState.status)
  }

  @Test
  fun `initial state is correct`() = runTest(testDispatcher) {
    whenever(mockRepository.getStockPriceUpdates()).thenReturn(flowOf())

    val viewModel = StockPriceViewModel(mockRepository)
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals(ConnectionStatus.OFFLINE, uiState.status)
    assertTrue(uiState.stocks.isEmpty())
    assertNull(uiState.lastServerMessageAt)
    assertNull(uiState.errorMessage)
  }

  @Test
  fun `multiple state updates are processed sequentially`() = runTest(testDispatcher) {
    val stateFlow = MutableStateFlow<StockPriceState>(
      StockPriceState.ConnectivityChange(ConnectionStatus.CONNECTED)
    )

    whenever(mockRepository.getStockPriceUpdates()).thenReturn(stateFlow)

    val viewModel = StockPriceViewModel(mockRepository)
    advanceUntilIdle()

    // First update: ConnectivityChange
    assertEquals(ConnectionStatus.CONNECTED, viewModel.uiState.value.status)

    // Second update: PriceUpdated
    val stocks = listOf(
      StockUiModel("id1", "AAPL", 100.0, 1.0, 1000L, null),
      StockUiModel("id2", "TSLA", 200.0, 2.0, 2000L, null)
    )
    stateFlow.value = StockPriceState.PriceUpdated(stocks, 3000L)
    advanceUntilIdle()

    assertEquals(ConnectionStatus.CONNECTED, viewModel.uiState.value.status)
    assertEquals(2, viewModel.uiState.value.stocks.size)
    assertEquals(3000L, viewModel.uiState.value.lastServerMessageAt)

    // Third update: Error
    stateFlow.value = StockPriceState.Error("Error occurred")
    advanceUntilIdle()

    assertEquals(ConnectionStatus.ERROR, viewModel.uiState.value.status)
    assertEquals("Error occurred", viewModel.uiState.value.errorMessage)
  }

  @Test
  fun `empty stocks list is handled correctly`() = runTest(testDispatcher) {
    val stateFlow = MutableStateFlow<StockPriceState>(
      StockPriceState.PriceUpdated(emptyList(), 1000L)
    )

    whenever(mockRepository.getStockPriceUpdates()).thenReturn(stateFlow)

    val viewModel = StockPriceViewModel(mockRepository)
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals(ConnectionStatus.CONNECTED, uiState.status)
    assertTrue(uiState.stocks.isEmpty())
    assertEquals(1000L, uiState.lastServerMessageAt)
  }

}
