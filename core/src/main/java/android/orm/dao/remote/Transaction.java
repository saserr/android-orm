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

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.net.Uri;
import android.orm.Route;
import android.orm.model.Plan;
import android.orm.sql.Writer;
import android.orm.sql.statement.Select;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static android.orm.sql.Value.Write.Operation.Insert;
import static android.orm.sql.Value.Write.Operation.Update;
import static android.orm.sql.Writables.writable;
import static java.lang.System.arraycopy;

public class Transaction {

    @NonNull
    private final Executor mExecutor;

    @NonNls
    @Nullable
    private String mAuthority;
    @NonNull
    private Collection<Producer<ContentProviderOperation>> mBatch = new ArrayList<>();

    public Transaction(@NonNull final Executor executor) {
        super();

        mExecutor = executor;
    }

    @NonNull
    public final Access at(@NonNull final Route route, @NonNull final Object... arguments) {
        return new WriteAccess(route, arguments);
    }

    public final android.orm.access.Result<Result> commit() {
        final android.orm.access.Result<Result> result = ((mAuthority == null) || mBatch.isEmpty()) ?
                android.orm.access.Result.<Result>nothing() :
                mExecutor.execute(mAuthority, Collections.unmodifiableCollection(mBatch));
        mAuthority = null;
        mBatch = new ArrayList<>();
        return result;
    }

    public interface Access extends android.orm.Access.Write<Access, Access, Access> {
    }

    public interface Executor {
        @NonNull
        android.orm.access.Result<Result> execute(@NonNull final String authority,
                                                  @NonNull final Collection<Producer<ContentProviderOperation>> batch);
    }

    private class WriteAccess extends android.orm.Access.Write.Base<Access, Access, Access> implements Access {

        @NonNull
        private final Uri mUri;

        private WriteAccess(@NonNull final Route route, @NonNull final Object... arguments) {
            super();

            mUri = route.createUri(arguments);
            final String authority = mUri.getAuthority();
            if ((mAuthority != null) && !mAuthority.equals(authority)) {
                throw new IllegalArgumentException(
                        "Different authority " + authority + "! Expected " + mAuthority +
                                ". Transaction between different authorities is not supported, because underlying Android API doesn't support it."
                );
            }

            if (mAuthority == null) {
                mAuthority = authority;
            }
        }

        @NonNull
        @Override
        protected final <M> Access insert(@NonNull final M model,
                                          @NonNull final Plan.Write plan) {
            // TODO invoke afterCreate somehow
            if (!plan.isEmpty()) {
                mBatch.add(new Insert(mUri, plan));
            }
            return this;
        }

        @NonNull
        @Override
        protected final <M> Access update(@NonNull final M model,
                                          @NonNull final Select.Where where,
                                          @NonNull final Plan.Write plan) {
            // TODO invoke afterUpdate somehow
            if (!plan.isEmpty()) {
                mBatch.add(new Update(mUri, plan, where));
            }
            return this;
        }

        @NonNull
        @Override
        public final Access delete(@NonNull final Select.Where where) {
            mBatch.add(new Delete(mUri, where));
            return this;
        }
    }

    public static class Result {

        @NonNull
        private final String mAuthority;
        @NonNull
        private final ContentProviderResult[] mResults;

        public Result(@NonNull final String authority,
                      @NonNull final ContentProviderResult... results) {
            super();

            mAuthority = authority;
            mResults = new ContentProviderResult[results.length];
            arraycopy(results, 0, mResults, 0, mResults.length);
        }

        @NonNull
        public final String getAuthority() {
            return mAuthority;
        }

        @NonNull
        public final ContentProviderResult[] getResults() {
            final ContentProviderResult[] results = new ContentProviderResult[mResults.length];
            arraycopy(mResults, 0, results, 0, results.length);
            return results;
        }
    }

    private static class Insert implements Producer<ContentProviderOperation> {

        @NonNull
        private final Uri mUri;
        @NonNull
        private final Writer mWriter;

        private Insert(@NonNull final Uri uri, @NonNull final Writer writer) {
            super();

            mUri = uri;
            mWriter = writer;
        }

        @NonNull
        @Override
        public final ContentProviderOperation produce() {
            final ContentValues values = new ContentValues();
            mWriter.write(Insert, writable(values));
            return ContentProviderOperation.newInsert(mUri).withValues(values).build();
        }
    }

    private static class Update implements Producer<ContentProviderOperation> {

        @NonNull
        private final Uri mUri;
        @NonNull
        private final Writer mWriter;
        @NonNull
        private final Select.Where mWhere;

        private Update(@NonNull final Uri uri,
                       @NonNull final Writer writer,
                       @NonNull final Select.Where where) {
            super();

            mUri = uri;
            mWriter = writer;
            mWhere = where;
        }

        @NonNull
        @Override
        public final ContentProviderOperation produce() {
            final ContentValues values = new ContentValues();
            mWriter.write(Update, writable(values));
            return ContentProviderOperation.newUpdate(mUri)
                    .withSelection(mWhere.toSQL(), null)
                    .withValues(values)
                    .build();
        }
    }

    private static class Delete implements Producer<ContentProviderOperation> {

        @NonNull
        private final Uri mUri;
        @NonNull
        private final Select.Where mWhere;

        private Delete(@NonNull final Uri uri, @NonNull final Select.Where where) {
            super();

            mUri = uri;
            mWhere = where;
        }

        @NonNull
        @Override
        public final ContentProviderOperation produce() {
            return ContentProviderOperation.newDelete(mUri)
                    .withSelection(mWhere.toSQL(), null)
                    .build();
        }
    }
}
