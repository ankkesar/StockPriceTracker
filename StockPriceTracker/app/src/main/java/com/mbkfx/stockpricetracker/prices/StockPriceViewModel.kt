package com.mbkfx.stockpricetracker.prices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mbkfx.stockpricetracker.di.StockPriceModule
import com.mbkfx.stockpricetracker.domain.model.ConnectionStatus
import com.mbkfx.stockpricetracker.domain.model.StockPriceState
import com.mbkfx.stockpricetracker.domain.repository.StockPriceRepository
import com.mbkfx.stockpricetracker.prices.model.StockPriceUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StockPriceViewModel(
  private val repository: StockPriceRepository = StockPriceModule.repository
) : ViewModel() {

  private val _uiState = MutableStateFlow(StockPriceUiState())
  val uiState = _uiState.asStateFlow()

  init {
    observeState()
  }

  private fun observeState() {
    viewModelScope.launch {

      repository.getStockPriceUpdates().conflate().collectLatest { event ->
        when (event) {
          is StockPriceState.ConnectivityChange -> {
            _uiState.update {
              it.copy(
                status = event.connectionStatus,
              )
            }
          }
          is StockPriceState.PriceUpdated -> {
            _uiState.update {
              it.copy(
                status = ConnectionStatus.CONNECTED,
                stocks = event.stocks,
                lastServerMessageAt = event.updatedAt
              )
            }
          }
          is StockPriceState.Error -> {
            _uiState.update {
              it.copy(
                errorMessage = event.message,
                status = ConnectionStatus.ERROR
              )
            }
          }

          is StockPriceState.Shutdown->{
            _uiState.update {
              it.copy(
                status = ConnectionStatus.OFFLINE
              )
            }
          }
        }
      }
    }
  }

  fun retry(){
    repository.stopStockPriceService()
    repository.startStockPriceService()
  }

  fun start() {
    repository.startStockPriceService()
  }

  fun stop() {
    repository.stopStockPriceService()
    _uiState.update {
      it.copy(
        status = ConnectionStatus.OFFLINE
      )
    }
  }

  override fun onCleared() {
    repository.stopStockPriceService()
    super.onCleared()
  }
}
