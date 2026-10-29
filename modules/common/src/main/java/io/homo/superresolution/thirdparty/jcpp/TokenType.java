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

import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import static io.homo.superresolution.thirdparty.jcpp.Token.*;

/**
 *
 * @author shevek
 */
/* pp */ class TokenType {

    private static final List<TokenType> TYPES = new ArrayList<TokenType>();

    private static void addTokenType(@Nonnegative int type, @Nonnull String name, @CheckForNull String text) {
        while (TYPES.size() <= type)
            TYPES.add(null);
        TYPES.set(type, new TokenType(name, text));
    }

    private static void addTokenType(@Nonnegative int type, @Nonnull String name) {
        addTokenType(type, name, null);
    }

    @CheckForNull
    public static TokenType getTokenType(@Nonnegative int type) {
        try {
            return TYPES.get(type);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    @Nonnull
    public static String getTokenName(@Nonnegative int type) {
        if (type < 0)
            return "Invalid" + type;
        TokenType tokenType = getTokenType(type);
        if (tokenType == null)
            return "Unknown" + type;
        return tokenType.getName();
    }

    @CheckForNull
    public static String getTokenText(@Nonnegative int type) {
        TokenType tokenType = getTokenType(type);
        if (tokenType == null)
            return null;
        return tokenType.getText();
    }

    static {
        for (int i = 0; i < 255; i++) {
            String text = String.valueOf((char) i);
            addTokenType(i, text, text);
        }
        addTokenType(AND_EQ, "AND_EQ", "&=");
        addTokenType(ARROW, "ARROW", "->");
        addTokenType(CHARACTER, "CHARACTER");
        addTokenType(CCOMMENT, "CCOMMENT");
        addTokenType(CPPCOMMENT, "CPPCOMMENT");
        addTokenType(DEC, "DEC", "--");
        addTokenType(DIV_EQ, "DIV_EQ", "/=");
        addTokenType(ELLIPSIS, "ELLIPSIS", "...");
        addTokenType(EOF, "EOF");
        addTokenType(EQ, "EQ", "==");
        addTokenType(GE, "GE", ">=");
        addTokenType(HASH, "HASH", "#");
        addTokenType(HEADER, "HEADER");
        addTokenType(IDENTIFIER, "IDENTIFIER");
        addTokenType(INC, "INC", "++");
        addTokenType(NUMBER, "NUMBER");
        addTokenType(LAND, "LAND", "&&");
        addTokenType(LAND_EQ, "LAND_EQ", "&&=");
        addTokenType(LE, "LE", "<=");
        addTokenType(LITERAL, "LITERAL");
        addTokenType(LOR, "LOR", "||");
        addTokenType(LOR_EQ, "LOR_EQ", "||=");
        addTokenType(LSH, "LSH", "<<");
        addTokenType(LSH_EQ, "LSH_EQ", "<<=");
        addTokenType(MOD_EQ, "MOD_EQ", "%=");
        addTokenType(MULT_EQ, "MULT_EQ", "*=");
        addTokenType(NE, "NE", "!=");
        addTokenType(NL, "NL");
        addTokenType(OR_EQ, "OR_EQ", "|=");
        addTokenType(PASTE, "PASTE", "##");
        addTokenType(PLUS_EQ, "PLUS_EQ", "+=");
        addTokenType(RANGE, "RANGE", "..");
        addTokenType(RSH, "RSH", ">>");
        addTokenType(RSH_EQ, "RSH_EQ", ">>=");
        addTokenType(SQSTRING, "SQSTRING");
        addTokenType(STRING, "STRING");
        addTokenType(SUB_EQ, "SUB_EQ", "-=");
        addTokenType(WHITESPACE, "WHITESPACE");
        addTokenType(XOR_EQ, "XOR_EQ", "^=");
        addTokenType(M_ARG, "M_ARG");
        addTokenType(M_PASTE, "M_PASTE");
        addTokenType(M_STRING, "M_STRING");
        addTokenType(P_LINE, "P_LINE");
        addTokenType(INVALID, "INVALID");
    }

    private final String name;
    private final String text;

    /* pp */ TokenType(@Nonnull String name, @CheckForNull String text) {
        this.name = name;
        this.text = text;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    @CheckForNull
    public String getText() {
        return text;
    }
}
