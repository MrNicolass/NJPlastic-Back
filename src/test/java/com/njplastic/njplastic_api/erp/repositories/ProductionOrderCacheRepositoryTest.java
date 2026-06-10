package com.njplastic.njplastic_api.erp.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.njplastic.njplastic_api.erp.entities.ProductionOrderCache;

/**
 * Contract test for the {@link ProductionOrderCacheRepository} interface. The
 * project convention is pure-Mockito unit tests without spinning up a real
 * persistence context, so this verifies that the interface exposes the JPA
 * Repository plus Specification executor needed by {@code ProductionOrderService}.
 */
class ProductionOrderCacheRepositoryTest {

  @Test
  void extendsJpaRepositoryWithProductionOrderCacheAndUuid() {
    ParameterizedType jpaRepo = (ParameterizedType) java.util.Arrays.stream(
        ProductionOrderCacheRepository.class.getGenericInterfaces())
        .filter(t -> t instanceof ParameterizedType pt
            && pt.getRawType().equals(JpaRepository.class))
        .findFirst().orElseThrow();
    assertThat(jpaRepo.getActualTypeArguments())
        .containsExactly(ProductionOrderCache.class, UUID.class);
  }

  @Test
  void extendsJpaSpecificationExecutorForProductionOrderCache() {
    ParameterizedType specExec = (ParameterizedType) java.util.Arrays.stream(
        ProductionOrderCacheRepository.class.getGenericInterfaces())
        .filter(t -> t instanceof ParameterizedType pt
            && pt.getRawType().equals(JpaSpecificationExecutor.class))
        .findFirst().orElseThrow();
    assertThat(specExec.getActualTypeArguments())
        .containsExactly(ProductionOrderCache.class);
  }

  @Test
  void exposesFindByErpOrderId() throws Exception {
    Method method = ProductionOrderCacheRepository.class.getMethod("findByErpOrderId", String.class);
    assertThat(method.getReturnType()).isEqualTo(Optional.class);
    assertThat(((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments())
        .containsExactly(ProductionOrderCache.class);
  }
}
