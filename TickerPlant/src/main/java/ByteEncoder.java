import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ByteEncoder {
    private ByteEncoder() {

    }

    public static byte[] longToByteArray(final long value) {
        return new byte[]{
                (byte) value,
                (byte) (value >> 8 & 0xff),
                (byte) (value >> 16 & 0xff),
                (byte) (value >> 24 & 0xff),
                (byte) (value >> 32 & 0xff),
                (byte) (value >> 40 & 0xff),
                (byte) (value >> 48 & 0xff),
                (byte) (value >> 56 & 0xff)};
    }
    public static byte[] stringToByteArray(final String value, final int size) {
        byte[] outputArray = new byte[size];
        Arrays.fill(outputArray, (byte) ' ');
        final byte[] valueArray = value.getBytes(StandardCharsets.UTF_8);
        if (valueArray.length > outputArray.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        System.arraycopy(valueArray, 0, outputArray, 0, valueArray.length);
        return outputArray;
    }

    public static byte[] intToByteArray(int value) {
        return new byte[]{
                (byte) value,
                (byte) (value >> 8 & 0xff),
                (byte) (value >> 16 & 0xff),
                (byte) (value >> 24 & 0xff)};
    }

}
