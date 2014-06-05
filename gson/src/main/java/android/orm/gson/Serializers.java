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

package android.orm.gson;

import android.orm.model.Mapper;
import android.orm.model.Reading;
import android.orm.sql.Value;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import static android.orm.util.Maybes.something;

public final class Serializers {

    @NonNull
    public static <V> Serializer<JsonElement> serializer(@NonNull final Gson gson,
                                                         @NonNull final Value.Read<V> value) {
        return new Serializer<>(value.getName(), Reading.Item.Create.from(value).map(new ToJson<V>(gson)));
    }

    @NonNull
    public static <M> Serializer<JsonElement> serializer(@NonNull final Gson gson,
                                                         @NonNull final Mapper.Read<M> mapper) {
        return new Serializer<>(mapper.getName(), mapper.prepareRead().map(new ToJson<M>(gson)));
    }

    private static class ToJson<V> extends Function.Base<Maybe<V>, Maybe<JsonElement>> {

        @NonNull
        private final Gson mGson;

        private ToJson(@NonNull final Gson gson) {
            super();

            mGson = gson;
        }

        @NonNull
        @Override
        public final Maybe<JsonElement> invoke(@NonNull final Maybe<V> result) {
            return result.isSomething() ?
                    something(mGson.toJsonTree(result.get())) :
                    Maybes.<JsonElement>nothing();
        }
    }

    private Serializers() {
        super();
    }
}
