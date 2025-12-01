package com.mbkfx.stockpricetracker.prices.model

data class StockUiModel(
  val id: String,
  val symbol: String,
  val price: Double,
  val change: Double,
  val updatedAt: Long,
  val logoUrl: String?
) {
  val direction: PriceDirection
    get() = when {
      change > 0 -> PriceDirection.UP
      change < 0 -> PriceDirection.DOWN
      else -> PriceDirection.FLAT
    }
}

enum class PriceDirection {
  UP,
  DOWN,
  FLAT
}

