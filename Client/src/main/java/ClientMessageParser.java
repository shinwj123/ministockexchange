import quickfix.field.OrdType;
import quickfix.field.Side;
import quickfix.field.Symbol;

import java.util.ArrayList;
import java.util.Arrays;

import static quickfix.field.Side.BUY;
import static quickfix.field.Side.SELL;

public class ClientMessageParser {
    private final String script;

    public ClientMessageParser(String script) {
        //"NVDA  Side.BUY  OrdType.LIMIT  200.25  10\nAAPL  Side.BUY  OrdType.MARKET  160  10"
        this.script = script;
    }

    public ArrayList<String[]> setOrderArray(String script) {
        ArrayList<String[]> orderArray = new ArrayList<String[]>();

        var scriptArray = script.trim().split("\\n");

        for (int i = 0; i < scriptArray.length; i++) {
            String[] tempArray = scriptArray[i].split("\\s\\s");
            orderArray.add(tempArray);
        }

        return orderArray;
    }

    public String[] getSingleOrder(ArrayList<String[]> orderArray, int n) {
        String[] singleOrder = orderArray.get(n);
        return singleOrder;
    }

    public Symbol getSymbol(String[] singleOrder) {
        String symbolString = Arrays.deepToString(new String[]{singleOrder[0]})
                .replace(",", "")  //remove the commas
                .replace("[", "")  //remove the right bracket
                .replace("]", "")  //remove the left bracket
                .trim();

        Symbol symbol = new Symbol(symbolString);

        return symbol;
    }

    public int getQuantity(String[] singleOrder) {
        String quantityString = Arrays.toString(new String[]{singleOrder[4]})
                .replace(",", "")  //remove the commas
                .replace("[", "")  //remove the right bracket
                .replace("]", "")  //remove the left bracket
                .trim();

        int quantity = Integer.parseInt(quantityString);

        return quantity;
    }

    public Side getSide(String[] singleOrder) {
        String sideString = Arrays.toString(new String[]{singleOrder[1]})
                .replace(",", "")  //remove the commas
                .replace("[", "")  //remove the right bracket
                .replace("]", "")  //remove the left bracket
                .trim();

        char sideChar = 0;

        if (sideString.equals("Side.BUY"))  {
            sideChar = BUY;
        } else if (sideString.equals("Side.SELL")) {
            sideChar = SELL;
        }

        Side side = new Side(sideChar);

        return side;
    }

    public OrdType getOrdType(String[] singleOrder) {
        String typeString = Arrays.toString(new String[]{singleOrder[2]})
                .replace(",", "")  //remove the commas
                .replace("[", "")  //remove the right bracket
                .replace("]", "")  //remove the left bracket
                .trim();

        char typeChar = 0;

        if (typeString.equals("OrdType.LIMIT"))  {
            typeChar = OrdType.LIMIT;
        } else if (typeString.equals("OrdType.MARKET")) {
            typeChar = OrdType.MARKET;
        }

        OrdType ordType = new OrdType(typeChar);

        return ordType;
    }
}
