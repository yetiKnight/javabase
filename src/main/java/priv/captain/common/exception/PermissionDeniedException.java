package priv.captain.common.exception;

/**
 * 权限不足异常
 */
public class PermissionDeniedException extends RuntimeException {

    public PermissionDeniedException(String message) {
        super(message);
    }
}
