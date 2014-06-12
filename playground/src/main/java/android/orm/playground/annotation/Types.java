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

package android.orm.playground.annotation;

import android.net.Uri;
import android.orm.sql.Type;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

import static android.orm.sql.Types.Bool;
import static android.orm.sql.Types.Decimal;
import static android.orm.sql.Types.File;
import static android.orm.sql.Types.Integer;
import static android.orm.sql.Types.Real;
import static android.orm.sql.Types.Text;
import static android.orm.sql.Types.Uri;
import static android.orm.sql.Types.enumName;

public class Types {

    private final Semaphore mSemaphore = new Semaphore(1);
    private final Map<Class<?>, Type<?>> mTypes = new HashMap<>();

    public Types() {
        super();
    }

    public final <E extends Enum<E>> void register(@NonNull final Class<E> klass) {
        register(klass, enumName(klass));
    }

    public final <V> void register(@NonNull final Class<V> klass, @NonNull final Type<V> type) {
        mSemaphore.acquireUninterruptibly();
        try {
            if (mTypes.containsKey(klass)) {
                @NonNls final String error = "Type for class '" + klass.getName() + "' is already registered";
                throw new IllegalArgumentException(error);
            }
            mTypes.put(klass, type);
        } finally {
            mSemaphore.release();
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public final <V> Type<V> get(@NonNull final Class<V> klass) {
        return (Type<V>) mTypes.get(klass);
    }

    @NonNull
    public static Types withStandardTypes() {
        final Types result = new Types();

        result.register(String.class, Text);
        result.register(Long.class, Integer);
        result.register(Double.class, Real);
        result.register(Boolean.class, Bool);
        result.register(BigDecimal.class, Decimal);
        result.register(File.class, File);
        result.register(Uri.class, Uri);

        return result;
    }
}
