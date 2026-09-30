package com.newagedevs.smartvpn.binding;

import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.BindingAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.newagedevs.smartvpn.model.VpnServer;
import com.newagedevs.smartvpn.view.adapter.FavoriteServerAdapter;
import com.newagedevs.smartvpn.view.adapter.ServerAdapter;

import java.util.List;

/**
 * RecyclerView data-binding adapters.
 *
 * <p>These are Java classes on purpose: the data binding annotation processor is a
 * Java processor, and adapters declared inside a Kotlin {@code object} are not
 * visible to it, which made every {@code app:adapter*} attribute fail to resolve.
 */
public final class RecyclerViewBindingAdapters {

    private RecyclerViewBindingAdapters() {
    }

    @BindingAdapter("adapter")
    public static void bindAdapter(RecyclerView view, RecyclerView.Adapter<?> adapter) {
        view.setAdapter(adapter);
    }

    @BindingAdapter("toast")
    public static void bindToast(ConstraintLayout view, @Nullable String text) {
        if (text != null && !text.trim().isEmpty()) {
            Toast.makeText(view.getContext(), text, Toast.LENGTH_SHORT).show();
        }
    }

    @BindingAdapter("adapterServerList")
    public static void bindAdapterServerList(RecyclerView view, @Nullable List<VpnServer> items) {
        if (items == null || items.isEmpty()) return;
        RecyclerView.Adapter<?> adapter = view.getAdapter();
        if (adapter instanceof ServerAdapter) {
            ((ServerAdapter) adapter).updateServerList(items);
        }
    }

    @BindingAdapter("favoriteServerList")
    public static void bindFavoriteServerList(RecyclerView view, @Nullable List<VpnServer> items) {
        if (items == null || items.isEmpty()) return;
        RecyclerView.Adapter<?> adapter = view.getAdapter();
        if (adapter instanceof FavoriteServerAdapter) {
            ((FavoriteServerAdapter) adapter).updateServerList(items);
        }
    }
}
