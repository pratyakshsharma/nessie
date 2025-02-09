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
package org.projectnessie.client.http.v2api;

import org.projectnessie.client.builder.BaseAssignReferenceBuilder;
import org.projectnessie.client.http.HttpClient;
import org.projectnessie.error.NessieConflictException;
import org.projectnessie.error.NessieNotFoundException;
import org.projectnessie.model.Reference;
import org.projectnessie.model.SingleReferenceResponse;

abstract class BaseHttpAssignReference<T extends Reference, B>
    extends BaseAssignReferenceBuilder<B> {

  private final HttpClient client;

  BaseHttpAssignReference(HttpClient client) {
    this.client = client;
  }

  public void assign() throws NessieNotFoundException, NessieConflictException {
    assignAndGet();
  }

  @SuppressWarnings("unchecked")
  public T assignAndGet() throws NessieNotFoundException, NessieConflictException {
    return (T)
        client
            .newRequest()
            .path("trees/{ref}")
            .resolveTemplate("ref", Reference.toPathString(refName, expectedHash))
            .queryParam("type", type != null ? type.name() : null)
            .unwrap(NessieNotFoundException.class, NessieConflictException.class)
            .put(assignTo)
            .readEntity(SingleReferenceResponse.class)
            .getReference();
  }
}
