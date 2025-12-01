package com.mbkfx.stockpricetracker.data.service

import org.json.JSONObject

data class StockPriceDto(
  val id: String,
  val symbol: String,
  var price: Double,
  val change: Double,
  val updatedAt: Long
){
  fun toJson(): String {
    return JSONObject()
      .put("id", id)
      .put("symbol", symbol)
      .put("price", price)
      .put("change", change)
      .put("updatedAt", updatedAt)
      .toString()
  }
}

