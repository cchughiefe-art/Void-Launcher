package com.android.launcher3.agent;

public final class BrowserTabsTest {
    private static void check(boolean yes){if(!yes)throw new AssertionError();}
    public static void main(String[] args){
        BrowserTabs tabs=new BrowserTabs("https://x.com");int first=tabs.selected();int second=tabs.add("https://github.com");int third=tabs.add("https://example.org");
        tabs.select(first);check(tabs.get(second).url.equals("https://github.com"));check(tabs.selected()==first);
        tabs.close(second);check(tabs.get(third).id==third);try{tabs.get(second);throw new AssertionError();}catch(IllegalArgumentException expected){}
        tabs.close(first);tabs.close(third);check(tabs.all().size()==1);check(tabs.selected()>third);
        while(tabs.all().size()<BrowserTabs.LIMIT)tabs.add("about:blank");try{tabs.add("about:blank");throw new AssertionError();}catch(IllegalStateException expected){}
        check(BrowserAddress.resolve("x.com").equals("https://x.com"));check(BrowserAddress.resolve("how tabs work").contains("duckduckgo.com/?q=how+tabs+work"));
        check(!BrowserAddress.canRemember("https://x.com/i/flow/login"));check(!BrowserAddress.canRemember("https://example.com/?flow_token=example"));
        check(BrowserAddress.canRemember("https://x.com/TeaonaX"));
        try{BrowserAddress.resolve("javascript:alert(1)");throw new AssertionError();}catch(IllegalArgumentException expected){}
        AgentPolicy.browserUrl("https://example.com/file.zip");try{AgentPolicy.publicUrl("https://example.com/file.zip");throw new AssertionError();}catch(IllegalArgumentException expected){}
        System.out.println("Browser tab targeting, closure, restore-address and history privacy checks passed");
    }
}
