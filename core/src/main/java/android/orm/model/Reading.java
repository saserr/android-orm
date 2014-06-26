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

import android.orm.sql.Readable;
import android.orm.sql.Select;
import android.orm.sql.Value;
import android.orm.util.Converter;
import android.orm.util.Function;
import android.orm.util.Lens;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.orm.util.Producer;
import android.orm.util.Producers;
import android.support.annotation.NonNull;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.orm.model.Instances.setter;
import static android.orm.util.Maybes.nothing;
import static android.orm.util.Maybes.something;
import static android.orm.util.Producers.constant;

public interface Reading<M> {

    @NonNull
    Plan.Read<M> preparePlan();

    @NonNull
    Plan.Read<M> preparePlan(@NonNull final M m);

    @NonNull
    <N> Reading<N> map(@NonNull final Converter<Maybe<M>, Maybe<N>> converter);

    interface Single<M> extends Reading<M> {

        @NonNull
        <N> Single<Pair<M, N>> and(@NonNull final Single<N> other);

        @NonNull
        <N> Single<N> map(@NonNull final Converter<Maybe<M>, Maybe<N>> converter);

        abstract class Base<M> implements Single<M> {

            @NonNull
            @Override
            public final <N> Single<Pair<M, N>> and(@NonNull final Single<N> other) {
                return Readings.convert(this, other);
            }

            @NonNull
            @Override
            public final <N> Single<N> map(@NonNull final Converter<Maybe<M>, Maybe<N>> converter) {
                return Readings.convert(this, converter);
            }
        }
    }

    interface Many<M> extends Reading<M> {

        @NonNull
        <N> Many<Pair<M, N>> and(@NonNull final Many<N> other);

        @NonNull
        <N> Many<N> map(@NonNull final Converter<Maybe<M>, Maybe<N>> converter);

        abstract class Base<M> implements Many<M> {

            @NonNull
            @Override
            public final <N> Many<Pair<M, N>> and(@NonNull final Many<N> other) {
                return Readings.convert(this, other);
            }

            @NonNull
            @Override
            public final <N> Many<N> map(@NonNull final Converter<Maybe<M>, Maybe<N>> converter) {
                return Readings.convert(this, converter);
            }
        }
    }

    abstract class Item<M> {

        @NonNull
        private final Select.Projection mProjection;

        protected Item(@NonNull final Select.Projection projection) {
            super();

            mProjection = projection;
        }

        @NonNull
        public abstract <N> Item<Pair<M, N>> and(@NonNull final Item<N> other);

        @NonNull
        public abstract <N> Item<N> map(@NonNull final Function<Maybe<M>, Maybe<N>> converter);

        @NonNull
        public abstract Producer<Maybe<M>> read(@NonNull final Readable input);

        @NonNull
        public final Select.Projection getProjection() {
            return mProjection;
        }

        public final boolean isEmpty() {
            return mProjection.isEmpty();
        }

        public interface Action {
            @NonNull
            Runnable read(@NonNull final Readable input);
        }

        @NonNull
        public static <V> Action action(@NonNull final Value.Read<V> value,
                                        @NonNull final Instance.Setter<V> setter) {
            return new Action() {
                @NonNull
                @Override
                public Runnable read(@NonNull final Readable input) {
                    return set(value.read(input), setter);
                }
            };
        }

        @NonNull
        public static <V> Action action(@NonNull final Item<V> item,
                                        @NonNull final Instance.Setter<V> setter) {
            return new Action() {
                @NonNull
                @Override
                public Runnable read(@NonNull final Readable input) {
                    return set(item.read(input), setter);
                }
            };
        }

        @NonNull
        public static Action compose(@NonNull final Collection<Action> actions) {
            return new Action() {
                @NonNull
                @Override
                public Runnable read(@NonNull final Readable input) {
                    final Collection<Runnable> updates = new ArrayList<>(actions.size());

                    for (final Action action : actions) {
                        updates.add(action.read(input));
                    }

                    return compose(updates);
                }
            };
        }

        public abstract static class Create<M> extends Item<M> {

            private static final Create<Object> EMPTY = new Create<Object>(Select.Projection.Nothing) {
                @NonNull
                @Override
                public Producer<Maybe<Object>> read(@NonNull final Readable input) {
                    return constant(nothing());
                }
            };

            protected Create(@NonNull final Select.Projection projection) {
                super(projection);
            }

