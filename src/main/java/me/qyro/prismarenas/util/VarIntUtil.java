package me.qyro.prismarenas.util;

public final class VarIntUtil {

    private VarIntUtil() {
    }

    public static void write(java.io.DataOutput out, int value) throws java.io.IOException {
        while ((value & ~0x7F) != 0) {
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    public static int read(java.io.DataInput in) throws java.io.IOException {
        int value = 0;
        int shift = 0;
        int b;
        do {
            b = in.readUnsignedByte();
            value |= (b & 0x7F) << shift;
            shift += 7;
        } while ((b & 0x80) != 0);
        return value;
    }
}
