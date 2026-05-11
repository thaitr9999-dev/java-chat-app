package com.kimtruong.chat_app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "jwt.secret=test-secret-key-for-testing-only-min-32-chars!!",
    "jwt.expiration-ms=86400000",
    "file.upload-dir=uploads"
})
class ChatAppApplicationTests {

    @Test
    void contextLoads() {
    }

}