package com.njplastic.njplastic_api.reports.services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.njplastic.njplastic_api.reports.entities.ReportHistory;
import com.njplastic.njplastic_api.reports.exceptions.ReportArtifactNotFoundException;
import com.njplastic.njplastic_api.reports.repositories.ReportHistoryRepository;

import lombok.RequiredArgsConstructor;

/**
 * Resolves a {@code report_history} row to the on-disk artifact and wraps
 * it as a Spring {@link Resource} that the controller streams back. Throws
 * {@link ReportArtifactNotFoundException} when either the row is missing or
 * the file at {@code path} cannot be found - the retention reaper may have
 * removed it between the library listing and the download click.
 */
@Service
@RequiredArgsConstructor
public class ReportDownloadService {

  private final ReportHistoryRepository historyRepository;

  /**
   * Build a downloadable resource for a stored report.
   *
   * @param historyId id of the row in {@code report_history}
   * @return the row and the wrapped artifact
   */
  public ResolvedArtifact resolve(UUID historyId) {
    ReportHistory history = historyRepository.findById(historyId)
        .orElseThrow(() -> new ReportArtifactNotFoundException("Report not found: " + historyId));
    Path path = Path.of(history.getPath());
    if (!Files.exists(path) || !Files.isReadable(path)) {
      throw new ReportArtifactNotFoundException("Report artifact missing on disk: " + historyId);
    }
    Resource resource = new FileSystemResource(path);
    return new ResolvedArtifact(history, resource);
  }

  /**
   * Pair of the metadata row and the on-disk resource ready for streaming.
   *
   * @param history  source row
   * @param resource Spring resource pointing at the file
   */
  public record ResolvedArtifact(ReportHistory history, Resource resource) {
  }
}
