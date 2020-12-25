package project.daihao18.panel.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

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

    /**
     * 科学计数法
     *
     * @param numberStr
     * @return
     */
    public static String convertNumber(String numberStr) {
        BigDecimal resultNumber = new BigDecimal("1");
        // 是否为科学计数法
        if (numberStr.indexOf("E") != -1) {
            int frontLeng = numberStr.indexOf("E");
            String frontNumber = numberStr.substring(0, frontLeng);
            BigDecimal front = new BigDecimal(frontNumber);
            int bankLeng = numberStr.indexOf("+");
            String bankNumber = numberStr.substring(bankLeng + 1, numberStr.length());
            int bankint = Integer.valueOf(bankNumber.split("E")[1]);
            BigDecimal base = new BigDecimal("10");
            BigDecimal bank = new BigDecimal("1");
            for (int k = 0; k < bankint; k++) {
                bank = bank.multiply(base);
            }
            resultNumber = front.multiply(bank);
        } else {
            resultNumber = new BigDecimal(numberStr);
        }
        // 截取小数点，前面的数字
        String resultStr = String.valueOf(resultNumber);
        int point = resultStr.indexOf(".");
        resultStr = resultStr.substring(0, point);
        return resultStr;
    }
}