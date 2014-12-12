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

package android.orm.model;

import android.orm.Model;
import android.orm.sql.Value;
import android.orm.sql.Writer;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Collection;

import static android.orm.model.Instances.instance;

public final class Instance {

    public interface Getter<V> {
        @Nullable
        V get();
    }

    public interface Setter<V> {
        void set(@Nullable final V v);
    }

    public interface Access<V> extends Getter<V>, Setter<V> {
    }

    public interface Readable {

        @NonNls
        @NonNull
        String getName();

        @NonNull
        Reading.Item.Action prepareRead();

        @NonNull
        Readable and(@NonNull final Readable other);

        @NonNull
        ReadWrite and(@NonNull final Value.Constant other);

        @NonNull
        ReadWrite and(@NonNull final Writable other);

        abstract class Base implements Readable {

            @NonNull
            @Override
            public final Readable and(@NonNull final Readable other) {
                return Instances.compose(this, other);
            }

            @NonNull
            @Override
            public final ReadWrite and(@NonNull final Value.Constant other) {
                return and(instance(other));
            }

            @NonNull
            @Override
            public final ReadWrite and(@NonNull final Writable other) {
                return Instances.combine(this, other);
            }
        }

        class Builder {

            @NonNls
            @NonNull
            private final String mName;

            private final Collection<Producer<Reading.Item.Action>> mProducers = new ArrayList<>();
            private final Collection<Observer.Read> mObservers = new ArrayList<>();

            public Builder(@NonNls @NonNull final String name) {
                super();

                mName = name;
            }

            @NonNls
            @NonNull
            public final String getName() {
                return mName;
            }

            @NonNull
            public final Builder with(@NonNull final Model model) {
                return with(Model.toInstance(model));
            }

            @NonNull
            public final Builder with(@NonNull final Readable instance) {
                return with(instance, produce(instance));
            }

            @NonNull
            public final <V> Builder with(@NonNull final Value.Read<V> value,
                                          @NonNull final Setter<V> setter) {
                return with(setter, produce(value, setter));
            }

            @NonNull
            public final <M> Builder with(@NonNull final Mapper.Read<M> mapper,
                                          @NonNull final Setter<M> setter) {
                return with(setter, produce(mapper, setter));
            }

            @NonNull
            public final <M> Builder with(@NonNull final Mapper.Read<M> mapper,
                                          @NonNull final Access<M> access) {
                return with(access, produce(mapper, access));
            }

            @NonNull
            public final Readable build() {
                return new CompositeReadable(mName, mProducers, mObservers);
            }

            @NonNull
            private Builder with(@Nullable final Object observer,
                                 @NonNull final Producer<Reading.Item.Action> producer) {
                if (observer instanceof Observer.Read) {
                    mObservers.add((Observer.Read) observer);
                }
                mProducers.add(producer);
                return this;
            }

            @NonNull
            private static Producer<Reading.Item.Action> produce(@NonNull final Readable instance) {
                return new Producer<Reading.Item.Action>() {
                    @NonNull
                    @Override
                    public Reading.Item.Action produce() {
                        return instance.prepareRead();
                    }
                };
            }

            @NonNull
            private static <V> Producer<Reading.Item.Action> produce(@NonNull final Value.Read<V> value,
                                                                     @NonNull final Setter<V> setter) {
                return new Producer<Reading.Item.Action>() {
                    @NonNull
                    @Override
                    public Reading.Item.Action produce() {
                        return Reading.Item.action(value, setter);
                    }
                };
            }

            @NonNull
            private static <M> Producer<Reading.Item.Action> produce(@NonNull final Mapper.Read<M> mapper,
                                                                     @NonNull final Setter<M> setter) {
                return new Producer<Reading.Item.Action>() {
                    @NonNull
                    @Override
                    public Reading.Item.Action produce() {
                        return Reading.Item.action(mapper, setter);
                    }
                };
            }

            @NonNull
            private static <M> Producer<Reading.Item.Action> produce(@NonNull final Mapper.Read<M> mapper,
                                                                     @NonNull final Access<M> access) {
                return new Producer<Reading.Item.Action>() {
                    @NonNull
                    @Override
                    public Reading.Item.Action produce() {
                        return Reading.Item.action(mapper, access);
                    }
                };
            }

            private static class CompositeReadable extends Base implements Observer.Read {

                @NonNls
                @NonNull
                private final String mName;
                @NonNull
                private final Collection<Producer<Reading.Item.Action>> mProducers;
                @NonNull
                private final Observer.Read mObserver;

