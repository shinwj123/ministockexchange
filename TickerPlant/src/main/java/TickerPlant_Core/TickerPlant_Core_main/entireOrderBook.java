package TickerPlant_Core_main;

import java.util.Comparator;
import java.util.List;

import TickerPlant_API_main.PriceLevel;

public class entireOrderBook extends BookSide {
    private final String stockSymbol;

    private final BookSide bidSide;
    private final BookSide askSide;

    public entireOrderBookrderBook(final String symbol) {
        this.symbol = symbol;
        this.bidSide = new BookSide(Comparator.reverseOrder());
        this.askSide = new BookSide(Comparator.naturalOrder());
    }

    public void priceLevelUpdate(final IEXPriceLevelUpdateMessage iexPriceLevelUpdateMessage) {

        //based on the message, if it is sell, put into the ask side
        //if it is buying, put into the bidside
        // else, through illigal arg exception since messagetype is unknown...
    }

    @Override
    public String getSymbol() {
        return stockSymbol
    }

    @Override
    public List<PriceLevel> getBidLevels() {
        return bidSide.getLevels();
    }

    @Override
    public List<PriceLevel> getAskLevels() {
        return askSide.getLevels();
    }

    @Override
    public PriceLevel getBestAskOffer() {
        return askSide.getBestOffer();
    }

    @Override
    public PriceLevel getBestBidOffer() {
        return bidSide.getBestOffer();
    }

    @Override
    public String toString() {
       //print the entire order book
    }

}
