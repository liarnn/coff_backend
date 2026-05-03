package coffee_backend_4j;

import coffee_backend_4j.utils.MqProducerUtil;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;

/**
 * @author LiARnn
 * @version 1.0
 */
@SpringBootTest
public class test {
    @Resource
    private MqProducerUtil mqProducerUtil;
    @Test
    public void test1() {
        mqProducerUtil.sendMessage("我是一条测试消息");
        System.out.println("消息发送成功");
    }
    @Test
    public void test2() {
        String originalFilename ="dwwda.jpg";

//        拿到后缀
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String filename = UUID.randomUUID() + extension;

        System.out.println(filename);
    }
}
