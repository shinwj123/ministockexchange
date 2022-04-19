package TickerPlantAPI;
import java.util.List;

public interface OrderBook {
    String getStockSymbols();

    List <PriceLevel> getAllBidLevels();

    List <PriceLevel> getAllAskLevels();

    PriceLevel getBestBidLevel();

    PriceLevel getBestAskLevel();

}
