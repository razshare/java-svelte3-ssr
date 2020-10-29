package com.mycompany.arcanossrsvelte;
import java.nio.file.Path;

public class Starter {
    public static void main(String[] args) {
        Svelte3SSR ssr = new Svelte3SSR(Path.of(System.getProperty("user.dir")));
        Svelte3SSRResult result = ssr.render("<b>hello world!</b>");
        System.out.println(result.html);
    }
}
