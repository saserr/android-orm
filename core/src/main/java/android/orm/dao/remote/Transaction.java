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
import android.content.ContentValues;
import android.net.Uri;
import android.orm.DAO;
import android.orm.Route;
import android.orm.dao.Result;
import android.orm.model.Plan;
import android.orm.sql.Select;
import android.orm.sql.Writer;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static android.orm.model.Observer.beforeCreate;
import static android.orm.model.Observer.beforeUpdate;
import static android.orm.sql.Value.Write.Operation.Insert;
import static android.orm.sql.Value.Write.Operation.Update;
import static android.orm.sql.Writables.writable;

public class Transaction implements android.orm.dao.Transaction.Remote {

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

    @Override
    @NonNull
    public final android.orm.dao.Transaction.Remote.Access at(@NonNull final Route route, @NonNull final Object... arguments) {
        return new Access(route, arguments);
    }

    @NonNull
    @Override
    public final Result<android.orm.dao.Transaction.Remote.CommitResult> commit() {
        final Result<android.orm.dao.Transaction.Remote.CommitResult> result = ((mAuthority == null) || mBatch.isEmpty()) ?
                Result.<android.orm.dao.Transaction.Remote.CommitResult>nothing() :
                mExecutor.execute(mAuthority, Collections.unmodifiableCollection(mBatch));
        mAuthority = null;
        mBatch = new ArrayList<>();
        return result;
    }

    public interface Executor {
        @NonNull
        Result<android.orm.dao.Transaction.Remote.CommitResult> execute(@NonNull final String authority,
                                                                        @NonNull final Collection<Producer<ContentProviderOperation>> batch);
    }

    private class Access extends DAO.Access.Write.Base<android.orm.dao.Transaction.Remote.Access, android.orm.dao.Transaction.Remote.Access, android.orm.dao.Transaction.Remote.Access> implements android.orm.dao.Transaction.Remote.Access {

        @NonNull
        private final Uri mUri;

        private Access(@NonNull final Route route, @NonNull final Object... arguments) {
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
        protected final <M> android.orm.dao.Transaction.Remote.Access insert(@Nullable final M model,
                                                                             @NonNull final Plan.Write plan) {
            beforeCreate(model);
            // TODO invoke afterCreate somehow
            if (!plan.isEmpty()) {
                mBatch.add(new Insert(mUri, plan));
            }
            return this;
        }

        @NonNull
        @Override
        protected final <M> android.orm.dao.Transaction.Remote.Access update(@NonNull final Select.Where where,
                                                                             @Nullable final M model,
                                                                             @NonNull final Plan.Write plan) {
            beforeUpdate(model);
            // TODO invoke afterUpdate somehow
            if (!plan.isEmpty()) {
                mBatch.add(new Update(mUri, plan, where));
            }
            return this;
        }

        @NonNull
        @Override
        public final android.orm.dao.Transaction.Remote.Access delete(@NonNull final Select.Where where) {
            mBatch.add(new Delete(mUri, where));
            return this;
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
