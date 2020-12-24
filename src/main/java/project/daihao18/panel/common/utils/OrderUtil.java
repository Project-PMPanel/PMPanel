package project.daihao18.panel.common.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @ClassName: OrderUtil
 * @Description:
 * @Author: code18
 * @Date: 2020-11-02 21:41
 */
public class OrderUtil {

    private static int sequence = 1;

    public static synchronized String getOrderId() {
        sequence = sequence >= 999999 ? 1 : sequence + 1;
        String datetime = new SimpleDateFormat("yyyyMMddHHmm")
                .format(new Date());
        String s = Integer.toString(sequence);
        return datetime + addLeftZero(s, 4);
    }

    public static String addLeftZero(String s, int length) {
        // StringBuilder sb=new StringBuilder();
        int old = s.length();
        if (length > old) {
            char[] c = new char[length];
            char[] x = s.toCharArray();
            if (x.length > length) {
                throw new IllegalArgumentException(
                        "Numeric value is larger than intended length: " + s
                                + " LEN " + length);
            }
            int lim = c.length - x.length;
            for (int i = 0; i < lim; i++) {
                c[i] = '0';
            }
            System.arraycopy(x, 0, c, lim, x.length);
            return new String(c);
        }
        return s.substring(0, length);
    }
}