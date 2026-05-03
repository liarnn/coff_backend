package coffee_backend_4j.utils;

import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * @author LiARnn
 * @version 1.0
 */
@Component
public class MqProducerUtil {
    @Resource
    private RabbitTemplate rabbitTemplate;
    public void sendMessage(Object message){

        rabbitTemplate.convertAndSend("test.exchange","test.routing.key", message);

    }
}
