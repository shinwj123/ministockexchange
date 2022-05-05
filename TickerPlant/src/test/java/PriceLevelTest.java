import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Objects;

class PriceLevelTest {
    @Test
    public void successfullyCreatePriceLevelInstance() {
        final String symbol = "GOOGL";
        final long timestamp = 123456789L;
        final StockPrice price = new StockPrice(12345L);
        final int size = 250;

        PriceLevel priceLevel = new PriceLevel(symbol, timestamp, price, size);
        Assertions.assertTrue(Objects.equals(priceLevel.getStockSymbol(), "GOOGL") &&
                priceLevel.getTimeStamp() == 123456789L &&
                priceLevel.getStockPrice().getNumber() == 12345L &&
                priceLevel.getSize() == 250
                );
    }

    @Test
    public void createTwoIdenticalPriceLevelGivenSameInputs() {
        final String symbol = "GOOGL";
        final long timestamp = 123456789L;
        final StockPrice price = new StockPrice(12345L);
        final int size = 250;

        PriceLevel priceLevel1 = new PriceLevel(symbol, timestamp, price, size);
        PriceLevel priceLevel2 = new PriceLevel(symbol, timestamp, price, size);
        Assertions.assertTrue(priceLevel1.equals(priceLevel2) &&
                priceLevel1.hashCode() == priceLevel2.hashCode()
        );
    }



}