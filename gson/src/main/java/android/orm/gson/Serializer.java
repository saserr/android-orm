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

package android.orm.gson;

import android.orm.model.Mapper;
import android.orm.model.Reading;
import android.orm.sql.Value;
import android.orm.util.Producer;
import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NonNls;

import static android.orm.util.Lenses.combine;

public class Serializer<E extends JsonElement> extends Mapper.Read.Base<E> {

    @NonNls
    @NonNull
    private final String mName;
    @NonNull
    private final Reading.Item.Builder<E> mReading;
    @NonNull
    private final Reading.Item.Create<E> mCreate;

    public Serializer(@NonNls @NonNull final String name,
                      @NonNull final Reading.Item.Builder<E> reading) {
        super();

        mName = name;
        mReading = reading;
        mCreate = reading.build();
    }

    @NonNls
    @NonNull
    @Override
    public final String getName() {
        return mName;
    }

    @NonNull
    @Override
    public final Reading.Item.Create<E> prepareRead() {
        return mCreate;
    }

    @NonNull
    @Override
    public final Reading.Item<E> prepareRead(@NonNull final E element) {
        return mReading.build(element);
    }

    @NonNull
    public static Builder builder(@NonNls @NonNull final String name,
                                  @NonNull final Gson gson) {
        return new Builder(name, gson);
    }

    public static class Builder {

        private static final Producer<JsonObject> PRODUCER = new Producer<JsonObject>() {
            @NonNull
            @Override
            public JsonObject produce() {
                return new JsonObject();
            }
        };

        @NonNls
        @NonNull
        private final String mName;
        @NonNull
        private final Gson mGson;
        @NonNull
        private final Reading.Item.Builder<JsonObject> mReading;

        public Builder(@NonNls @NonNull final String name,
                       @NonNull final Gson gson) {
            super();

            mName = name;
            mGson = gson;
            mReading = Reading.Item.builder(name, PRODUCER);
        }

        @NonNull
        public final <V> Builder with(@NonNull final Value.Read<V> value) {
            return with(value.getName(), value);
        }

        @NonNull
        public final <V> Builder with(@NonNls @NonNull final String name,
                                      @NonNull final Value.Read<V> value) {
            mReading.with(value, Serializers.<V>lens(mGson, name));
            return this;
        }

        @NonNull
        public final <M> Builder with(@NonNls @NonNull final String name,
                                      @NonNull final Class<M> klass,
                                      @NonNull final Mapper.Read<M> mapper) {
            mReading.with(mapper, combine(Deserializers.lens(mGson, klass, name), Serializers.<M>lens(mGson, name)));
            return this;
        }

        @NonNull
        public final Serializer<JsonObject> build() {
            return new Serializer<>(mName, mReading);
        }
    }
}