            @NonNull
            @Override
            public final <N> Item<Pair<M, N>> and(@NonNull final Item<N> other) {
                return (isEmpty() || other.isEmpty()) ?
                        Create.<Pair<M, N>>empty() :
                        ((other instanceof Create) ?
                                new Composition<>(this, (Create<N>) other) :
                                new Update.Composition<>(this, other));
            }

            @NonNull
            public final <N> Create<Pair<M, N>> and(@NonNull final Create<N> other) {
                return (isEmpty() || other.isEmpty()) ?
                        Create.<Pair<M, N>>empty() :
                        new Composition<>(this, other);
            }

            @NonNull
            @Override
            public final <N> Create<N> map(@NonNull final Function<Maybe<M>, Maybe<N>> converter) {
                return isEmpty() ? Create.<N>empty() : new Converted<>(this, converter);
            }

            @NonNull
            @SuppressWarnings("unchecked")
            public static <V> Create<V> empty() {
                return (Create<V>) EMPTY;
            }

            @NonNull
            public static <M extends Instance.Readable> Create<M> from(@NonNull final Select.Projection projection,
                                                                       @NonNull final Producer<M> producer) {
                return projection.isEmpty() ?
                        Create.<M>empty() :
                        new Create<M>(projection) {
                            @NonNull
                            @Override
                            public Producer<Maybe<M>> read(@NonNull final Readable input) {
                                final M model = producer.produce();
                                model.prepareRead().read(input).run();
                                return constant(something(model));
                            }
                        };
            }

            @NonNull
            public static <M> Create<M> from(@NonNull final Value.Read<M> value) {
                final Select.Projection projection = value.getProjection();
                return projection.isEmpty() ?
                        Create.<M>empty() :
                        new Create<M>(projection) {
                            @NonNull
                            @Override
                            public Producer<Maybe<M>> read(@NonNull final Readable input) {
                                return constant(value.read(input));
                            }
                        };
            }

            private static class Composition<M, N> extends Create<Pair<M, N>> {

                @NonNull
                private final Create<M> mFirst;
                @NonNull
                private final Create<N> mSecond;

                private Composition(@NonNull final Create<M> first,
                                    @NonNull final Create<N> second) {
                    super(first.getProjection().and(second.getProjection()));

                    mFirst = first;
                    mSecond = second;
                }

                @NonNull
                @Override
                public final Producer<Maybe<Pair<M, N>>> read(@NonNull final Readable input) {
                    final Producer<Maybe<M>> result1 = mFirst.read(input);
                    final Producer<Maybe<N>> result2 = mSecond.read(input);
                    final Producer<Pair<Maybe<M>, Maybe<N>>> result = Producers.compose(result1, result2);
                    return constant(Producers.convert(result, Maybes.<M, N>liftPair()).produce());
                }
            }

            private static class Converted<M, N> extends Create<N> {

                @NonNull
                private final Create<M> mCreate;
                @NonNull
                private final Function<Maybe<M>, Maybe<N>> mConverter;

                private Converted(@NonNull final Create<M> create,
                                  @NonNull final Function<Maybe<M>, Maybe<N>> converter) {
                    super(create.getProjection());

                    mCreate = create;
                    mConverter = converter;
                }

                @NonNull
                @Override
                public final Producer<Maybe<N>> read(@NonNull final Readable input) {
                    return constant(Producers.convert(mCreate.read(input), mConverter).produce());
                }
            }
        }

        public abstract static class Update<M> extends Item<M> {

            private static final Update<Object> EMPTY = new Update<Object>(Select.Projection.Nothing) {
                @NonNull
                @Override
                public Producer<Maybe<Object>> read(@NonNull final Readable input) {
                    return constant(nothing());
                }
            };

            protected Update(@NonNull final Select.Projection projection) {
                super(projection);
            }

            @NonNull
            @Override
            public final <N> Update<Pair<M, N>> and(@NonNull final Item<N> other) {
                return (isEmpty() || other.isEmpty()) ?
                        Update.<Pair<M, N>>empty() :
                        new Composition<>(this, other);
            }

            @NonNull
            @Override
            public final <N> Update<N> map(@NonNull final Function<Maybe<M>, Maybe<N>> converter) {
                return isEmpty() ? Update.<N>empty() : new Converted<>(this, converter);
            }

            @NonNull
            @SuppressWarnings("unchecked")
            public static <V> Update<V> empty() {
                return (Update<V>) EMPTY;
            }

