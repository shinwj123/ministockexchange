import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClientMessageParse {
    private final String script;

    public ClientMessageParse(String script) {
        //"NVDA  Side.BUY  OrderType.LIMIT  200.25  10\nAAPL  Side.BUY  OrderType.LIMIT  160  10"
        this.script = script;
        ArrayList<String[]> orderArray = new ArrayList<String[]>();

        var scriptArray = script.trim().split("\\n");

        for (int i = 0; i < scriptArray.length; i++) {
            String[] tempArray = scriptArray[i].split("\\s\\s");
            orderArray.add(tempArray);
        }


    }


}
