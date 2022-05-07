package project.daihao18.panel;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestTemplate;

@MapperScan(basePackages = {"project.daihao18.panel.mapper"})
@EnableTransactionManagement
@SpringBootApplication// (exclude = {JacksonAutoConfiguration.class})
public class PanelApplication {

    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }

    public static void main(String[] args) {
        SpringApplication.run(PanelApplication.class, args);
    }
}
