package project.daihao18.panel.common.utils;

import lombok.extern.slf4j.Slf4j;

/**
 * @ClassName: FlowSizeConverterUtil
 * @Description:
 * @Author: code18
 * @Date: 2020-11-02 14:32
 */
@Slf4j
public class FlowSizeConverterUtil {

    /**
     * 字节转换GB
     *
     * @param bytes
     * @return
     */
    public static Integer BytesToGb(Long bytes) {
        Long result = bytes.longValue() / 1073741824;
        return result.intValue();
    }

    /**
     * GB转换字节
     *
     * @param gb
     * @return
     */
    public static Long GbToBytes(Integer gb) {
        Long result = gb.longValue() * 1073741824;
        return result;
    }

    /**
     * 字节转换其他单位
     *
     * @param bytes
     * @return
     */
    public static String BytesConverter(Long bytes) {
        if (bytes.doubleValue() < 1024) {
            return String.format("%.2f", bytes.doubleValue()) + "B";
        }
        if ((bytes.doubleValue() >= 1024) && (bytes.doubleValue() < 1024 * 1024)) { //KB
            return String.format("%.2f", bytes.doubleValue() / 1024) + "KB";
        } else if ((bytes.doubleValue() >= 1024 * 1024) && (bytes.doubleValue() < 1024 * 1024 * 1024)) {//MB
            return String.format("%.2f", bytes.doubleValue() / (1024 * 1024)) + "MB";
        } else {//GB
            return String.format("%.2f", bytes.doubleValue() / (1024 * 1024 * 1024)) + "GB";
        }
    }
}