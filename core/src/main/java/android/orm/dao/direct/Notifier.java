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

package android.orm.dao.direct;

import android.content.ContentResolver;
import android.net.Uri;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public interface Notifier {

    void notifyChange(@NonNull final Uri uri);

    class Immediate implements Notifier {

        @NonNull
        private final ContentResolver mResolver;

        public Immediate(@NonNull final ContentResolver resolver) {
            super();

            mResolver = resolver;
        }

        @Override
        public final void notifyChange(@NonNull final Uri uri) {
            mResolver.notifyChange(uri, null);
        }
    }

    class Delayed implements Notifier {

        @NonNull
        private final ContentResolver mResolver;

        private final List<Uri> mUris = new ArrayList<>();

        public Delayed(@NonNull final ContentResolver resolver) {
            super();

            mResolver = resolver;
        }

        @Override
        public final void notifyChange(@NonNull final Uri uri) {
            mUris.add(uri);
        }

        public final void sendAll() {
            for (final Uri uri : mUris) {
                mResolver.notifyChange(uri, null);
            }
        }
    }
}
