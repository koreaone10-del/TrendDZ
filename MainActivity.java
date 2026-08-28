package com.chibani.trenddz;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import android.graphics.drawable.GradientDrawable;
import org.json.*;
import java.util.*;

public class MainActivity extends Activity {
    LinearLayout content; ScrollView scroll; WebView web; EditText urlInput, nameInput, costInput, sellInput;
    TextView status; String pendingUrl="", pendingProvider="", pendingId="", pendingImage="", pendingTitle="";
    ArrayList<JSONObject> products = new ArrayList<>();
    android.content.SharedPreferences prefs;

    int white=Color.rgb(245,247,251), muted=Color.rgb(170,180,200), accent=Color.rgb(25,211,174), panel=Color.rgb(21,29,49), panel2=Color.rgb(28,39,64), yellow=Color.rgb(255,210,63);

    @Override public void onCreate(Bundle b){ super.onCreate(b); setContentView(R.layout.activity_main);
        content=findViewById(R.id.content); scroll=findViewById(R.id.scroll); web=findViewById(R.id.web);
        prefs=getSharedPreferences("trenddz",MODE_PRIVATE); loadProducts(); setupWeb(); showAdd();
        findViewById(R.id.tabAdd).setOnClickListener(v->showAdd());
        findViewById(R.id.tabProducts).setOnClickListener(v->showProducts());
        findViewById(R.id.tabExport).setOnClickListener(v->exportProductsJs());
    }

