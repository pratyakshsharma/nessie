/*
 * Copyright (C) 2022 Dremio
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.projectnessie.gc.contents;

import com.google.errorprone.annotations.MustBeClosed;
import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import org.immutables.value.Value;
import org.projectnessie.gc.contents.spi.PersistenceSpi;
import org.projectnessie.gc.files.DeleteResult;
import org.projectnessie.gc.files.DeleteSummary;
import org.projectnessie.gc.files.FileDeleter;
import org.projectnessie.gc.files.FileReference;
import org.projectnessie.gc.identify.IdentifyLiveContents;

/**
 * Represents a set of identified live contents from an {@link
 * IdentifyLiveContents#identifyLiveContents() identify-live-contents run}.
 */
@Value.Immutable
public abstract class LiveContentSet {

  public static ImmutableLiveContentSet.Builder builder() {
    return ImmutableLiveContentSet.builder();
  }

  public ImmutableLiveContentSet.Builder unbuild() {
    return ImmutableLiveContentSet.builder().from(this);
  }

  abstract PersistenceSpi persistenceSpi();

  public abstract UUID id();

  public abstract Instant created();

  @Nullable
  @jakarta.annotation.Nullable
  public abstract Instant identifyCompleted();

  @Nullable
  @jakarta.annotation.Nullable
  public abstract Instant expiryStarted();

  @Nullable
  @jakarta.annotation.Nullable
  public abstract Instant expiryCompleted();

  public abstract Status status();

  @Nullable
  @jakarta.annotation.Nullable
  public abstract String errorMessage();

  public void delete() {
    persistenceSpi().deleteLiveContentSet(id());
  }

  public long fetchDistinctContentIdCount() {
    return persistenceSpi().fetchDistinctContentIdCount(id());
  }

  @MustBeClosed
  public Stream<String> fetchContentIds() {
    return persistenceSpi().fetchContentIds(id());
  }

  @MustBeClosed
  public Stream<ContentReference> fetchContentReferences(
      @NotNull @jakarta.validation.constraints.NotNull String contentId) {
    return persistenceSpi().fetchContentReferences(id(), contentId);
  }

  public void associateBaseLocations(String contentId, Collection<URI> baseLocations) {
    // TODO detect duplicate base locations for different content-IDs
    persistenceSpi().associateBaseLocations(id(), contentId, baseLocations);
  }

  @MustBeClosed
  public Stream<URI> fetchAllBaseLocations() {
    return persistenceSpi().fetchAllBaseLocations(id());
  }

  @MustBeClosed
  public Stream<URI> fetchBaseLocations(
      @NotNull @jakarta.validation.constraints.NotNull String contentId) {
    return persistenceSpi().fetchBaseLocations(id(), contentId);
  }

  public LiveContentSet startExpireContents(
      @NotNull @jakarta.validation.constraints.NotNull Instant started) {
    return persistenceSpi().startExpireContents(id(), started);
  }

  public LiveContentSet finishedExpireContents(
      @NotNull @jakarta.validation.constraints.NotNull Instant finished,
      @Nullable @jakarta.annotation.Nullable Throwable failure) {
    return persistenceSpi().finishedExpireContents(id(), finished, failure);
  }

  /**
   * Records the given files to be later returned by {@link #fetchFileDeletions()}, ignores
   * duplicates.
   *
   * @return the number of actually added files
   */
  public long addFileDeletions(Stream<FileReference> files) {
    return persistenceSpi().addFileDeletions(id(), files);
  }

  /**
   * Returns the {@link #addFileDeletions(Stream)} recorded file deletions (aka deferred deletes)
   * grouped by base path.
   */
  @MustBeClosed
  public Stream<FileReference> fetchFileDeletions() {
    return persistenceSpi().fetchFileDeletions(id());
  }

  public FileDeleter fileDeleter() {
    return new FileDeleter() {
      @Override
      public DeleteResult delete(FileReference fileReference) {
        addFileDeletions(Stream.of(fileReference));
        return DeleteResult.SUCCESS;
      }

      @Override
      public DeleteSummary deleteMultiple(URI baseUri, Stream<FileReference> fileObjects) {
        long count = addFileDeletions(fileObjects);
        return DeleteSummary.of(count, 0);
      }
    };
  }

  public enum Status {
    IDENTIFY_IN_PROGRESS,
    IDENTIFY_SUCCESS,
    IDENTIFY_FAILED,
    EXPIRY_IN_PROGRESS,
    EXPIRY_SUCCESS,
    EXPIRY_FAILED
  }
}
