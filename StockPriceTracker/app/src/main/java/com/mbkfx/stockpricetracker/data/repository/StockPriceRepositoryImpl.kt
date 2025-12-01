package com.mbkfx.stockpricetracker.data.repository

import com.mbkfx.stockpricetracker.data.service.StaticStockLogoService
import com.mbkfx.stockpricetracker.data.service.StockLogoService
import com.mbkfx.stockpricetracker.data.service.dto.StockPriceDto
import com.mbkfx.stockpricetracker.data.service.StockPriceService
import com.mbkfx.stockpricetracker.data.service.WebSocketStockPriceService
import com.mbkfx.stockpricetracker.data.service.dto.StockImage
import com.mbkfx.stockpricetracker.data.service.exception.StockPriceServiceClosedException
import com.mbkfx.stockpricetracker.data.service.exception.StockPriceServiceConnectionException
import com.mbkfx.stockpricetracker.domain.model.ConnectionStatus
import com.mbkfx.stockpricetracker.domain.model.StockPriceState
import com.mbkfx.stockpricetracker.prices.model.StockUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

internal class StockPriceRepositoryImpl(
  private val service: StockPriceService = WebSocketStockPriceService(),
  private val logoService: StockLogoService = StaticStockLogoService()
) : StockPriceRepository {
  private val latestPrices = mutableMapOf <String, StockUiModel>()

  override fun getStockImages(): Flow<StockImage> = flow{
    for((id, stock) in latestPrices){
      emit(StockImage(id, logoService.logoUrlFor(id)))
    }
  }

  override fun getStockPriceUpdates():Flow<StockPriceState> {

    return merge(service.getStockPriceStream()   // Flow<StockDto?>
      .map { stockDto ->
          latestPrices[stockDto.id] = stockDto.toUiModel()
        StockPriceState.PriceUpdated(
          stocks = latestPrices.values.sortedByDescending { it.price },
          updatedAt = System.currentTimeMillis()
        )
      }.catch { it ->
        when (it) {
          is StockPriceServiceConnectionException ->
            StockPriceState.ConnectivityChange(ConnectionStatus.ERROR)

          is StockPriceServiceClosedException ->
            StockPriceState.ConnectivityChange(ConnectionStatus.OFFLINE)

          else ->
            it.message?.let { messg -> StockPriceState.Error(messg) }
        }
      },
      service.errors.map {
          ex-> ex?.let { StockPriceState.Error(ex.message ?:"Service error")  }?:StockPriceState.Error("")
      })
      .flowOn(Dispatchers.IO)
  }

  override fun startStockPriceService() { service.startStockPriceService()
  }
  override fun stopStockPriceService() = service.stopStockPriceService()

  private fun StockPriceDto.toUiModel(): StockUiModel =
    StockUiModel(
      id = id,
      symbol = symbol,
      price = price,
      change = change,
      updatedAt = updatedAt,
      logoUrl = logoService.logoUrlFor(symbol)
    )
}
