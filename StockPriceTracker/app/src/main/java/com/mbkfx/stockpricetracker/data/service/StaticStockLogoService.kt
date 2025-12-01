package com.mbkfx.stockpricetracker.data.service

/**
 * Static implementation that returns a deterministic logo URL per symbol.
 * Replace with a network-backed implementation if needed.
 */
class StaticStockLogoService : StockLogoService {

  override fun logoUrlFor(id: String): String {
    // Example URLs from a hypothetical CDN; customize as needed.
    val base = "https://example.com/logos/"
    return "$base${id.uppercase()}.png"
  }
}


