import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static java.lang.Integer.MAX_VALUE;

class OrderBookTPTest {
    private final String testSymbol = "GOOGL";

    @Test
    public void byteCodeMessageTranslateCorrectly() {
        //this test uses example bytecode from IEX documentation directly to perform the test
        StockPrice price = new StockPrice(17532L);
        String timeStamp = new SimpleDateFormat("2016.08.23.15.30.ss").format(new Date());
        PriceLevel sample = new PriceLevel("AAPL", 14875433212L, price, 100);
        byte[] priceLevelUpdateMessageByte = BookSide.IEXPriceLevelUpdateMessage(true, sample, false);


        StringBuilder sb = new StringBuilder();
        for (byte b : priceLevelUpdateMessageByte) {
            sb.append(String.format("%02X ", b));
        }


        toPriceLevelUpdateMessage fromByte = new toPriceLevelUpdateMessage(priceLevelUpdateMessageByte);
        String message = fromByte.toString();
        Assertions.assertTrue(Objects.equals(fromByte.getEventFlag(), "EVENT_PROCESSING_COMPLETE" ) &&
                Objects.equals(fromByte.getSymbol(), "AAPL") &&
                fromByte.getTimestamp() == 14875433212L &&
                fromByte.getPrice() == 17532L &&
                fromByte.getSize() == 100 &&
                fromByte.getMessageType() == "PRICE_LEVEL_UPDATE_BUY"
        );

    }

    @Test
    public void shouldSuccessfullyAddBidQuote() {
        final MessageFromME testMessage = new MessageFromME(
                225,
                0,
                123456789L,
                testSymbol,
                100,
                12345L,
                1);
        final OrderBookTP testBook = new OrderBookTP(testSymbol);
        testBook.priceLevelUpdate(testMessage);

        final List <PriceLevel> bidPriceLevels = testBook.getAllBidLevels();

        final PriceLevel priceLevel = bidPriceLevels.get(0); //there should be only 1 element in the list;

        Assertions.assertTrue(bidPriceLevels.size() == 1 &&
                priceLevel.getTimeStamp() == testMessage.getTimeStamp() &&
                priceLevel.getSize() == testMessage.getSize() &&
                priceLevel.getStockPrice().getNumber() == testMessage.getPrice() &&
                testBook.getAllAskLevels().isEmpty());

    }

    @Test
    public void shouldSuccessfullyAddAskQuote() {
        final MessageFromME testMessage = new MessageFromME(
                225,
                0,
                123456789L,
                testSymbol,
                100,
                12345L,
                0);
        final OrderBookTP testBook = new OrderBookTP(testSymbol);
        testBook.priceLevelUpdate(testMessage);

        final List <PriceLevel> askPriceLevels = testBook.getAllAskLevels();

        final PriceLevel priceLevel = askPriceLevels.get(0); //there should be only 1 element in the list;

        Assertions.assertTrue(askPriceLevels.size() == 1 &&
                priceLevel.getTimeStamp() == testMessage.getTimeStamp() &&
                priceLevel.getSize() == testMessage.getSize() &&
                priceLevel.getStockPrice().getNumber() == testMessage.getPrice() &&
                testBook.getAllBidLevels().isEmpty());

    }

    @Test
    public void shouldUpdateBidPriceLevel() {
        MessageFromME testMessage1 = new MessageFromME(
                225,
                0,
                123456789L,
                testSymbol,
                100,
                12345L,
                1);
        MessageFromME testMessage2 = new MessageFromME(
                225,
                0,
                223456789L,
                testSymbol,
                50,
                12345L,
                1);
        final OrderBookTP testBook = new OrderBookTP(testSymbol);

        testBook.priceLevelUpdate(testMessage1);

        Assertions.assertTrue(testBook.getAllBidLevels().get(0).getSize() == testMessage1.getSize());


        testBook.priceLevelUpdate(testMessage2);
        Assertions.assertTrue(testBook.getAllBidLevels().get(0).getSize() == testMessage1.getSize() + testMessage2.getSize());

    }