                private CompositeReadable(@NonNls @NonNull final String name,
                                          @NonNull final Collection<Producer<Reading.Item.Action>> producers,
                                          @NonNull final Collection<Observer.Read> observers) {
                    super();

                    mName = name;
                    mProducers = new ArrayList<>(producers);
                    mObserver = Observer.read(new ArrayList<>(observers));
                }

                @NonNull
                @Override
                public final String getName() {
                    return mName;
                }

                @NonNull
                @Override
                public final Reading.Item.Action prepareRead() {
                    final Collection<Reading.Item.Action> actions = new ArrayList<>(mProducers.size());

                    for (final Producer<Reading.Item.Action> producer : mProducers) {
                        actions.add(producer.produce());
                    }

                    return Reading.Item.compose(actions);
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
        }
    }

    public interface Writable {

        @NonNls
        @NonNull
        String getName();

        @NonNull
        Plan.Write prepareWrite();

        @NonNull
        Writable and(@NonNull final Value.Constant other);

        @NonNull
        Writable and(@NonNull final Writable other);

        @NonNull
        ReadWrite and(@NonNull final Readable other);

        abstract class Base implements Writable {

            @NonNull
            @Override
            public final Writable and(@NonNull final Value.Constant other) {
                return and(instance(other));
            }

            @NonNull
            @Override
            public final Writable and(@NonNull final Writable other) {
                return Instances.compose(this, other);
            }

            @NonNull
            @Override
            public final ReadWrite and(@NonNull final Readable other) {
                return Instances.combine(other, this);
            }
        }

        class Builder {

            @NonNls
            @NonNull
            private final String mName;

            private final Collection<Producer<Plan.Write>> mProducers = new ArrayList<>();
            private final Collection<Observer.Write> mObservers = new ArrayList<>();

            public Builder(@NonNls @NonNull final String name) {
                super();

                mName = name;
            }

            @NonNls
            @NonNull
            public final String getName() {
                return mName;
            }

            @NonNull
            public final Builder with(@NonNull final Model model) {
                return with(Model.toInstance(model));
            }

            @NonNull
            public final Builder with(@NonNull final Writable instance) {
                return with(instance, produce(instance));
            }

            @NonNull
            public final Builder with(@NonNull final Writer writer) {
                return with(writer, produce(writer));
            }

            @NonNull
            public final <V> Builder with(@NonNull final Value.Write<V> value,
                                          @NonNull final Getter<V> getter) {
                return with(getter, produce(value, getter));
            }

            @NonNull
            public final <M> Builder with(@NonNull final Mapper.Write<M> mapper,
                                          @NonNull final Getter<M> getter) {
                return with(getter, produce(mapper, getter));
            }

            @NonNull
            public final Writable build() {
                return new CompositeWritable(mName, mProducers, mObservers);
            }

            @NonNull
            private Builder with(@Nullable final Object observer,
                                 @NonNull final Producer<Plan.Write> producer) {
                if (observer instanceof Observer.Write) {
                    mObservers.add((Observer.Write) observer);
                }
                mProducers.add(producer);
                return this;
            }

            @NonNull
            private static Producer<Plan.Write> produce(@NonNull final Writable instance) {
                return new Producer<Plan.Write>() {
                    @NonNull
                    @Override
                    public Plan.Write produce() {
                        return instance.prepareWrite();
                    }
                };
            }

            @NonNull
            private static Producer<Plan.Write> produce(@NonNull final Writer writer) {
                return new Producer<Plan.Write>() {
                    @NonNull
                    @Override
                    public Plan.Write produce() {
                        return Plans.write(writer);
                    }
                };
            }

            @NonNull
            private static <V> Producer<Plan.Write> produce(@NonNull final Value.Write<V> value,
                                                            @NonNull final Getter<V> getter) {
                return new Producer<Plan.Write>() {
                    @NonNull
                    @Override
                    public Plan.Write produce() {
                        return Plans.write(value, getter);
                    }
                };
            }

            @NonNull
            private static <M> Producer<Plan.Write> produce(@NonNull final Mapper.Write<M> mapper,
                                                            @NonNull final Getter<M> getter) {
                return new Producer<Plan.Write>() {
                    @NonNull
                    @Override
                    public Plan.Write produce() {
                        return Plans.write(mapper, getter);
                    }
                };
            }

            private static class CompositeWritable extends Base implements Observer.Write {

                @NonNls
                @NonNull
                private final String mName;

