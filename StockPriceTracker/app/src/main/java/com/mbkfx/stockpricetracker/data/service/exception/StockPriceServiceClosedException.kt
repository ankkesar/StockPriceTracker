package com.mbkfx.stockpricetracker.data.service.exception

import com.mbkfx.stockpricetracker.data.service.exception.StockPriceServiceException

data class StockPriceServiceClosedException(override val message: String) : StockPriceServiceException(message) {

}
