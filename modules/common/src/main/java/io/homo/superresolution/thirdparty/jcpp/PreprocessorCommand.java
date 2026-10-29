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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.homo.superresolution.thirdparty.jcpp;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 *
 * @author shevek
 */
public enum PreprocessorCommand {

    PP_DEFINE("define"),
    PP_ELIF("elif"),
    PP_ELSE("else"),
    PP_ENDIF("endif"),
    PP_ERROR("error"),
    PP_IF("if"),
    PP_IFDEF("ifdef"),
    PP_IFNDEF("ifndef"),
    PP_INCLUDE("include"),
    PP_LINE("line"),
    PP_PRAGMA("pragma"),
    PP_UNDEF("undef"),
    PP_WARNING("warning"),
    PP_INCLUDE_NEXT("include_next"),
    PP_IMPORT("import");
    private final String text;
    /* pp */ PreprocessorCommand(String text) {
        this.text = text;
    }

    @CheckForNull
    public static PreprocessorCommand forText(@Nonnull String text) {
        for (PreprocessorCommand ppcmd : PreprocessorCommand.values())
            if (ppcmd.text.equals(text))
                return ppcmd;
        return null;
    }
}
