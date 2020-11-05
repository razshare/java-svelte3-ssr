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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

/**
 *
 * @author Administrator
 */
public class Svelte3SSR {
    protected Context context;
    
    public Svelte3SSR(Path wk){
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
        "const component = function(filename){" +
            "return eval(" +
                "ssr.compile(" +
                    "FileReaderJS.readString(filename)" +
                ")" +
            ");" +
        "};"
        //"const { component } = require('./compile.js');" 
        ;
        
        context.eval("js",tools);
        
        Value ssr = context.eval("js", "(function(service){ssr=service;});");
        ssr.executeVoid(this);
        
        context.eval("js","console.log('JavaScript initialized.');");
    }
    
    public Context getContext(){
        return context;
    }
    
    private static final Pattern PATTERN_SVELTE_ITEMS = Pattern.compile("(?<=import).*(?=from)",Pattern.MULTILINE|Pattern.DOTALL);
    private static final Pattern PATTERN_SVELTE_PATH = Pattern.compile("(?<=from)\\s+[\"'].*[\"'];?",Pattern.MULTILINE|Pattern.DOTALL);
    private static final Pattern PATTERN_REQUIRES = Pattern.compile("import\\s\\{?\\s*\\n*([A-z_$][A-z0-9_$]*\\s*\\n*,*\\s*\\n*?)*\\s*\\n*\\}*\\s*\\n*from\\s*\\n*[\"'][A-z0-9_$\\/@\\.,;:|-]*[\"'\\s*\\n*];?",Pattern.MULTILINE|Pattern.DOTALL);
    private static final Pattern PATTERN_USE_STRICT = Pattern.compile("[\"']use strict[\"'];",Pattern.MULTILINE);
    
    public String compileFile(String filename, String charset) throws IOException{
        return compileFile(filename, charset, new LinkedHashMap<>());
    }
    
    public String compileFile(String filename, String charset, LinkedHashMap<String,HashMap<String,String>> imports) throws IOException{
        return compileFile(filename,charset,imports,true);
    }
    public String compileFile(String filename, String charset, LinkedHashMap<String,HashMap<String,String>> imports, boolean addImports) throws IOException{
        return compile(Files.readString(Path.of(filename), Charset.forName(charset)), imports, true);
    }
    
    private void requireSvelteComponent(String[] names, String path, LinkedHashMap<String,HashMap<String,String>> imports) throws IOException{
        path = path.trim();
        if(!imports.containsKey(path))
            imports.put(path, new HashMap<>());
        
        HashMap<String,String> importedPath = imports.get(path);
        
        for(String item : names){
            item = item.trim();
            if(path.endsWith(".svelte")){
                if(imports.get(path).containsKey(item)){
                    continue;
                }
                String imported = compileFile(path, "UTF-8", imports, false);
                
                imported = 
                    "const " + item + " = (function(){\n" +
                        "const exports = {};\n" +
                        imported + "\n" +
                        "return exports.default;\n" +
                    "})();"
                ;
                /*
                System.out.println("#######################");
                System.out.println("imported:\n");
                System.out.println(imported);
                System.out.println("#######################");
                */
                
                importedPath.put(item, imported);
            }
//            else{
//                
//                if(names.length > 1) {
//                    String script = "(function (){const {"+item+"} = require('"+path+"'); if(!"+item+") FileReaderJS.readString('"+path+"'); else return "+item+".toString();})";
//                    String value = context.eval("js",script).execute().asString();
//                    importedPath.put(item, value);
//                }else {
//                    importedPath.put(item, Files.readString(Path.of(path)));
//                }
//            }
        }
    }
    
    private void parseRequires(String requires, LinkedHashMap<String,HashMap<String,String>> imports) throws IOException{
        Matcher mpath = PATTERN_SVELTE_PATH.matcher(requires);
        if(!mpath.find())
            return;
        String path = mpath.group().replaceAll("('|;|\\\")+", "");
        Matcher mitems = PATTERN_SVELTE_ITEMS.matcher(requires);
        if(!mitems.find())
            return;
        String sitems = mitems
                .group()
                .replaceAll("(\\{|\\})+", "")
                .replaceAll(".*default:", "");
        String[] items = sitems.split(",");
        requireSvelteComponent(items, path, imports);
    }
    
    
    
    public String compile(String source) throws IOException{
        return compile(source, true);
    }
    public String compile(String source, boolean addImports) throws IOException{
        return compile(source, new LinkedHashMap<>(), addImports);
    }
    
    public String compile(String source, LinkedHashMap<String,HashMap<String,String>> imports, boolean addImports) throws IOException{
        
        Matcher m = PATTERN_REQUIRES.matcher(source);
        while(m.find()){
            String item = m.group();
            //System.out.println("import:"+item);
            parseRequires(item.trim(),imports);
        }
        source = m.replaceAll("");
        
        Value app = context.eval("js", "(function(source){return compile(source,{generate:'ssr',format:'cjs'}).js.code;});");
        String result = app.execute(source).asString();
        
        ArrayList<String> headers = new ArrayList<>();
        if(addImports)
            imports.forEach(((path, importsObject) -> {
                importsObject.forEach((name, contents) -> {
                    headers.add(contents);
                });
            }));
        result = String.join("\n", 
            String.join("\n", headers),
            result
        );
        return result;
    }
    
    public Value render(String compiledSource){
        return render(compiledSource, new HashMap<>());
    }
    
    public Value render(String compiledSource, HashMap<String,Object> props){
        compiledSource = 
            "'use strict';\n" +
            "(function (){\n" +
                "const exports = {};\n" +
                PATTERN_USE_STRICT.matcher(compiledSource).replaceAll("") + "\n" +
                "return exports;\n" +
            "});"
        ;
        
        Value app = context.eval("js", compiledSource);
        Value result = app.execute();
        //System.out.println(result.toString());
        Value def = result.getMember("default");
        return def.getMember("render").execute(ProxyObject.fromMap(props));
    }
}
