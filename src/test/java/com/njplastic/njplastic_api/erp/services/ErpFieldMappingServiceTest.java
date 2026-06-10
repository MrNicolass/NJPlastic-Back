package com.njplastic.njplastic_api.erp.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.njplastic.njplastic_api.erp.dtos.ErpFieldMappingUpdateRequestDTO;
import com.njplastic.njplastic_api.erp.dtos.ErpFieldMappingUpdateRequestDTO.MappingEntry;
import com.njplastic.njplastic_api.erp.entities.ErpFieldMapping;
import com.njplastic.njplastic_api.erp.repositories.ErpFieldMappingRepository;

@ExtendWith(MockitoExtension.class)
class ErpFieldMappingServiceTest {

  @Mock
  private ErpFieldMappingRepository repository;

  @InjectMocks
  private ErpFieldMappingService service;

  @Test
  void getMapping_delegatesToRepositorySortedByNjField() {
    ErpFieldMapping row = ErpFieldMapping.builder()
        .entityType("PRODUCTION_ORDER").njField("erpOrderId").erpField("id_op")
        .dataType("STRING").required(true).build();
    when(repository.findAllByEntityTypeOrderByNjFieldAsc("PRODUCTION_ORDER"))
        .thenReturn(List.of(row));

    List<ErpFieldMapping> result = service.getMapping("PRODUCTION_ORDER");

    assertThat(result).containsExactly(row);
  }

  @Test
  void replaceMapping_deletesPriorRowsThenSavesNewSetStampingTheAuthor() {
    UUID updatedBy = UUID.fromString("3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c");
    ErpFieldMappingUpdateRequestDTO request = ErpFieldMappingUpdateRequestDTO.builder()
        .entityType("PRODUCTION_ORDER")
        .mappings(List.of(
            MappingEntry.builder().njField("erpOrderId").erpField("id_op").dataType("STRING").required(true).build(),
            MappingEntry.builder().njField("targetQuantity").erpField("quantidade_alvo").dataType("INTEGER").required(false).build()))
        .build();
    when(repository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

    List<ErpFieldMapping> result = service.replaceMapping(request, updatedBy);

    verify(repository).deleteByEntityType("PRODUCTION_ORDER");
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<ErpFieldMapping>> captor = ArgumentCaptor.forClass(List.class);
    verify(repository).saveAll(captor.capture());
    List<ErpFieldMapping> saved = captor.getValue();
    assertThat(saved).hasSize(2);
    assertThat(saved).allSatisfy(row -> {
      assertThat(row.getEntityType()).isEqualTo("PRODUCTION_ORDER");
      assertThat(row.getUpdatedBy()).isEqualTo(updatedBy);
      assertThat(row.getUpdatedAt()).isNotNull();
    });
    assertThat(saved.get(0).getNjField()).isEqualTo("erpOrderId");
    assertThat(saved.get(0).getErpField()).isEqualTo("id_op");
    assertThat(saved.get(1).getNjField()).isEqualTo("targetQuantity");
    assertThat(saved.get(1).isRequired()).isFalse();
    assertThat(result).hasSize(2);
  }

  @Test
  void replaceMapping_withEmptyListDeletesAndPersistsEmptySet() {
    ErpFieldMappingUpdateRequestDTO request = ErpFieldMappingUpdateRequestDTO.builder()
        .entityType("PRODUCTION_ORDER")
        .mappings(List.of())
        .build();
    when(repository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

    List<ErpFieldMapping> result = service.replaceMapping(request, UUID.randomUUID());

    verify(repository).deleteByEntityType(eq("PRODUCTION_ORDER"));
    assertThat(result).isEmpty();
  }
}
