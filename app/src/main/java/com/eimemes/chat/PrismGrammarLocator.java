package com.eimemes.chat;

import io.noties.prism4j.annotations.PrismBundle;

@PrismBundle(
    include = {
        "javascript", "python", "java", "kotlin", "cpp", "c",
        "css", "markup", "json", "bash", "sql", "typescript",
        "go", "swift", "php", "ruby", "rust", "clike"
    },
    grammarLocatorClassName = ".GrammarLocatorDef"
)
public class PrismGrammarLocator {
    // Annotation processor generates GrammarLocatorDef.java at build time
}
