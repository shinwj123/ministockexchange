package TickerPlantCore;

import TickerPlantAPI.MessageFromME;
import TickerPlantAPI.PriceLevel;
import TickerPlantAPI.StockPrice;
import TickerPlantByte.ByteEncoder;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import java.util.TreeMap;

public class BookSide {
    private final TreeMap<StockPrice, PriceLevel> bookSideTree;


    BookSide(final Comparator<StockPrice> comparator) {
        this.bookSideTree = new TreeMap<StockPrice, PriceLevel>(comparator);
    }


    /*PriceLevel priceLevelUpdateFromMessage(final MessageFromME messageFromME) {
        StockPrice priceKey = new StockPrice(messageFromME.getPrice());
        if (messageFromME.getOrderStatus() == 0){
            //if add size to a price level
            if (bookSideTree.containsKey((priceKey))) {
                PriceLevel previousLevel = bookSideTree.get(priceKey);
                int currentSize = previousLevel.getSize();
                int newSize = currentSize + messageFromME.getSize();
                PriceLevel newLevel = toPriceLevel(messageFromME, newSize);
                bookSideTree.put(priceKey, newLevel);
                return newLevel;

            } else {
                PriceLevel newLevel = toPriceLevel(messageFromME, messageFromME.getSize());
                bookSideTree.put(priceKey, newLevel);
                return newLevel;
            }
        } else {
            // if decrease size to a price level
            if (bookSideTree.containsKey((priceKey))) {
                PriceLevel previousLevel = bookSideTree.get(priceKey);
                int currentSize = previousLevel.getSize();
                int newSize = currentSize - messageFromME.getSize();
                if (newSize > 0) {
                    PriceLevel newLevel = toPriceLevel(messageFromME, newSize);
                    bookSideTree.put(priceKey, newLevel);
                    return newLevel;
                } else {
                    bookSideTree.remove(priceKey);

                    return toPriceLevel(messageFromME,0);

                }

            } else {
                throw new IllegalArgumentException("cannot have price level with negative stock size");
            }

            //throw new IllegalArgumentException("Unknown Event Flag. Cannot process price level update");
        }


    }*/

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
        //find a way to send out the message

        int currentSize = previousLevel.getSize();
        int newSize = currentSize + messageFromME.getSize();
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


    public static byte[] IEXPriceLevelUpdateMessage(boolean buySide,
                                                    PriceLevel priceLevel,
                                                    boolean Processing) {
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
        if (Processing) {
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
        byte[] sizeByte = ByteEncoder.intToByteArray(priceLevel.getSize());
        System.arraycopy(sizeByte, 0, toReturn, 18, 4);
        byte[] priceByte = ByteEncoder.longToByteArray(priceLevel.getStockPrice().getNumber());
        System.arraycopy(priceByte, 0, toReturn, 22, 8);

        return toReturn;
    }

    List<PriceLevel> getLevels() {
        //get all the price levels from the map and put into an arraylist
        return new ArrayList<>(bookSideTree.values());
    }

    PriceLevel getBestOffer() {
        //get the price level at the first entry of tree. if ask, return lowest ask, if bid, return highest bid
        return bookSideTree.firstEntry().getValue();
    }

    PriceLevel getSpecificLevel(StockPrice priceInput) {
        return bookSideTree.get(priceInput);
    }



    private PriceLevel toPriceLevel(final MessageFromME messageFromME, int newSize) {
        //extract info from message to construct the pricelevel objects.
        StockPrice price = new StockPrice(messageFromME.getPrice());
        return new PriceLevel(
                messageFromME.getSymbol(),
                messageFromME.getTimeStamp(),
                price,
                newSize
        );
    }


}
