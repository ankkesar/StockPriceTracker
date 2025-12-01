package com.mbkfx.stockpricetracker.di

import com.mbkfx.stockpricetracker.data.repository.StockPriceRepositoryImpl
import com.mbkfx.stockpricetracker.data.repository.StockPriceRepository

object StockPriceModule {
  val repository: StockPriceRepository by lazy { StockPriceRepositoryImpl() }
}

