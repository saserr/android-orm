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

import org.jetbrains.annotations.NonNls;

import java.util.Arrays;

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

    public static Instance.Readable compose(@NonNull final Instance.Readable first,
                                            @NonNull final Instance.Readable second) {
        return new ReadableComposition(first, second);
    }

    @NonNull
    public static Instance.Writable compose(@NonNull final Instance.Writable first,
                                            @NonNull final Instance.Writable second) {
        return new WritableComposition(first, second);
    }

    @NonNull
    public static Instance.ReadWrite combine(@NonNull final Instance.Readable read,
                                             @NonNull final Instance.Writable write) {
        return new Combine(read, write);
    }

    private static class ReadableComposition extends Instance.Readable.Base implements Observer.Read {

        @NonNull
        private final Instance.Readable mFirst;
        @NonNull
        private final Instance.Readable mSecond;
        @NonNls
        @NonNull
        private final String mName;
        @NonNull
        private final Observer.Read mObserver;

        private ReadableComposition(@NonNull final Instance.Readable first,
                                    @NonNull final Instance.Readable second) {
            super();

            mFirst = first;
            mSecond = second;
            mName = '(' + mFirst.getName() + ", " + mSecond.getName() + ')';

            if (first instanceof Observer.Read) {
                mObserver = (second instanceof Observer.Read) ?
                        Observer.read((Observer.Read) first, (Observer.Read) second) :
                        (Observer.Read) first;
            } else {
                mObserver = (second instanceof Observer.Read) ?
                        (Observer.Read) second :
                        DUMMY;
            }
        }

        @NonNull
        @Override
        public final String getName() {
            return mName;
        }

        @NonNull
        @Override
        public final Reading.Item.Action prepareRead() {
            return Reading.Item.compose(Arrays.asList(mFirst.prepareRead(), mSecond.prepareRead()));
        }

        @Override
        public final void beforeRead() {
            mObserver.beforeRead();
        }

        @Override
        public final void afterRead() {
            mObserver.afterRead();
        }
    }

    private static class WritableComposition extends Instance.Writable.Base implements Observer.Write {

        @NonNull
        private final Instance.Writable mFirst;
        @NonNull
        private final Instance.Writable mSecond;
        @NonNull
        private final Observer.Write mObserver;

        private WritableComposition(@NonNull final Instance.Writable first,
                                    @NonNull final Instance.Writable second) {
            super();

            mFirst = first;
            mSecond = second;

            if (first instanceof Observer.Write) {
                mObserver = (second instanceof Observer.Write) ?
                        Observer.write((Observer.Write) first, (Observer.Write) second) :
                        (Observer.Write) first;
            } else {
                mObserver = (second instanceof Observer.Write) ?
                        (Observer.Write) second :
                        DUMMY;
            }
        }

        @NonNull
        @Override
        public final Plan.Write prepareWrite() {
            return Plans.compose(Arrays.asList(mFirst.prepareWrite(), mSecond.prepareWrite()));
        }

        @Override
        public final void beforeCreate() {
            mObserver.beforeCreate();
        }

        @Override
        public final void afterCreate() {
            mObserver.afterCreate();
        }

        @Override
        public final void beforeUpdate() {
            mObserver.beforeUpdate();
        }

        @Override
        public final void afterUpdate() {
            mObserver.afterUpdate();
        }

        @Override
        public final void beforeSave() {
            mObserver.beforeSave();
        }

        @Override
        public final void afterSave() {
            mObserver.afterSave();
        }
    }

    private static class Combine extends Instance.ReadWrite.Base implements Observer.ReadWrite {

        @NonNull
        private final Instance.Readable mRead;
        @NonNull
        private final Instance.Writable mWrite;
        @NonNull
        private final Observer.ReadWrite mObserver;

        private Combine(@NonNull final Instance.Readable read,
                        @NonNull final Instance.Writable write) {

            mRead = read;
            mWrite = write;
            mObserver = Observer.combine(
                    (read instanceof Observer.Read) ? (Observer.Read) read : Observer.Read.DUMMY,
                    (write instanceof Observer.Write) ? (Observer.Write) write : Observer.Write.DUMMY
            );
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

        @Override
        public final void beforeRead() {
            mObserver.beforeRead();
        }

        @Override
        public final void afterRead() {
            mObserver.afterRead();
        }

        @Override
        public final void beforeCreate() {
            mObserver.beforeCreate();
        }

        @Override
        public final void afterCreate() {
            mObserver.afterCreate();
        }

        @Override
        public final void beforeUpdate() {
            mObserver.beforeUpdate();
        }

        @Override
        public final void afterUpdate() {
            mObserver.afterUpdate();
        }

        @Override
        public final void beforeSave() {
            mObserver.beforeSave();
        }

        @Override
        public final void afterSave() {
            mObserver.afterSave();
        }
    }

    private Instances() {
        super();
    }
}
