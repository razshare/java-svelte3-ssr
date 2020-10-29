/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.tncrazvan.svelte3ssr;

/**
 *
 * @author Administrator
 */
public class Svelte3SSRResult{
    public String head = "";
    public String html = "";
    public String css = "";
    public String build(){
        return build("en");
    }
    public String build(String lang){
        return 
        "<!DOCTYPE html>\n" +
        "<html lang=\""+lang+"\">\n" +
        "<head>\n" +
        "<style>" +
        css +
        "</style>" +
        head +
        "</head>\n" +
        "<body>\n" +
        html +
        "</body>\n" +
        "</html>";
    }
}