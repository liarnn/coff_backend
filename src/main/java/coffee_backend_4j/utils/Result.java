
package coffee_backend_4j.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> ok() {
        return new Result<>(200, "success", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> okWithPage(T data, Long total) {
        Map<String, Object> pageMap = new HashMap<>();
        pageMap.put("data", data);
        pageMap.put("total", total);
        return new Result<>(200, "success", (T) pageMap);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }

    public static <T> Result<T> unauthorized() {
        return new Result<>(401, "未登录或登陆过期...", null);
    }

    public String getMsg() {
        return message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageData<T> {
        private T data;
        private Integer page;
        private Integer size;
        private Long total;
    }
}
