package com.example.postgres_mcp;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class PostgresMcpApplication {

	public static void main(String[] args) {
		SpringApplication.run(PostgresMcpApplication.class, args);
	}

	@Bean
	ApplicationRunner testConnection(JdbcTemplate jdbcTemplate) {
		return args -> {
			try {
				String result = jdbcTemplate.queryForObject("SELECT current_database()", String.class);
				System.out.println("✅ DB connected: " + result);
			} catch (Exception e) {
				System.err.println("❌ DB connection failed: " + e.getMessage());
				e.printStackTrace();
			}
		};
	}
}