            @NonNull
            public static <M extends Instance.Readable> Update<M> from(@NonNull final M model) {
                final Select.Projection projection = model.getProjection();
                final Action action = model.prepareRead();
                return projection.isEmpty() ?
                        Update.<M>empty() :
                        new Update<M>(projection) {
                            @NonNull
                            @Override
                            public Producer<Maybe<M>> read(@NonNull final Readable input) {
                                return update(model, action, input);
                            }
                        };
            }

            private static class Composition<M, N> extends Update<Pair<M, N>> {

                @NonNull
                private final Item<M> mFirst;
                @NonNull
                private final Item<N> mSecond;

                private Composition(@NonNull final Item<M> first, @NonNull final Item<N> second) {
                    super(first.getProjection().and(second.getProjection()));

                    mFirst = first;
                    mSecond = second;
                }

                @NonNull
                @Override
                public final Producer<Maybe<Pair<M, N>>> read(@NonNull final Readable input) {
                    final Producer<Maybe<M>> result1 = mFirst.read(input);
                    final Producer<Maybe<N>> result2 = mSecond.read(input);
                    return Producers.convert(Producers.compose(result1, result2), Maybes.<M, N>liftPair());
                }
            }

            private static class Converted<M, N> extends Update<N> {

                @NonNull
                private final Update<M> mUpdate;
                @NonNull
                private final Function<Maybe<M>, Maybe<N>> mConverter;

                private Converted(@NonNull final Update<M> update,
                                  @NonNull final Function<Maybe<M>, Maybe<N>> converter) {
                    super(update.getProjection());

                    mUpdate = update;
                    mConverter = converter;
                }

                @NonNull
                @Override
                public final Producer<Maybe<N>> read(@NonNull final Readable input) {
                    return Producers.convert(mUpdate.read(input), mConverter);
                }
            }
        }

        @NonNull
        public static <M> Builder<M> builder(@NonNull final Value.Read<M> producer) {
            return new Builder<>(producer);
        }

        public static class Builder<M> {

            @NonNull
            private final Value.Read<M> mProducer;
            @NonNull
            private Select.Projection mCreateProjection;
            @NonNull
            private Select.Projection mProducerOnlyProjection;

            private final Collection<Function<M, Action>> mEntries;

            public Builder(@NonNull final Value.Read<M> producer) {
                super();

                mProducer = producer;
                mProducerOnlyProjection = producer.getProjection();
                mCreateProjection = mProducerOnlyProjection;
                mEntries = new ArrayList<>();
            }

            public Builder(@NonNull final Builder<M> builder) {
                super();

                mProducer = builder.mProducer;
                mCreateProjection = builder.mCreateProjection;
                mProducerOnlyProjection = builder.mProducerOnlyProjection;
                mEntries = new ArrayList<>(builder.mEntries);
            }

            @NonNull
            public final <V> Builder<M> with(@NonNull final Value.Read<V> value,
                                             @NonNull final Lens.Write<M, Maybe<V>> lens) {
                add(value.getProjection(), entry(value, lens));
                return this;
            }

            @NonNull
            public final <V> Builder<M> with(@NonNull final Mapper.Read<V> mapper,
                                             @NonNull final Lens.ReadWrite<M, Maybe<V>> lens) {
                add(mapper.getProjection(), entry(mapper, lens));
                return this;
            }

            @NonNull
            public final Create<M> build() {
                return build(mProducer, mCreateProjection, mEntries);
            }

            @NonNull
            public final Update<M> build(@NonNull final M model) {
                return build(model, mProducer, mCreateProjection, mProducerOnlyProjection, mEntries);
            }

            protected final void add(@NonNull final Select.Projection projection,
                                     @NonNull final Function<M, Action> entry) {
                mCreateProjection = mCreateProjection.and(projection);
                mProducerOnlyProjection = mProducerOnlyProjection.without(projection);
                mEntries.add(entry);
            }

            @NonNull
            private static <M> Create<M> build(@NonNull final Value.Read<M> producer,
                                               @NonNull final Select.Projection projection,
                                               @NonNull final Collection<Function<M, Action>> entries) {
                return projection.isEmpty() ?
                        Create.<M>empty() :
                        new Create<M>(projection) {
                            @NonNull
                            @Override
                            public Producer<Maybe<M>> read(@NonNull final Readable input) {
                                return create(producer, entries, input);
                            }
                        };
            }

