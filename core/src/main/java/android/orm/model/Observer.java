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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public final class Observer {

    public static void beforeRead(@Nullable final Object model) {
        if (model != null) {
            if (model instanceof Read) {
                ((Read) model).beforeRead();
            }

            if (model instanceof Iterable<?>) {
                for (final Object element : (Iterable<?>) model) {
                    beforeRead(element);
                }
            } else if (model.getClass().isArray()) {
                for (final Object element : (Object[]) model) {
                    beforeRead(element);
                }
            }
        }
    }

    public static void afterRead(@Nullable final Object model) {
        if (model != null) {
            if (model instanceof Read) {
                ((Read) model).afterRead();
            }

            if (model instanceof Iterable<?>) {
                for (final Object element : (Iterable<?>) model) {
                    afterRead(element);
                }
            } else if (model.getClass().isArray()) {
                for (final Object element : (Object[]) model) {
                    afterRead(element);
                }
            }
        }
    }

    public static void beforeInsert(@Nullable final Object model) {
        if (model != null) {
            if (model instanceof Write) {
                ((Write) model).beforeInsert();
                ((Write) model).beforeSave();
            }

            if (model instanceof Iterable<?>) {
                for (final Object element : (Iterable<?>) model) {
                    beforeInsert(element);
                }
            } else if (model.getClass().isArray()) {
                for (final Object element : (Object[]) model) {
                    beforeInsert(element);
                }
            }
        }
    }

    public static void afterInsert(@Nullable final Object model) {
        if (model != null) {
            if (model instanceof Write) {
                ((Write) model).afterSave();
                ((Write) model).afterInsert();
            }

            if (model instanceof Iterable<?>) {
                for (final Object element : (Iterable<?>) model) {
                    afterInsert(element);
                }
            } else if (model.getClass().isArray()) {
                for (final Object element : (Object[]) model) {
                    afterInsert(element);
                }
            }
        }
    }

    public static void beforeUpdate(@Nullable final Object model) {
        if (model != null) {
            if (model instanceof Write) {
                ((Write) model).beforeUpdate();
                ((Write) model).beforeSave();
            }

            if (model instanceof Iterable<?>) {
                for (final Object element : (Iterable<?>) model) {
                    beforeUpdate(element);
                }
            } else if (model.getClass().isArray()) {
                for (final Object element : (Object[]) model) {
                    beforeUpdate(element);
                }
            }
        }
    }

    public static void afterUpdate(@Nullable final Object model) {
        if (model != null) {
            if (model instanceof Write) {
                ((Write) model).afterSave();
                ((Write) model).afterUpdate();
            }

            if (model instanceof Iterable<?>) {
                for (final Object element : (Iterable<?>) model) {
                    afterUpdate(element);
                }
            } else if (model.getClass().isArray()) {
                for (final Object element : (Object[]) model) {
                    afterUpdate(element);
                }
            }
        }
    }

    public interface Read {

        void beforeRead();

        void afterRead();

        Read DUMMY = new Base() {
            /* ignore everything */
        };

        abstract class Base implements Read {

            @Override
            public void beforeRead() {/* do nothing */}

            @Override
            public void afterRead() {/* do nothing */}
        }
    }

    public interface Write {

        void beforeInsert();

        void afterInsert();

        void beforeUpdate();

        void afterUpdate();

        void beforeSave();

        void afterSave();

        Write DUMMY = new Base() {
            /* ignore everything */
        };

        abstract class Base implements Write {

            @Override
            public void beforeInsert() {/* do nothing */}

            @Override
            public void afterInsert() {/* do nothing */}

            @Override
            public void beforeUpdate() {/* do nothing */}

            @Override
            public void afterUpdate() {/* do nothing */}

            @Override
            public void beforeSave() {/* do nothing */}

            @Override
            public void afterSave() {/* do nothing */}
        }
    }

    public interface ReadWrite extends Read, Write {

        ReadWrite DUMMY = new Base() {
            /* ignore everything */
        };

        abstract class Base implements ReadWrite {

            @Override
            public void beforeRead() {/* do nothing */}

            @Override
            public void afterRead() {/* do nothing */}

            @Override
            public void beforeInsert() {/* do nothing */}

            @Override
            public void afterInsert() {/* do nothing */}

            @Override
            public void beforeUpdate() {/* do nothing */}

            @Override
            public void afterUpdate() {/* do nothing */}

            @Override
            public void beforeSave() {/* do nothing */}

            @Override
            public void afterSave() {/* do nothing */}
        }
    }

    @NonNull
    public static Read read(@NonNull final Iterable<Read> observers) {
        return new Read() {

            @Override
            public void beforeRead() {
                for (final Read observer : observers) {
                    observer.beforeRead();
                }
            }

            @Override
            public void afterRead() {
                for (final Read observer : observers) {
                    observer.afterRead();
                }
            }
        };
    }

    @NonNull
    public static Write write(@NonNull final Iterable<Write> observers) {
        return new Write() {

            @Override
            public void beforeInsert() {
                for (final Write observer : observers) {
                    observer.beforeInsert();
                }
            }

            @Override
            public void afterInsert() {
                for (final Write observer : observers) {
                    observer.afterInsert();
                }
            }

            @Override
            public void beforeUpdate() {
                for (final Write observer : observers) {
                    observer.beforeUpdate();
                }
            }

            @Override
            public void afterUpdate() {
                for (final Write observer : observers) {
                    observer.afterUpdate();
                }
            }

            @Override
            public void beforeSave() {
                for (final Write observer : observers) {
                    observer.beforeSave();
                }
            }

            @Override
            public void afterSave() {
                for (final Write observer : observers) {
                    observer.afterSave();
                }
            }
        };
    }

    @NonNull
    public static ReadWrite combine(@NonNull final Read read, @NonNull final Write write) {
        return new ReadWrite() {

            @Override
            public void beforeRead() {
                read.beforeRead();
            }

            @Override
            public void afterRead() {
                read.afterRead();
            }

            @Override
            public void beforeInsert() {
                write.beforeInsert();
            }

            @Override
            public void afterInsert() {
                write.afterInsert();
            }

            @Override
            public void beforeUpdate() {
                write.beforeUpdate();
            }

            @Override
            public void afterUpdate() {
                write.afterUpdate();
            }

            @Override
            public void beforeSave() {
                write.beforeSave();
            }

            @Override
            public void afterSave() {
                write.afterSave();
            }
        };
    }

    private Observer() {
        super();
    }
}
