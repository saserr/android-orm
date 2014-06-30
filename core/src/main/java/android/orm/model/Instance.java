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
import android.orm.util.Maybe;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Collection;

import static android.orm.model.Instances.combine;
import static android.orm.model.Plans.write;
import static android.orm.model.Reading.Item.action;

public final class Instance {

    public interface Getter<V> {
        @NonNull
        Maybe<V> get();
    }

    public interface Setter<V> {
        void set(@NonNull final Maybe<V> v);
    }

    public interface Access<V> extends Getter<V>, Setter<V> {
    }

    public interface Readable {

        @NonNls
        @NonNull
        String getName();

        @NonNull
        Reading.Item.Action prepareRead();

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
                        return action(value, setter);
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
                        return action(mapper, setter);
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
                        return action(mapper, access);
                    }
                };
            }

            private static class CompositeReadable extends Observer.Read.Delegate implements Readable {

                @NonNls
                @NonNull
                private final String mName;
                @NonNull
                private final Collection<Producer<Reading.Item.Action>> mProducers;

                private CompositeReadable(@NonNls @NonNull final String name,
                                          @NonNull final Collection<Producer<Reading.Item.Action>> producers,
                                          @NonNull final Collection<Observer.Read> observers) {
                    super(observers);

                    mName = name;
                    mProducers = new ArrayList<>(producers);
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
            }
        }
    }

    public interface Writable {

        @NonNull
        Plan.Write prepareWrite();

        class Builder {

            private final Collection<Producer<Plan.Write>> mProducers = new ArrayList<>();
            private final Collection<Observer.Write> mObservers = new ArrayList<>();

            public Builder() {
                super();
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
                return new CompositeWritable(mProducers, mObservers);
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
                        return write(writer);
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
                        return write(value, getter);
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
                        return write(mapper, getter);
                    }
                };
            }

            private static class CompositeWritable extends Observer.Write.Delegate implements Writable {

                @NonNull
                private final Collection<Producer<Plan.Write>> mProducers;

                private CompositeWritable(@NonNull final Collection<Producer<Plan.Write>> producers,
                                          @NonNull final Collection<Observer.Write> observers) {
                    super(observers);

                    mProducers = new ArrayList<>(producers);
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
            }
        }
    }

    public interface ReadWrite extends Readable, Writable {

        class Builder {

            @NonNls
            @NonNull
            private final String mName;
            @NonNull
            private final Readable.Builder mRead;

            private final Writable.Builder mWrite = new Writable.Builder();

            public Builder(@NonNls @NonNull final String name) {
                super();

                mName = name;
                mRead = new Readable.Builder(name);
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
                return combine(mRead.build(), mWrite.build());
            }
        }
    }

    @NonNull
    public static Readable.Builder readable(@NonNls @NonNull final String name) {
        return new Readable.Builder(name);
    }

    @NonNull
    public static Writable.Builder writable() {
        return new Writable.Builder();
    }

    @NonNull
    public static ReadWrite.Builder builder(@NonNls @NonNull final String name) {
        return new ReadWrite.Builder(name);
    }

    private Instance() {
        super();
    }
}
