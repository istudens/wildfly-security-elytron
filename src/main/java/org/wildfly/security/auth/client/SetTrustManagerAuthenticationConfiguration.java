/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.security.auth.client;

import static org.wildfly.common.math.HashMath.multiHashUnordered;

import javax.net.ssl.X509TrustManager;

import org.wildfly.security.SecurityFactory;
import org.wildfly.security.auth.client.AuthenticationConfiguration.HandlesCallbacks;

/**
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
class SetTrustManagerAuthenticationConfiguration extends AuthenticationConfiguration implements HandlesCallbacks {

    private final SecurityFactory<X509TrustManager> trustManagerFactory;

    SetTrustManagerAuthenticationConfiguration(final AuthenticationConfiguration parent, final SecurityFactory<X509TrustManager> trustManagerFactory) {
        super(parent.without(SetCallbackHandlerAuthenticationConfiguration.class));
        this.trustManagerFactory = trustManagerFactory;
    }

    AuthenticationConfiguration reparent(final AuthenticationConfiguration newParent) {
        return new SetTrustManagerAuthenticationConfiguration(newParent, trustManagerFactory);
    }

    SecurityFactory<X509TrustManager> getX509TrustManagerFactory() {
        return trustManagerFactory;
    }

    boolean halfEqual(final AuthenticationConfiguration other) {
        return trustManagerFactory.equals(other.getX509TrustManagerFactory()) && parentHalfEqual(other);
    }

    int calcHashCode() {
        return multiHashUnordered(parentHashCode(), 8527, trustManagerFactory.hashCode());
    }

    @Override
    StringBuilder asString(StringBuilder sb) {
        return parentAsString(sb).append("TrustManager,");
    }

}
