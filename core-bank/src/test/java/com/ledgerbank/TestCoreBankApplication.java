package com.ledgerbank;

import org.springframework.boot.SpringApplication;

/**
 * Local-dev entrypoint: boots the app against a throwaway Testcontainers Postgres.
 * Run with {@code ./gradlew bootTestRun} when you don't have docker-compose up.
 */
public class TestCoreBankApplication {

	public static void main(String[] args) {
		SpringApplication.from(CoreBankApplication::main)
				.with(TestcontainersConfiguration.class)
				.run(args);
	}
}
