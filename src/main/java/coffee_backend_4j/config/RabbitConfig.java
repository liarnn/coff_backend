package coffee_backend_4j.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author LiARnn
 * @version 1.0
 */
@Configuration
public class RabbitConfig {

    // 1. 声明队列
    @Bean
    public Queue testQueue() {
        return new Queue("test.queue", true);
    }

    // 2. 声明交换机
    @Bean
    public DirectExchange testExchange() {
        return new DirectExchange("test.exchange", true, false);
    }

    // 3. 绑定队列和交换机
    @Bean
    public Binding testBinding() {
        return BindingBuilder
                .bind(testQueue())
                .to(testExchange())
                .with("test.routing.key");
    }
}
