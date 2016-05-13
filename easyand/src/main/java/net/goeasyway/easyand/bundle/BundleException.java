package net.goeasyway.easyand.bundle;


/**
 * Copyright (C) 2015-present, goeasyway.net
 * Project: EasyPlug an open source Android Plugin Framework
 * Author: goeasyway@163.com
 * Site: www.goeasyway.net
 * Class Description:
 * Create Date: 2016/5/12
 */
public class BundleException extends Exception {

    public final static int	ERROR_CODE_NONE			= 0; // 成功

    public final static int ERROR_CODE_COPY_FILE_APK = 40;
    public final static int ERROR_CODE_COPY_FILE_SO = 41;
	
	int errorCode;

	public BundleException(int errorCode) {
		this.errorCode = errorCode;
	}
	
	public BundleException(int errorCode, String detailMessage) {
		super(detailMessage);
		this.errorCode = errorCode;
	}
	
	public int getErrorCode() {
		return errorCode;
	}
}
