package com.kts.out.imageserver.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

@Slf4j
public class APIUtils {
    public static final String HTTP = "http://";
    public static final String HTTPS = "https://";
    public static final String FACE_EXTRACT_PATH = "/upload";
    public static final String FACE_GENERATE_PATH = "/v2/APIFaceComAdd";

    private static final String FACE_EXTRACT_FILE_KEY = "image";
    private static final String FACE_EXTRACT_VALUE_KEY = "userId";

    private static final String FACE_GENERATE_FILE_KEY = "data";
    private static final String FACE_GENERATE_VALUE_KEY = "params";

    public static final ArrayList<String> extractKeyList = new ArrayList<>(Arrays.asList(FACE_EXTRACT_FILE_KEY, FACE_EXTRACT_VALUE_KEY));
    public static final ArrayList<String> generateKeyList = new ArrayList<>(Arrays.asList(FACE_GENERATE_FILE_KEY, FACE_GENERATE_VALUE_KEY));

    public static final String FACE_GENERATE_ADDRESS_DEV = "localhost:9080";
    public static final String FACE_GENERATE_ADDRESS_LOCAL = "192.168.0.30:9080";


    /**
     *
     * @param ts
     * @param <T>
     * @return
     */
    @SafeVarargs
    public static <T> LinkedList<T> addElement(T... ts) {
        if (ts.length == 0)
            log.error("[API_UTILS] addElement() T...ts Empty");
        return new LinkedList<>(Arrays.asList(ts));
    }
}
