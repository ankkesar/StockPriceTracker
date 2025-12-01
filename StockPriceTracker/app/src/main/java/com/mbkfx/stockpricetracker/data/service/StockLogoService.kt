package com.mbkfx.stockpricetracker.data.service

/**
 * Simple abstraction to provide a logo URL for a given stock symbol.
 * In a real app this could hit a REST API; here we use a static mapping.
 */
interface StockLogoService {
  fun logoUrlFor(id: String): String
}


