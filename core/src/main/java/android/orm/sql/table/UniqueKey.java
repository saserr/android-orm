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

package android.orm.sql.table;

import android.orm.sql.Value;
import android.orm.sql.fragment.ConflictResolution;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class UniqueKey<V> extends Uniqueness<V> {

    @NonNull
    private final Value.ReadWrite<V> mValue;

    private UniqueKey(@NonNull final Value.ReadWrite<V> value,
                      @Nullable final ConflictResolution resolution) {
        super(Type.UniqueKey, value, resolution);

        mValue = value;
    }

    @NonNull
    @Override
    public final UniqueKey<V> onConflict(@NonNull final ConflictResolution resolution) {
        return new UniqueKey<>(mValue, resolution);
    }

    @NonNull
    public static <V> UniqueKey<V> on(@NonNull final Value.ReadWrite<V> value) {
        return new UniqueKey<>(value, null);
    }
}
