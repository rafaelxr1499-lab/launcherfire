package com.tvlauncher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    // Tarjetas destacadas (edita a tu gusto): titulo, subtitulo, paquete, color1, color2
    private static final String[][] CARDS = {
        {"Prime Video", "Continuar viendo", "com.amazon.avod", "#1B3A5C", "#00A8E1"},
        {"Netflix", "Series y peliculas", "com.netflix.ninja", "#3B0A0A", "#E50914"},
        {"YouTube", "Tus videos", "com.amazon.firetv.youtube", "#4A1010", "#FF3D3D"},
        {"Plex", "Tu biblioteca", "com.plexapp.android", "#1F1F1F", "#E5A00D"},
    };
    private static final int BLUE = 0xFF1F6BFF, DARK = 0xFF1C1C1E;

    private LinearLayout row1, row2;
    private TextView clock;
    private float d;
    private final Handler handler = new Handler();
    private final Runnable tick = new Runnable() {
        public void run() {
            clock.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
            handler.postDelayed(this, 15000);
        }
    };

    private int dp(int v) { return (int) (v * d + 0.5f); }

    private GradientDrawable rounded(int color, int r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(r));
        return g;
    }

    private TextView tv(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    private void focusStyle(final View v, final int normal, final int focused, final int r, final float scale) {
        v.setFocusable(true);
        v.setClickable(true);
        v.setBackground(rounded(normal, r));
        v.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View x, boolean f) {
                x.setBackground(rounded(f ? focused : normal, r));
                x.animate().scaleX(f ? scale : 1f).scaleY(f ? scale : 1f).setDuration(120).start();
            }
        });
    }

    private HorizontalScrollView hscroll(LinearLayout inner) {
        HorizontalScrollView h = new HorizontalScrollView(this);
        h.setHorizontalScrollBarEnabled(false);
        h.setClipChildren(false);
        h.setClipToPadding(false);
        inner.setClipChildren(false);
        inner.setClipToPadding(false);
        inner.setOrientation(LinearLayout.HORIZONTAL);
        inner.setPadding(dp(16), dp(14), dp(16), dp(14));
        h.addView(inner);
        return h;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        d = getResources().getDisplayMetrics().density;

        ScrollView root = new ScrollView(this);
        root.setFillViewport(true);
        root.setVerticalScrollBarEnabled(false);
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFFE8EBF0, 0xFFAEB8C6}));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(40), dp(16), dp(40), dp(24));
        col.setClipChildren(false);
        col.setClipToPadding(false);
        root.setClipChildren(false);
        root.addView(col, new ViewGroup.LayoutParams(-1, -2));

        // Barra superior: hora, wifi, ajustes
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        clock = tv("--:--", 14, 0xFF6B7380, false);
        clock.setPadding(dp(10), dp(6), dp(10), dp(6));
        TextView wifi = tv("Wi-Fi", 13, 0xFF6B7380, false);
        TextView cfg = tv("\u2699", 20, 0xFF6B7380, false);
        for (TextView t : new TextView[]{wifi, cfg}) {
            t.setPadding(dp(12), dp(6), dp(12), dp(6));
            focusStyle(t, 0x00000000, 0x66FFFFFF, 20, 1.1f);
        }
        wifi.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { open(Settings.ACTION_WIFI_SETTINGS); }
        });
        cfg.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { open(Settings.ACTION_SETTINGS); }
        });
        bar.addView(clock);
        bar.addView(wifi);
        bar.addView(cfg);
        col.addView(bar);

        // Tarjetas destacadas
        LinearLayout cards = new LinearLayout(this);
        col.addView(hscroll(cards));
        for (final String[] c : CARDS) cards.addView(card(c));

        // Fila 1 (apps TV) dentro de panel translucido
        row1 = new LinearLayout(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setBackground(rounded(0x55FFFFFF, 22));
        panel.addView(hscroll(row1), new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(-1, -2);
        pl.topMargin = dp(8);
        col.addView(panel, pl);

        // Fila 2 (resto de apps)
        row2 = new LinearLayout(this);
        LinearLayout.LayoutParams l2 = new LinearLayout.LayoutParams(-1, -2);
        l2.topMargin = dp(8);
        col.addView(hscroll(row2), l2);

        setContentView(root);
    }

    private View card(final String[] c) {
        FrameLayout f = new FrameLayout(this);
        final int c1 = Color.parseColor(c[3]), c2 = Color.parseColor(c[4]);
        final GradientDrawable normal = grad(c1, c2, false);
        final GradientDrawable focus = grad(c1, c2, true);
        f.setBackground(normal);
        f.setFocusable(true);
        f.setClickable(true);
        TextView title = tv(c[0], 20, Color.WHITE, true);
        TextView sub = tv(c[1], 13, 0xCCFFFFFF, false);
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.BOTTOM);
        t.setPadding(dp(18), dp(12), dp(18), dp(16));
        t.addView(title);
        t.addView(sub);
        f.addView(t, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(380), dp(170));
        lp.rightMargin = dp(18);
        f.setLayoutParams(lp);
        f.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View x, boolean h) {
                x.setBackground(h ? focus : normal);
                x.animate().scaleX(h ? 1.05f : 1f).scaleY(h ? 1.05f : 1f).setDuration(120).start();
            }
        });
        final String pkg = c[2];
        f.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
                if (i == null) Toast.makeText(MainActivity.this, "App no instalada", Toast.LENGTH_SHORT).show();
                else startActivity(i);
            }
        });
        return f;
    }

    private GradientDrawable grad(int c1, int c2, boolean stroke) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{c1, c2});
        g.setCornerRadius(dp(18));
        if (stroke) g.setStroke(dp(3), Color.WHITE);
        return g;
    }

    private void open(String action) {
        try { startActivity(new Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
        catch (Exception e) { Toast.makeText(this, "No disponible", Toast.LENGTH_SHORT).show(); }
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
        handler.post(tick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    @Override
    public void onBackPressed() { /* un launcher no cierra con Atras */ }

    private void load() {
        PackageManager pm = getPackageManager();
        Map<String, ResolveInfo> lean = new LinkedHashMap<String, ResolveInfo>();
        Map<String, ResolveInfo> other = new LinkedHashMap<String, ResolveInfo>();
        for (ResolveInfo ri : pm.queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0))
            lean.put(ri.activityInfo.packageName, ri);
        for (ResolveInfo ri : pm.queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0))
            if (!lean.containsKey(ri.activityInfo.packageName)) other.put(ri.activityInfo.packageName, ri);
        lean.remove(getPackageName());
        other.remove(getPackageName());
        fill(row1, lean, pm);
        fill(row2, other, pm);
    }

    private void fill(LinearLayout row, Map<String, ResolveInfo> map, final PackageManager pm) {
        row.removeAllViews();
        List<ResolveInfo> list = new ArrayList<ResolveInfo>(map.values());
        Collections.sort(list, new Comparator<ResolveInfo>() {
            public int compare(ResolveInfo a, ResolveInfo b) {
                return a.loadLabel(pm).toString().compareToIgnoreCase(b.loadLabel(pm).toString());
            }
        });
        for (ResolveInfo ri : list) row.addView(tile(ri, pm));
    }

    private View tile(final ResolveInfo ri, PackageManager pm) {
        final String pkg = ri.activityInfo.packageName;
        FrameLayout t = new FrameLayout(this);
        focusStyle(t, DARK, BLUE, 16, 1.12f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(190), dp(110));
        lp.setMargins(dp(8), 0, dp(8), 0);
        t.setLayoutParams(lp);

        Drawable banner = null;
        try {
            banner = ri.activityInfo.loadBanner(pm);
            if (banner == null) banner = ri.activityInfo.applicationInfo.loadBanner(pm);
        } catch (Exception ignored) { }

        if (banner != null) {
            ImageView iv = new ImageView(this);
            iv.setImageDrawable(banner);
            iv.setScaleType(ImageView.ScaleType.FIT_XY);
            iv.setPadding(dp(6), dp(6), dp(6), dp(6));
            t.addView(iv, new FrameLayout.LayoutParams(-1, -1));
        } else {
            LinearLayout c = new LinearLayout(this);
            c.setOrientation(LinearLayout.VERTICAL);
            c.setGravity(Gravity.CENTER);
            ImageView ic = new ImageView(this);
            ic.setImageDrawable(ri.loadIcon(pm));
            c.addView(ic, new LinearLayout.LayoutParams(dp(52), dp(52)));
            TextView n = tv(ri.loadLabel(pm).toString(), 12, Color.WHITE, false);
            n.setSingleLine(true);
            n.setEllipsize(TextUtils.TruncateAt.END);
            n.setGravity(Gravity.CENTER);
            n.setPadding(dp(6), dp(4), dp(6), 0);
            c.addView(n);
            t.addView(c, new FrameLayout.LayoutParams(-1, -1));
        }

        t.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_MAIN)
                        .setComponent(new ComponentName(pkg, ri.activityInfo.name))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try { startActivity(i); } catch (Exception e) { }
            }
        });
        // Mantener pulsado: info de la app (desinstalar, forzar detencion...)
        t.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + pkg)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return true;
            }
        });
        return t;
    }
}
