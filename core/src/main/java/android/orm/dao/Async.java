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

package android.orm.dao;

import android.orm.Access;
import android.orm.DAO;
import android.orm.dao.async.ExecutionContext;
import android.orm.sql.Expression;
import android.orm.sql.Statement;
import android.orm.util.Maybe;
import android.orm.util.ObjectPool;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.concurrent.ExecutorService;

import static android.orm.dao.async.Executors.create;
import static android.orm.util.Maybes.nothing;

public class Async implements DAO.Async {

    @NonNull
    private final DAO.Direct mDirectDAO;
    @NonNull
    private final ExecutionContext mExecutionContext;

    public Async(@NonNull final DAO.Direct dao, @NonNull final ExecutorService executor) {
        super();

        mDirectDAO = dao;
        mExecutionContext = new ExecutionContext(executor);
    }

    @Override
    public final void setErrorHandler(@Nullable final ErrorHandler handler) {
        mExecutionContext.setErrorHandler(handler);
    }

    @NonNull
    @Override
    public final <K> Access.Async.Single<K> access(@NonNull final Executor.Direct.Single.Factory<? super DAO.Direct, K> factory) {
        final Executor.Async.Single<K> executor = create(mExecutionContext, factory.create(mDirectDAO));
        return new android.orm.dao.async.Access.Single<>(executor);
    }

    @NonNull
    @Override
    public final <K> Access.Async.Many<K> access(@NonNull final Executor.Direct.Many.Factory<? super DAO.Direct, K> factory) {
        final Executor.Async.Many<K> executor = create(mExecutionContext, factory.create(mDirectDAO));
        return new android.orm.dao.async.Access.Many<>(executor);
    }

    @NonNull
    @Override
    public final Result<Void> execute(@NonNull final Statement statement) {
        final Task.Statement task = Task.Statement.Pool.borrow();
        task.init(mDirectDAO, statement);
        return mExecutionContext.execute(task);
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public final <V> Result<V> execute(@NonNull final Expression<V> expression) {
        final Task.Expression task = Task.Expression.Pool.borrow();
        task.init(mDirectDAO, expression);
        return (Result<V>) (Object) mExecutionContext.execute(task);
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public final <V> Result<V> execute(@NonNull final Transaction.Direct<V> transaction) {
        final Task.Transaction task = Task.Transaction.Pool.borrow();
        task.init(mDirectDAO, transaction);
        return (Result<V>) (Object) mExecutionContext.execute(task);
    }

    private static final class Task {

        public static class Statement implements ExecutionContext.Task<Void> {

            public static final ObjectPool<Statement> Pool = new ObjectPool<Statement>() {
                @NonNull
                @Override
                protected Statement produce(@NonNull final Receipt<Statement> receipt) {
                    return new Statement(receipt);
                }
            };

            @NonNull
            private final ObjectPool.Receipt<Statement> mReceipt;

            private DAO.Direct mDAO;
            private android.orm.sql.Statement mStatement;

            private Statement(@NonNull final ObjectPool.Receipt<Statement> receipt) {
                super();

                mReceipt = receipt;
            }

            public final void init(@NonNull final DAO.Direct dao,
                                   @NonNull final android.orm.sql.Statement statement) {
                mDAO = dao;
                mStatement = statement;
            }

            @NonNull
            @Override
            public final Maybe<Void> run() {
                try {
                    mDAO.execute(mStatement);
                } finally {
                    mDAO = null;
                    mStatement = null;
                    mReceipt.yield();
                }

                return nothing();
            }
        }

        public static class Expression implements ExecutionContext.Task<Object> {

            public static final ObjectPool<Expression> Pool = new ObjectPool<Expression>() {
                @NonNull
                @Override
                protected Expression produce(@NonNull final Receipt<Expression> receipt) {
                    return new Expression(receipt);
                }
            };

            @NonNull
            private final ObjectPool.Receipt<Expression> mReceipt;

            private DAO.Direct mDAO;
            private android.orm.sql.Expression<Object> mExpression;

            private Expression(@NonNull final ObjectPool.Receipt<Expression> receipt) {
                super();

                mReceipt = receipt;
            }

            @SuppressWarnings("unchecked")
            public final void init(@NonNull final DAO.Direct dao,
                                   @NonNull final android.orm.sql.Expression<?> expression) {
                mDAO = dao;
                mExpression = (android.orm.sql.Expression<Object>) expression;
            }

            @NonNull
            @Override
            public final Maybe<Object> run() {
                final Maybe<Object> result;

                try {
                    result = mDAO.execute(mExpression);
                } finally {
                    mDAO = null;
                    mExpression = null;
                    mReceipt.yield();
                }

                return result;
            }
        }

        public static class Transaction implements ExecutionContext.Task<Object> {

            public static final ObjectPool<Transaction> Pool = new ObjectPool<Transaction>() {
                @NonNull
                @Override
                protected Transaction produce(@NonNull final Receipt<Transaction> receipt) {
                    return new Transaction(receipt);
                }
            };

            @NonNull
            private final ObjectPool.Receipt<Transaction> mReceipt;

            private DAO.Direct mDAO;
            private android.orm.dao.Transaction.Direct<Object> mTransaction;

            private Transaction(@NonNull final ObjectPool.Receipt<Transaction> receipt) {
                super();

                mReceipt = receipt;
            }

            @SuppressWarnings("unchecked")
            public final void init(@NonNull final DAO.Direct dao,
                                   @NonNull final android.orm.dao.Transaction.Direct<?> transaction) {
                mDAO = dao;
                mTransaction = (android.orm.dao.Transaction.Direct<Object>) transaction;
            }

            @NonNull
            @Override
            public final Maybe<Object> run() {
                final Maybe<Object> result;

                try {
                    result = mDAO.execute(mTransaction);
                } finally {
                    mDAO = null;
                    mTransaction = null;
                    mReceipt.yield();
                }

                return result;
            }
        }

        private Task() {
            super();
        }
    }
}
