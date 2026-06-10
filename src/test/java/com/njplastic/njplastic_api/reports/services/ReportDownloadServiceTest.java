package com.njplastic.njplastic_api.reports.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.njplastic.njplastic_api.reports.entities.ReportHistory;
import com.njplastic.njplastic_api.reports.enums.ReportFormat;
import com.njplastic.njplastic_api.reports.enums.ReportType;
import com.njplastic.njplastic_api.reports.exceptions.ReportArtifactNotFoundException;
import com.njplastic.njplastic_api.reports.repositories.ReportHistoryRepository;
import com.njplastic.njplastic_api.reports.services.ReportDownloadService.ResolvedArtifact;

@ExtendWith(MockitoExtension.class)
class ReportDownloadServiceTest {

  @Mock
  private ReportHistoryRepository historyRepository;

  @InjectMocks
  private ReportDownloadService service;

  @Test
  void resolve_returnsHistoryAndResourceWhenFileExists(@TempDir Path tempDir) throws Exception {
    Path file = tempDir.resolve("report.csv");
    Files.writeString(file, "header\nrow1");
    UUID id = UUID.randomUUID();
    ReportHistory history = ReportHistory.builder()
        .id(id).type(ReportType.SHIFT).format(ReportFormat.CSV)
        .path(file.toString()).sizeBytes(Files.size(file)).build();
    when(historyRepository.findById(id)).thenReturn(Optional.of(history));

    ResolvedArtifact resolved = service.resolve(id);

    assertThat(resolved.history()).isEqualTo(history);
    assertThat(resolved.resource().exists()).isTrue();
    assertThat(resolved.resource().contentLength()).isEqualTo(Files.size(file));
  }

  @Test
  void resolve_throwsArtifactNotFoundWhenHistoryRowMissing() {
    UUID id = UUID.randomUUID();
    when(historyRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.resolve(id))
        .isInstanceOf(ReportArtifactNotFoundException.class)
        .hasMessageContaining(id.toString());
  }

  @Test
  void resolve_throwsArtifactNotFoundWhenFileMissingOnDisk(@TempDir Path tempDir) {
    UUID id = UUID.randomUUID();
    ReportHistory history = ReportHistory.builder()
        .id(id).type(ReportType.DAILY).format(ReportFormat.PDF)
        .path(tempDir.resolve("missing.pdf").toString()).sizeBytes(0L).build();
    when(historyRepository.findById(id)).thenReturn(Optional.of(history));

    assertThatThrownBy(() -> service.resolve(id))
        .isInstanceOf(ReportArtifactNotFoundException.class)
        .hasMessageContaining("missing on disk");
  }
}
