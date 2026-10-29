/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.thirdparty.jcpp;

/*
 * Anarres C Preprocessor
 * Copyright (c) 2007-2015, Shevek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A handler for preprocessor events, primarily errors and warnings.
 *
 * If no PreprocessorListener is installed in a Preprocessor, all
 * error and warning events will throw an exception. Installing a
 * listener allows more intelligent handling of these events.
 */
public class DefaultPreprocessorListener implements PreprocessorListener {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultPreprocessorListener.class);

    private int errors;
    private int warnings;

    public DefaultPreprocessorListener() {
        clear();
    }

    public void clear() {
        errors = 0;
        warnings = 0;
    }

    @Nonnegative
    public int getErrors() {
        return errors;
    }

    @Nonnegative
    public int getWarnings() {
        return warnings;
    }

    protected void print(@Nonnull String msg) {
        LOG.info(msg);
    }

    /**
     * Handles a warning.
     *
     * The behaviour of this method is defined by the
     * implementation. It may simply record the error message, or
     * it may throw an exception.
     */
    @Override
    public void handleWarning(Source source, int line, int column,
            String msg)
            throws LexerException {
        warnings++;
        print(source.getName() + ":" + line + ":" + column
                + ": warning: " + msg);
    }

    /**
     * Handles an error.
     *
     * The behaviour of this method is defined by the
     * implementation. It may simply record the error message, or
     * it may throw an exception.
     */
    @Override
    public void handleError(Source source, int line, int column,
            String msg)
            throws LexerException {
        errors++;
        print(source.getName() + ":" + line + ":" + column
                + ": error: " + msg);
    }

    @Override
    public void handleSourceChange(Source source, SourceChangeEvent event) {
    }

}
