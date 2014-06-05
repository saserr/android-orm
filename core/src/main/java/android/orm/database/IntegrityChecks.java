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

package android.orm.database;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.NonNull;

import org.jetbrains.annotations.NonNls;

public final class IntegrityChecks {

    public static final IntegrityCheck None = new IntegrityCheck() {

        @Override
        public void check(@NonNull final SQLiteDatabase database) {
            /* do nothing */
        }

        @NonNull
        @Override
        public IntegrityCheck and(@NonNull final IntegrityCheck other) {
            return other;
        }
    };

    public static final IntegrityCheck ForeignKey = new IntegrityCheck.Base() {

        @NonNls
        private static final String PRAGMA = "pragma foreign_key_check";

        @Override
        public void check(@NonNull final SQLiteDatabase database) {
            final Cursor cursor = database.rawQuery(PRAGMA, null);
            try {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    @NonNls final String referencingTableName = cursor.getString(0);
                    final long referencingRowId = cursor.getLong(1);
                    @NonNls final String referencedTableName = cursor.getString(2);
                    final long referenceId = cursor.getLong(3);
                    throw new SQLException("Row with id " + referencingRowId + " in table " +
                            referencingTableName + " references unknown data in table " +
                            referencedTableName + " based on the foreign key check with id " +
                            referenceId);
                }
            } finally {
                cursor.close();
            }
        }
    };

    public static final IntegrityCheck QuickData = new IntegrityCheck.Base() {

        @NonNls
        private static final String PRAGMA = "pragma quick_check(1);";
        @NonNls
        private static final String OK = "ok";

        @Override
        public void check(@NonNull final SQLiteDatabase database) {
            SQLiteStatement statement = null;
            try {
                statement = database.compileStatement(PRAGMA);
                @NonNls final String result = statement.simpleQueryForString();
                if (!result.equalsIgnoreCase(OK)) {
                    throw new SQLException("'pragma quick_check' failed with the error '" + result + '\'');
                }
            } finally {
                if (statement != null) {
                    statement.close();
                }
            }
        }
    };

    public static final IntegrityCheck Quick = QuickData.and(ForeignKey);

    public static final IntegrityCheck FullData = new IntegrityCheck.Base() {

        @NonNls
        private static final String PRAGMA = "pragma integrity_check(1);";
        @NonNls
        private static final String OK = "ok";

        @Override
        public void check(@NonNull final SQLiteDatabase database) {
            SQLiteStatement statement = null;
            try {
                statement = database.compileStatement(PRAGMA);
                @NonNls final String result = statement.simpleQueryForString();
                if (!result.equalsIgnoreCase(OK)) {
                    throw new SQLException("'pragma integrity_check' failed with the error '" + result + '\'');
                }
            } finally {
                if (statement != null) {
                    statement.close();
                }
            }
        }
    };

    public static final IntegrityCheck Full = FullData.and(ForeignKey);

    @NonNull
    public static IntegrityCheck compose(@NonNull final IntegrityCheck first,
                                         @NonNull final IntegrityCheck second) {
        return new Composition(first, second);
    }

    private static class Composition extends IntegrityCheck.Base {

        @NonNull
        private final IntegrityCheck mFirst;
        @NonNull
        private final IntegrityCheck mSecond;

        private Composition(@NonNull final IntegrityCheck first,
                            @NonNull final IntegrityCheck second) {
            super();

            mFirst = first;
            mSecond = second;
        }

        @Override
        public final void check(@NonNull final SQLiteDatabase database) {
            mFirst.check(database);
            mSecond.check(database);
        }
    }
}
