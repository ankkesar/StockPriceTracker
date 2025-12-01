package com.mbkfx.stockpricetracker.data.service

import android.util.Log
import com.mbkfx.stockpricetracker.data.service.exception.StockPriceServiceConnectionException
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.IOException
import org.json.JSONObject
import java.net.SocketException
import java.net.UnknownHostException
import kotlin.math.pow

internal class WebSocketStockPriceService(
  private val client: OkHttpClient = OkHttpClient.Builder()
    .readTimeout(0, TimeUnit.MILLISECONDS)
    .pingInterval(MarketStatus.OPEN.freq, TimeUnit.SECONDS)
    .build()
) : StockPriceService {

  private val initialPrices = DEFAULT_SYMBOLS.mapIndexed { index, symbol ->
    // Generate fixed ID based on position, not symbol (so ID stays same if symbol changes)
    val id = UUID.nameUUIDFromBytes("stock_$index".toByteArray()).toString()
    StockPriceDto(
      id = id,
      symbol = symbol,
      price = randomStartingPrice(),
      change = 0.0,
      updatedAt = System.currentTimeMillis()
    )
  }

  private var reconnectAttempt: Int = 1
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
  private val stockCache = initialPrices.associateBy { it.id }.toMutableMap()
  private val _errors = MutableStateFlow<Throwable?>(null)
  private val supervisor = SupervisorJob()
  private var scope = CoroutineScope(ioDispatcher + supervisor)
  private var webSocket: WebSocket? = null
  private var sendJob: Job? = null
  private var reconnectJob: Job? = null
  private var started = false
  private val _stockPriceStream = MutableSharedFlow<StockPriceDto>(
    replay = 1,
    extraBufferCapacity = 100,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
  )
  override val errors = _errors.asStateFlow()

  override fun getStockPriceStream(): Flow<StockPriceDto> {
    if(webSocket==null){
      started = true
      connect()
    }
    return _stockPriceStream.asSharedFlow()
  }

  override fun startStockPriceService() {
    if (started) return
    started = true
    connect()
  }
  override fun stopStockPriceService() {
    reconnectJob?.cancel()
    reconnectJob = null
    stopSendingLoop()
    webSocket?.close(1000, "Stopped by User")
    webSocket = null
    reconnectAttempt = 0
    _errors.value = null
  }

  /**
   * Web socket listener
   */
  private val socketListener = object : WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
      _errors.value = null
      startSendingLoop()
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
      handleIncomingPayload(text)
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
      handleIncomingPayload(bytes.utf8())
    }

    override fun onFailure(webSocket: WebSocket, exception: Throwable, response: Response?) {
      if (!started) return
      stopSendingLoop()
      when(exception){
        is SocketException, is UnknownHostException, is IOException->{
          if(reconnectAttempt++> MAX_RETRIES){
            _errors.value= StockPriceServiceConnectionException(exception.message?:"Exceeded max retry attempts")
          }
          scheduleReconnect()
        }
        else->_errors.value = exception
      }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
      if (!started) return
      stopSendingLoop()
      when(code){
        1000 -> {
          // Normal closure (user initiated stop)
          started = false
          _errors.value = null
        }
        1011-> scheduleReconnect(code)// need to handle all server driven error codes which qualify for retry
        else->{
          started = false
          _errors.value = StockPriceServiceConnectionException("Connection closed with code $code")
          Log.e("Service closed", "with code $code")
        }
      }
    }
  }

  private fun connect() {
    if (!started) return
    reconnectJob?.cancel()
    val request = Request.Builder()
      .url(POSTMAN_ECHO_URL)
      .build()
    webSocket = client.newWebSocket(request, socketListener)
  }

  private fun startSendingLoop() {
    sendJob?.cancel()
    sendJob = scope.launch {
      while (isActive && started) {
        stockCache.values.forEach { stock ->
          val payload = nextPayloadFor(stock.id)
          webSocket?.send(payload.toJson())
        }
        delay(SEND_INTERVAL_MS)
      }
    }
  }

  private fun stopSendingLoop() {
    sendJob?.cancel()
    sendJob = null
  }

  private fun scheduleReconnect(code : Int? = null) {
    if (!started) return
    reconnectJob?.cancel()
    reconnectJob = scope.launch {
      val delayMs = (RECONNECT_DELAY_MS * RETRIES_BACKOFF.pow(reconnectAttempt)).toLong()
      delay(delayMs)
      if (!started) return@launch
      connect()
    }
    Log.e("Price service", "Retry with code $code")
  }

  private fun handleIncomingPayload(raw: String) {
    val payload = runCatching {
      val json = JSONObject(raw)
      StockPriceDto(
        id = json.getString("id"),
        symbol = json.getString("symbol"),
        price = json.getDouble("price"),
        change = json.optDouble("change", 0.0),
        updatedAt = json.getLong("updatedAt")// emulates server timestamp
      )
    }.getOrNull() ?: return

    scope.launch {
//      stockPriceChannel.send(payload)
      _stockPriceStream.emit(payload)
    }
  }

  private fun nextPayloadFor(id: String): StockPriceDto {
    val symbol = stockCache[id]?.symbol ?: ""
    val previousPrice = stockCache[id]?.price ?: randomStartingPrice()
    val change = Random.nextDouble(-MAX_CHANGE_DELTA, MAX_CHANGE_DELTA)
    val candidatePrice = (previousPrice + change).coerceAtLeast(MIN_PRICE)
    val updatedStock = StockPriceDto(id, symbol, candidatePrice, change, System.currentTimeMillis())
    stockCache[id] = updatedStock
    return updatedStock
  }

  companion object {
    private const val POSTMAN_ECHO_URL = "wss://ws.postman-echo.com/raw"
    private const val SEND_INTERVAL_MS = 2_000L
    private const val RECONNECT_DELAY_MS = 1_000L
    private const val MIN_PRICE = 1.0
    private const val MAX_CHANGE_DELTA = 5.0
    private const val MAX_RETRIES = 5
    private const val RETRIES_BACKOFF = 1.1F

    private val DEFAULT_SYMBOLS = listOf(
      "AAPL", "GOOG", "MSFT", "AMZN", "TSLA",
      "NVDA", "NFLX", "META", "AMD", "INTC",
      "SHOP", "UBER", "LYFT", "SPOT", "ADBE",
      "ORCL", "CRM", "PYPL", "SQ", "BABA",
      "V", "MA", "JPM", "DIS", "IBM"
    )
    private fun randomStartingPrice(): Double = Random.nextDouble(50.0, 500.0)

  }

  /**
   * It emulates an API, that will control ping rate to reduce server load,
   * and improve battery usage
   */
  enum class MarketStatus(val freq: Long) {

    OPEN(30L),
    AFTER_HOURS(60L),
    CLOSED(100000L);
  }
}

