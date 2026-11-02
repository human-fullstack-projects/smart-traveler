package com.smarttraveler.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityMappingTest {
    @Test
    void allExerdTablesHaveEntitiesAndPrimaryKeys() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        Set<BeanDefinition> entities =
                scanner.findCandidateComponents("com.smarttraveler.domain.entity");
        assertEquals(63, entities.size());
        for (BeanDefinition definition : entities) {
            Class<?> entityClass = Class.forName(definition.getBeanClassName());
            boolean hasId = Arrays.stream(entityClass.getDeclaredFields())
                    .map(Field::getAnnotations)
                    .flatMap(Arrays::stream)
                    .anyMatch(annotation -> annotation.annotationType() == Id.class);
            assertTrue(hasId, entityClass.getSimpleName() + " must declare an @Id field");
        }
    }
}
