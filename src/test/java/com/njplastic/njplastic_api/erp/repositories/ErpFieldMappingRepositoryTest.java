package com.njplastic.njplastic_api.erp.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import com.njplastic.njplastic_api.erp.entities.ErpFieldMapping;

/**
 * Contract test for the {@link ErpFieldMappingRepository} interface. The
 * project convention is pure-Mockito unit tests without spinning up a real
 * persistence context, so this verifies the Spring Data JPA derived-query
 * signatures exposed by the interface are stable and bound to the right entity.
 */
class ErpFieldMappingRepositoryTest {

  @Test
  void extendsJpaRepositoryWithErpFieldMappingAndUuid() {
    ParameterizedType jpaRepo = (ParameterizedType) java.util.Arrays.stream(
        ErpFieldMappingRepository.class.getGenericInterfaces())
        .filter(t -> t instanceof ParameterizedType pt
            && pt.getRawType().equals(JpaRepository.class))
        .findFirst().orElseThrow();
    assertThat(jpaRepo.getActualTypeArguments())
        .containsExactly(ErpFieldMapping.class, UUID.class);
  }

  @Test
  void exposesFindAllByEntityTypeOrderByNjFieldAsc() throws Exception {
    Method method = ErpFieldMappingRepository.class
        .getMethod("findAllByEntityTypeOrderByNjFieldAsc", String.class);
    assertThat(method.getReturnType()).isEqualTo(List.class);
    assertThat(((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments())
        .containsExactly(ErpFieldMapping.class);
  }

  @Test
  void exposesDeleteByEntityType() throws Exception {
    Method method = ErpFieldMappingRepository.class
        .getMethod("deleteByEntityType", String.class);
    assertThat(method.getReturnType()).isEqualTo(void.class);
  }
}
