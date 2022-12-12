package com.backend.searchengine.util;

import java.util.*;

public class IdGenerator {
    private static final int ID_LENGTH = 6;
    private final static Random random = new Random();
    private final static String  CHARS = "ABCDEFHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    public static String generateCrawlId() {
        StringBuilder res = new StringBuilder();

        for (int i = 0; i < ID_LENGTH; i++) {
            res.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }

        return res.toString();
    }
}
