package com.mbkfx.stockpricetracker.prices.model

import com.mbkfx.stockpricetracker.domain.model.ConnectionStatus

data class StockPriceUiState(
  val stocks: List<StockUiModel> = emptyList(),
  val status: ConnectionStatus = ConnectionStatus.OFFLINE,
  val lastServerMessageAt: Long? = null,
  val errorMessage: String? = null
)