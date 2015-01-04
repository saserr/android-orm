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
import android.orm.model.Binding;
import android.orm.model.Instance;
import android.orm.model.Mapper;
import android.orm.model.Mappers;
import android.orm.model.Reading;
import android.orm.sql.Select;
import android.orm.sql.Value;
import android.orm.sql.Writer;
import android.orm.sql.Writers;
import android.orm.util.Maybe;
import android.orm.util.Producer;
import android.support.annotation.NonNull;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static android.orm.util.Maybes.something;

public class Form extends Instance.ReadWrite.Base {

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
        boolean valid = true;

        final int size = mWrites.size();
        for (int i = 0; (i < size) && valid; i++) {
            valid = mWrites.get(i).isValid();
        }

        return valid;
    }

    @SuppressWarnings("unchecked")
    public final void clear() {
        final Maybe<Object> value = something(null);
        for (final Entry.Read<?> entry : mReads) {
            ((Entry.Read<Object>) entry).set(value);
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
    public final Writer prepareWrite() {
        final Collection<Writer> writers = new ArrayList<>(mWrites.size());

        for (final Entry.Write entry : mWrites) {
            writers.add(entry.prepareWrite(mContext));
        }

        return Writers.compose(writers);
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
        public final <V> Builder bind(@NonNull final Binding.Read<V> binding,
                                      @NonNull final Value.Write<V> value) {
            return bind(binding, Mappers.write(value));
        }

        @NonNull
        public final <V> Builder bind(@NonNull final Binding.Read<V> binding,
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

        public interface Read<V> {

            void set(@NonNull final Maybe<V> v);

            @NonNull
            Reading.Item.Action prepareRead(@NonNull final Context context);
        }

        public interface Write {

            boolean isValid();

            @NonNull
            Writer prepareWrite(@NonNull final Context context);
        }

        public interface ReadWrite<V> extends Read<V>, Write {
        }

        private Entry() {
            super();
        }
    }

    @NonNull
    private static <V> Entry.Write entry(@NonNull final Binding.Read<V> binding,
                                         @NonNull final Mapper.Write<V> mapper) {
        return new Entry.Write() {

            @Override
            public boolean isValid() {
                return true;
            }

            @NonNull
            @Override
            public Writer prepareWrite(@NonNull final Context context) {
                return write(mapper, binding);
            }
        };
    }

    @NonNull
    private static <V> Entry.ReadWrite<V> entry(@NonNull final Binding.ReadWrite<V> binding,
                                                @NonNull final Mapper.ReadWrite<V> mapper) {
        return new Entry.ReadWrite<V>() {

            @Override
            public boolean isValid() {
                return true;
            }

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
            public Writer prepareWrite(@NonNull final Context context) {
                return write(mapper, binding);
            }

            @Override
            public void set(@NonNull final Maybe<V> value) {
                binding.set(value);
            }
        };
    }

    @NonNull
    public static <V> Reading.Item.Action action(@NonNull final Reading.Item<V> reading,
                                                 @NonNull final Binding.Write<V> binding) {
        return new Reading.Item.Action() {

            @NonNull
            @Override
            public Select.Projection getProjection() {
                return reading.getProjection();
            }

            @NonNull
            @Override
            public Runnable read(@NonNull final android.orm.sql.Readable input) {
                return set(reading.read(input), binding);
            }
        };
    }

    @NonNull
    public static <V> Writer write(@NonNull final Mapper.Write<V> mapper,
                                   @NonNull final Binding.Read<V> binding) {
        return mapper.prepareWrite(binding.get());
    }

    @NonNull
    private static <V> Runnable set(@NonNull final Producer<Maybe<V>> producer,
                                    @NonNull final Binding.Write<V> binding) {
        return new Runnable() {
            @Override
            public void run() {
                binding.set(producer.produce());
            }
        };
    }
}
