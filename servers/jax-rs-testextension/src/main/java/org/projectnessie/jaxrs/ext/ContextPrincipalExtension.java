/*
 * Copyright (C) 2023 Dremio
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
package org.projectnessie.jaxrs.ext;

import java.security.Principal;
import java.util.function.Supplier;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.util.TypeLiteral;
import javax.ws.rs.core.SecurityContext;

public class ContextPrincipalExtension implements Extension {
  private final Supplier<Principal> principal;

  public ContextPrincipalExtension(Supplier<SecurityContext> securityContext) {
    this.principal =
        () -> {
          SecurityContext context = securityContext.get();
          return context == null ? null : context.getUserPrincipal();
        };
  }

  @SuppressWarnings("unused")
  public void afterBeanDiscovery(@Observes AfterBeanDiscovery abd, BeanManager bm) {
    abd.addBean()
        .addType(new TypeLiteral<Supplier<Principal>>() {})
        .addQualifier(Default.Literal.INSTANCE)
        .scope(RequestScoped.class)
        .produceWith(i -> principal);
  }
}
