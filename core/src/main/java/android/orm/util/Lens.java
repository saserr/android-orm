/*
 * Copyright 2013 the original author or authors
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

package android.orm.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public final class Lens {

    public interface Read<M, V> {
        @Nullable
        V get(@NonNull final M m);
    }

    public interface Write<M, V> {
        void set(@NonNull final M m, @Nullable final V v);
    }

    public interface ReadWrite<M, V> extends Read<M, V>, Write<M, V> {
    }

    private Lens() {
        super();
    }
}
