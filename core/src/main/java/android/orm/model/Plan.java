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
import android.orm.sql.Reader;
import android.orm.sql.Select;
import android.orm.sql.Value;
import android.orm.sql.Values;
import android.orm.sql.Writer;
import android.orm.util.Function;
import android.orm.util.Lens;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.orm.util.Producer;
import android.orm.util.Producers;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.orm.util.Lenses.get;
import static android.orm.util.Maybes.something;

public final class Plan {

    public static final class Read {

        @NonNull
        public static <M extends Instance.Readable> Reader.Element.Create<M> from(@NonNull final Select.Projection projection,
                                                                                  @NonNull final Producer<M> producer) {
            return new Reader.Element.Create<M>() {

                @NonNull
                @Override
                public Select.Projection getProjection() {
                    return projection;
                }

                @NonNull
                @Override
                public Producer<Maybe<M>> read(@NonNull final Readable input) {
                    final M model = producer.produce();
                    model.prepareRead().read(input).run();
                    return Producers.constant(something(model));
                }
            };
        }

        @NonNull
        public static <M extends Instance.Readable> Reader.Element.Update<M> from(@NonNull final M model) {
            return new Reader.Element.Update<M>() {

                private final Instance.Readable.Action mAction = model.prepareRead();

                @NonNull
                @Override
                public Select.Projection getProjection() {
                    return mAction.getProjection();
                }

                @NonNull
                @Override
                public Producer<Maybe<M>> read(@NonNull final Readable input) {
                    return lazy(model, mAction.read(input));
                }
            };
        }

        @NonNull
        public static <M> Reader.Element.Create<M> from(@NonNull final Value.Read<M> value) {
            return new Reader.Element.Create<M>() {

                @NonNull
                @Override
                public Select.Projection getProjection() {
                    return value.getProjection();
                }

                @NonNull
                @Override
                public Producer<Maybe<M>> read(@NonNull final Readable input) {
                    return Producers.constant(value.read(input));
                }
            };
        }

        @NonNull
        public static <M> Builder<M> builder(@NonNull final Producer<M> producer) {
            return new Builder<>(new Reader.Element.Create<M>() {

                private final Producer<Maybe<M>> mProducer = Maybes.lift(producer);

                @NonNull
                @Override
                public Select.Projection getProjection() {
                    return Select.Projection.Nothing;
                }

                @NonNull
                @Override
                public Producer<Maybe<M>> read(@NonNull final Readable input) {
                    return mProducer;
                }
            });
        }

        private static <M> void eager(@NonNull final Maybe<M> result,
                                      @NonNull final Collection<Function<M, Instance.Readable.Action>> factories,
                                      @NonNull final Readable input) {
            if (result.isSomething()) {
                final M model = result.get();
                if (model != null) {
                    for (final Function<M, Instance.Readable.Action> factory : factories) {
                        factory.invoke(model).read(input).run();
                    }
                }
            }
        }

        @NonNull
        private static <M> Producer<Maybe<M>> lazy(@NonNull final M model,
                                                   @NonNull final Runnable update) {
            return new Producer<Maybe<M>>() {

                private final Maybe<M> mResult = something(model);
                private final AtomicBoolean mNeedsExecution = new AtomicBoolean(true);

                @NonNull
                @Override
                public Maybe<M> produce() {
                    if (mNeedsExecution.getAndSet(false)) {
                        update.run();
                    }

                    return mResult;
                }
            };
        }

        public static class Builder<M> {

            @NonNull
            private final Reader.Element.Create<M> mCreate;
            @NonNull
            private Select.Projection mCreateProjection;
            @NonNull
            private final Collection<Function<M, Instance.Readable.Action>> mFactories;

            public Builder(@NonNull final Reader.Element.Create<M> create) {
                super();

                mCreate = create;
                mCreateProjection = create.getProjection();
                mFactories = new LinkedList<>();
            }

            public Builder(@NonNull final Builder<M> builder) {
                super();

                mCreate = builder.mCreate;
                mCreateProjection = builder.mCreateProjection;
                mFactories = new LinkedList<>(builder.mFactories);
            }

            @NonNull
            public final <V> Builder<M> with(@NonNull final Value.Read<V> value,
                                             @NonNull final Lens.Write<M, Maybe<V>> lens) {
                return with(value.getProjection(), factory(value, lens));
            }

            @NonNull
            public final <V> Builder<M> with(@NonNull final Mapper.Read<V> mapper,
                                             @NonNull final Lens.ReadWrite<M, Maybe<V>> lens) {
                return with(mapper.prepareReader().getProjection(), factory(mapper, lens));
            }

            @NonNull
            public final Reader.Element.Create<M> build() {
                return build(mCreate, mCreateProjection, new ArrayList<>(mFactories));
            }

            @NonNull
            public final Reader.Element<M> build(@NonNull final M model) {
                final Collection<Instance.Readable.Action> actions = new ArrayList<>(mFactories.size());
                for (final Function<M, Instance.Readable.Action> factory : mFactories) {
                    actions.add(factory.invoke(model));
                }
                final Instance.Readable.Action action = Instances.compose(actions);
                return mCreate.getProjection().without(action.getProjection()).isEmpty() ?
                        build(model, action) :
                        build();
            }

