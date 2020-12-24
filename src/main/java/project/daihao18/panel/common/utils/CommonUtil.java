package project.daihao18.panel.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName: CommonUtil
 * @Description:
 * @Author: code18
 * @Date: 2020-12-05 18:09
 */
@Slf4j
@Component
public class CommonUtil {

    private static Map<String, String> getCharacterMapId() {
        Map<String, String> map = new HashMap<>();
        int a = 97;
        for (int i = 0; i < 26; i++) {
            char ch = (char) (a + i);
            map.put(String.valueOf(ch), String.valueOf(i));
        }
        return map;
    }

    private static Map<String, String> getIdMapCharacter() {
        Map<String, String> map = new HashMap<>();
        int a = 97;
        for (int i = 0; i < 26; i++) {
            char ch = (char) (a + i);
            map.put(String.valueOf(i), String.valueOf(ch));
        }
        return map;
    }

    /**
     * 订阅链接id简单加密
     * @param userId
     * @return
     */
    public static String subsEncryptId(Integer userId) {
        // 第一个字母为length,为用户id长度,后面拼接该长度的字符串
        int length = String.valueOf(userId).length();
        // ID->字符串
        Map<String, String> ID_MAP_CHARACTER = getIdMapCharacter();
        String prefix = ID_MAP_CHARACTER.get(String.valueOf(length));
        String suffix = "";
        char[] idChar = userId.toString().toCharArray();
        for (int i = 0; i < length; i++) {
            String s = ID_MAP_CHARACTER.get(String.valueOf(idChar[i]));
            suffix += s;
        }
        return prefix + suffix;
    }

    /**
     * 订阅链接id解密,获取id
     * @param link
     * @return
     */
    public static Integer subsDecryptId(String link) {
        Map<String, String> CHARACTER_MAP_ID = getCharacterMapId();
        int length = Integer.parseInt(CHARACTER_MAP_ID.get(link.substring(0, 1)));
        String StringId = link.substring(1, length + 1);
        String id = "";
        char[] chs = StringId.toCharArray();
        for (int i = 0; i < StringId.length(); i++) {
            id += CHARACTER_MAP_ID.get(String.valueOf(chs[i]));
        }
        return Integer.parseInt(id);
    }

    /**
     * 订阅链接id解密,获取link
     * @param allLink
     * @return
     */
    public static String subsLinkDecryptId(String allLink) {
        Map<String, String> CHARACTER_MAP_ID = getCharacterMapId();
        int length = Integer.parseInt(CHARACTER_MAP_ID.get(allLink.substring(0, 1)));
        String link = allLink.substring(length + 1);
        return link;
    }
}