    void setupWeb(){
        WebSettings s=web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setUserAgentString("Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/126 Mobile Safari/537.36");
        web.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView view,String url){
                String js="(function(){\n"+
                "function m(a,b){var e=document.querySelector('meta['+a+'=\\\"'+b+'\\\"]');return e?e.content:'';}\n"+
                "var title=document.title||m('property','og:title')||m('name','twitter:title')||'';\n"+
                "var img=m('property','og:image')||m('name','twitter:image')||m('property','og:image:url')||'';\n"+
                "if(!img){var l=document.querySelector('link[rel=image_src]');if(l)img=l.href;}\n"+
                "if(!img){var a=document.querySelectorAll('img');for(var i=0;i<a.length;i++){var x=a[i].getAttribute('data-src')||a[i].getAttribute('data-lazy-src')||a[i].src;if(x&&x.indexOf('data:')!==0){img=x;break;}}}\n"+
                "var canonical=(document.querySelector('link[rel=canonical]')||{}).href||location.href;\n"+
                "var text=(document.body?document.body.innerText:'').slice(0,8000);\n"+
                "return JSON.stringify({title:title,img:img,canonical:canonical,text:text,url:location.href});})()";
                view.evaluateJavascript(js, value->{
                    try { String raw=JSONObject.NULL.toString().equals(value)?"":value; if(raw.startsWith("\"")&&raw.endsWith("\"")) raw=new JSONArray("["+raw+"]").getString(0);
                        JSONObject o=new JSONObject(raw); pendingImage=o.optString("img",""); pendingTitle=o.optString("title","");
                        if(pendingImage.length()>0) { status.setText("✅ الصورة الأصلية تم جلب رابطها مباشرة من الموقع"); }
                        else status.setText("⚠️ لم نجد og:image؛ يمكنك لصق رابط الصورة يدويًا");
                        if(nameInput!=null && nameInput.getText().toString().trim().isEmpty()) nameInput.setText(pendingTitle);
                    } catch(Exception e){ status.setText("⚠️ تعذر قراءة بيانات الصفحة؛ أدخلها يدويًا"); }
                });
            }
        });
    }

    void showAdd(){ content.removeAllViews();
        addHeader("🔥 إضافة منتج ذكية", "ألصق رابط المنتج فقط. TrendDZ يحدد المنصة والرقم ويحاول جلب صورة المنتج الأصلية تلقائيًا.");
        urlInput=edit("رابط المنتج", "https://www.babaalgeria.com/product/346"); content.addView(urlInput);
        Button analyze=button("🔎 تحليل الرابط وجلب الصورة"); content.addView(analyze); analyze.setOnClickListener(v->analyzeUrl());
        status=txt("جاهز. لا يتم حفظ كلمة مرور أي منصة.", muted,13); content.addView(status);
        nameInput=edit("اسم المنتج", ""); content.addView(nameInput);
        costInput=edit("سعر المورد (دج)", ""); content.addView(costInput);
        sellInput=edit("سعر البيع (دج)", ""); content.addView(sellInput);
        EditText image=edit("رابط الصورة (يملأ تلقائيًا)", ""); image.setTag("image"); content.addView(image);
        Button save=button("💾 حفظ المنتج"); content.addView(save); save.setOnClickListener(v->{
            if(pendingImage.length()>0) image.setText(pendingImage); saveProduct(image.getText().toString());
        });
        Button open=button("🌐 فتح صفحة المنتج"); content.addView(open); open.setOnClickListener(v->{ if(!urlInput.getText().toString().trim().isEmpty()) startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(urlInput.getText().toString().trim()))); });
        addInfoCard("المنصات المدعومة", "Sawa9ly • Baba Algeria • DropDZ • Prix Choc\nكل منصة لها طريقة طلب مستقلة. رابط الصورة يُحفظ كما هو من موقع التسويق.");
    }

    void analyzeUrl(){ String u=urlInput.getText().toString().trim(); if(u.isEmpty()) return;
        Provider p=detect(u); pendingUrl=u; pendingProvider=p.name; pendingId=p.id; pendingImage=""; pendingTitle="";
        status.setText("⏳ يتم فتح الصفحة لاستخراج og:image والصورة المباشرة...");
        web.setVisibility(View.VISIBLE); web.loadUrl(u);
        addInfoCard("تم التعرف", p.label+"\nID: "+(p.id.isEmpty()?"غير مستخرج":p.id)+"\nطريقة الطلب: "+p.orderMode);
    }

    Provider detect(String u){
        Uri x=Uri.parse(u); String host=x.getHost()==null?"":x.getHost().toLowerCase();
        if(host.contains("sawa9ly.app")){ return new Provider("sawa9ly","Sawa9ly",firstPathId(x,"product"),"PRODUCT_THEN_FORM"); }
        if(host.contains("babaalgeria.com")){ return new Provider("baba","Baba Algeria",firstPathId(x,"product"),"PRODUCT_THEN_FORM"); }
        if(host.contains("dropdz.space")){ return new Provider("dropdz","DropDZ",firstPathId(x,"product"),"PRODUCT_OR_FORM"); }
        if(host.contains("prix-choc.vercel.app")){ String p=x.getQueryParameter("p"); return new Provider("prixchoc","Prix Choc",p==null?"":p,"LANDING_THEN_FORM"); }
        return new Provider("custom","Custom", "", "CUSTOM");
    }
    String firstPathId(Uri u,String before){ List<String> seg=u.getPathSegments(); for(int i=0;i<seg.size()-1;i++) if(seg.get(i).equalsIgnoreCase(before)) return seg.get(i+1); return ""; }

    void saveProduct(String image){ try{
        JSONObject o=new JSONObject(); Provider p=detect(pendingUrl); String name=nameInput.getText().toString().trim(); if(name.isEmpty()) name=pendingTitle;
        int cost=num(costInput.getText().toString()), sell=num(sellInput.getText().toString());
        o.put("id", UUID.randomUUID().toString().substring(0,8)); o.put("provider",p.key); o.put("providerProductId",p.id); o.put("name",name); o.put("costPrice",cost); o.put("sellingPrice",sell); o.put("image",image.trim()); o.put("productUrl",pendingUrl); o.put("orderMode",p.orderMode); o.put("trendScore",score(name,cost,sell));
        products.add(0,o); persist(); Toast.makeText(this,"تم حفظ المنتج ✅",Toast.LENGTH_SHORT).show(); showProducts();
    }catch(Exception e){Toast.makeText(this,"خطأ في حفظ المنتج",Toast.LENGTH_SHORT).show();}}
    int num(String s){try{return Integer.parseInt(s.replaceAll("[^0-9]",""));}catch(Exception e){return 0;}}
    int score(String n,int c,int s){ int z=50; String t=n.toLowerCase(); if(s>0&&c>0){double m=(s-c)/(double)s; if(m>=.45)z+=18; else if(m>=.30)z+=10; else if(m<.15)z-=10;} if(n.length()<65)z+=8; if(t.matches(".*(ذكي|محمول|منظم|led|لعبة|سيارة|مطبخ|تنظيف|هدية|usb|portable|smart|mini|beauty|home|kids).*"))z+=12; return Math.max(0,Math.min(100,z)); }

    void showProducts(){ content.removeAllViews(); addHeader("📦 منتجاتك", products.size()+" منتج محفوظ محليًا على الهاتف.");
        if(products.isEmpty()){ addInfoCard("لا توجد منتجات", "ابدأ من تبويب إضافة منتج والصق رابط Sawa9ly أو Baba Algeria أو DropDZ أو Prix Choc."); return; }
        for(JSONObject o:products){ addProductCard(o); }
    }
    void addProductCard(JSONObject o){
        LinearLayout card=card(); String name=o.optString("name","بدون اسم"); int score=o.optInt("trendScore",0);
        card.addView(txt(o.optString("provider").toUpperCase()+"  •  #"+o.optString("providerProductId"),accent,12));
        card.addView(txt(name,white,17)); card.addView(txt("التكلفة: "+o.optInt("costPrice")+" دج   |   البيع: "+o.optInt("sellingPrice")+" دج   |   Trend: "+score+"/100",muted,13));
        card.addView(txt("🖼 "+(o.optString("image").isEmpty()?"لا يوجد رابط صورة":"رابط صورة مباشر محفوظ"), o.optString("image").isEmpty()?Color.rgb(255,93,115):accent,12));
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        Button open=button("فتح"); Button del=button("حذف"); row.addView(open,new LinearLayout.LayoutParams(0,46,1)); row.addView(del,new LinearLayout.LayoutParams(0,46,1)); card.addView(row);
        open.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(o.optString("productUrl")))));
        del.setOnClickListener(v->{products.remove(o);persist();showProducts();}); content.addView(card);
    }

    void exportProductsJs(){ StringBuilder b=new StringBuilder(); b.append("const storeData = {\n"); int i=1; for(JSONObject o:products){ b.append("  ").append(quote(String.valueOf(i++))).append(": {"); b.append(" provider: ").append(quote(o.optString("provider"))).append(","); b.append(" providerProductId: ").append(quote(o.optString("providerProductId"))).append(","); b.append(" name: ").append(quote(o.optString("name"))).append(","); b.append(" price: ").append(o.optInt("sellingPrice")).append(","); b.append(" costPrice: ").append(o.optInt("costPrice")).append(","); b.append(" image: ").append(quote(o.optString("image"))).append(","); b.append(" productUrl: ").append(quote(o.optString("productUrl"))).append(","); b.append(" orderMode: ").append(quote(o.optString("orderMode"))).append(","); b.append(" trendScore: ").append(o.optInt("trendScore")); b.append(" },\n"); } b.append("};\n");
        Intent send=new Intent(Intent.ACTION_SEND); send.setType("text/javascript"); send.putExtra(Intent.EXTRA_TEXT,b.toString()); startActivity(Intent.createChooser(send,"مشاركة products.js"));
    }
    String quote(String s){return "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n")+"\"";}
    void persist(){ org.json.JSONArray a=new org.json.JSONArray(); for(JSONObject o:products)a.put(o); prefs.edit().putString("products",a.toString()).apply(); }
    void loadProducts(){ try{JSONArray a=new JSONArray(prefs.getString("products","[]")); for(int i=0;i<a.length();i++)products.add(a.getJSONObject(i));}catch(Exception ignored){} }

    void addHeader(String h,String s){ content.addView(txt(h,white,22)); content.addView(txt(s,muted,13)); }
    TextView txt(String s,int c,float size){TextView t=new TextView(this);t.setText(s);t.setTextColor(c);t.setTextSize(size);t.setPadding(12,8,12,8);t.setTextDirection(View.TEXT_DIRECTION_ANY_RTL);return t;}
    EditText edit(String hint,String value){EditText e=new EditText(this);e.setHint(hint);e.setText(value);e.setTextColor(white);e.setHintTextColor(muted);e.setTextSize(15);e.setPadding(16,10,16,10); GradientDrawable g=new GradientDrawable();g.setColor(panel);g.setCornerRadius(18);e.setBackground(g); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,58);p.setMargins(0,7,0,7);e.setLayoutParams(p);return e;}
    Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(13);b.setTextColor(Color.BLACK);b.setAllCaps(false);b.setBackgroundColor(accent);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,52);p.setMargins(0,7,0,7);b.setLayoutParams(p);return b;}
    LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(12,12,12,12);GradientDrawable g=new GradientDrawable();g.setColor(panel);g.setCornerRadius(24);c.setBackground(g);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,7,0,7);c.setLayoutParams(p);return c;}
    void addInfoCard(String title,String body){LinearLayout c=card();c.addView(txt(title, yellow,15));c.addView(txt(body,white,13));content.addView(c);}

    static class Provider{String key,name,label,id,orderMode;Provider(String k,String n,String i,String m){key=k;name=n;label=n;id=i;orderMode=m;}}
}
