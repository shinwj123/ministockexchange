import org.agrona.concurrent.SystemEpochNanoClock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

public class BookSide {
    final TreeMap<StockPrice, PriceLevel> bookSideTree;

    BookSide(final Comparator<StockPrice> comparator) {
        this.bookSideTree = new TreeMap<StockPrice, PriceLevel>(comparator);
    }

    void priceLevelUpdateFromMessage(final MessageFromME messageFromME) {
        StockPrice priceKey = new StockPrice(messageFromME.getPrice());
        //if add size to a price level
        PriceLevel previousLevel = getSpecificLevel(priceKey);
        if (previousLevel == null) {
            previousLevel = toPriceLevel(messageFromME, 0);
        }

        byte[] preProcessingMessage = IEXPriceLevelUpdateMessage(true, previousLevel, true);
        if (messageFromME.getSide() == 1) {
            preProcessingMessage = IEXPriceLevelUpdateMessage(false, previousLevel, true);
        }
        //find a way to send out the message using market data publisher class

        long currentSize = previousLevel.getSize();
        long newSize = currentSize + messageFromME.getSize();
        if (messageFromME.getOrderStatus() != 0) {
            newSize = currentSize - messageFromME.getSize();
        }

        PriceLevel newLevel = toPriceLevel(messageFromME, newSize);
        if (newSize > 0) {
            bookSideTree.put(priceKey, newLevel);
        } else {
            bookSideTree.remove(priceKey);
        }

        byte[] postProcessingMessage = IEXPriceLevelUpdateMessage(true, newLevel, false);
        if (messageFromME.getSide() == 1) {
            postProcessingMessage = IEXPriceLevelUpdateMessage(false, newLevel, false);
        //return newLevel;

        }
        //find a way to send out the message
    }


    long priceLevelUpdateFromMessage(String symbol, StockPrice stockPrice, long deltaQuantity, byte side, PriceLevel previousLevel) {
        //if add size to a price level
        long currentSize = previousLevel.getSize();
        long newSize = currentSize + deltaQuantity;

        PriceLevel newLevel = toPriceLevel(symbol, stockPrice, newSize);
        if (newSize > 0) {
            bookSideTree.put(stockPrice, newLevel);
        } else {
            bookSideTree.remove(stockPrice);
        }
        return newSize;
    }

    public static byte[] IEXPriceLevelUpdateMessage(boolean buySide,
                                                    PriceLevel priceLevel,
                                                    boolean processing) {
        byte buyUpdateTag = (byte) 0x38;
        byte sellUpdateTag = (byte) 0x35;
        byte eventProcessing = (byte) 0x0;
        byte eventComplete = (byte) 0x1;

        byte eventFlag = buyUpdateTag;

        if (buySide) {
            eventFlag= buyUpdateTag;
        } else {
            eventFlag = sellUpdateTag;
        }
        byte messageType = eventProcessing;
        if (processing) {
            messageType = eventProcessing;
        } else {
            messageType = eventComplete;
        }
        byte[] toReturn = new byte[30];
        toReturn[0] = eventFlag;
        toReturn[1] = messageType;

        byte[] timeStampByte = ByteEncoder.longToByteArray(priceLevel.getTimeStamp());
        System.arraycopy(timeStampByte, 0, toReturn, 2, 8);
        byte[] symbolByte = ByteEncoder.stringToByteArray(priceLevel.getStockSymbol(), 8);
        System.arraycopy(symbolByte, 0, toReturn, 10, 8);
        byte[] sizeByte = ByteEncoder.longToByteArray(priceLevel.getSize());
        System.arraycopy(sizeByte, 0, toReturn, 18, 4);
        byte[] priceByte = ByteEncoder.longToByteArray(priceLevel.getStockPrice().getNumber());
        System.arraycopy(priceByte, 0, toReturn, 22, 8);

        return toReturn;
    }

    List<PriceLevel> getLevels() {
        //get all the price levels from the map and put into an arraylist
        return new ArrayList<>(bookSideTree.values());
    }

    PriceLevel getBestSideOffer() {
        //get the price level at the first entry of tree. if ask, return lowest ask, if bid, return highest bid

        // add new logic if tree is emptied , best ask 0, bast bid infinite

        // move this one to orderbooktp
        return bookSideTree.firstEntry().getValue();
    }

    PriceLevel getSpecificLevel(StockPrice priceInput) {
        return bookSideTree.get(priceInput);
    }



    public static PriceLevel toPriceLevel(final MessageFromME messageFromME, long newSize) {
        //extract info from message to construct the pricelevel objects.
        StockPrice price = new StockPrice(messageFromME.getPrice());
        return new PriceLevel(
                messageFromME.getSymbol(),
                messageFromME.getTimeStamp(),
                price,
                newSize
        );
    }


    public static PriceLevel toPriceLevel(String symbol, StockPrice stockPrice, long newSize) {
        //extract info from message to construct the pricelevel objects.
        return new PriceLevel(
                symbol,
                new SystemEpochNanoClock().nanoTime(),
                stockPrice,
                newSize
        );
    }
}
