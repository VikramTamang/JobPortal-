package com.jobportal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

/**
 * Without this, returning Page<T> directly from a controller serializes
 * Spring's internal PageImpl, which logs a warning every time and isn't
 * meant for public API contracts. VIA_DTO wraps it in PagedModel instead,
 * giving a stable { content, page: { size, number, totalElements,... } }
 * shape — used here and extensively in Phase 6's job search.
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class WebConfig {
}