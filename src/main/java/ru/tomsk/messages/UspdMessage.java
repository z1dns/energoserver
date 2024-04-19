package ru.tomsk.messages;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class UspdMessage implements Serialization {
    static final int TRM_COUNT = 250;
    private static final int DATA_LENGTH = 2 + TRM_COUNT * TrmMessage.length();
    private static final int CRC_LENGTH = 2;
    private boolean correctCRC = false;
    short idField = 0; //unsigned
    TrmMessage[] trmMessageArray = new TrmMessage[TRM_COUNT];
    short crcField = 0; //unsigned

    public static int length() {
        return DATA_LENGTH + CRC_LENGTH;
    }

    public boolean isCRCCorrect() {
        return correctCRC;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UspdMessage that = (UspdMessage) o;
        return idField == that.idField && crcField == that.crcField && Arrays.equals(trmMessageArray, that.trmMessageArray);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(idField, crcField);
        result = 31 * result + Arrays.hashCode(trmMessageArray);
        return result;
    }

    @Override
    public String toString() {
        return "UspdMessage{" +
                "correctCRC=" + correctCRC +
                ", idField=" + idField +
                ", TrmPacketArray=\n\t" + arrayToString() +
                ", \ncrcField=" + crcField +
                '}';
    }

    @Override
    public byte[] serialize() {
        byte [] data = new byte[length()];
        var buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort(idField);
        for (var trmMessage : trmMessageArray) {
            buffer.put(trmMessage.serialize());
        }
        crcField =  CRC16.calculate(data, 0, DATA_LENGTH);
        buffer.putShort(crcField);
        return data;
    }

    @Override
    public void deserialize(byte[] bytes) throws IllegalArgumentException {
        if (bytes.length != length()) {
            throw new IllegalArgumentException(String.format("Incorrect count of bytes(%d) for deserialize %s", bytes.length, this.getClass()));
        }
        var buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        idField = buffer.getShort();
        for (int trmIdx = 0; trmIdx < TRM_COUNT; ++trmIdx) {
            var trmMessage = new TrmMessage();
            byte[] trmData = new byte[TrmMessage.length()];
            buffer.get(trmData);
            trmMessage.deserialize(trmData);
            trmMessageArray[trmIdx] = trmMessage;
        }
        crcField = buffer.getShort();
        correctCRC = crcField == CRC16.calculate(bytes, 0, DATA_LENGTH);
    }

    private String arrayToString() {
        return Arrays.stream(trmMessageArray).map(TrmMessage::toString).collect(Collectors.joining("\n\t"));
    }
}