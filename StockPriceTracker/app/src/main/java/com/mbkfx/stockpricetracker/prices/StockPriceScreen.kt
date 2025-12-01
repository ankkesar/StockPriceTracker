package com.mbkfx.stockpricetracker.prices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mbkfx.stockpricetracker.domain.model.ConnectionStatus
import com.mbkfx.stockpricetracker.prices.model.PriceDirection
import com.mbkfx.stockpricetracker.prices.model.StockPriceUiState
import com.mbkfx.stockpricetracker.prices.model.StockUiModel
import com.mbkfx.stockpricetracker.ui.theme.StockPriceTrackerTheme
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StockPriceRoute(
  viewModel: StockPriceViewModel = viewModel()
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  StockPriceScreen(
    state = uiState,
    onRetry = { viewModel.retry() },
    onStart = { viewModel.start() },
    onStop = { viewModel.stop() }
  )
}

@Composable
fun StockPriceScreen(
  state: StockPriceUiState,
  onRetry: () -> Unit = {},
  onStart: () -> Unit = {},
  onStop: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val formatter = remember { NumberFormat.getCurrencyInstance(Locale.US) }
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(horizontal = 16.dp, vertical = 12.dp)
  ) {
    ConnectionStatusBanner(
      state = state,
      onRetry = onRetry,
      onStart = onStart,
      onStop = onStop
    )
    Spacer(modifier = Modifier.height(12.dp))
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      items(state.stocks, key = { it.id }) { stock ->
        StockRow(
          stock = stock,
          formattedPrice = formatter.format(stock.price)
        )
      }
    }
  }
}

@Composable
private fun ConnectionStatusBanner(
  state: StockPriceUiState,
  onRetry: () -> Unit = {},
  onStart: () -> Unit = {},
  onStop: () -> Unit = {}
) {
  val (label, tint) = when (state.status) {
    ConnectionStatus.CONNECTED -> "Live prices" to MaterialTheme.colorScheme.primary
    ConnectionStatus.OFFLINE -> "Offline…" to MaterialTheme.colorScheme.tertiary
    ConnectionStatus.ERROR -> "Connection lost" to MaterialTheme.colorScheme.error
    ConnectionStatus.CONNECTING -> "Connecting" to MaterialTheme.colorScheme.secondary
  }
  
  // Connection indicator color: green for connected, red for error/offline
  val indicatorColor = when (state.status) {
    ConnectionStatus.CONNECTED -> Color(0xFF4CAF50) // Green
    ConnectionStatus.ERROR -> Color(0xFFF44336) // Red
    ConnectionStatus.OFFLINE -> Color(0xFF9E9E9E) // Gray
    ConnectionStatus.CONNECTING -> Color(0xFF9E9E9E) // Gray
  }
  
  val showRetryButton = state.status == ConnectionStatus.ERROR
  val isConnected = state.status == ConnectionStatus.CONNECTED
  val lastUpdateText = state.lastServerMessageAt?.let {
    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    "• Updated ${formatter.format(Date(it))}"
  }
  
  Surface(
    shape = MaterialTheme.shapes.medium,
    color = tint.copy(alpha = 0.12f),
    tonalElevation = 0.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Connection indicator on the left
      Box(
        modifier = Modifier
          .size(12.dp)
          .background(color = indicatorColor, shape = CircleShape)
      )
      
      Spacer(modifier = Modifier.width(12.dp))
      
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = label,
          style = MaterialTheme.typography.titleMedium.copy(color = tint, fontWeight = FontWeight.SemiBold)
        )
        state.errorMessage?.let { error ->
          Text(
            text = error,
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error)
          )
        }
      }
      
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Start/Stop text buttons
        if (state.status == ConnectionStatus.CONNECTED) {
          TextButton(onClick = onStop) {
            Text(
              text = "Stop",
              color = MaterialTheme.colorScheme.error
            )
          }
        } else {
          TextButton(onClick = onStart) {
            Text(
              text = "Start",
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
        
//        if (showRetryButton) {
//          IconButton(onClick = onRetry) {
//            Icon(
//              imageVector = Icons.Rounded.Refresh,
//              contentDescription = "Retry connection",
//              tint = tint
//            )
//          }
//        }
        
        lastUpdateText?.let {
          Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

@Composable
private fun StockRow(
  stock: StockUiModel,
  formattedPrice: String,
  modifier: Modifier = Modifier
) {
  val changeColor = when (stock.direction) {
    PriceDirection.UP -> Color(0xFF2E7D32)
    PriceDirection.DOWN -> Color(0xFFC62828)
    PriceDirection.FLAT -> MaterialTheme.colorScheme.onSurfaceVariant
  }
  val changeIcon = when (stock.direction) {
    PriceDirection.UP -> Icons.Rounded.ArrowDropUp
    PriceDirection.DOWN -> Icons.Rounded.ArrowDropDown
    PriceDirection.FLAT -> null
  }

  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column {
        Text(
          text = stock.symbol,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
      }
      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = formattedPrice,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          color = changeColor
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          changeIcon?.let { icon ->
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = changeColor
            )
          }
          Text(
            text = "${if (stock.change > 0) "+" else ""}${String.format(Locale.US, "%.2f", stock.change)}",
            color = changeColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
          )
        }
      }
    }
  }
}


@Preview(showBackground = true)
@Composable
private fun StockPriceScreenPreview() {
  StockPriceTrackerTheme {
    StockPriceScreen(
      state = StockPriceUiState(
        stocks = listOf(
          StockUiModel("id-aapl", "AAPL", 190.30, 1.45, System.currentTimeMillis(), logoUrl = null),
          StockUiModel("id-tsla", "TSLA", 245.01, -3.12, System.currentTimeMillis() - 40_000L, logoUrl = null)
        ),
        status = ConnectionStatus.CONNECTED,
        lastServerMessageAt = System.currentTimeMillis()
      )
    )
  }
}

