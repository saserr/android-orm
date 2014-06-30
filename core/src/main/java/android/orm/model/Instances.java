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

    @NonNull
    public static <V> Instance.Access<V> access(@NonNull final Instance.Getter<V> getter,
                                                @NonNull final Instance.Setter<V> setter) {
        return new Instance.Access<V>() {

            @NonNull
            @Override
            public Maybe<V> get() {
                return getter.get();
            }

            @Override
            public void set(@NonNull final Maybe<V> value) {
                setter.set(value);
            }
        };
    }

    @NonNull
    public static <M, V> Instance.Access<V> access(@NonNull final M model,
                                                   @NonNull final Lens.ReadWrite<M, Maybe<V>> lens) {
        return access(getter(model, lens), setter(model, lens));
    }

    @NonNull
    public static Instance.ReadWrite combine(@NonNull final Instance.Readable read,
                                             @NonNull final Instance.Writable write) {
        return new Combine(read, write);
    }

    private static class Combine extends Observer.ReadWrite.Composition implements Instance.ReadWrite {

        @NonNull
        private final Instance.Readable mRead;
        @NonNull
        private final Instance.Writable mWrite;

        private Combine(@NonNull final Instance.Readable read,
                        @NonNull final Instance.Writable write) {
            super(
                    (read instanceof Observer.Read) ? (Observer.Read) read : Observer.Read.DUMMY,
                    (write instanceof Observer.Write) ? (Observer.Write) write : Observer.Write.DUMMY
            );

            mRead = read;
            mWrite = write;
        }

        @NonNull
        @Override
        public final String getName() {
            return mRead.getName();
        }

        @NonNull
        @Override
        public final Reading.Item.Action prepareRead() {
            return mRead.prepareRead();
        }

        @NonNull
        @Override
        public final Plan.Write prepareWrite() {
            return mWrite.prepareWrite();
        }
    }

    private Instances() {
        super();
    }
}
