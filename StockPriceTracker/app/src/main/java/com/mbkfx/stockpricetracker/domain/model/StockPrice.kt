package com.mbkfx.stockpricetracker.domain.model

data class StockPrice(
  val id: String,
  val symbol: String,
  val price: Double,
  val change: Double,
  val updatedAt: Long
)

