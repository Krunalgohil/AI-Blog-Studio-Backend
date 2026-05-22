package com.aiblogstudio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point for the Medium AI Autonomous Publisher.
 * <p>
 * This application autonomously researches topics, writes human-like blogs,
 * optimizes for SEO, and publishes to Medium — all with a single button click.
 */
@SpringBootApplication
@EnableAspectJAutoProxy
@EnableAsync
public class AIBlogStudio {
    public static void main(String[] args) {
        SpringApplication.run(AIBlogStudio.class, args);
    }
}
