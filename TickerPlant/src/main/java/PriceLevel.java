import java.util.Objects;

public class PriceLevel {
    private final String stockSymbol;
    private final long timeStamp;
    private final StockPrice stockPrice;
    private int size;

    public PriceLevel (
             String stockSymbolInput,
             long timeStampInput,
             StockPrice stockPriceInput,
             int sizeInput) {
        this.stockSymbol = Objects.requireNonNull(stockSymbolInput, "stockSymbol");
        this.timeStamp = Objects.requireNonNull(timeStampInput, "timeStamp");
        this.stockPrice = Objects.requireNonNull(stockPriceInput, "stockPrice");
        this.size = Objects.requireNonNull(sizeInput, "size");
    }

    public String getStockSymbol() {
        return this.stockSymbol;
    }

    public long getTimeStamp() {
        return this.timeStamp;
    }

    public StockPrice getStockPrice() {
        return this.stockPrice;
    }

    public int getSize() {
        return this.size;
    }

    public void setSize(int sizeInput) {
        this.size = sizeInput;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || this.getClass() != other.getClass()){
            return false;
        }
        PriceLevel otherPrice = (PriceLevel) other;
        return Objects.equals(this.stockSymbol, otherPrice.stockSymbol) &&
                this.timeStamp == otherPrice.timeStamp &&
                this.stockPrice == otherPrice.stockPrice &&
                this.size == otherPrice.size;
    }

    @Override
    public int hashCode() {
        return Objects.hash(stockSymbol, timeStamp, stockPrice, size);
    }

    @Override
    public String toString() {
        return "PriceLevel{" +
                "symbol='" + stockSymbol + '\'' +
                ", timestamp=" + timeStamp +
                ", price=" + stockPrice +
                ", size=" + size +
                '}';
    }
}
