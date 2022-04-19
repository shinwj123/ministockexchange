package TickerPlantCore;


import java.util.Comparator;
import java.util.List;

import TickerPlantAPI.*;
import TickerPlantByte.ByteEncoder;




public class OrderBookTP implements OrderBook {
    private final String stockSymbol;

    private final BookSide bidSide;
    private final BookSide askSide;




    public OrderBookTP(final String symbol) {
        this.stockSymbol = symbol;
        this.bidSide = new BookSide(Comparator.reverseOrder());
        this.askSide = new BookSide(Comparator.naturalOrder());
    }

    public void priceLevelUpdate(final MessageFromME messageFromME) {
        byte eventProcessing = (byte) 0x0;
        byte eventComplete = (byte) 0x1;
        byte buyUpdateTag = (byte) 0x38;
        byte sellUpdateTag = (byte) 0x35;

        //based on the message, if it is sell, put into the ask side
        //if it is buying, put into the bidside
        // else, through illigal arg exception since messagetype is unknown...
        if (messageFromME.getSide() == 0) {
            StockPrice stockPrice = new StockPrice(messageFromME.getPrice());
            PriceLevel preProcessingLevel = new PriceLevel(messageFromME.getSymbol(), messageFromME.getTimeStamp(),stockPrice, messageFromME.getSize());
            PriceLevel temp = askSide.getSpecificLevel(stockPrice);
            if (temp != null) {
                preProcessingLevel = temp;
            }
            byte[] preProcessingMessage = IEXPriceLevelUpdateMessage(
                    eventProcessing,
                    preProcessingLevel,
                    sellUpdateTag
            );

            // find some way to send the message using MarketDataPublisher
            PriceLevel postProcessingLevel = askSide.priceLevelUpdateFromMessage(messageFromME);
            byte[] postProcessingMessage = IEXPriceLevelUpdateMessage(
                    eventComplete,
                    postProcessingLevel,
                    sellUpdateTag
            );

        } else if (messageFromME.getSide() == 1) {

            StockPrice stockPrice = new StockPrice(messageFromME.getPrice());
            PriceLevel preProcessingLevel = new PriceLevel(messageFromME.getSymbol(), messageFromME.getTimeStamp(),stockPrice, messageFromME.getSize());
            PriceLevel temp = askSide.getSpecificLevel(stockPrice);
            if (temp != null) {
                preProcessingLevel = temp;
            }
            byte[] preProcessingMessage = IEXPriceLevelUpdateMessage(
                    eventProcessing,
                    preProcessingLevel,
                    buyUpdateTag
            );
            PriceLevel postProcessingLevel = bidSide.priceLevelUpdateFromMessage(messageFromME);

            byte[] postProcessingMessage = IEXPriceLevelUpdateMessage(
                    eventComplete,
                    postProcessingLevel,
                    buyUpdateTag
            );

        } else {
            throw new IllegalArgumentException("Unknown Price Level Update side. Cannot proceed.");
        }
    }

    public static byte[] IEXPriceLevelUpdateMessage(byte eventFlag,
                                                    PriceLevel priceLevel,
                                                    byte messageType) {
        byte[] toReturn = new byte[30];
        toReturn[0] = messageType;
        toReturn[1] = eventFlag;
        byte[] timeStampByte = ByteEncoder.longToByteArray(priceLevel.getTimeStamp());
        System.arraycopy(timeStampByte, 0, toReturn, 2, 8);
        byte[] symbolByte = ByteEncoder.stringToByteArray(priceLevel.getStockSymbol(), 8);
        System.arraycopy(symbolByte, 0, toReturn, 10, 8);
        byte[] sizeByte = ByteEncoder.intToByteArray(priceLevel.getSize());
        System.arraycopy(sizeByte, 0, toReturn, 18, 4);
        byte[] priceByte = ByteEncoder.longToByteArray(priceLevel.getStockPrice().getNumber());
        System.arraycopy(priceByte, 0, toReturn, 22, 8);

        return toReturn;
    }

    @Override
    public String getStockSymbols() {
        return stockSymbol;
    }

    @Override
    public List<PriceLevel> getAllBidLevels() {
        return bidSide.getLevels();
    }

    @Override
    public List<PriceLevel> getAllAskLevels() {
        return askSide.getLevels();
    }

    @Override
    public PriceLevel getBestBidLevel() {
        return bidSide.getBestOffer();
    }

    @Override
    public PriceLevel getBestAskLevel() {
        return askSide.getBestOffer();
    }

    @Override
    public String toString() {
       //print the entire order book
        return "";
    }

}
