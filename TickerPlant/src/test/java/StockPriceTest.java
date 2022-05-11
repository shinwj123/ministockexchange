import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Objects;

class StockPriceTest {
    @Test
    void successfullyCreateStockPriceInstance() {
        final long price = 123456L;
        final StockPrice stockPrice = new StockPrice(price);

        Assertions.assertTrue(stockPrice.getNumber() == price);
    }

    @Test
    void priceProperlyFormatted() {

        final long price = 123456L;
        final StockPrice stockPrice = new StockPrice(price);
        Assertions.assertTrue(Objects.equals(stockPrice.toString(), "12.3456"));

    }

    @Test
    void createTwoIdenticalStockPriceGivenSameInputs() {
        final long price = 123456L;
        final StockPrice stockPrice1 = new StockPrice(price);
        final StockPrice stockPrice2 = new StockPrice(price);
        Assertions.assertTrue(stockPrice1.equals(stockPrice2) &&
                stockPrice1.hashCode() == stockPrice2.hashCode());

    }

    @Test
    void comparisonWorksProperly() {
        final long price1 = 12345L;
        final long price2 = 123456L;
        final StockPrice stockPrice1 = new StockPrice(price1);
        final StockPrice stockPrice2 = new StockPrice(price2);
        final StockPrice stockPrice3 = new StockPrice(price1);

        Assertions.assertTrue(stockPrice1.compareTo(stockPrice2) == -1 &&
                stockPrice2.compareTo(stockPrice1) == 1 &&
                stockPrice3.compareTo(stockPrice1) == 0);
    }

}