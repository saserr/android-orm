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

package android.orm.remote.dao.direct;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.orm.sql.Writer;
import android.orm.sql.fragment.Condition;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;

import static android.orm.sql.Value.Write.Operation.Update;
import static android.orm.sql.Writables.writable;
import static android.orm.util.Maybes.something;
import static android.util.Log.INFO;

public class Update implements Function<Pair<Condition, Writer>, Maybe<Integer>> {

    private static final String TAG = Update.class.getSimpleName();

    @NonNull
    private final ContentResolver mResolver;
    @NonNull
    private final Uri mUri;

    public Update(@NonNull final ContentResolver resolver, @NonNull final Uri uri) {
        super();

        mResolver = resolver;
        mUri = uri;
    }

    @NonNull
    @Override
    public final Maybe<Integer> invoke(@NonNull final Pair<Condition, Writer> args) {
        final ContentValues values = new ContentValues();
        final Writer writer = args.second;
        writer.write(Update, writable(values));

        final int updated;

        if (values.size() > 0) {
            final Condition condition = args.first.and(writer.onUpdate());
            updated = mResolver.update(mUri, values, condition.toSQL(), null);
        } else {
            updated = 0;
            if (Log.isLoggable(TAG, INFO)) {
                Log.i(TAG, "Nothing was updated"); //NON-NLS
            }
        }

        return (updated > 0) ? something(updated) : Maybes.<Integer>nothing();
    }
}
