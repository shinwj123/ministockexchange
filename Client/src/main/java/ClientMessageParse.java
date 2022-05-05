import quickfix.field.OrdType;
import quickfix.field.Side;
import quickfix.field.Symbol;

import java.util.ArrayList;
import java.util.Arrays;

import static quickfix.field.OrdType.LIMIT;

public class ClientMessageParse {
    private final String script;

    public ClientMessageParse(String script) {
        //"NVDA  Side.BUY  OrdType.LIMIT  200.25  10\nAAPL  Side.BUY  OrdType.MARKET  160  10"
        this.script = script;
        ArrayList<String[]> orderArray = new ArrayList<String[]>();

        var scriptArray = script.trim().split("\\n");

        for (int i = 0; i < scriptArray.length; i++) {
            String[] tempArray = scriptArray[i].split("\\s\\s");
            orderArray.add(tempArray);
        }


    }

    public static void main(String[] args) {
        var script = "NVDA  Side.BUY  OrdType.LIMIT  200.25  10\nAAPL  Side.BUY  OrdType.LIMIT  160  16";
        System.out.println(script);
        var scriptArray = script.trim().split("\\n");
        System.out.println(Arrays.toString(scriptArray));
        ArrayList<String[]> orderArray = new ArrayList<String[]>();
        for (int i = 0; i < scriptArray.length; i++) {
            String[] temparray = scriptArray[i].split("\\s\\s");
            orderArray.add(temparray);
        }

        System.out.println(Arrays.deepToString(orderArray.toArray()));

        String[] singleOrder = orderArray.get(1);

        //getting symbol
        String symbolString = Arrays.deepToString(new String[]{singleOrder[0]})
                .replace(",", "")  //remove the commas
                .replace("[", "")  //remove the right bracket
                .replace("]", "")  //remove the left bracket
                .trim();
        System.out.println(symbolString);

        Symbol symbol = new Symbol(symbolString);

        //getting quantity
        String quantityString = Arrays.toString(new String[]{singleOrder[4]})
                .replace(",", "")  //remove the commas
                .replace("[", "")  //remove the right bracket
                .replace("]", "")  //remove the left bracket
                .trim();
        System.out.println(quantityString);

        int quantity = Integer.parseInt(quantityString);
        System.out.println(quantity);

        //getting side
        String sideString = Arrays.toString(new String[]{singleOrder[1]})
                .replace(",", "")  //remove the commas
                .replace("[", "")  //remove the right bracket
                .replace("]", "")  //remove the left bracket
                .trim();
        System.out.println(sideString);

        char sideChar = 0;

        if (sideString == "Side.BUY")  {
            sideChar = Side.BUY;
        } else if (sideString == "Side.SELL") {
            sideChar = Side.SELL;
        }

        Side side = new Side(sideChar);

        //getting orderType
        String typeString = Arrays.toString(new String[]{singleOrder[2]})
                .replace(",", "")  //remove the commas
                .replace("[", "")  //remove the right bracket
                .replace("]", "")  //remove the left bracket
                .trim();
        System.out.println(typeString);

        char typeChar = 0;

        if (sideString == "OrdType.LIMIT")  {
            typeChar = OrdType.LIMIT;
        } else if (sideString == "OrdType.MARKET") {
            typeChar = OrdType.MARKET;
        }

        OrdType ordType = new OrdType(typeChar);


//        String value = new String();
//        for (int n = 0; n < orderArray.size(); n++) {
//            String[] temp = orderArray.get(n);
//            value = Arrays.toString(new String[]{temp[0]});
//            System.out.println(value);
//        }


    }

}
