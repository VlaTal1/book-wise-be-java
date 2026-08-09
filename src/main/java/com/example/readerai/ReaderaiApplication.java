package com.example.readerai;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class ReaderaiApplication {

    public static void main(String[] args) {
        setEnv();
        SpringApplication.run(ReaderaiApplication.class, args);
    }

    private static void setEnv() {
        Dotenv dotenv = Dotenv.load();
        System.setProperty("SUPABASE_URL", dotenv.get("SUPABASE_URL"));
        System.setProperty("INTERNAL_API_KEY", dotenv.get("INTERNAL_API_KEY"));
        System.setProperty("MINIO_ENDPOINT", dotenv.get("MINIO_ENDPOINT"));
        System.setProperty("MINIO_ACCESS_KEY", dotenv.get("MINIO_ACCESS_KEY"));
        System.setProperty("MINIO_SECRET_KEY", dotenv.get("MINIO_SECRET_KEY"));
        System.setProperty("MINIO_BUCKET_NAME", dotenv.get("MINIO_BUCKET_NAME"));
        System.setProperty("POSTGRES_URL", dotenv.get("POSTGRES_URL"));

        System.setProperty("RABBITMQ_HOST", dotenv.get("RABBITMQ_HOST", "localhost"));
        System.setProperty("RABBITMQ_PORT", dotenv.get("RABBITMQ_PORT", "5672"));
        System.setProperty("RABBITMQ_USERNAME", dotenv.get("RABBITMQ_USERNAME", "guest"));
        System.setProperty("RABBITMQ_PASSWORD", dotenv.get("RABBITMQ_PASSWORD", "guest"));
        System.setProperty("RABBITMQ_VIRTUAL_HOST", dotenv.get("RABBITMQ_VIRTUAL_HOST", "/"));
    }
}