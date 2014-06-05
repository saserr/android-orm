/*
 * Copyright 2014 the original author or authors
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

package android.orm.model;

import android.orm.util.Lens;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;

import static android.orm.util.Maybes.something;

public final class Instances {

    @NonNull
    public static <M, V> Instance.Getter<V> getter(@NonNull final M model,
                                                   @NonNull final Lens.Read<M, Maybe<V>> lens) {
        return new Instance.Getter<V>() {
            @NonNull
            @Override
            public Maybe<V> get() {
                Maybe<V> result = lens.get(model);
                if (result == null) {
                    result = something(null);
                }
                return result;
            }
        };
    }

    @NonNull
    public static <M, V> Instance.Setter<V> setter(@NonNull final M model,
                                                   @NonNull final Lens.Write<M, Maybe<V>> lens) {
        return new Instance.Setter<V>() {
            @Override
            public void set(@NonNull final Maybe<V> result) {
                lens.set(model, result);
            }
        };
    }

    private Instances() {
        super();
    }
}
