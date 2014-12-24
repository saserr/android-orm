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

import android.orm.sql.Value;
import android.orm.util.Lens;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import java.util.Arrays;

public final class Instances {

    @NonNull
    public static <M, V> Instance.Getter<V> getter(@NonNull final M model,
                                                   @NonNull final Lens.Read<M, V> lens) {
        return new LensGetter<>(model, lens);
    }

    @NonNull
    public static <M, V> Instance.Setter<V> setter(@NonNull final M model,
                                                   @NonNull final Lens.Write<M, V> lens) {
        return new LensSetter<>(model, lens);
    }

    @NonNull
    public static <M, V> Instance.Access<V> access(@NonNull final M model,
                                                   @NonNull final Lens.ReadWrite<M, V> lens) {
        return access(getter(model, lens), setter(model, lens));
    }

    @NonNull
    public static <V> Instance.Access<V> access(@NonNull final Instance.Getter<V> getter,
                                                @NonNull final Instance.Setter<V> setter) {
        return new AccessCombination<>(getter, setter);
    }

    @NonNull
    public static Instance.Writable instance(@NonNull final Value value) {
        return new Instance.Writable.Base() {

            @NonNls
            @NonNull
            @Override
            public String getName() {
                return value.getName();
            }

            @NonNull
            @Override
            public Plan.Write prepareWrite() {
                return Plans.write(value);
            }
        };
    }

    @NonNull
    public static <V> Instance.Readable instance(@NonNull final Binding.Write<V> binding,
                                                 @NonNull final Value.Read<V> value) {
        return new Instance.Readable.Base() {

            @NonNls
            @NonNull
            @Override
            public String getName() {
                return value.getName();
            }

            @NonNull
            @Override
            public Reading.Item.Action prepareRead() {
                return Reading.Item.action(binding, Reading.Item.Create.from(value));
            }
        };
    }

    @NonNull
    public static <V> Instance.Writable instance(@NonNull final Binding.Read<V> binding,
                                                 @NonNull final Value.Write<V> value) {
        return new Instance.Writable.Base() {

            @NonNls
            @NonNull
            @Override
            public String getName() {
                return value.getName();
            }

            @NonNull
            @Override
            public Plan.Write prepareWrite() {
                return Plans.write(binding.get(), value);
            }
        };
    }

    @NonNull
    public static <V> Instance.Readable instance(@NonNull final Binding.Write<V> binding,
                                                 @NonNull final Mapper.Read<V> mapper) {
        return new Instance.Readable.Base() {

            @NonNls
            @NonNull
            @Override
            public String getName() {
                return mapper.getName();
            }

            @NonNull
            @Override
            public Reading.Item.Action prepareRead() {
                return Reading.Item.action(binding, mapper.prepareRead());
            }
        };
    }

    @NonNull
    public static <V> Instance.Writable instance(@NonNull final Binding.Read<V> binding,
                                                 @NonNull final Mapper.Write<V> mapper) {
        return new Instance.Writable.Base() {

            @NonNls
            @NonNull
            @Override
            public String getName() {
                return mapper.getName();
            }

            @NonNull
            @Override
            public Plan.Write prepareWrite() {
                return mapper.prepareWrite(binding.get());
            }
        };
    }

    @NonNull
    public static <V> Instance.ReadWrite instance(@NonNull final Binding.ReadWrite<V> binding,
                                                  @NonNull final Mapper.ReadWrite<V> mapper) {
        return new Instance.ReadWrite.Base() {

            @NonNls
            @NonNull
            @Override
            public String getName() {
                return mapper.getName();
            }

            @NonNull
            @Override
            public Reading.Item.Action prepareRead() {
                final V value = binding.get().getOrElse(null);
                return (value == null) ?
                        Reading.Item.action(binding, mapper.prepareRead()) :
                        Reading.Item.action(binding, mapper.prepareRead(value));
            }

            @NonNull
            @Override
            public Plan.Write prepareWrite() {
                return mapper.prepareWrite(binding.get());
            }
        };
    }

    @NonNull
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
        return new InstanceCombination(read, write);
    }

    private static class LensGetter<M, V> implements Instance.Getter<V> {

        @NonNull
        private final M mModel;
        @NonNull
        private final Lens.Read<M, V> mLens;

        private LensGetter(@NonNull final M model, @NonNull final Lens.Read<M, V> lens) {
            super();

            mLens = lens;
            mModel = model;
        }

        @Nullable
        @Override
        public final V get() {
            return mLens.get(mModel);
        }
    }

    private static class LensSetter<M, V> implements Instance.Setter<V> {

        @NonNull
        private final M mModel;
        @NonNull
        private final Lens.Write<M, V> mLens;

        private LensSetter(@NonNull final M model,
                           @NonNull final Lens.Write<M, V> lens) {
            super();

            mModel = model;
            mLens = lens;
        }

        @Override
        public final void set(@Nullable final V value) {
            mLens.set(mModel, value);
        }
    }

    private static class AccessCombination<V> implements Instance.Access<V> {

        @NonNull
        private final Instance.Getter<V> mGetter;
        @NonNull
        private final Instance.Setter<V> mSetter;

        private AccessCombination(@NonNull final Instance.Getter<V> getter,
                                  @NonNull final Instance.Setter<V> setter) {
            super();

            mGetter = getter;
            mSetter = setter;
        }

        @Nullable
        @Override
        public final V get() {
            return mGetter.get();
        }

        @Override
        public final void set(@Nullable final V value) {
            mSetter.set(value);
        }
    }

    private static class ReadableComposition extends Instance.Readable.Base implements Observer.Read {

        @NonNls
        @NonNull
        private final String mName;
        @NonNull
        private final Instance.Readable mFirst;
        @NonNull
        private final Instance.Readable mSecond;
        @NonNull
        private final Observer.Read mObserver;

        private ReadableComposition(@NonNull final Instance.Readable first,
                                    @NonNull final Instance.Readable second) {
            super();

            mName = '(' + first.getName() + ", " + second.getName() + ')';
            mFirst = first;
            mSecond = second;

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

        @NonNls
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

        @NonNls
        @NonNull
        private final String mName;
        @NonNull
        private final Instance.Writable mFirst;
        @NonNull
        private final Instance.Writable mSecond;
        @NonNull
        private final Observer.Write mObserver;

        private WritableComposition(@NonNull final Instance.Writable first,
                                    @NonNull final Instance.Writable second) {
            super();

            mName = '(' + first.getName() + ", " + second.getName() + ')';
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

        @NonNls
        @NonNull
        @Override
        public final String getName() {
            return mName;
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

    private static class InstanceCombination extends Instance.ReadWrite.Base implements Observer.ReadWrite {

        @NonNull
        private final Instance.Readable mRead;
        @NonNull
        private final Instance.Writable mWrite;
        @NonNull
        private final Observer.ReadWrite mObserver;

        private InstanceCombination(@NonNull final Instance.Readable read,
                                    @NonNull final Instance.Writable write) {
            super();

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
