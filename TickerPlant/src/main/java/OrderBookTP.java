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
        // else, through illigal arg exception since messagetype is unknown...
        if (messageFromME.getSide() == 0) {
            // find some way to send the message using MarketDataPublisher
            askSide.priceLevelUpdateFromMessage(messageFromME);


        } else if (messageFromME.getSide() == 1) {
            bidSide.priceLevelUpdateFromMessage(messageFromME);

        } else {
            throw new IllegalArgumentException("Unknown Price Level Update side. Cannot proceed.");
        }
    }

    public void priceLevelUpdate(String symbol, StockPrice stockPrice, long deltaQuantity, byte side, PriceLevel previousLevel) {
        //based on the message, if it is sell, put into the ask side
        //if it is buying, put into the bidside
        // else, through illigal arg exception since messagetype is unknown...

        byte buyUpdateTag = (byte) 0x38;
        byte sellUpdateTag = (byte) 0x35;
        byte eventProcessing = (byte) 0x0;
        byte eventComplete = (byte) 0x1;
        if (side == sellUpdateTag) {
            // find some way to send the message using MarketDataPublisher
            askSide.priceLevelUpdateFromMessage(symbol, stockPrice, deltaQuantity, side, previousLevel);


        } else if (side == buyUpdateTag) {
            bidSide.priceLevelUpdateFromMessage(symbol, stockPrice, deltaQuantity, side, previousLevel);

        } else {
            throw new IllegalArgumentException("Unknown Price Level Update side. Cannot proceed.");
        }
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
        byte[] sizeByte = ByteEncoder.longToByteArray(priceLevel.getSize());
        System.arraycopy(sizeByte, 0, toReturn, 18, 4);
        byte[] priceByte = ByteEncoder.longToByteArray(priceLevel.getStockPrice().getNumber());
        System.arraycopy(priceByte, 0, toReturn, 22, 8);

        return toReturn;
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
            PriceLevel toReturn = new PriceLevel(stockSymbol, System.nanoTime(), price, 0);
            return toReturn;
        } else {
            return bidSide.getBestSideOffer();
        }

    }


    public PriceLevel getBestAskLevel() {
        if (askSide.bookSideTree.isEmpty()) {
            StockPrice price = new StockPrice(MAX_VALUE);
            PriceLevel toReturn = new PriceLevel(stockSymbol, System.nanoTime(), price, 0);
            return toReturn;
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
