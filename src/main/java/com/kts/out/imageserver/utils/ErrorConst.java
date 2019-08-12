package com.kts.out.imageserver.utils;

/**
 * 클래스명 보면 알수 있음.
 */
public class ErrorConst {
    public static final String AUTH_FAILED_EXCEPTION =  "권한이 없습니다.";
    public static final String NO_RESOURCE_EXCEPTION = "리소스가 존재하지 않습니다."; //404
    public static final String FULL_REQUEST_EXCEPTION = "현재 응답을 받을 수 없습니다. 잠시 후 다시 시도해주세요."; //500
    public static final String NOT_SUPPORTED_FILE = "지원하지 않는 확장자입니다."; //400

    public static final String NO_RESULT = "분석 결과가 없습니다.";
    public static final String STORAGE_EXCEPTION = "디렉토리 초기화를 할 수 없습니다."; //404
    public static final String EMPTY_FILE_EXCEPTION = "빈 파일입니다.";
    public static final String NO_FILE_PATH_EXCEPTION = "경로를 확인하세요.";
    public static final String SAVE_FILE_EXCEPTION = "파일을 저장할 수 없습니다.";
}
