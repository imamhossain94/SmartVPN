package com.newagedevs.smartvpn.binding;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.databinding.BindingAdapter;

import com.bumptech.glide.Glide;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.newagedevs.smartvpn.R;
import com.newagedevs.smartvpn.model.VpnServer;

import java.util.Locale;

/**
 * Image and shimmer data-binding adapters.
 *
 * <p>Java on purpose: see {@link RecyclerViewBindingAdapters}.
 */
public final class ViewBindingAdapters {

    private ViewBindingAdapters() {
    }

    @BindingAdapter("loadServerFlag")
    public static void bindServerFlagImage(ImageView view, @Nullable VpnServer src) {
        if (src == null) return;
        Glide.with(view.getContext())
                .load(flagDrawable(view, src.getCountryLong()))
                .into(view);
    }

    @BindingAdapter("loadServerPing")
    public static void bindServerPingImage(ImageView view, @Nullable VpnServer src) {
        if (src == null) return;
        int res;
        Integer ping = parsePing(src.getPing());
        if (ping == null) {
            res = R.drawable.ic_network_0;
        } else if (ping <= 10) {
            res = R.drawable.ic_network_4;
        } else if (ping <= 20) {
            res = R.drawable.ic_network_3;
        } else if (ping <= 50) {
            res = R.drawable.ic_network_2;
        } else if (ping <= 100) {
            res = R.drawable.ic_network_1;
        } else {
            res = R.drawable.ic_network_0;
        }
        Glide.with(view.getContext()).load(res).into(view);
    }

    @BindingAdapter("startShimmer")
    public static void bindShimmerAnimation(ShimmerFrameLayout view, @Nullable Boolean value) {
        if (value == null) return;
        if (value) {
            view.setVisibility(View.VISIBLE);
            view.startShimmer();
        } else {
            view.stopShimmer();
            view.setVisibility(View.GONE);
        }
    }

    @Nullable
    private static Integer parsePing(@Nullable String ping) {
        if (ping == null) return null;
        try {
            return Integer.valueOf(ping.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Resolves the flag drawable for a country.
     *
     * <p>VPNGate reports names such as "united states" or "cote d'ivoire", so a
     * naive lookup misses and used to fall back to the generic flag for most
     * countries. Names are normalised and a small set of aliases is mapped
     * explicitly; anything still unresolved gets the neutral flag.
     */
    @SuppressLint("DiscouragedApi")
    private static Drawable flagDrawable(ImageView view, @Nullable String countryLong) {
        String name = normalise(countryLong);
        int resId = 0;
        if (!name.isEmpty()) {
            resId = view.getResources().getIdentifier("flag_" + name, "drawable", view.getContext().getPackageName());
        }
        if (resId == 0) {
            resId = R.drawable.flag_common;
        }
        Drawable drawable = ActivityCompat.getDrawable(view.getContext(), resId);
        return drawable != null ? drawable : ActivityCompat.getDrawable(view.getContext(), R.drawable.flag_common);
    }

    private static String normalise(@Nullable String countryLong) {
        if (countryLong == null) return "";
        String name = countryLong.toLowerCase(Locale.US).trim();
        if (name.contains("united states") || name.contains("united states of america")) {
            return "usa";
        }
        if (name.contains("korea")) return "korea";
        if (name.contains("viet")) return "vietnam";
        if (name.contains("russia")) return "russia";
        if (name.contains("united kingdom")) return "uk";
        // Keep only [a-z0-9] so names with spaces, apostrophes or accents resolve.
        StringBuilder sb = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) sb.append(c);
        }
        return sb.toString();
    }
}
