/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
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

package io.homo.superresolution.common.minecraft.handler.shadercompat;

import io.homo.superresolution.thirdparty.jcpp.Feature;
import io.homo.superresolution.thirdparty.jcpp.LexerException;
import io.homo.superresolution.thirdparty.jcpp.Preprocessor;
import io.homo.superresolution.thirdparty.jcpp.PreprocessorCommand;
import io.homo.superresolution.thirdparty.jcpp.StringLexerSource;
import io.homo.superresolution.thirdparty.jcpp.Token;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonMacroPreprocessor {
    private final Map<String, String> macros;

    public JsonMacroPreprocessor() {
        this.macros = new HashMap<>();
    }

    public void addMacro(String name, String value) {
        macros.put(name, value);
    }

    public void removeMacro(String name) {
        macros.remove(name);
    }

    public Map<String, String> getMacros() {
        return Map.copyOf(macros);
    }

    public String process(String input) {
        String sanitized = sanitizeSource(input);

        try {
            Preprocessor pp = new Preprocessor();
            pp.addFeature(Feature.KEEPCOMMENTS);

            for (Map.Entry<String, String> entry : macros.entrySet()) {
                pp.addMacro(entry.getKey(), entry.getValue());
            }

            pp.addInput(new StringLexerSource(sanitized, true));

            StringBuilder result = new StringBuilder();
            for (;;) {
                Token tok = pp.token();
                if (tok == null || tok.getType() == Token.EOF) {
                    break;
                }
                result.append(tok.getText());
            }
            return result.toString();

        } catch (LexerException e) {
            throw new RuntimeException("预处理 JSON 文本时发生词法错误", e);
        } catch (Exception e) {
            throw new RuntimeException("预处理 JSON 文本失败", e);
        }
    }

    private static String sanitizeSource(String source) {
        return Arrays.stream(source.split("\\R"))
                .map(line -> {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("#")) {
                        for (PreprocessorCommand cmd : PreprocessorCommand.values()) {
                            String cmdName = cmd.name().replace("PP_", "");
                            if (trimmed.startsWith("#" + cmdName.toLowerCase(Locale.ROOT))) {
                                return line;
                            }
                        }
                        return "";
                    } else {
                        return line.replace("#", "");
                    }
                })
                .collect(Collectors.joining("\n")) + "\n";
    }
}
