package org.openqa.selenium.server;

import java.util.ArrayList;
import java.util.List;

public class HtmlIdentifier {
    private static List<Rule> rules = new ArrayList<Rule>();
    private static boolean logging = SeleniumServer.isDebugMode();
    
    static {
        rules.add(new ExtensionRule(new String[]{"html", "htm"}, 10000));
        rules.add(new ExtensionRule(new String[]{"jsp", "asp", "php", "pl"}, 100));
        // ebay dll contains HTML snippets which fool InjectionHelper.  -nas
        rules.add(new ExtensionRule(new String[]{"dll", "gif", "ico", "jpg", "jpeg", "png", "dwr", "js"}, -1000));
        rules.add(new ContentRule("<html", 1000, -100));
        rules.add(new ContentRule("<!DOCTYPE html", 1000, -100));
        rules.add(new ContentTypeRule("text/html", 100, -1000));
        rules.add(new Rule("dojo catcher", -100000, 0) {
            public int score(String path, String contentType, String contentPreview) {
                
                if (path == null) {
                    return 0;
                }

                // dojo should never be processed
                if (path.contains("/dojo/")) {
                    return -100000;
                }

                return 0;
            }
        });
    }

    public static boolean shouldBeInjected(String path, String contentType, String contentPreview) {
        int score = 0;

        if (logging) {
            SeleniumServer.log("HtmlIdentifier.shouldBeInjected(\"" + path + "\", \"" + contentType + "\", \"...\")");
        }        
        
        for (Rule rule : rules) {
            int scoreDelta = rule.score(path, contentType, contentPreview);
            if (logging) {
                SeleniumServer.log("    applied rule " + rule + ": " + scoreDelta);
            }
            score += scoreDelta;
        }
        boolean shouldInject = (score > 200);
        if (logging) {
            SeleniumServer.log("    total : " + score + " (should " + (shouldInject ? "" : "not ") + "inject)");
        }        
        return shouldInject;
    }

    static abstract class Rule {
        protected final int missingScore;
        protected final int score;
        protected String name;
        public Rule(String name, int score, int missingScore) {
            this.name = name;
            this.score = score;
            this.missingScore = missingScore;
        }
        abstract int score(String path, String contentType, String contentPreview);
        public String toString() {
            return "[" + name + " rule: match=" + score +
                    (missingScore==0 ? "": (", failure to match -> " + missingScore))
                    + "]";
        }
    }

    static class ExtensionRule extends Rule {
        List<String> exts = new ArrayList<String>();

        public ExtensionRule(String ext, int score) {
            super("extension " + ext, score, 0);
            exts.add(ext);
        }

        public ExtensionRule(String[] ext, int score) {
            super(null, score, 0);
            for (String s : ext) {
                exts.add(s);
            }
            name = "extension " + exts;
        }

        public int score(String path, String contentType, String contentPreview) {
            if (path == null || !path.contains(".")) {
                return 0;
            }

            for (String ext : exts) {
                if (path.endsWith("." + ext)) {
                    return score;
                }
            }
            
            return 0;
        }
    }

    static class ContentRule extends Rule {
        String contentInLowerCase;

        public ContentRule(String content, int score, int missingScore) {
            super("content " + content, score, missingScore);
            this.contentInLowerCase = content.toLowerCase();
        }

        public int score(String path, String contentType, String contentPreview) {
            if (contentPreview == null) {
                return 0;
            }

            if (contentPreview.toLowerCase().contains(contentInLowerCase)) {
                return score;
            }
            return missingScore;
        }
    }

    static class ContentTypeRule extends Rule {
        String type;

        public ContentTypeRule(String type, int score, int missingScore) {
            super("content type " + type, score, missingScore);
            this.type = type;
        }

        public int score(String path, String contentType, String contentPreview) {
            if (contentType.contains(type)) {
                return score;
            }
            return missingScore;
        }
    }

    public static void setLogging(boolean b) {
        HtmlIdentifier.logging = b;
    }
}
