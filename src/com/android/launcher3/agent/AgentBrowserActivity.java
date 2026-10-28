package com.android.launcher3.agent;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.text.InputType;
import android.util.Rational;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;

/** Service-owned tabs survive activity minimization, PiP and hiding. */
public class AgentBrowserActivity extends Activity implements AgentWebEngine.BrowserUi {
    private static final int BG=Color.rgb(12,17,26), FG=Color.rgb(232,238,247);
    private static final String HIDE="com.voidlauncher.agent.HIDE_WINDOW";
    private static final int PICK_FILE=8043;
    private final Handler main=new Handler();
    private AgentSettings settings;
    private AgentBrowserService service;
    private FrameLayout browserHost;
    private LinearLayout chrome, findBar;
    private EditText address, findText;
    private TextView status;
    private ProgressBar progress;
    private Button tabButton, backButton, forwardButton;
    private boolean bound, resumed, attaching, suppressPip, receiverRegistered;
    private int attachedTab=-1;
    private ValueCallback<Uri[]> fileCallback;
    private WebView filePage;
    private String filePageUrl;
    private final BroadcastReceiver hideReceiver=new BroadcastReceiver(){
        @Override public void onReceive(Context c,Intent i){if(HIDE.equals(i.getAction()))hideBrowser();}
    };
    private final Runnable refresh=new Runnable(){@Override public void run(){if(!resumed)return; updateToolbar(); main.postDelayed(this,1000);}};
    private final ServiceConnection connection=new ServiceConnection(){
        @Override public void onServiceConnected(ComponentName n,IBinder b){
            service=((AgentBrowserService.LocalBinder)b).service(); service.setCloseScreen(()->closeWindow());
            if(service.engine()!=null){service.engine().setUi(AgentBrowserActivity.this); attachBrowser();}else toast("Browser engine unavailable");
        }
        @Override public void onServiceDisconnected(ComponentName n){service=null; attachedTab=-1; status.setText("Browser disconnected · reopen to recover");}
    };
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        if(Build.VERSION.SDK_INT<28){Toast.makeText(this,"Agent Browser requires Android 9+",Toast.LENGTH_LONG).show();finish();return;}
        settings=new AgentSettings(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        LinearLayout root=column(); chrome=column(); chrome.setPadding(dp(8),dp(4),dp(8),0); root.addView(chrome);
        LinearLayout top=row();
        TextView brand=label("VOID",18); brand.setPadding(dp(8),0,dp(8),0); top.addView(brand);
        status=label("Opening…",12); top.addView(status,new LinearLayout.LayoutParams(0,dp(34),1));
        status.setOnClickListener(v->showConnection());
        tabButton=button(top,"1",this::showTabs); tabButton.setContentDescription("Tabs");
        button(top,"+",()->run(()->{service.engine().newTab("about:blank");attachBrowser();})).setContentDescription("New tab");
        button(top,"⋮",this::showMenu).setContentDescription("Browser menu"); chrome.addView(top);
        LinearLayout nav=row();
        address=new EditText(this); address.setSingleLine();address.setTextColor(FG);address.setHintTextColor(Color.GRAY);
        address.setTextSize(15);address.setHint("Search or enter HTTPS address");address.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);address.setImeOptions(EditorInfo.IME_ACTION_GO);
        address.setSelectAllOnFocus(true); address.setOnEditorActionListener((v,a,e)->{if(a==EditorInfo.IME_ACTION_GO){navigate();return true;}return false;});
        nav.addView(address,new LinearLayout.LayoutParams(0,dp(48),1));button(nav,"Go",this::navigate);chrome.addView(nav);
        LinearLayout tools=row();
        backButton=button(tools,"‹",()->run(()->service.engine().back()));backButton.setContentDescription("Back");
        forwardButton=button(tools,"›",()->run(()->service.engine().forward()));forwardButton.setContentDescription("Forward");
        button(tools,"↻",()->run(()->service.engine().reload())).setContentDescription("Reload");
        button(tools,"☆",this::bookmark).setContentDescription("Bookmark this page");
        button(tools,"PiP",this::enterPip).setContentDescription("Picture in picture");
        button(tools,"Hide",this::hideBrowser);chrome.addView(tools);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);chrome.addView(progress,new LinearLayout.LayoutParams(-1,dp(2)));
        findBar=row();findBar.setVisibility(View.GONE);findText=new EditText(this);findText.setSingleLine();findText.setTextColor(FG);findText.setTextSize(14);findText.setHint("Find in page");findText.setHintTextColor(Color.GRAY);
        findBar.addView(findText,new LinearLayout.LayoutParams(0,dp(42),1));button(findBar,"Find",()->run(()->service.engine().find(findText.getText().toString())));button(findBar,"↓",()->run(()->service.engine().findNext(true)));button(findBar,"×",()->{findBar.setVisibility(View.GONE);run(()->service.engine().clearFind());});chrome.addView(findBar);
        browserHost=new FrameLayout(this);root.addView(browserHost,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
        browserHost.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(!isInPictureInPictureMode())updatePip();});
        if(Build.VERSION.SDK_INT>=33)registerReceiver(hideReceiver,new IntentFilter(HIDE),Context.RECEIVER_NOT_EXPORTED);else registerReceiver(hideReceiver,new IntentFilter(HIDE));receiverRegistered=true;
    }
    @Override protected void onStart(){super.onStart();if(settings==null)return;
        try{startForegroundService(new Intent(this,AgentBrowserService.class).setAction(AgentBrowserService.OPEN_BROWSER));}catch(RuntimeException e){toast("Android could not keep the browser open");}
        if(!bound)bound=bindService(new Intent(this,AgentBrowserService.class),connection,BIND_AUTO_CREATE);
        else if(service!=null){service.engine().setUi(this);attachBrowser();}
    }
    @Override protected void onResume(){super.onResume();if(settings==null)return;resumed=true;if(fileCallback==null)suppressPip=false;main.post(refresh);updatePip();}
    @Override protected void onPause(){resumed=false;main.removeCallbacks(refresh);super.onPause();}
    @Override protected void onStop(){
        if(!isInPictureInPictureMode()){
            if(service!=null&&service.engine()!=null){service.engine().setUi(null);service.setCloseScreen(null);service.engine().detach(getApplicationContext());}
            if(bound){unbindService(connection);bound=false;}service=null;attachedTab=-1;
        }
        super.onStop();
    }
    @Override protected void onDestroy(){
        main.removeCallbacks(refresh);if(receiverRegistered)unregisterReceiver(hideReceiver);
        if(fileCallback!=null){fileCallback.onReceiveValue(null);fileCallback=null;}
        if(bound){if(service!=null&&service.engine()!=null){service.engine().setUi(null);service.setCloseScreen(null);service.engine().detach(getApplicationContext());}unbindService(connection);bound=false;}
        super.onDestroy();
    }
    @Override public void onConfigurationChanged(Configuration config){super.onConfigurationChanged(config);updatePip();}
    @Override protected void onUserLeaveHint(){super.onUserLeaveHint();if(Build.VERSION.SDK_INT<31&&shouldPip())enterPip();}
    @Override public void onPictureInPictureModeChanged(boolean inPip,Configuration config){
        super.onPictureInPictureModeChanged(inPip,config);if(chrome!=null)chrome.setVisibility(inPip?View.GONE:View.VISIBLE);
        if(!inPip&&service!=null){service.engine().setUi(this);attachBrowser();}if(!inPip)updatePip();
    }
    @Override public void onBackPressed(){if(service!=null&&service.engine().canBack()){run(()->service.engine().back());return;}hideBrowser();}
    private boolean pipSupported(){return getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE);}
    private boolean shouldPip(){return settings!=null&&settings.prefs.getBoolean("auto_pip",true)&&!suppressPip&&!isFinishing()&&pipSupported()&&service!=null;}
    private PictureInPictureParams pipParams(){
        PendingIntent hide=PendingIntent.getBroadcast(this,8044,new Intent(HIDE).setPackage(getPackageName()),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        RemoteAction action=new RemoteAction(Icon.createWithResource(this,android.R.drawable.ic_menu_close_clear_cancel),"Hide","Hide browser; reveal from notification",hide);
        PictureInPictureParams.Builder b=new PictureInPictureParams.Builder().setAspectRatio(new Rational(9,16)).setActions(Collections.singletonList(action));
        Rect rect=new Rect();if(browserHost!=null&&browserHost.getGlobalVisibleRect(rect)&&!rect.isEmpty())b.setSourceRectHint(rect);
        if(Build.VERSION.SDK_INT>=31)b.setAutoEnterEnabled(shouldPip()).setSeamlessResizeEnabled(false);
        return b.build();
    }
    private void updatePip(){if(settings==null||!pipSupported()||isInPictureInPictureMode())return;try{setPictureInPictureParams(pipParams());}catch(RuntimeException ignored){}}
    private void enterPip(){if(!pipSupported()){toast("PiP is unavailable on this phone. Hide and reveal from the notification instead.");return;}
        if(service==null)return;address.clearFocus();try{if(!enterPictureInPictureMode(pipParams()))toast("Android did not enter PiP");}catch(RuntimeException e){toast("PiP is unavailable right now");}}
    private void hideBrowser(){suppressPip=true;updatePip();finish();}
    private void closeWindow(){suppressPip=true;updatePip();finish();}
    private void attachBrowser(){
        if(service==null||attaching||service.engine()==null)return;attaching=true;
        try{WebView page=service.engine().view(this);browserHost.removeAllViews();browserHost.addView(page,new FrameLayout.LayoutParams(-1,-1));attachedTab=service.engine().tabs().selected();updateToolbar();}
        finally{attaching=false;}updatePip();
    }
    @Override public void changed(){if(service==null)return;if(attachedTab!=service.engine().tabs().selected())attachBrowser();else updateToolbar();}
    private void updateToolbar(){if(service==null||service.engine()==null||attaching)return;
        AgentWebEngine e=service.engine();tabButton.setText(String.valueOf(e.tabs().all().size()));
        if(!address.hasFocus()){String url=e.activeUrl();address.setText(url.equals("about:blank")?"":url);}
        backButton.setEnabled(e.canBack());forwardButton.setEnabled(e.canForward());progress.setProgress(e.progress());progress.setVisibility(e.progress()==100?View.INVISIBLE:View.VISIBLE);
        long last=service.lastContact();boolean fresh=last>0&&System.currentTimeMillis()-last<30000;
        status.setText(settings.enabled()?(fresh?"● Connected":"● Connecting…"):"○ Agent off");
    }
    private void navigate(){run(()->{String url=BrowserAddress.resolve(address.getText().toString());address.clearFocus();((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(address.getWindowToken(),0);service.engine().navigateLocal(url);});}
    private void showTabs(){if(service==null)return;java.util.List<BrowserTabs.Tab> tabs=service.engine().tabs().all();String[] labels=new String[tabs.size()];int selected=0;
        for(int i=0;i<tabs.size();i++){BrowserTabs.Tab t=tabs.get(i);labels[i]="#"+t.id+" · "+(t.title==null?"Page":t.title)+"\n"+displayUrl(t.url);if(t.id==service.engine().tabs().selected())selected=i;}
        new AlertDialog.Builder(this).setTitle("Tabs · agent can access all tabs").setSingleChoiceItems(labels,selected,(d,i)->{run(()->{service.engine().selectTab(tabs.get(i).id);attachBrowser();});d.dismiss();})
            .setNeutralButton("New tab",(d,w)->run(()->{service.engine().newTab("about:blank");attachBrowser();}))
            .setPositiveButton("Close current tab",(d,w)->run(()->{service.engine().closeTab(service.engine().tabs().selected());attachBrowser();})).setNegativeButton("Done",null).show();
    }
    private void showMenu(){String[] items={"Start agent","Stop agent","Bookmarks","History","Find in page","Share page","Open in system browser","Settings","Hide browser","Close browser"};
        new AlertDialog.Builder(this).setTitle("Browser").setItems(items,(d,n)->{switch(n){
            case 0:startConnection();break;case 1:if(service!=null)service.stopConnection();break;case 2:showSaved("browser_bookmarks","Bookmarks");break;case 3:showSaved("browser_history","History");break;
            case 4:findBar.setVisibility(View.VISIBLE);findText.requestFocus();break;case 5:share();break;case 6:if(service!=null)external(service.engine().activeUrl());break;case 7:showSettings();break;case 8:hideBrowser();break;
            case 9:if(service!=null)service.closeBrowser();else closeWindow();break;
        }}).show();}
    private void bookmark(){if(service==null)return;String url=service.engine().activeUrl();if(!BrowserAddress.canRemember(url)){toast("Sign-in and transient pages are not saved");return;}
        try{JSONArray a=new JSONArray(settings.prefs.getString("browser_bookmarks","[]"));for(int i=0;i<a.length();i++)if(a.getJSONObject(i).optString("url").equals(url)){toast("Already bookmarked");return;}
            if(a.length()>=100){toast("Bookmark limit reached");return;}a.put(new JSONObject().put("url",url).put("title",service.engine().activeTitle()));settings.prefs.edit().putString("browser_bookmarks",a.toString()).apply();toast("Bookmark saved");}catch(Exception e){toast("Could not save bookmark");}}
    private void showSaved(String key,String title){try{JSONArray a=new JSONArray(settings.prefs.getString(key,"[]"));if(a.length()==0){toast("No "+title.toLowerCase()+" yet");return;}String[] labels=new String[a.length()];for(int i=0;i<a.length();i++){JSONObject j=a.getJSONObject(i);labels[i]=j.optString("title","Page")+"\n"+displayUrl(j.optString("url"));}
        new AlertDialog.Builder(this).setTitle(title).setItems(labels,(d,i)->{try{String url=a.getJSONObject(i).getString("url");new AlertDialog.Builder(this).setTitle("Saved page").setItems(new String[]{"Open","Open in new tab","Remove"},(dialog,choice)->{if(choice==2){JSONArray out=new JSONArray();for(int n=0;n<a.length();n++)if(n!=i)out.put(a.opt(n));settings.prefs.edit().putString(key,out.toString()).apply();}else run(()->{if(choice==1){service.engine().newTab(url);attachBrowser();}else service.engine().navigateLocal(url);});}).show();}catch(Exception e){toast("Saved address unavailable");}}).setNegativeButton("Done",null).show();
        }catch(Exception e){toast("Could not read saved pages");}}
    private void share(){if(service==null)return;String url=service.engine().activeUrl();if(!BrowserAddress.canRemember(url)){toast("Sign-in pages are not shared");return;}suppressPip=true;updatePip();startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,url),"Share page"));}
    private void external(String url){try{url=AgentPolicy.browserUrl(url);suppressPip=true;updatePip();startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE));}catch(Exception e){toast("No browser could open this address");}}
    @Override public void download(String url){new AlertDialog.Builder(this).setTitle("Download file").setMessage("Open this download in your system browser? Its own download manager and sign-in apply.\n\n"+displayUrl(url)).setPositiveButton("Open",(d,w)->external(url)).setNegativeButton("Cancel",null).show();}
    @Override public boolean chooseFiles(WebView page,ValueCallback<Uri[]> callback,WebChromeClient.FileChooserParams params){
        if(fileCallback!=null)fileCallback.onReceiveValue(null);fileCallback=callback;filePage=page;filePageUrl=page.getUrl();
        Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");String[] types=params.getAcceptTypes();if(types!=null&&types.length>0&&!types[0].isEmpty())pick.putExtra(Intent.EXTRA_MIME_TYPES,types);pick.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,params.getMode()==WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE);
        suppressPip=true;updatePip();try{startActivityForResult(pick,PICK_FILE);}catch(RuntimeException e){fileCallback.onReceiveValue(null);fileCallback=null;toast("File picker unavailable");}return true;
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=PICK_FILE||fileCallback==null)return;
        ValueCallback<Uri[]> callback=fileCallback;fileCallback=null;ArrayList<Uri> files=new ArrayList<>();
        boolean samePage=false;try{samePage=filePage!=null&&java.util.Objects.equals(filePageUrl,filePage.getUrl());}catch(RuntimeException ignored){}
        if(result==RESULT_OK&&data!=null&&samePage){ClipData clip=data.getClipData();if(clip!=null){for(int i=0;i<Math.min(clip.getItemCount(),20);i++)files.add(clip.getItemAt(i).getUri());}else if(data.getData()!=null)files.add(data.getData());}
        callback.onReceiveValue(files.isEmpty()?null:files.toArray(new Uri[0]));filePage=null;suppressPip=false;updatePip();
    }
    private void showConnection(){new AlertDialog.Builder(this).setTitle("Agent connection").setMessage(settings.prefs.getString("status","Off")+"\nThe agent can list and target every tab. Captures go through your relay into your private void-x-bot repository.").setPositiveButton("Start",(d,w)->startConnection()).setNeutralButton("Stop",(d,w)->{if(service!=null)service.stopConnection();}).setNegativeButton("Done",null).show();}
    private void startConnection(){try{if(settings.deviceKey().isEmpty()){showSettings();return;}if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){suppressPip=true;updatePip();requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},8042);return;}settings.enabled(true);settings.status("Connecting…");startForegroundService(new Intent(this,AgentBrowserService.class));}catch(Exception e){settings.enabled(false);toast("Could not start. Check the saved device key.");}}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] results){super.onRequestPermissionsResult(r,p,results);suppressPip=false;if(r==8042&&results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)startConnection();updatePip();}
    private void showSettings(){LinearLayout box=column();box.setPadding(dp(18),dp(8),dp(18),0);TextView note=label("Save the existing device key once. Relay discovery is automatic. Agent access covers all tabs. Enable page actions below to allow filling fields, clicking buttons and publishing. Passwords and verification codes remain manual.",14);box.addView(note);
        EditText key=new EditText(this);key.setTextColor(FG);key.setHintTextColor(Color.GRAY);key.setHint("Device key · blank keeps saved key");key.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);key.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);box.addView(key);
        CheckBox pip=new CheckBox(this);pip.setText("Float in PiP when I leave the browser");pip.setTextColor(FG);pip.setChecked(settings.prefs.getBoolean("auto_pip",true));box.addView(pip);
        CheckBox actions=new CheckBox(this);actions.setText("Allow agent forms, clicks and posting");actions.setTextColor(FG);actions.setChecked(settings.prefs.getBoolean("page_actions",false));box.addView(actions);
        CheckBox boot=new CheckBox(this);boot.setText("Resume enabled agent after restart");boot.setTextColor(FG);boot.setChecked(settings.prefs.getBoolean("resume_boot",true));box.addView(boot);
        Button battery=new Button(this);battery.setText("Android app / battery settings");battery.setOnClickListener(v->{suppressPip=true;updatePip();startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));});box.addView(battery);
        suppressPip=true;updatePip();AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Browser settings").setView(box).setNegativeButton("Cancel",null).setNeutralButton("Forget key",(d,w)->{if(service!=null)service.stopConnection();settings.enabled(false);settings.prefs.edit().remove("key_enc").remove("key_iv").apply();}).setPositiveButton("Save",(d,w)->{try{if(!key.getText().toString().trim().isEmpty())settings.saveDeviceKey(key.getText().toString().trim());settings.prefs.edit().putBoolean("page_actions",actions.isChecked()).putBoolean("auto_pip",pip.isChecked()).putBoolean("resume_boot",boot.isChecked()).apply();toast("Settings saved");}catch(Exception e){toast("Device key could not be saved");}}).create();dialog.setOnDismissListener(d->{suppressPip=false;updatePip();});dialog.show();}
    private void run(Runnable action){if(service==null){toast("Browser is opening…");return;}try{action.run();updateToolbar();}catch(Exception e){toast(e.getMessage()==null?"Browser action failed":e.getMessage());}}
    private String displayUrl(String value){if(value==null||value.equals("about:blank"))return "New tab";try{return Uri.parse(value).getHost();}catch(Exception e){return "Page";}}
    private void toast(String text){Toast.makeText(this,text,Toast.LENGTH_SHORT).show();}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setBackgroundColor(BG);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private TextView label(String text,int size){TextView t=new TextView(this);t.setText(text);t.setTextColor(FG);t.setTextSize(size);return t;}
    private Button button(LinearLayout parent,String text,Runnable action){Button b=new Button(this);b.setText(text);b.setTextSize(14);b.setTextColor(FG);b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(dp(9),0,dp(9),0);b.setOnClickListener(v->action.run());parent.addView(b,new LinearLayout.LayoutParams(-2,dp(40)));return b;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
