package com.mbkfx.stockpricetracker.domain.repository

import com.mbkfx.stockpricetracker.data.repository.StockImage
import com.mbkfx.stockpricetracker.domain.model.StockPriceState
import kotlinx.coroutines.flow.Flow

interface StockPriceRepository {
  /** Stream of state updates for stock prices and connectivity. */
  fun startStockPriceService()
  fun stopStockPriceService()
  fun getStockPriceUpdates(): Flow<StockPriceState>
  fun getStockImages(): Flow<StockImage>
}

