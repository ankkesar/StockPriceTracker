#!/bin/bash

# Script to run all unit tests for StockPriceTracker
# Usage: ./run-tests.sh

echo "=========================================="
echo "Running StockPriceTracker Unit Tests"
echo "=========================================="
echo ""

# Navigate to project root
cd "$(dirname "$0")"

# Run all unit tests
echo "Executing: ./gradlew test"
echo ""
./gradlew test --info

# Check if tests passed
if [ $? -eq 0 ]; then
    echo ""
    echo "=========================================="
    echo "✓ All tests passed successfully!"
    echo "=========================================="
    exit 0
else
    echo ""
    echo "=========================================="
    echo "✗ Some tests failed. Check output above."
    echo "=========================================="
    exit 1
fi

