package io.homo.superresolution.common.render.gl.shader;


import java.util.ArrayList;
import java.util.Collection;

public class ShaderInclude {
    public String name;
    public ArrayList<String> textList;

    private ShaderInclude() {}

    public static ShaderInclude create(Collection<String> textList, String name) {
        ShaderInclude i = new ShaderInclude();
        i.textList = new ArrayList<>();
        i.textList.addAll(textList);
        i.name = name;
        return i;
    }

}
