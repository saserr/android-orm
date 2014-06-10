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

package android.orm.dao.remote;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.orm.sql.Writer;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.support.annotation.NonNull;
import android.util.Log;

import static android.orm.sql.Value.Write.Operation.Insert;
import static android.orm.sql.Writables.writable;
import static android.orm.util.Maybes.something;
import static android.util.Log.INFO;

public class Insert extends Function.Base<Writer, Maybe<Uri>> {

    private static final String TAG = Insert.class.getSimpleName();

    @NonNull
    private final ContentResolver mResolver;
    @NonNull
    private final Uri mUri;

    public Insert(@NonNull final ContentResolver resolver, @NonNull final Uri uri) {
        super();

        mResolver = resolver;
        mUri = uri;
    }

    @NonNull
    @Override
    public final Maybe<Uri> invoke(@NonNull final Writer writer) {
        final ContentValues values = new ContentValues();
        writer.write(Insert, writable(values));
        if ((values.size() <= 0) && Log.isLoggable(TAG, INFO)) {
            Log.i(TAG, "An empty row will be written"); //NON-NLS
        }

        final Uri uri = mResolver.insert(mUri, values);
        return (uri == null) ? Maybes.<Uri>nothing() : something(uri);
    }
}
