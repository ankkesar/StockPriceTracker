package com.mbkfx.stockpricetracker.di

import com.mbkfx.stockpricetracker.data.repository.StockPriceRepositoryImpl
import com.mbkfx.stockpricetracker.domain.repository.StockPriceRepository

object StockPriceModule {
  val repository: StockPriceRepository by lazy { StockPriceRepositoryImpl() }
}