            @NonNull
            private static <M> Update<M> build(@NonNull final M model,
                                               @NonNull final Value.Read<M> producer,
                                               @NonNull final Select.Projection projection,
                                               @NonNull final Select.Projection producerOnly,
                                               @NonNull final Collection<Function<M, Action>> entries) {
                final Collection<Action> actions = new ArrayList<>(entries.size());
                for (final Function<M, Action> entry : entries) {
                    actions.add(entry.invoke(model));
                }
                final Action action = compose(actions);

                return projection.isEmpty() ?
                        Update.<M>empty() :
                        new Update<M>(projection) {
                            @NonNull
                            @Override
                            public Producer<Maybe<M>> read(@NonNull final Readable input) {
                                return producerOnly.isAny(input.getKeys()) ?
                                        create(producer, entries, input) :
                                        update(model, action, input);
                            }
                        };
            }

            @NonNull
            private static <M, V> Function<M, Action> entry(@NonNull final Value.Read<V> value,
                                                            @NonNull final Lens.Write<M, Maybe<V>> lens) {
                return new Function<M, Action>() {
                    @NonNull
                    @Override
                    public Action invoke(@NonNull final M model) {
                        return action(value, setter(model, lens));
                    }
                };
            }

            @NonNull
            private static <M, V> Function<M, Action> entry(@NonNull final Mapper.Read<V> mapper,
                                                            @NonNull final Lens.ReadWrite<M, Maybe<V>> lens) {
                return new Function<M, Action>() {
                    @NonNull
                    @Override
                    public Action invoke(@NonNull final M model) {
                        final Maybe<V> result = lens.get(model);
                        final V value = (result == null) ? null : result.getOrElse(null);
                        return action(
                                (value == null) ?
                                        mapper.prepareRead() :
                                        mapper.prepareRead(value),
                                setter(model, lens)
                        );
                    }
                };
            }
        }

        @NonNull
        private static <V> Runnable set(@NonNull final Maybe<V> result,
                                        @NonNull final Instance.Setter<V> setter) {
            return new Runnable() {
                @Override
                public void run() {
                    setter.set(result);
                }
            };
        }

        @NonNull
        private static <V> Runnable set(@NonNull final Producer<Maybe<V>> producer,
                                        @NonNull final Instance.Setter<V> setter) {
            return new Runnable() {
                @Override
                public void run() {
                    setter.set(producer.produce());
                }
            };
        }

        private static <M> Producer<Maybe<M>> create(@NonNull final Value.Read<M> producer,
                                                     @NonNull final Collection<Function<M, Action>> entries,
                                                     @NonNull final Readable input) {
            final Maybe<M> result = producer.read(input);
            final Producer<Maybe<M>> product;

            if (result.isSomething()) {
                final M model = result.get();
                if (model == null) {
                    product = constant(result);
                } else {
                    final Collection<Runnable> updates = new ArrayList<>(entries.size());
                    for (final Function<M, Action> entry : entries) {
                        updates.add(entry.invoke(model).read(input));
                    }
                    product = eager(model, compose(updates));
                }
            } else {
                product = constant(result);
            }

            return product;
        }

        private static <M> Producer<Maybe<M>> update(@NonNull final M model,
                                                     @NonNull final Action action,
                                                     @NonNull final Readable input) {
            return lazy(model, action.read(input));
        }

        private static Runnable compose(@NonNull final Iterable<Runnable> tasks) {
            return new Runnable() {
                @Override
                public void run() {
                    for (final Runnable task : tasks) {
                        task.run();
                    }
                }
            };
        }

        @NonNull
        public static <V> Producer<Maybe<V>> eager(@NonNull final V value,
                                                   @NonNull final Runnable runnable) {
            runnable.run();
            final Maybe<V> result = something(value);
            return constant(result);
        }

        @NonNull
        public static <V> Producer<Maybe<V>> lazy(@NonNull final V value,
                                                  @NonNull final Runnable runnable) {
            return new Producer<Maybe<V>>() {

                private final Maybe<V> mResult = something(value);
                private final AtomicBoolean mNeedsExecution = new AtomicBoolean(true);

                @NonNull
                @Override
                public Maybe<V> produce() {
                    if (mNeedsExecution.getAndSet(false)) {
                        runnable.run();
                    }

                    return mResult;
                }
            };
        }
    }
}
