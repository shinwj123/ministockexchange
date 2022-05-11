import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Objects;

class PriceLevelUpdateMessageTest {

    @Test
    public void successfullyCreatePriceLevelUpdateMessageInstance() {
        final String symbol = "GOOGL";
        final long timestamp = 123456789L;
        final StockPrice price = new StockPrice(12345L);
        final int size = 250;

        PriceLevel priceLevel = new PriceLevel(symbol, timestamp, price, size);
        byte[] messageByte = BookSide.IEXPriceLevelUpdateMessage(true, priceLevel, true);

        toPriceLevelUpdateMessage testingMessage = new toPriceLevelUpdateMessage(messageByte);
        Assertions.assertTrue(Objects.equals(testingMessage.getMessageType(), "PRICE_LEVEL_UPDATE_BUY") &&
                Objects.equals(testingMessage.getEventFlag(),"ORDER_BOOK_IS_PROCESSING_EVENT") &&
                testingMessage.getTimestamp() == 123456789L &&
                testingMessage.getPrice() == 12345L &&
                testingMessage.getSize() == 250
        );
    }



}