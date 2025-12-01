# Stock Price Tracker

A real-time stock price tracking Android application built with Jetpack Compose, following Clean Architecture principles. The app connects to a WebSocket server to receive live price updates for multiple stock symbols.

## Architecture

The application follows Clean Architecture with three main layers:

- **Domain Layer**: Core business logic, models, and repository interfaces
- **Data Layer**: Repository implementations, services, and data sources
- **Presentation Layer**: UI components, ViewModels, and Compose screens

## Assumption
- **Webservice**: Generates new data for all symbols together every 2 seconds
- Stock id is used as primary key, as stock symbol could change
- Uses map to store data locally in webservice to save manually generated payload 
- Decouples static data from websocket price payload, to reduce network overhead
- **Application**:
- Uses manual DI, will use HILT for prod app
- I am aware of API Design that could make fetchPrice ws configurable for stock list, 
- to use at multiple places, but scope was fixed to 25 stocks
- App auto recovers from connectivity drops with retry backoff, it can be configurable on device/server load
- With Mock API server/connectivity error is passed as it is to UI, it will be handled by actual wss

## Performance optimisation(To be implemented)
- We can pull large data from backend, as per available Network type.
- This data can be buffered with Stateflow and consumed as per device speed
- Converting a cold flow into hot flow can share dataset, 
- if UI is recreated
- Using conflate to prevent ui jank

## Project Structure

```
app/src/main/java/com/mbkfx/stockpricetracker/
├── MainActivity.kt                    # Main entry point
├── di/
│   └── StockPriceModule.kt            # Dependency injection module
├── domain/
│   ├── model/
│   │   ├── ConnectionStatus.kt        # Connection status enum
│   │   ├── StockPrice.kt              # Domain stock price model
│   │   └── StockPriceState.kt         # Sealed class for state updates
│   └── repository/
│       └── StockPriceRepository.kt     # Repository interface
├── data/
│   ├── repository/
│   │   ├── StockImage.kt               # Stock image data model
│   │   └── StockPriceRepositoryImpl.kt # Repository implementation
│   └── service/
│       ├── StockPriceDto.kt            # Data transfer object
│       ├── StockPriceService.kt        # Service interface
│       ├── WebSocketStockPriceService.kt # WebSocket implementation
│       ├── StockLogoService.kt         # Logo service interface
│       ├── StaticStockLogoService.kt   # Static logo implementation
│       └── exception/
│           ├── StockPriceServiceException.kt
│           ├── StockPriceServiceConnectionException.kt
│           └── StockPriceServiceClosedException.kt
└── prices/
    ├── StockPriceModels.kt             # UI models
    ├── StockPriceViewModel.kt          # ViewModel
    └── StockPriceScreen.kt             # Compose UI screen
```

## Class Descriptions

### Domain Layer

#### `ConnectionStatus` (enum)
Represents the connection status of the stock price service.
- `OFFLINE`: Service is not connected
- `CONNECTED`: Service is connected and receiving updates
- `ERROR`: Connection error occurred

