package coffee_backend_4j.utils;

import lombok.Data;
import org.springframework.stereotype.Component;

/**
 * @author LiARnn
 * @version 1.0
 */

public class UserContext {
    private  static final ThreadLocal<Integer> userId = new ThreadLocal<>();

    public static void setUserId(Integer id) {
        userId.set(id);
    }

    public static Integer getUserId() {
        return userId.get();
    }
    public  static void clear() {
        userId.remove();
    }

}
