package TickerPlant_Core_main;

import TickerPlant_API_main.PriceLevel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.TreeMap;

public class BookSide {
    private final TreeMap<stockPrice, PriceLevel> bookSideTree;
    private final Queue<PriceLevel> unprocessedOrder = new LinkedList<PriceLevel>();

    BookSide(final Comparator<stockPrice> comparator) {
        this.bookSideTree = new TreeMap<>(comparator);
    }


    void priceLevelUpdateFromMessage(String message) {

        //if the message says orders are completed, then empty the queue and put everything poped from the queue to the tree
        // and we will put the order in the message to the map
        //if order is still tagged "processing", we will put the order into the queue
    }

    List<PriceLevel> getLevels() {
        //get all the price levels from the map and put into an arraylist

    }

    PriceLevel getBestOffer() {
        //get the price level at the first entry of tree. if ask, return lowest ask, if bid, return highest bid
    }


    private void drainPriceLevelQueue() {
        //empty the entire queue and put what ever in queue into the map using
         //   processPriceLevelToOffers(priceLevel);

    }

    private void processPriceLevelToBookSideTree(final PriceLevel priceLevel) {
        //if price level contains size zero stock, remove it from the treemap? this depends on the
        // logic of the system. how should we decide when to remove a pricelevel from the tree.
    }

    private void addEventToQueue(final IEXPriceLevelUpdateMessage iexPriceLevelUpdateMessage) {
        //add price level from the message to queue
    }

    private PriceLevel toPriceLevel(final IEXPriceLevelUpdateMessage iexPriceLevelUpdateMessage) {
        //extract info from message to construct the pricelevel objects.


}
