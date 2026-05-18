package com.kiran.urlshortener;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Integration test - requires PostgreSQL and Redis to be running")
class UrlshortenerApplicationTests {

	@Test
	void contextLoads() {
	}

}
