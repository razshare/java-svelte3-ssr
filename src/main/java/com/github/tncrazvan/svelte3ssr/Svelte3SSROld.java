/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.tncrazvan.svelte3ssr;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

/**
 *
 * @author Administrator
 */
public class Svelte3SSROld {
    protected Context context;
    
    public Svelte3SSROld(Path wk){
        context = Context
                    .newBuilder()
                    .currentWorkingDirectory(wk)
                    .allowAllAccess(true)
                    .allowIO(true)
                    .build();
        
        String tools = 
        "const System = Java.type('java.lang.System');" +
        "const FileReaderJS = Java.type('com.github.tncrazvan.svelte3ssr.FileReaderJS');" +
        "load('./node_modules/jvm-npm/src/main/javascript/jvm-npm.js');" +
        "const { compile } = require('./compiler.js');" +
        "const { component } = require('./compile.js');" 
        ;
        
        context.eval("js",tools);
        
        Value ssr = context.eval("js", "(function(service){ssr=service;});");
        ssr.executeVoid(this);
        
        context.eval("js","console.log('JavaScript initialized.');");
    }
    
    public Context getContext(){
        return context;
    }
    
    public String compileFile(String filename,String charset) throws IOException{
        return compile(Files.readString(Path.of(filename), Charset.forName(charset)));
    }
    
    public String compile(String source){
        Value app = context.eval("js", "(function(source){return compile(source,{generate:'ssr',format:'cjs'}).js.code;});");
        String result = app.execute(source).asString();
        return result;
    }
    
    public Value render(String compiledSource){
        return render(compiledSource, new HashMap<>());
    }
    
    public Value render(String compiledSource, HashMap<String,Object> props){
        compiledSource = compiledSource.replace("\"use strict\";","\"use strict\";(function (){\nconst exports = {};\n")+"\nreturn exports;})";
        Value app = context.eval("js", compiledSource);
        Value result = app.execute();
        Value def = result.getMember("default");
        return def.getMember("render").execute(ProxyObject.fromMap(props));
    }
}
