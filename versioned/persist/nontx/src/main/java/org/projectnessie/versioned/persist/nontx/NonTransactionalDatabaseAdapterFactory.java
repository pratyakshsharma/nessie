/*
 * Copyright (C) 2020 Dremio
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
package org.projectnessie.versioned.persist.nontx;

import org.projectnessie.versioned.persist.adapter.DatabaseAdapterFactory;
import org.projectnessie.versioned.persist.adapter.DatabaseConnectionProvider;
import org.projectnessie.versioned.persist.adapter.events.AdapterEventConsumer;

public abstract class NonTransactionalDatabaseAdapterFactory<
        ADAPTER extends NonTransactionalDatabaseAdapter<NonTransactionalDatabaseAdapterConfig>,
        CONNECTOR extends DatabaseConnectionProvider<?>>
    implements DatabaseAdapterFactory<
        ADAPTER,
        NonTransactionalDatabaseAdapterConfig,
        AdjustableNonTransactionalDatabaseAdapterConfig,
        CONNECTOR> {

  protected abstract ADAPTER create(
      NonTransactionalDatabaseAdapterConfig config,
      CONNECTOR connector,
      AdapterEventConsumer eventConsumer);

  @Override
  public Builder<
          ADAPTER,
          NonTransactionalDatabaseAdapterConfig,
          AdjustableNonTransactionalDatabaseAdapterConfig,
          CONNECTOR>
      newBuilder() {
    return new NonTxBuilder();
  }

  private class NonTxBuilder
      extends Builder<
          ADAPTER,
          NonTransactionalDatabaseAdapterConfig,
          AdjustableNonTransactionalDatabaseAdapterConfig,
          CONNECTOR> {
    @Override
    protected NonTransactionalDatabaseAdapterConfig getDefaultConfig() {
      return ImmutableAdjustableNonTransactionalDatabaseAdapterConfig.builder().build();
    }

    @Override
    protected AdjustableNonTransactionalDatabaseAdapterConfig adjustableConfig(
        NonTransactionalDatabaseAdapterConfig config) {
      return ImmutableAdjustableNonTransactionalDatabaseAdapterConfig.builder()
          .from(config)
          .build();
    }

    @Override
    public ADAPTER build() {
      return create(getConfig(), getConnector(), getEventConsumer());
    }
  }
}
