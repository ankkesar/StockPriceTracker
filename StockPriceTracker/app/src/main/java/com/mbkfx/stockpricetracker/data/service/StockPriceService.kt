package com.mbkfx.stockpricetracker.data.service

import com.mbkfx.stockpricetracker.data.service.dto.StockPriceDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface StockPriceService {
  fun getStockPriceStream() : Flow<StockPriceDto>
  fun startStockPriceService()
  fun stopStockPriceService()
  val errors: StateFlow<Throwable?>
}

