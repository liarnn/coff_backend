package coffee_backend_4j.exception;

/**
 * Service 层业务异常：用于中断流程并返回可读错误信息。
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}

