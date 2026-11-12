package pl.saski.gaba;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class MainActivity extends Activity {
    static final int BG = Color.rgb(15, 17, 26);
    static final int CARD = Color.rgb(31, 34, 49);
    static final int INK = Color.rgb(245, 245, 255);
    static final int MUTED = Color.rgb(166, 171, 193);
    static final int ACCENT = Color.rgb(192, 175, 255);
    static final int GOOD = Color.rgb(133, 221, 187);
    static final int WARN = Color.rgb(255, 175, 126);
    static final String[] SLOT_LABELS = {"Rano · leki", "Rano · Enterozoo", "Południe · Flora Care", "Wieczór · leki", "Wieczór · Enterozoo"};
    static final String[] SLOT_DEFAULTS = {"08:00", "10:00", "12:00", "20:00", "22:00"};
    static final String START_DAY = "2026-11-11";
    static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
    static final class Med {
        final String id, name, dose, note;
        final int[] slots;
        Med(String id, String name, String dose, String note, int... slots) {
            this.id = id; this.name = name; this.dose = dose; this.note = note; this.slots = slots;
        }
        boolean hasSlot(int s) { for (int i: slots) if (i==s) return true; return false; }
    }
    static final Med[] MEDS = {
        new Med("bactrim", "Bactrim", "1 ml", "Antybiotyk; 2 × dziennie, do kontroli", 0, 3),
        new Med("pyralgin", "Pyralgin", "0,16 ml", "2 × dziennie, do kontroli", 0, 3),
        new Med("tralieve", "Tralieve 20 mg", "¾ tabletki", "2 × dziennie, do kontroli", 0, 3),
        new Med("gasprid", "Gasprid 5 mg", "½ tabletki", "2 × dziennie, do kontroli", 0, 3),
        new Med("mirtagen", "Mirtagen", "⅓ tabletki", "1 × dziennie, do kontroli", 3),
        new Med("enterozoo", "Enterozoo", "2 ml", "2 × dziennie; co najmniej 1 h odstępu od innych leków doustnych; do końca porcji", 1, 4),
        new Med("floracare", "Flora Care", "1 kapsułka", "1 × dziennie; co najmniej 1 h od Bactrimu; do kontroli", 2)
    };
    static final String[] OBS = {"Apetyt", "Kał", "Mocz"};
    SharedPreferences prefs;
    Calendar selected = Calendar.getInstance();
    LinearLayout root;
    static String date(Calendar c) { return FORMAT.format(c.getTime()); }
    static String today() { return date(Calendar.getInstance()); }
    static boolean relevant(String d, int slot) {
        int comparison = d.compareTo(START_DAY);
        return comparison > 0 || (comparison == 0 && slot >= 3);
    }
    static String time(SharedPreferences p, int slot) { return p.getString("time_" + slot, SLOT_DEFAULTS[slot]); }
    static boolean active(SharedPreferences p, Med med) { return p.getBoolean("active_" + med.id, true); }
    static String key(String d, Med m, int slot) { return "dose_" + d + "_" + m.id + "_" + slot; }
    static boolean done(SharedPreferences p, String d, Med m, int slot) { return p.getLong(key(d,m,slot),0L) > 0L; }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("gaba_local_data", MODE_PRIVATE);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        Reminders.createChannel(this);
        Reminders.scheduleAll(this);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 19);
        render();
    }
    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if(code == 19 && results.length > 0 && results[0] != PackageManager.PERMISSION_GRANTED)
            toast("Powiadomienia są wyłączone. Można je włączyć w ustawieniach Androida.");
    }
    int dp(int d) { return (int) (getResources().getDisplayMetrics().density*d + .5f); }
    LinearLayout vertical() { LinearLayout ll = new LinearLayout(this); ll.setOrientation(LinearLayout.VERTICAL); return ll; }
    LinearLayout horizontal() { LinearLayout ll = new LinearLayout(this); ll.setOrientation(LinearLayout.HORIZONTAL); ll.setGravity(Gravity.CENTER_VERTICAL); return ll; }
    TextView label(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        if(bold) t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }
    LinearLayout.LayoutParams lp(int width, int height, int bottom) {
        LinearLayout.LayoutParams l=new LinearLayout.LayoutParams(width<0?width:dp(width),height<0?height:dp(height)); l.bottomMargin=dp(bottom); return l;
    }
    LinearLayout.LayoutParams weighted() { return new LinearLayout.LayoutParams(0, -2, 1f); }
    void pad(View v, int x, int y) { v.setPadding(dp(x),dp(y),dp(x),dp(y)); }
    void surface(LinearLayout v) { v.setBackground(round(CARD,18)); pad(v,16,14); }
    android.graphics.drawable.GradientDrawable round(int color, int radius) {
        android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(radius)); return g;
    }
    Button button(String name, Runnable act) {
        Button b = new Button(this); b.setText(name); b.setTextSize(12); b.setTextColor(INK);
        b.setAllCaps(false); b.setBackground(round(Color.rgb(58,52,82), 14));
        b.setOnClickListener(v->act.run()); return b;
    }
    void toast(String msg) { Toast.makeText(this,msg,Toast.LENGTH_LONG).show(); }
    void render() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        root=vertical(); pad(root,17,20); scroll.addView(root); setContentView(scroll);
        LinearLayout head = horizontal();
        LinearLayout title = vertical(); title.addView(label("GABA  /  TERAPIA",12,ACCENT,true));
        TextView h=label("Leki i kontrola",26,INK,true); title.addView(h);
        head.addView(title, new LinearLayout.LayoutParams(0,-2,1));
        Button settings=button("⚙ Ustawienia",this::settings); head.addView(settings,lp(116,44,0));
        root.addView(head, lp(-1,-2,15));
        root.addView(label("Zalecenia lek. wet. z 11.11.2026  •  lokalnie, bez internetu",12,MUTED,false), lp(-1,-2,17));
        LinearLayout dateRow=horizontal();
        Button prev=button("‹",()->{selected.add(Calendar.DATE,-1);render();}); dateRow.addView(prev,lp(45,40,0));
        TextView d=label(niceDate(selected),16,INK,true); d.setGravity(Gravity.CENTER); d.setOnClickListener(v->pickDate());
        dateRow.addView(d,weighted());
        Button next=button("›",()->{selected.add(Calendar.DATE,1);render();}); dateRow.addView(next,lp(45,40,0));
        root.addView(dateRow,lp(-1,-2,14));
        String day=date(selected);
        if(!day.equals(today())) {
            Button todayB=button("Przejdź do dzisiaj",()->{selected=Calendar.getInstance();render();});
            root.addView(todayB,lp(-1,42,12));
        }
        if(day.compareTo(START_DAY)<0) {
            notice("Terapia rozpoczęła się wieczorem 11 listopada 2026 r.",MUTED);
        } else {
            int completed=0, total=0;
            for(int slot=0;slot<5;slot++) if(relevant(day,slot))
                for(Med m:MEDS) if(m.hasSlot(slot) && active(prefs,m)) {total++;if(done(prefs,day,m,slot))completed++;}
            LinearLayout summary=vertical(); surface(summary);
            summary.addView(label("PODANE DAWKI",12,ACCENT,true),lp(-1,-2,5));
            summary.addView(label(completed+" / "+total+" potwierdzonych",21,INK,true),lp(-1,-2,7));
            android.widget.ProgressBar pb = new android.widget.ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
            pb.setMax(Math.max(1,total));pb.setProgress(completed);pb.setProgressTintList(android.content.res.ColorStateList.valueOf(GOOD));
            summary.addView(pb,lp(-1,7,0));root.addView(summary,lp(-1,-2,16));
            for(int slot=0;slot<5;slot++) if(relevant(day,slot)) addSlot(day,slot);
        }
        section("Obserwacja Gaby", "Apetyt, kał i mocz należy kontrolować codziennie.");
        LinearLayout monitor=vertical();surface(monitor);
        for(String name:OBS) {
            LinearLayout line=horizontal();TextView what=label(name,15,INK,true);line.addView(what,weighted());
            String value=prefs.getString("obs_"+day+"_"+name, "");
            String display=value.equals("ok")?"✓ W normie":value.equals("alert")?"! Niepokojące":"Dodaj wpis";
            Button b=button(display,()->setObservation(day,name));
            if(value.equals("alert")) b.setTextColor(WARN);
            line.addView(b,lp(136,43,0));monitor.addView(line,lp(-1,48,6));
        }
        root.addView(monitor,lp(-1,-2,16));
        section("Waga", "Codzienne pomiary w gramach, zapisane na urządzeniu.");
        LinearLayout weight=vertical(); surface(weight);
        LinearLayout wr=horizontal();
        int grams=prefs.getInt("weight_"+day,0);
        wr.addView(label(grams>0?(grams+" g"):"Brak pomiaru",20,INK,true),weighted());
        wr.addView(button("Zapisz wagę",()->weightDialog(day)),lp(136,43,0));
        weight.addView(wr,lp(-1,-2,8));
        WeightChart chart=new WeightChart(this,prefs);weight.addView(chart,lp(-1,166,0));
        root.addView(weight,lp(-1,-2,17));
        notice("Kontrola u lekarza: 15–16.11.2026. Przy pogorszeniu — wizyta natychmiast. Jeśli stan się pogarsza, lekarz zalecił dokarmianie 15–20 ml karmy ratunkowej 3–5 razy na dobę.",WARN);
        root.addView(label("Nie zmieniaj dawek ani nie kończ terapii bez uzgodnienia z weterynarzem. Godziny są proponowane, a nie wskazane przez lekarza.",12,MUTED,false),lp(-1,-2,18));
    }
    String niceDate(Calendar c) {return new SimpleDateFormat("EEEE, d MMMM yyyy",new Locale("pl","PL")).format(c.getTime());}
    void notice(String s,int color) { LinearLayout v=vertical();v.setBackground(round(Color.rgb(40,35,51),15));pad(v,14,13);v.addView(label(s,13,color,false));root.addView(v,lp(-1,-2,15)); }
    void section(String h,String info) {
        root.addView(label(h,19,INK,true),lp(-1,-2,5));root.addView(label(info,12,MUTED,false),lp(-1,-2,10));
    }
    void addSlot(String day,int slot) {
        ArrayList<Med> meds=new ArrayList<>();
        for(Med m:MEDS) if(m.hasSlot(slot)&&active(prefs,m)) meds.add(m);
        if(meds.isEmpty()) return;
        LinearLayout card=vertical(); surface(card);
        LinearLayout title=horizontal();title.addView(label(SLOT_LABELS[slot],17,INK,true),weighted());
        TextView at=label(time(prefs,slot),15,ACCENT,true);title.addView(at);
        card.addView(title,lp(-1,-2,9));
        final int s=slot;
        for(Med m:meds) {
            boolean isDone=done(prefs,day,m,s);
            LinearLayout dose=horizontal();dose.setPadding(0,dp(6),0,dp(8));
            CheckBox check=new CheckBox(this);check.setButtonTintList(android.content.res.ColorStateList.valueOf(isDone?GOOD:ACCENT));
            check.setChecked(isDone);check.setOnCheckedChangeListener((c,v)->{
                if(v) prefs.edit().putLong(key(day,m,s),System.currentTimeMillis()).apply();
                else prefs.edit().remove(key(day,m,s)).apply();
                render();
            });
            dose.addView(check,lp(42,46,0));
            LinearLayout info=vertical();TextView nm=label(m.name,16,isDone?GOOD:INK,true);info.addView(nm);
            String instruction=m.dose + "  •  " + m.note;
            if(isDone) instruction += "  •  Podano o " +
                new SimpleDateFormat("HH:mm",new Locale("pl","PL")).format(
                    new Date(prefs.getLong(key(day,m,s),0L)));
            info.addView(label(instruction,12,MUTED,false));
            dose.addView(info,weighted());
            dose.setOnClickListener(v->check.setChecked(!check.isChecked()));
            card.addView(dose);
        }
        root.addView(card,lp(-1,-2,12));
    }
    void pickDate() {
        DatePickerDialog picker=new DatePickerDialog(this,(v,y,m,d)->{
            selected.set(y,m,d);render();
        },selected.get(Calendar.YEAR),selected.get(Calendar.MONTH),selected.get(Calendar.DAY_OF_MONTH));picker.show();
    }
    void setObservation(String day,String name) {
        String[] opts={"W normie", "Niepokojące", "Usuń wpis"};
        new AlertDialog.Builder(this).setTitle(name+" · "+day).setItems(opts,(d,i)->{
            String key="obs_"+day+"_"+name;
            SharedPreferences.Editor e=prefs.edit();
            if(i==2)e.remove(key);else e.putString(key,i==0?"ok":"alert");e.apply();render();
        }).show();
    }
    void weightDialog(String day) {
        EditText value=new EditText(this);value.setInputType(2);value.setSingleLine();
        int old=prefs.getInt("weight_"+day,0);if(old>0)value.setText(String.valueOf(old));
        LinearLayout wrap=vertical();pad(wrap,18,5);wrap.addView(label("Masa ciała w gramach:",15,INK,false));wrap.addView(value);
        new AlertDialog.Builder(this).setTitle("Waga · "+day).setView(wrap).setPositiveButton("Zapisz",(d,w)->{
            try {int n=Integer.parseInt(value.getText().toString().trim());
                if(n<200||n>10000){toast("Podaj prawidłową wagę w zakresie 200–10000 g");return;}
                prefs.edit().putInt("weight_"+day,n).apply();render();
            }catch(Exception e){toast("Wpisz wagę w gramach, np. 2100");}
        }).setNeutralButton("Usuń pomiar",(d,w)->{prefs.edit().remove("weight_"+day).apply();render();})
        .setNegativeButton("Anuluj",null).show();
    }
    void settings() {
        String[] a={"Zmień godziny podawania", "Leki: odstawienie i wznowienie", "Przypomnienia", "Eksportuj kopię danych (JSON)", "Importuj kopię danych (JSON)"};
        new AlertDialog.Builder(this).setTitle("Ustawienia").setItems(a,(d,i)->{
            switch (i) {
                case 0: timeSettings(); break;
                case 1: medSettings(); break;
                case 2: notificationSettings(); break;
                case 3: exportData(); break;
                case 4: importData(); break;
            }
        }).setNegativeButton("Zamknij",null).show();
    }
    void timeSettings() {
        String[] opts=new String[5];for(int i=0;i<5;i++)opts[i]=SLOT_LABELS[i]+"   "+time(prefs,i);
        new AlertDialog.Builder(this).setTitle("Godziny podawania").setItems(opts,(d,i)->{
            String[] hm=time(prefs,i).split(":");
            TimePickerDialog pick=new TimePickerDialog(this,(v,h,min)->{
                String candidate=String.format(Locale.ROOT,"%02d:%02d",h,min);
                String[] proposed=new String[5];for(int j=0;j<5;j++)proposed[j]=time(prefs,j);
                proposed[i]=candidate;
                if(!intervalsValid(proposed)) {
                    new AlertDialog.Builder(this).setTitle("Nieprawidłowy odstęp")
                        .setMessage("Enterozoo musi być podawane co najmniej godzinę od wszystkich leków doustnych, a Flora Care co najmniej godzinę od Bactrimu. Nie zapisano zmiany.")
                        .setPositiveButton("OK",null).show();return;
                }
                prefs.edit().putString("time_"+i,candidate).apply();Reminders.scheduleAll(this);render();toast("Zapisano godzinę");
            },Integer.parseInt(hm[0]),Integer.parseInt(hm[1]),true);pick.show();
        }).setPositiveButton("OK",null).show();
    }
    static int minutes(String time) {String[] parts=time.split(":");return Integer.parseInt(parts[0])*60+Integer.parseInt(parts[1]);}
    static int dist(int a,int b) {int x=Math.abs(a-b);return Math.min(x,1440-x);}
    static boolean intervalsValid(String[] slots) {
        int[] t=new int[5];try{for(int i=0;i<5;i++)t[i]=minutes(slots[i]);}catch(Exception e){return false;}
        for(int enter: new int[]{1,4})for(int other: new int[]{0,2,3})if(dist(t[enter],t[other])<60)return false;
        for(int bact: new int[]{0,3})if(dist(t[bact],t[2])<60)return false;
        return true;
    }
    void medSettings() {
        String[] names=new String[MEDS.length];boolean[] checks=new boolean[MEDS.length];
        for(int i=0;i<MEDS.length;i++) {Med m=MEDS[i];names[i]=m.name+" · "+m.dose;checks[i]=active(prefs,m);}
        new AlertDialog.Builder(this).setTitle("Aktywne leki").setMultiChoiceItems(names,checks,(d,index,yes)->checks[index]=yes)
            .setPositiveButton("Zapisz",(d,i)->{
                SharedPreferences.Editor e=prefs.edit();for(int k=0;k<MEDS.length;k++)e.putBoolean("active_"+MEDS[k].id,checks[k]);
                e.apply();Reminders.scheduleAll(this);render();
            }).setNegativeButton("Anuluj",null).show();
    }
    void notificationSettings() {
        boolean enabled=prefs.getBoolean("reminders_enabled",true);
        new AlertDialog.Builder(this).setTitle("Przypomnienia")
            .setSingleChoiceItems(new String[]{"Włączone", "Wyłączone"},enabled?0:1,(d,i)->{
                prefs.edit().putBoolean("reminders_enabled",i==0).apply();
                Reminders.scheduleAll(this);d.dismiss();
                if(i==0 && Build.VERSION.SDK_INT>=31){
                    android.app.AlarmManager a=(android.app.AlarmManager)getSystemService(ALARM_SERVICE);
                    if(a!=null && !a.canScheduleExactAlarms())new AlertDialog.Builder(this)
                        .setTitle("Dokładność przypomnień")
                        .setMessage("Android może opóźniać alarmy. Dokładne alarmy można zezwolić w ustawieniach systemowych aplikacji.")
                        .setPositiveButton("Ustawienia",(dd,ii)->{
                            Intent intent=new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            intent.setData(Uri.parse("package:"+getPackageName()));
                            try{startActivity(intent);}catch(Exception ex){toast("Nie można otworzyć ustawień");}
                        }).setNegativeButton("Później",null).show();
                }
                toast(i==0?"Przypomnienia włączone":"Przypomnienia wyłączone");
            }).setNegativeButton("Zamknij",null).show();
    }
    static final int EXPORT=81, IMPORT=82;
    void exportData() {
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"Gaba-Leki-kopia-"+today()+".json");
        startActivityForResult(i,EXPORT);
    }
    void importData() {
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");
        startActivityForResult(i,IMPORT);
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();
        if(request==EXPORT)try(OutputStream out=getContentResolver().openOutputStream(uri)) {
            if(out==null)throw new Exception("Brak dostępu");
            JSONObject backup=new JSONObject();backup.put("application","Gaba-Leki");backup.put("version",2);
            backup.put("exported",System.currentTimeMillis());backup.put("preferences",new JSONObject(prefs.getAll()));
            out.write(backup.toString(2).getBytes(StandardCharsets.UTF_8));toast("Zapisano kopię danych");
        }catch(Exception e){toast("Eksport się nie udał: "+e.getMessage());}
        if(request==IMPORT)try(InputStream in=getContentResolver().openInputStream(uri)) {
            if(in==null)throw new Exception("Brak dostępu");
            ByteArrayOutputStream buffer=new ByteArrayOutputStream();byte[] bytes=new byte[8192];int n,total=0;
            while((n=in.read(bytes))!=-1) {total+=n;if(total>2_000_000)throw new Exception("Zbyt duży plik");buffer.write(bytes,0,n);}
            JSONObject backup=new JSONObject(buffer.toString(StandardCharsets.UTF_8.name()));
            if(!"Gaba-Leki".equals(backup.optString("application")))throw new Exception("To nie jest kopia aplikacji Gaba–Leki");
            JSONObject content=backup.getJSONObject("preferences");
            new AlertDialog.Builder(this).setTitle("Przywrócić dane?")
                .setMessage("Import zastąpi wszystkie bieżące wpisy, wagę i ustawienia. Tej operacji nie można cofnąć.")
                .setPositiveButton("Przywróć",(d,w)->{
                    SharedPreferences.Editor e=prefs.edit().clear();
                    java.util.Iterator<String> it=content.keys();
                    while(it.hasNext()) {
                        String k=it.next();Object v=content.opt(k);
                        if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);
                        else if(v instanceof Integer)e.putInt(k,(Integer)v);
                        else if(v instanceof Number)e.putLong(k,((Number)v).longValue());
                        else if(v instanceof String)e.putString(k,(String)v);
                    }
                    e.apply();selected=Calendar.getInstance();Reminders.scheduleAll(this);render();toast("Dane przywrócone");
                }).setNegativeButton("Anuluj",null).show();
        }catch(Exception e){toast("Import się nie udał: "+e.getMessage());}
    }
    static class WeightChart extends View {
        final SharedPreferences p;final Paint pen=new Paint(3);final TreeMap<String,Integer> points=new TreeMap<>();
        WeightChart(Context ctx,SharedPreferences p) {super(ctx);this.p=p;
            for(Map.Entry<String,?> entry:p.getAll().entrySet())
                if(entry.getKey().startsWith("weight_")&&entry.getValue() instanceof Number) {
                    String d=entry.getKey().substring(7);if(d.matches("\\d{4}-\\d{2}-\\d{2}"))
                        points.put(d,((Number)entry.getValue()).intValue());
                }
        }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);float w=getWidth(),h=getHeight();float den=getResources().getDisplayMetrics().density;
            if(points.isEmpty()) {pen.setColor(MUTED);pen.setTextSize(13*den);c.drawText("Brak danych do wykresu",14*den,h/2,pen);return;}
            ArrayList<Map.Entry<String,Integer>> items=new ArrayList<>(points.entrySet());
            int low=Integer.MAX_VALUE,high=0;for(Map.Entry<String,Integer> e:items){low=Math.min(low,e.getValue());high=Math.max(high,e.getValue());}
            int margin=Math.max(25,(high-low)/5);low-=margin;high+=margin;
            float l=12*den,r=w-16*den,top=22*den,bottom=h-24*den;
            pen.setStrokeWidth(1*den);pen.setColor(Color.rgb(60,65,83));
            for(int k=0;k<3;k++){float yy=top+(bottom-top)*k/2f;c.drawLine(l,yy,r,yy,pen);}
            Path path=new Path();
            for(int i=0;i<items.size();i++) {
                float x=items.size()==1?(l+r)/2:l+(r-l)*i/(items.size()-1f);
                float y=bottom-(items.get(i).getValue()-low)*(bottom-top)/(high-low);
                if(i==0)path.moveTo(x,y);else path.lineTo(x,y);
            }
            pen.setColor(ACCENT);pen.setStrokeWidth(2.5f*den);pen.setStyle(Paint.Style.STROKE);c.drawPath(path,pen);
            pen.setStyle(Paint.Style.FILL);
            for(int i=0;i<items.size();i++) {
                float x=items.size()==1?(l+r)/2:l+(r-l)*i/(items.size()-1f);
                float y=bottom-(items.get(i).getValue()-low)*(bottom-top)/(high-low);
                pen.setColor(GOOD);c.drawCircle(x,y,3.8f*den,pen);
            }
            pen.setColor(MUTED);pen.setTextSize(11*den);
            c.drawText(high+" g",l,12*den,pen);
            c.drawText(items.get(0).getKey().substring(5),l,h-6*den,pen);
            String last=items.get(items.size()-1).getKey().substring(5);
            c.drawText(last,r-pen.measureText(last),h-6*den,pen);
        }
    }
}