#### `StockPrice` (data class)
Domain model representing a stock price update.
- `id`: Unique identifier for the stock (fixed, doesn't change)
- `symbol`: Stock symbol (e.g., "AAPL") - can change
- `price`: Current stock price
- `change`: Price change amount
- `updatedAt`: Timestamp of the update

#### `StockPriceState` (sealed class)
Represents different states of the stock price stream.
- `ConnectivityChange`: Connection status changed
- `PriceUpdated`: New price update received
- `Error`: Error occurred
- `Shutdown`: Service was shut down

#### `StockPriceRepository` (interface)
Repository interface defining operations for stock price data.
- `start()`: Start the stock price stream
- `stop()`: Stop the stock price stream
- `getStockPriceUpdates()`: Flow of stock price state updates
- `getStockImages()`: Flow of stock image updates, 
- this API emulates separation of static content, to keep stock price payload light

### Data Layer

#### `StockPriceDto` (data class)
Data Transfer Object for stock price data from the service.
- `id`: Stock identifier
- `symbol`: Stock symbol
- `price`: Current price (mutable), since I am using initial value set. 
- It will be immutable with actual wss
- `change`: Price change
- `updatedAt`: Update timestamp
- `toJson()`: Converts to JSON string

#### `StockPriceService` (interface)
Interface for stock price streaming service.
Ensures maintainability, can be migrated a diff impl like grpc in future
- `getStockPriceStream()`: Flow of stock price DTOs
- `startStockService()`: Start the service
- `stopStockPriceService()`: Stop the service
- `errors`: StateFlow of errors

#### `WebSocketStockPriceService` (class)
WebSocket implementation of StockPriceService.
This service is designed to be performance efficient, with controlled ping rate, 
as per market status
- Connects to `wss://ws.postman-echo.com/raw`
- Generates mock price updates every 2 seconds
- Handles reconnection logic
- Manages WebSocket lifecycle

#### `StockLogoService` (interface)
Interface for retrieving stock logo URLs. Emulates static data
- `logoUrlFor(id: String)`: Returns logo URL for a stock ID

#### `StaticStockLogoService` (class)
Static implementation that returns deterministic logo URLs. Emulates static data
- Returns URLs in format: `https://example.com/logos/{ID}.png`

#### `StockPriceRepositoryImpl` (class)
Repository implementation that coordinates between service and domain.
- Converts DTOs to domain models
- Merges price updates with logo URLs
- Handles error states
- Maintains latest stock prices

#### `StockImage` (data class)
Data model for stock images.
- `id`: Stock identifier
- `url`: Logo URL

### Presentation Layer

#### `StockPriceViewModel` (class)
ViewModel managing stock price UI state.
- Observes repository state updates
- Converts domain models to UI models
- Handles retry logic
- Manages lifecycle (start/stop)

#### `StockUiModel` (data class)
UI model for displaying stock information.
Due to time constraints couldn't implement data validators, 
highlighting invalid data from API

- `id`: Stock identifier
- `symbol`: Stock symbol
- `price`: Current price
- `change`: Price change
- `updatedAt`: Update timestamp
- `logoUrl`: Logo URL (nullable) - not implemented
- `direction`: Computed property (UP/DOWN/FLAT)

#### `StockPriceUiState` (data class)
UI state container.
- `stocks`: List of stock UI models
- `status`: Connection status
- `lastServerMessageAt`: Last update timestamp
- `errorMessage`: Error message (nullable)

#### `StockPriceScreen` (Composable)
Main Compose screen displaying stock prices.
- Shows connection status banner
- Displays scrollable list of stocks
- [Bonus] Displays price changes with color indicators - price is green for increase
- and red for decrease

#### `StockPriceRoute` (Composable)
Route composable that sets up ViewModel and screen.

### Dependency Injection
- Used manual injection to save time, will use Hilt in prod app

#### `StockPriceModule` (object)
Simple DI module providing repository instance.
- `repository`: Lazy singleton repository instance

### Exception Classes

#### `StockPriceServiceException` (class)
Base exception for stock price service errors.

#### `StockPriceServiceConnectionException` (class)
Exception thrown when connection fails.

#### `StockPriceServiceClosedException` (class)
Exception thrown when service is closed.

## Features

- **Real-time Updates**: Receives live stock price updates via WebSocket
- **25 Stock Symbols**: Tracks multiple stocks simultaneously
- **Clean Architecture**: Separation of concerns with clear layer boundaries
- **Error Handling**: Graceful error handling with retry mechanism
- **Logo Support**: Displays stock logos (via logo service)
- **Fixed IDs**: Uses stable IDs that don't change when symbols change
- **State Management**: Reactive state management with Kotlin Flow

## Testing

### Running Tests

#### Using Gradle (Recommended)
```bash
# Run all unit tests
./gradlew test
```

#### Using Test Scripts
```bash
# Linux/Mac
./run-tests.sh
```

### Test Coverage

Unit tests are provided for:
- Repository (StockPriceRepositoryImpl)
- ViewModel (StockPriceViewModel)
- Webservice (WebSocketStockPriceService)

### Known Issues
- UI alignment
- Occasionally need to press start/stop button twice
- Mistakes in project structure
