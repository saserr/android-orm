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

package android.orm.playground;

import android.orm.model.Binding;
import android.orm.model.Instance;
import android.orm.model.Instances;
import android.orm.model.Mapper;
import android.orm.model.Mappers;
import android.orm.sql.Value;
import android.support.annotation.NonNull;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static android.orm.playground.Form.action;

public class Show extends Instance.Readable.Base {

    @NonNls
    @NonNull
    private final String mName;
    @NonNull
    private final List<Entry> mEntries;

    private Show(@NonNls @NonNull final String name,
                 @NonNull final List<Entry> entries) {
        super();

        mName = name;
        mEntries = new ArrayList<>(entries);
    }

    @NonNull
    @Override
    public final String getName() {
        return mName;
    }

    @NonNull
    @Override
    public final Instance.Readable.Action prepareRead() {
        final Collection<Instance.Readable.Action> actions = new ArrayList<>(mEntries.size());
        for (final Entry entry : mEntries) {
            actions.add(entry.prepareRead());
        }
        return Instances.compose(actions);
    }

    public static class Builder {

        @NonNls
        @NonNull
        private final String mName;

        private final List<Entry> mEntries = new ArrayList<>();

        public Builder(@NonNls @NonNull final String name) {
            super();

            mName = name;
        }

        @NonNull
        public final <V> Builder bind(@NonNull final Binding.Write<V> binding,
                                      @NonNull final Value.Read<V> value) {
            return bind(binding, Mappers.read(value));
        }

        @NonNull
        public final <V> Builder bind(@NonNull final Binding.Write<V> binding,
                                      @NonNull final Mapper.Read<V> mapper) {
            return with(entry(binding, mapper));
        }

        @NonNull
        public final <V> Builder bind(@NonNull final Binding.ReadWrite<V> binding,
                                      @NonNull final Value.Read<V> value) {
            return bind(binding, Mappers.read(value));
        }

        @NonNull
        public final <V> Builder bind(@NonNull final Binding.ReadWrite<V> binding,
                                      @NonNull final Mapper.Read<V> mapper) {
            return with(entry(binding, mapper));
        }

        @NonNull
        public final Show build() {
            return new Show(mName, mEntries);
        }

        private Builder with(@NonNull final Entry entry) {
            mEntries.add(entry);
            return this;
        }
    }

    private interface Entry {
        @NonNull
        Instance.Readable.Action prepareRead();
    }

    @NonNull
    private static <V> Entry entry(@NonNull final Binding.Write<V> binding,
                                   @NonNull final Mapper.Read<V> mapper) {
        return new Entry() {
            @NonNull
            @Override
            public Instance.Readable.Action prepareRead() {
                return action(mapper.prepareReader(), binding);
            }
        };
    }

    @NonNull
    private static <V> Entry entry(@NonNull final Binding.ReadWrite<V> binding,
                                   @NonNull final Mapper.Read<V> mapper) {
        return new Entry() {
            @NonNull
            @Override
            public Instance.Readable.Action prepareRead() {
                final V value = binding.get().getOrElse(null);
                return action(
                        (value == null) ? mapper.prepareReader() : mapper.prepareReader(value),
                        binding
                );
            }
        };
    }
}
