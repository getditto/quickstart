package com.ditto.example.spring.quickstart.service;

import jakarta.annotation.Nonnull;

public record Task(
        @Nonnull String id,
        @Nonnull String title,
        boolean done,
        boolean deleted
) { }
