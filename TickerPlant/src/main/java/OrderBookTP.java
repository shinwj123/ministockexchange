import java.util.Comparator;
import java.util.List;

import static java.lang.Integer.MAX_VALUE;





public class OrderBookTP implements OrderBook {
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

    public void priceLevelUpdate(String symbol, StockPrice stockPrice, long deltaQuantity, byte side, int direction, PriceLevel previousLevel) {
        //based on the message, if it is sell, put into the ask side
        //if it is buying, put into the bidside
        // else, through illigal arg exception since messagetype is unknown...

        byte buyUpdateTag = (byte) 0x38;
        byte sellUpdateTag = (byte) 0x35;
        byte eventProcessing = (byte) 0x0;
        byte eventComplete = (byte) 0x1;
        if (side == sellUpdateTag) {
            // find some way to send the message using MarketDataPublisher
            askSide.priceLevelUpdateFromMessage(symbol, stockPrice, deltaQuantity, side, direction, previousLevel);


        } else if (side == buyUpdateTag) {
            bidSide.priceLevelUpdateFromMessage(symbol, stockPrice, deltaQuantity, side, direction, previousLevel);

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
        if (bidSide.bookSideTree.isEmpty()) {
            StockPrice price = new StockPrice(0);
            PriceLevel toReturn = new PriceLevel(stockSymbol, System.nanoTime(), price, 0);
            return toReturn;
        } else {
            return bidSide.getBestSideOffer();
        }

    }

    @Override
    public PriceLevel getBestAskLevel() {
        if (askSide.bookSideTree.isEmpty()) {
            StockPrice price = new StockPrice(MAX_VALUE);
            PriceLevel toReturn = new PriceLevel(stockSymbol, System.nanoTime(), price, 0);
            return toReturn;
        } else {
            return askSide.getBestSideOffer();
        }
    }

    @Override
    public String toString() {
       //print the entire order book
        return "";
    }

}
