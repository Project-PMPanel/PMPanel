package project.daihao18.panel.common.utils;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * @ClassName: UuidUtil
 * @Description:
 * @Author: code18
 * @Date: 2020-11-26 11:42
 */
public class UuidUtil {

    private static UUID NAMESPACE_DNS = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");

    public static String uuid3(String info) {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[16 + info.getBytes().length]);
        buffer.putLong(NAMESPACE_DNS.getMostSignificantBits());
        buffer.putLong(NAMESPACE_DNS.getLeastSignificantBits());
        buffer.put(info.getBytes());

        byte[] uuidBytes = buffer.array();

        return UUID.nameUUIDFromBytes(uuidBytes).toString();
    }
}