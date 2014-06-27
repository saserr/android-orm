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

package android.orm.playground;

import android.content.Context;
import android.orm.model.Instance;
import android.orm.model.Mapper;
import android.orm.model.Mappers;
import android.orm.model.Plan;
import android.orm.model.Reading;
import android.orm.sql.Value;
import android.orm.util.Maybe;
import android.orm.util.Validations;
import android.support.annotation.NonNull;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static android.orm.model.Plans.EmptyWrite;
import static android.orm.model.Plans.compose;
import static android.orm.model.Plans.write;
import static android.orm.model.Reading.Item.action;
import static android.orm.util.Maybes.something;
import static android.orm.util.Validations.valid;

public class Form implements Instance.ReadWrite {

    private static final Validation.Result<Plan.Write> EMPTY = valid(EmptyWrite);

    @NonNull
    private final Context mContext;
    @NonNls
    @NonNull
    private final String mName;
    @NonNull
    private final List<Entry.Read<?>> mReads;
    @NonNull
    private final List<Entry.Write> mWrites;

    private Form(@NonNull final Context context,
                 @NonNls @NonNull final String name,
                 @NonNull final List<Entry.Read<?>> reads,
                 @NonNull final List<Entry.Write> writes) {
        super();

        mContext = context;
        mName = name;
        mReads = new ArrayList<>(reads);
        mWrites = new ArrayList<>(writes);
    }

    public final boolean isValid() {
        return prepare().isValid();
    }

    @SuppressWarnings("unchecked")
    public final void clear() {
        final Maybe<Object> value = something(null);
        for (final Entry.Read<?> entry : mReads) {
            ((Instance.Setter<Object>) entry).set(value);
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
        final Collection<Reading.Item.Action> actions = new ArrayList<>(mReads.size());
        for (final Entry.Read<?> entry : mReads) {
            actions.add(entry.prepareRead(mContext));
        }
        return Reading.Item.compose(actions);
    }

    @NonNull
    @Override
    public final Plan.Write prepareWrite() {
        return prepare().get();
    }

    @NonNull
    private Validation.Result<Plan.Write> prepare() {
        final Collection<Plan.Write> plans = new ArrayList<>(mWrites.size());
        Validation.Result<Plan.Write> last = EMPTY;

        for (int i = 0; (i < mWrites.size()) && last.isValid(); i++) {
            last = mWrites.get(i).prepareWrite(mContext);
            if (last.isValid()) {
                plans.add(last.get());
            }
        }

        return last.isValid() ? valid(compose(plans)) : last;
    }

    public static Builder builder(@NonNls @NonNull final String name) {
        return new Builder(name);
    }

    public static class Builder {

        @NonNls
        @NonNull
        private final String mName;

        private final List<Entry.Read<?>> mReads = new ArrayList<>();
        private final List<Entry.Write> mWrites = new ArrayList<>();

        public Builder(@NonNls @NonNull final String name) {
            super();

            mName = name;
        }

        @NonNull
        public final <V> Builder bind(@NonNull final Binding.Readable<V> binding,
                                      @NonNull final Value.Write<V> value) {
            return bind(binding, Mappers.write(value));
        }

        @NonNull
        public final <V> Builder bind(@NonNull final Binding.Readable<V> binding,
                                      @NonNull final Mapper.Write<V> mapper) {
            return with(entry(binding, mapper));
        }

        @NonNull
        public final <V> Builder bind(@NonNull final Binding.ReadWrite<V> binding,
                                      @NonNull final Value.ReadWrite<V> value) {
            return bind(binding, Mappers.mapper(value));
        }

        @NonNull
        public final <V> Builder bind(@NonNull final Binding.ReadWrite<V> binding,
                                      @NonNull final Mapper.ReadWrite<V> mapper) {
            return with(entry(binding, mapper));
        }

        @NonNull
        public final <V> Builder bind(@NonNull final Binding.Validated<V> binding,
                                      @NonNull final Value.ReadWrite<V> value) {
            return bind(binding, Mappers.mapper(value));
        }

        @NonNull
        public final <M> Builder bind(@NonNull final Binding.Validated<M> binding,
                                      @NonNull final Mapper.ReadWrite<M> mapper) {
            return with(entry(binding, mapper));
        }

        @NonNull
        public final Form build(@NonNull final Context context) {
            return new Form(context, mName, mReads, mWrites);
        }

        private Builder with(@NonNull final Entry.Write entry) {
            mWrites.add(entry);
            return this;
        }

        private <V> Builder with(@NonNull final Entry.ReadWrite<V> entry) {
            mReads.add(entry);
            mWrites.add(entry);
            return this;
        }
    }

    private static final class Entry {

        public interface Read<V> extends Instance.Setter<V> {

            @NonNull
            Reading.Item.Action prepareRead(@NonNull final Context context);
        }

        public interface Write {
            @NonNull
            Validation.Result<Plan.Write> prepareWrite(@NonNull final Context context);
        }

        public interface ReadWrite<V> extends Read<V>, Write {
        }

        private Entry() {
            super();
        }
    }

    @NonNull
    private static <V> Entry.Write entry(@NonNull final Binding.Readable<V> binding,
                                         @NonNull final Mapper.Write<V> mapper) {
        return new Entry.Write() {
            @NonNull
            @Override
            public Validation.Result<Plan.Write> prepareWrite(@NonNull final Context context) {
                return valid(write(mapper, binding));
            }
        };
    }

    @NonNull
    private static <V> Entry.ReadWrite<V> entry(@NonNull final Binding.ReadWrite<V> binding,
                                                @NonNull final Mapper.ReadWrite<V> mapper) {
        return new Entry.ReadWrite<V>() {

            @NonNull
            @Override
            public Reading.Item.Action prepareRead(@NonNull final Context context) {
                final V value = binding.get().getOrElse(null);
                return action(
                        (value == null) ? mapper.prepareRead() : mapper.prepareRead(value),
                        binding
                );
            }

            @NonNull
            @Override
            public Validation.Result<Plan.Write> prepareWrite(@NonNull final Context context) {
                return valid(write(mapper, binding));
            }

            @Override
            public void set(@NonNull final Maybe<V> value) {
                binding.set(value);
            }
        };
    }

    @NonNull
    private static <V> Entry.ReadWrite<V> entry(@NonNull final Binding.Validated<V> binding,
                                                @NonNull final Mapper.ReadWrite<V> mapper) {
        return new Entry.ReadWrite<V>() {

            @NonNull
            @Override
            public Reading.Item.Action prepareRead(@NonNull final Context context) {
                final Validation.Result<Maybe<V>> value = binding.get(context);
                final Reading.Item<V> item;

                if (value.isValid()) {
                    final V v = value.get().getOrElse(null);
                    item = (v == null) ? mapper.prepareRead() : mapper.prepareRead(v);
                } else {
                    item = mapper.prepareRead();
                }

                return action(item, binding);
            }

            @NonNull
            @Override
            public Validation.Result<Plan.Write> prepareWrite(@NonNull final Context context) {
                final Validation.Result<Maybe<V>> value = binding.get(context);
                final Validation.Result<Plan.Write> result;

                if (value.isValid()) {
                    result = valid(mapper.prepareWrite(value.get()));
                    binding.setErrors(Collections.<String>emptyList());
                } else {
                    result = Validations.safeCast((Validation.Result.Invalid<Maybe<V>>) value);
                }

                return result;
            }

            @Override
            public void set(@NonNull final Maybe<V> value) {
                binding.set(value);
            }
        };
    }
}