    @Test
    public void shouldUpdateAskPriceLevel() {
        MessageFromME testMessage1 = new MessageFromME(
                225,
                0,
                123456789L,
                testSymbol,
                100,
                12345L,
                0);
        MessageFromME testMessage2 = new MessageFromME(
                225,
                0,
                223456789L,
                testSymbol,
                50,
                12345L,
                0);
        final OrderBookTP testBook = new OrderBookTP(testSymbol);

        testBook.priceLevelUpdate(testMessage1);

        Assertions.assertTrue(testBook.getAllAskLevels().get(0).getSize() == testMessage1.getSize());


        testBook.priceLevelUpdate(testMessage2);
        Assertions.assertTrue(testBook.getAllAskLevels().get(0).getSize() == testMessage1.getSize() + testMessage2.getSize());

    }

    @Test
    public void shouldRemoveBidPriceLevelAndReturnPrescribedPriceLevel() {
        MessageFromME testMessage1 = new MessageFromME(
                225,
                0,
                123456789L,
                testSymbol,
                100,
                12345L,
                1);
        MessageFromME testMessage2 = new MessageFromME(
                225,
                1,
                223456789L,
                testSymbol,
                100,
                12345L,
                1);
        final OrderBookTP testBook = new OrderBookTP(testSymbol);

        testBook.priceLevelUpdate(testMessage1);
        Assertions.assertTrue(testBook.getAllBidLevels().get(0).getSize() == testMessage1.getSize());


        testBook.priceLevelUpdate(testMessage2);
        Assertions.assertTrue(testBook.getAllBidLevels().isEmpty());

        final PriceLevel emptyBid = testBook.getBestBidLevel();

        Assertions.assertTrue(emptyBid.getSize() == 0 &&
                emptyBid.getStockPrice().getNumber() == 0);
    }

    @Test
    public void shouldRemoveAskPriceLevelAndReturnPrescribedPriceLevel() {
        MessageFromME testMessage1 = new MessageFromME(
                225,
                0,
                123456789L,
                testSymbol,
                100,
                12345L,
                0);
        MessageFromME testMessage2 = new MessageFromME(
                225,
                1,
                223456789L,
                testSymbol,
                100,
                12345L,
                0);
        final OrderBookTP testBook = new OrderBookTP(testSymbol);

        testBook.priceLevelUpdate(testMessage1);
        Assertions.assertTrue(testBook.getAllAskLevels().get(0).getSize() == testMessage1.getSize());


        testBook.priceLevelUpdate(testMessage2);
        Assertions.assertTrue(testBook.getAllAskLevels().isEmpty());

        final PriceLevel emptyAsk = testBook.getBestAskLevel();

        Assertions.assertTrue(emptyAsk.getSize() == 0 &&
                emptyAsk.getStockPrice().getNumber() == MAX_VALUE);

    }


    @Test
    public void shouldSuccessfullyReturnBestBid() {
        final MessageFromME testMessage1 = new MessageFromME(
                225,
                0,
                123456789L,
                testSymbol,
                100,
                12345L,
                1);


        final MessageFromME testMessage2 = new MessageFromME(
                225,
                0,
                123456789L,
                testSymbol,
                100,
                123456L,
                1);
        final OrderBookTP testBook = new OrderBookTP(testSymbol);

        testBook.priceLevelUpdate(testMessage1);

        testBook.priceLevelUpdate(testMessage2);

        final PriceLevel bestBid= testBook.getBestBidLevel();
        Assertions.assertTrue(bestBid.getStockPrice().getNumber() == testMessage2.getPrice() &&
                testBook.getAllAskLevels().isEmpty());
    }


    @Test
    public void shouldSuccessfullyReturnBestAsk() {
        final MessageFromME testMessage1 = new MessageFromME(
                225,
                0,
                123456789L,
                testSymbol,
                100,
                12345L,
                0);


        final MessageFromME testMessage2 = new MessageFromME(
                225,
                0,
                123456789L,
                testSymbol,
                100,
                123456L,
                0);
        final OrderBookTP testBook = new OrderBookTP(testSymbol);

        testBook.priceLevelUpdate(testMessage1);

        testBook.priceLevelUpdate(testMessage2);

        final PriceLevel bestBid= testBook.getBestAskLevel();
        Assertions.assertTrue(bestBid.getStockPrice().getNumber() == testMessage1.getPrice() &&
                testBook.getAllBidLevels().isEmpty());
    }

    //edge case -- what if no price level in bid or ask

}