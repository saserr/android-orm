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

package android.orm.model;

import android.support.annotation.Nullable;

public final class Observer {

    public interface Read {

        void beforeRead();

        void afterRead();

        abstract class Base implements Read {

            @Override
            public void beforeRead() {/* do nothing */}

            @Override
            public void afterRead() {/* do nothing */}
        }

        Read DUMMY = new Base() {
            /* ignore everything */
        };
    }

    public interface Write {

        void beforeCreate();

        void afterCreate();

        void beforeUpdate();

        void afterUpdate();

        void beforeSave();

        void afterSave();

        abstract class Base implements Write {

            @Override
            public void beforeCreate() {/* do nothing */}

            @Override
            public void afterCreate() {/* do nothing */}

            @Override
            public void beforeUpdate() {/* do nothing */}

            @Override
            public void afterUpdate() {/* do nothing */}

            @Override
            public void beforeSave() {/* do nothing */}

            @Override
            public void afterSave() {/* do nothing */}
        }

        Write DUMMY = new Base() {
            /* ignore everything */
        };
    }

    public interface ReadWrite extends Read, Write {

        abstract class Base implements ReadWrite {

            @Override
            public void beforeRead() {/* do nothing */}

            @Override
            public void afterRead() {/* do nothing */}

            @Override
            public void beforeCreate() {/* do nothing */}

            @Override
            public void afterCreate() {/* do nothing */}

            @Override
            public void beforeUpdate() {/* do nothing */}

            @Override
            public void afterUpdate() {/* do nothing */}

            @Override
            public void beforeSave() {/* do nothing */}

            @Override
            public void afterSave() {/* do nothing */}
        }

        ReadWrite DUMMY = new Base() {
            /* ignore everything */
        };
    }

    public static <M> void beforeRead(@Nullable final M model) {
        if (model instanceof Read) {
            ((Read) model).beforeRead();
        }
    }

    public static <M> void afterRead(@Nullable final M model) {
        if (model instanceof Read) {
            ((Read) model).afterRead();
        }
    }

    public static <M> void beforeCreate(@Nullable final M model) {
        if (model instanceof Write) {
            ((Write) model).beforeCreate();
            ((Write) model).beforeSave();
        }
    }

    public static <M> void afterCreate(@Nullable final M model) {
        if (model instanceof Write) {
            ((Write) model).afterSave();
            ((Write) model).afterCreate();
        }
    }

    public static <M> void beforeUpdate(@Nullable final M model) {
        if (model instanceof Write) {
            ((Write) model).beforeUpdate();
            ((Write) model).beforeSave();
        }
    }

    public static <M> void afterUpdate(@Nullable final M model) {
        if (model instanceof Write) {
            ((Write) model).afterSave();
            ((Write) model).afterUpdate();
        }
    }

    private Observer() {
        super();
    }
}
