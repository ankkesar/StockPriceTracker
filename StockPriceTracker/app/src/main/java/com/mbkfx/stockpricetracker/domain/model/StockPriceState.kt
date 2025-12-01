package com.mbkfx.stockpricetracker.domain.model

import com.mbkfx.stockpricetracker.prices.model.StockUiModel

sealed class StockPriceState() {
  data class ConnectivityChange(
    val connectionStatus: ConnectionStatus,
  ) : StockPriceState()


  data class PriceUpdated(
    val stocks: List<StockUiModel> = emptyList(),
    val updatedAt: Long = 0L
  ) : StockPriceState()

  data class Error(
    val message: String,
  ) : StockPriceState()

  object Shutdown : StockPriceState()
}
