package com.mbkfx.stockpricetracker.data.service.exception

data class StockPriceServiceConnectionException(override val message: String) : StockPriceServiceException(message) {

}
