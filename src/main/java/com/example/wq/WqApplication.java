package com.example.wq;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class 	WqApplication {

	public static void main(String[] args) {
		// 加载.env文件
		Dotenv dotenv = Dotenv.configure()
				.ignoreIfMissing()
				.load();

		// 将环境变量设置到系统属性中
		dotenv.entries().forEach(entry ->
			System.setProperty(entry.getKey(), entry.getValue())
		);

		SpringApplication.run(WqApplication.class, args);
	}

}
