/*
 * Anarres C Preprocessor
 * Copyright (c) 2007-2015, Shevek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.homo.superresolution.thirdparty.jcpp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.Path;

/**
 * An ant task for jcpp.
 */
public class CppTask extends Copy {

    private class Listener extends DefaultPreprocessorListener {

        @Override
        protected void print(String msg) {
            log(msg);
        }
    }

    public static class Macro {

        private String name;
        private String value;

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private final Listener listener = new Listener();
    private final List<Macro> macros = new ArrayList<Macro>();
    private Path systemincludepath;
    private Path localincludepath;

    public void addMacro(Macro macro) {
        macros.add(macro);
    }

    public void addSystemincludepath(Path path) {
        if (systemincludepath == null)
            systemincludepath = new Path(getProject());
        systemincludepath.add(path);
    }

    public void addLocalincludepath(Path path) {
        if (localincludepath == null)
            localincludepath = new Path(getProject());
        localincludepath.add(path);
    }

    /*
     public void execute() {
     FileWriter writer = null;
     try {
     if (input == null)
     throw new BuildException("Input not specified");
     if (output == null)
     throw new BuildException("Output not specified");
     cpp.addInput(this.input);
     writer = new FileWriter(this.output);
     for (;;) {
     Token	tok = cpp.token();
     if (tok != null && tok.getType() == Token.EOF)
     break;
     writer.write(tok.getText());
     }
     }
     catch (Exception e) {
     throw new BuildException(e);
     }
     finally {
     if (writer != null) {
     try {
     writer.close();
     }
     catch (IOException e) {
     }
     }
     }
     }
     */
    private void preprocess(File input, File output) throws Exception {
        if (input == null)
            throw new BuildException("Input not specified");
        if (output == null)
            throw new BuildException("Output not specified");

        Preprocessor cpp = new Preprocessor();
        cpp.setListener(listener);
        for (Macro macro : macros)
            cpp.addMacro(macro.getName(), macro.getValue());
        if (systemincludepath != null)
            cpp.setSystemIncludePath(Arrays.asList(systemincludepath.list()));
        if (localincludepath != null)
            cpp.setQuoteIncludePath(Arrays.asList(localincludepath.list()));

        File dir = output.getParentFile();
        if (!dir.exists()) {
            if (!dir.mkdirs())
                throw new BuildException("Failed to make parent directory " + dir);
        } else if (!dir.isDirectory()) {
            throw new BuildException("Parent directory of output file " + output + " exists, but is not a directory.");
        }
        FileWriter writer = null;
        try {
            cpp.addInput(input);
            writer = new FileWriter(output);
            for (;;) {
                Token tok = cpp.token();
                if (tok == null)
                    break;
                if (tok.getType() == Token.EOF)
                    break;
                writer.write(tok.getText());
            }
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                }
            }
        }
    }

    @Override
    protected void doFileOperations() {
        if (fileCopyMap.size() > 0) {
            log("Copying " + fileCopyMap.size()
                    + " file" + (fileCopyMap.size() == 1 ? "" : "s")
                    + " to " + destDir.getAbsolutePath());

            Enumeration<String> e = fileCopyMap.keys();

            while (e.hasMoreElements()) {
                String fromFile = e.nextElement();
                String[] toFiles = (String[]) fileCopyMap.get(fromFile);

                for (String toFile : toFiles) {
                    if (fromFile.equals(toFile)) {
                        log("Skipping self-copy of " + fromFile, verbosity);
                        continue;
                    }

                    try {
                        log("Copying " + fromFile + " to " + toFile, verbosity);

                        /*
                         FilterSetCollection executionFilters
                         = new FilterSetCollection();
                         if (filtering) {
                         executionFilters
                         .addFilterSet(getProject().getGlobalFilterSet());
                         }
                         for (Enumeration filterEnum = getFilterSets().elements();
                         filterEnum.hasMoreElements();) {
                         executionFilters
                         .addFilterSet((FilterSet) filterEnum.nextElement());
                         }
                         */
                        File srcFile = new File(fromFile);
                        File dstFile = new File(toFile);
                        preprocess(srcFile, dstFile);
                    } catch (Exception ioe) {
                        // ioe.printStackTrace();
                        String msg = "Failed to copy " + fromFile + " to " + toFile
                                + " due to " + ioe.getMessage();
                        File targetFile = new File(toFile);
                        if (targetFile.exists() && !targetFile.delete()) {
                            msg += " and I couldn't delete the corrupt " + toFile;
                        }
                        throw new BuildException(msg, ioe, getLocation());
                    }
                }
            }
        }

    }

}
