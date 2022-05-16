import java.util.Comparator;
import java.util.Formatter;
import java.util.List;

import static java.lang.Integer.MAX_VALUE;

public class OrderBookTP {
    private final String stockSymbol;

    public final BookSide bidSide;
    public final BookSide askSide;

    public OrderBookTP(final String symbol) {
        this.stockSymbol = symbol;
        this.bidSide = new BookSide(Comparator.reverseOrder());
        this.askSide = new BookSide(Comparator.naturalOrder());
    }

    public void priceLevelUpdate(final MessageFromME messageFromME) {
        //based on the message, if it is sell, put into the ask side
        //if it is buying, put into the bidside
        // else, through illegal arg exception since messagetype is unknown...
        if (messageFromME.getSide() == 0) {
            // find some way to send the message using MarketDataPublisher
            askSide.priceLevelUpdateFromMessage(messageFromME);


        } else if (messageFromME.getSide() == 1) {
            bidSide.priceLevelUpdateFromMessage(messageFromME);

        } else {
            throw new IllegalArgumentException("Unknown Price Level Update side. Cannot proceed.");
        }
    }

    public long priceLevelUpdate(String symbol, StockPrice stockPrice, long deltaQuantity, byte side, PriceLevel previousLevel) {
        //based on the message, if it is sell, put into the ask side
        //if it is buying, put into the bidside
        // else, through illegal arg exception since messagetype is unknown...

        byte buyUpdateTag = (byte) 0x38;
        byte sellUpdateTag = (byte) 0x35;
        if (side == sellUpdateTag) {
            // find some way to send the message using MarketDataPublisher
            return askSide.priceLevelUpdateFromMessage(symbol, stockPrice, deltaQuantity, side, previousLevel);


        } else if (side == buyUpdateTag) {
            return bidSide.priceLevelUpdateFromMessage(symbol, stockPrice, deltaQuantity, side, previousLevel);

        } else {
            throw new IllegalArgumentException("Unknown Price Level Update side. Cannot proceed.");
        }
    }

    public String getStockSymbols() {
        return stockSymbol;
    }


    public List<PriceLevel> getAllBidLevels() {
        return bidSide.getLevels();
    }


    public List<PriceLevel> getAllAskLevels() {
        return askSide.getLevels();
    }


    public PriceLevel getBestBidLevel() {
        if (bidSide.bookSideTree.isEmpty()) {
            StockPrice price = new StockPrice(0);
            return new PriceLevel(stockSymbol, System.nanoTime(), price, 0);
        } else {
            return bidSide.getBestSideOffer();
        }

    }


    public PriceLevel getBestAskLevel() {
        if (askSide.bookSideTree.isEmpty()) {
            StockPrice price = new StockPrice(MAX_VALUE);
            return new PriceLevel(stockSymbol, System.nanoTime(), price, 0);
        } else {
            return askSide.getBestSideOffer();
        }
    }

    public void printOrderBook() {
        Formatter formatter = new Formatter();
        formatter.format("%12s %12s\n", " ", stockSymbol);
        formatter.format("%12s %12s %12s\n", "BID", "Price", "ASK");
        for (PriceLevel level : askSide.bookSideTree.descendingMap().values()) {
            formatter.format("%12s %12s %12d\n", " ", level.printPrice(), level.getSize());
        }
        formatter.format("%s\n", "-".repeat(50));
        for (PriceLevel level : bidSide.bookSideTree.values()) {
            formatter.format("%12d %12s %12s\n", level.getSize(), level.printPrice(), " ");
        }
        System.out.println(formatter);
    }

}
