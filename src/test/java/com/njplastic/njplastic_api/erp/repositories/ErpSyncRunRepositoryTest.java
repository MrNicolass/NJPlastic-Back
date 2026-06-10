package com.njplastic.njplastic_api.erp.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.njplastic.njplastic_api.erp.entities.ErpSyncRun;
import com.njplastic.njplastic_api.erp.enums.ErpSyncStatus;

/**
 * Contract test for the {@link ErpSyncRunRepository} interface. The project
 * convention is pure-Mockito unit tests without spinning up a real persistence
 * context, so this verifies the Spring Data JPA derived-query signatures and
 * the custom JPQL query are present and bound to the right entity.
 */
class ErpSyncRunRepositoryTest {

  @Test
  void extendsJpaRepositoryWithErpSyncRunAndUuid() {
    ParameterizedType jpaRepo = (ParameterizedType) java.util.Arrays.stream(
        ErpSyncRunRepository.class.getGenericInterfaces())
        .filter(t -> t instanceof ParameterizedType pt
            && pt.getRawType().equals(JpaRepository.class))
        .findFirst().orElseThrow();
    assertThat(jpaRepo.getActualTypeArguments())
        .containsExactly(ErpSyncRun.class, UUID.class);
  }

  @Test
  void exposesFindTopByOrderByStartedAtDesc() throws Exception {
    Method method = ErpSyncRunRepository.class.getMethod("findTopByOrderByStartedAtDesc");
    assertThat(method.getReturnType()).isEqualTo(Optional.class);
    assertThat(((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments())
        .containsExactly(ErpSyncRun.class);
  }

  @Test
  void exposesFindAllByOrderByStartedAtDescPageable() throws Exception {
    Method method = ErpSyncRunRepository.class
        .getMethod("findAllByOrderByStartedAtDesc", Pageable.class);
    assertThat(method.getReturnType()).isEqualTo(List.class);
    assertThat(((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments())
        .containsExactly(ErpSyncRun.class);
  }

  @Test
  void exposesCountByStartedAtAfter() throws Exception {
    Method method = ErpSyncRunRepository.class
        .getMethod("countByStartedAtAfter", OffsetDateTime.class);
    assertThat(method.getReturnType()).isEqualTo(long.class);
  }

  @Test
  void exposesCountByStartedAtAfterAndStatus() throws Exception {
    Method method = ErpSyncRunRepository.class
        .getMethod("countByStartedAtAfterAndStatus", OffsetDateTime.class, ErpSyncStatus.class);
    assertThat(method.getReturnType()).isEqualTo(long.class);
  }

  @Test
  void averageDurationMsAfter_isAnnotatedWithQuery() throws Exception {
    Method method = ErpSyncRunRepository.class
        .getMethod("averageDurationMsAfter", OffsetDateTime.class);
    Query annotation = method.getAnnotation(Query.class);
    assertThat(annotation).isNotNull();
    assertThat(annotation.value())
        .contains("AVG(r.durationMs)")
        .contains("FROM ErpSyncRun r")
        .contains(":threshold");
    assertThat(method.getReturnType()).isEqualTo(Double.class);
  }
}
