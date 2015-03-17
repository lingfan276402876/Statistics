package uc.game.statistics.exception;


/**
 * 通用的业务逻辑异常基础类
 * 
 * @author wangjun
 * 
 */
public class GeneralLogicException extends RuntimeException {

	private static final long serialVersionUID = -7221937047819077839L;
	
	private int errorCode = 99;

	public GeneralLogicException() {
		super();
	}

	public GeneralLogicException(String message, Throwable cause) {
		super(message, cause);
	}

	public GeneralLogicException(String message) {
		super(message);
	}
	
	public GeneralLogicException(int errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public GeneralLogicException(Throwable cause) {
		super(cause);
	}
	
	public int getErrorCode() {
		return this.errorCode;
	}
}
