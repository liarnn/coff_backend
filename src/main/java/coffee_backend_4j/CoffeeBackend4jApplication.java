package coffee_backend_4j;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
@MapperScan("coffee_backend_4j.mapper")
public class CoffeeBackend4jApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoffeeBackend4jApplication.class, args);
    }

}
