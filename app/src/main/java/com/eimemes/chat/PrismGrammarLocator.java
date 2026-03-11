package com.eimemes.chat;

import io.noties.prism4j.annotations.PrismBundle;

@PrismBundle(
    include = {
        "javascript", "python", "java", "kotlin", "c",
        "css", "json", "sql", "go", "swift", "php", "ruby", "rust", "clike"
    },
    grammarLocatorClassName = "com.eimemes.chat.GrammarLocatorDef"
)
public class PrismGrammarLocator {
    // Annotation processor generates GrammarLocatorDef.java at build time
}

