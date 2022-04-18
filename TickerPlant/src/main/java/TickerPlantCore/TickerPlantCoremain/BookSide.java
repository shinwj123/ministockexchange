package TickerPlantCoremain;

import TickerPlantAPImain.MessageFromME;
import TickerPlantAPImain.PriceLevel;
import TickerPlantAPImain.StockPrice;



import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import java.util.TreeMap;

public class BookSide {
    private final TreeMap<StockPrice, PriceLevel> bookSideTree;


    BookSide(final Comparator<StockPrice> comparator) {
        this.bookSideTree = new TreeMap<StockPrice, PriceLevel>(comparator);
    }


    PriceLevel priceLevelUpdateFromMessage(final MessageFromME messageFromME) {
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
