package TickerPlantAPImain;


import java.util.Objects;

import static java.lang.Long.compare;

public class StockPrice implements Comparable<StockPrice>{

    private final long number;

    public StockPrice(final long number) {
        this.number = number;
    }

    public long getNumber() {
        return number;
    }



    @Override
    public int compareTo(final StockPrice stockPrice) {
        return compare(this.getNumber(), stockPrice.getNumber());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockPrice stockPrice = (StockPrice) o;
        return number == stockPrice.number;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(number);
    }

    @Override
    public String toString() {
        return this.toString();
    }
}
