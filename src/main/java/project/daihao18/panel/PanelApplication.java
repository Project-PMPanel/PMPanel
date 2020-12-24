package project.daihao18.panel;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@MapperScan(basePackages = {"project.daihao18.panel.mapper"})
@EnableTransactionManagement
@SpringBootApplication// (exclude = {JacksonAutoConfiguration.class})
public class PanelApplication {

    public static void main(String[] args) {
        SpringApplication.run(PanelApplication.class, args);
    }
}