                @NonNull
                private final Collection<Producer<Plan.Write>> mProducers;
                @NonNull
                private final Observer.Write mObserver;

                private CompositeWritable(@NonNls @NonNull final String name,
                                          @NonNull final Collection<Producer<Plan.Write>> producers,
                                          @NonNull final Collection<Observer.Write> observers) {
                    super();

                    mName = name;
                    mProducers = new ArrayList<>(producers);
                    mObserver = Observer.write(new ArrayList<>(observers));
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
                    final Collection<Plan.Write> plans = new ArrayList<>(mProducers.size());

                    for (final Producer<Plan.Write> producer : mProducers) {
                        plans.add(producer.produce());
                    }

                    return Plans.compose(plans);
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
        }
    }

    public interface ReadWrite extends Readable, Writable {

        @NonNull
        @Override
        ReadWrite and(@NonNull final Value.Constant other);

        @NonNull
        ReadWrite and(@NonNull final ReadWrite other);

        abstract class Base implements ReadWrite {

            @NonNull
            @Override
            public final ReadWrite and(@NonNull final Readable other) {
                return Instances.combine(Instances.compose(this, other), this);
            }

            @NonNull
            @Override
            public final ReadWrite and(@NonNull final Value.Constant other) {
                return and(instance(other));
            }

            @NonNull
            @Override
            public final ReadWrite and(@NonNull final Writable other) {
                return Instances.combine(this, Instances.compose(this, other));
            }

            @NonNull
            @Override
            public final ReadWrite and(@NonNull final ReadWrite other) {
                return Instances.combine(
                        Instances.compose(this, (Readable) other),
                        Instances.compose(this, (Writable) other)
                );
            }
        }

        class Builder {

            @NonNls
            @NonNull
            private final String mName;
            @NonNull
            private final Readable.Builder mRead;
            @NonNull
            private final Writable.Builder mWrite;

            public Builder(@NonNls @NonNull final String name) {
                super();

                mName = name;
                mRead = new Readable.Builder(name);
                mWrite = new Writable.Builder(name);
            }

            @NonNls
            @NonNull
            public final String getName() {
                return mName;
            }

            @NonNull
            public final Builder with(@NonNull final Model model) {
                mRead.with(model);
                mWrite.with(model);
                return this;
            }

            @NonNull
            public final Builder with(@NonNull final Readable instance) {
                mRead.with(instance);
                return this;
            }

            @NonNull
            public final Builder with(@NonNull final Writable instance) {
                mWrite.with(instance);
                return this;
            }

            @NonNull
            public final Builder with(@NonNull final ReadWrite instance) {
                mRead.with(instance);
                mWrite.with(instance);
                return this;
            }

            @NonNull
            public final Builder with(@NonNull final Writer writer) {
                mWrite.with(writer);
                return this;
            }

            @NonNull
            public final <V> Builder with(@NonNull final Value.Read<V> value,
                                          @NonNull final Setter<V> setter) {
                mRead.with(value, setter);
                return this;
            }

            @NonNull
            public final <V> Builder with(@NonNull final Value.Write<V> value,
                                          @NonNull final Getter<V> getter) {
                mWrite.with(value, getter);
                return this;
            }

            @NonNull
            public final <V> Builder with(@NonNull final Value.ReadWrite<V> value,
                                          @NonNull final Access<V> access) {
                mRead.with(value, access);
                mWrite.with(value, access);
                return this;
            }

            @NonNull
            public final <M> Builder with(@NonNull final Mapper.Read<M> mapper,
                                          @NonNull final Setter<M> setter) {
                mRead.with(mapper, setter);
                return this;
            }

            @NonNull
            public final <M> Builder with(@NonNull final Mapper.Read<M> mapper,
                                          @NonNull final Access<M> access) {
                mRead.with(mapper, access);
                return this;
            }

            @NonNull
            public final <M> Builder with(@NonNull final Mapper.Write<M> mapper,
                                          @NonNull final Getter<M> getter) {
                mWrite.with(mapper, getter);
                return this;
            }

            @NonNull
            public final <M> Builder with(@NonNull final Mapper.ReadWrite<M> mapper,
                                          @NonNull final Access<M> access) {
                mRead.with(mapper, access);
                mWrite.with(mapper, access);
                return this;
            }

            @NonNull
            public final ReadWrite build() {
                return Instances.combine(mRead.build(), mWrite.build());
            }
        }
    }

    private Instance() {
        super();
    }
}