            @NonNull
            private Builder<M> with(@NonNull final Select.Projection projection,
                                    @NonNull final Function<M, Instance.Readable.Action> factory) {
                mCreateProjection = mCreateProjection.and(projection);
                mFactories.add(factory);
                return this;
            }

            @NonNull
            private static <M> Reader.Element.Create<M> build(@NonNull final Reader.Element.Create<M> create,
                                                              @NonNull final Select.Projection projection,
                                                              @NonNull final Collection<Function<M, Instance.Readable.Action>> factories) {
                return new Reader.Element.Create<M>() {

                    @NonNull
                    @Override
                    public Select.Projection getProjection() {
                        return projection;
                    }

                    @NonNull
                    @Override
                    public Producer<Maybe<M>> read(@NonNull final Readable input) {
                        final Maybe<M> result = create.read(input).produce();
                        eager(result, factories, input);
                        return Producers.constant(result);
                    }
                };
            }

            @NonNull
            private static <M> Reader.Element.Update<M> build(@NonNull final M model,
                                                              @NonNull final Instance.Readable.Action action) {
                return new Reader.Element.Update<M>() {

                    @NonNull
                    @Override
                    public Select.Projection getProjection() {
                        return action.getProjection();
                    }

                    @NonNull
                    @Override
                    public Producer<Maybe<M>> read(@NonNull final Readable input) {
                        return lazy(model, action.read(input));
                    }
                };
            }

            @NonNull
            private static <M, V> Function<M, Instance.Readable.Action> factory(@NonNull final Value.Read<V> value,
                                                                                @NonNull final Lens.Write<M, Maybe<V>> lens) {
                return new Function<M, Instance.Readable.Action>() {
                    @NonNull
                    @Override
                    public Instance.Readable.Action invoke(@NonNull final M model) {
                        return Instances.action(value, new Instance.Setter<V>() {
                            @Override
                            public void set(@Nullable final V value) {
                                lens.set(model, something(value));
                            }
                        });
                    }
                };
            }

            @NonNull
            private static <M, V> Function<M, Instance.Readable.Action> factory(@NonNull final Mapper.Read<V> mapper,
                                                                                @NonNull final Lens.ReadWrite<M, Maybe<V>> lens) {
                return new Function<M, Instance.Readable.Action>() {
                    @NonNull
                    @Override
                    public Instance.Readable.Action invoke(@NonNull final M model) {
                        return Instances.action(mapper, new Instance.Access<V>() {

                            @Nullable
                            @Override
                            public V get() {
                                final Maybe<V> value = lens.get(model);
                                return (value == null) ? null : value.getOrElse(null);
                            }

                            @Override
                            public void set(@Nullable final V value) {
                                lens.set(model, something(value));
                            }
                        });
                    }
                };
            }
        }

        private Read() {
            super();
        }
    }

    public static final class Write {

        public static class Builder<M> {

            @NonNull
            private final Writer.Builder<Maybe<M>> mWriter;

            public Builder() {
                super();

                mWriter = new Writer.Builder<>();
            }

            public Builder(@NonNull final Builder<M> builder) {
                super();

                mWriter = new Writer.Builder<>(builder.mWriter);
            }

            @NonNull
            public final Builder<M> with(@NonNull final Writer writer) {
                mWriter.with(writer);
                return this;
            }

            @NonNull
            public final <V> Builder<M> with(@NonNull final Value.Write<V> value,
                                             @NonNull final Lens.Read<M, Maybe<V>> lens) {
                mWriter.with(factory(value, lens));
                return this;
            }

            @NonNull
            public final <V> Builder<M> with(@NonNull final Mapper.Write<V> mapper,
                                             @NonNull final Lens.Read<M, Maybe<V>> lens) {
                mWriter.with(factory(mapper, lens));
                return this;
            }

            @NonNull
            public final Writer build(@NonNull final Maybe<M> model) {
                return mWriter.build(model);
            }

            @NonNull
            private static <M, V> Function<Maybe<M>, Writer> factory(@NonNull final Value.Write<V> value,
                                                                     @NonNull final Lens.Read<M, Maybe<V>> lens) {
                return new Function<Maybe<M>, Writer>() {
                    @NonNull
                    @Override
                    public Writer invoke(@NonNull final Maybe<M> model) {
                        return Values.value(value, get(model, lens));
                    }
                };
            }

            @NonNull
            private static <M, V> Function<Maybe<M>, Writer> factory(@NonNull final Mapper.Write<V> mapper,
                                                                     @NonNull final Lens.Read<M, Maybe<V>> lens) {
                return new Function<Maybe<M>, Writer>() {
                    @NonNull
                    @Override
                    public Writer invoke(@NonNull final Maybe<M> model) {
                        return mapper.prepareWriter(get(model, lens));
                    }
                };
            }
        }

        private Write() {
            super();
        }
    }

    private Plan() {
        super();
    }
}
