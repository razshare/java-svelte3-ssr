/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.tncrazvan.svelte3ssr;

import java.nio.file.Path;
import java.util.HashMap;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

/**
 *
 * @author Administrator
 */
public class Svelte3SSR {
    private Context context;
    
    public Svelte3SSR(Path wk){
        context = Context
                    .newBuilder()
                    .currentWorkingDirectory(wk)
                    .allowAllAccess(true)
                    .allowIO(true)
                    .build();
        
        String tools = 
        "const System = Java.type(\"java.lang.System\");\n" +

        "load('./node_modules/jvm-npm/src/main/javascript/jvm-npm.js');\n" +

        "const { create_ssr_component } = require('./node_modules/svelte/internal/index.js');\n" +
        "const { compile } = require('./compiler.js');\n" +
        "console.log(\"JavaScript initialized.\");\n"
        ;
        
        context.eval("js",tools);
    }
    
    
    
    public Svelte3SSRResult render(String source){
        return render(source, new HashMap<>(){{}});
    }
    
    public Svelte3SSRResult render(String source,HashMap<String, Object> props){
        String compileScript = 
            "(function main(source){\n" +
            "   return compile(source, {\n" +
            "       generate: \"ssr\"," +
            "       format: \"cjs\"\n" +
            "   }).js.code;\n" +
            "});"
            ;
            Value compileValue = context.eval("js",compileScript);
            String compileResult = compileValue.execute(source).toString();
            compileResult = compileResult.replace("\"use strict\";", "");
            compileResult = compileResult.replace("const { create_ssr_component } = require(\"svelte/internal\");", "");
            compileResult = compileResult.replace("exports.default = Component;", "");
            
            String renderScript = 
            "\"use strict\";\n"
            + "(function (props){\n" +
                compileResult +
                "\nreturn Component.render({...props});\n" +
            "});";
            Value renderValue = context.eval("js",renderScript);
            
            Value ssrValue = renderValue.execute(ProxyObject.fromMap(props));
            Svelte3SSRResult ssr = new Svelte3SSRResult();
            ssr.html = ssrValue.getMember("html").asString();
            ssr.head = ssrValue.getMember("head").asString();
            ssr.css = ssrValue.getMember("css").getMember("code").asString();
            return ssr;
    }
}
