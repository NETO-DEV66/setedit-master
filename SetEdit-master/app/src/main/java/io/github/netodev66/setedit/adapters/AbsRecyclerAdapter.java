package io.github.netodev66.setedit.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Filter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Pair;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import io.github.netodev66.setedit.EditorUtils;
import io.github.netodev66.setedit.R;
import io.github.netodev66.setedit.TableTypeInt;
import io.github.netodev66.setedit.boot.ActionItem;
import io.github.netodev66.setedit.boot.BootUtils;
import io.github.netodev66.setedit.shortcut.ShortcutUtils;
import io.github.netodev66.setedit.utils.ActionResult;
import io.github.netodev66.util.UiUtils;

public abstract class AbsRecyclerAdapter extends RecyclerView.Adapter<AbsRecyclerAdapter.ViewHolder> {
    protected final FragmentActivity context;
    private String constraint;

    public AbsRecyclerAdapter(FragmentActivity context) {
        setHasStableIds(true);
        this.context = context;
    }

    public abstract void refresh();

    @NonNull
    public abstract List<Pair<String, String>> getAllItems();

    @TableTypeInt
    public abstract int getListType();

    public void filter(String constraint) {
        this.constraint = constraint;
        getFilter().filter(constraint);
    }

    public void filter() {
        getFilter().filter(constraint);
    }

    public boolean canSetOnReboot() {
        return false;
    }

    public boolean canCreateShortcut() {
        return false;
    }

    public boolean canCreate() {
        return false;
    }

    public boolean canEdit() {
        return false;
    }

    public boolean canDelete() {
        return false;
    }

    public void create(String keyName, String newValue) {
    }

    public void update(String keyName, String newValue) {
    }

    public void delete(String keyName) {
    }

    @NonNull
    @Override
    public final ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_setting, parent, false);
        return new ViewHolder(view);
    }

    public abstract Pair<String, String> getItem(int position);

    @Override
    public abstract long getItemId(int position);

    @Override
    public abstract int getItemCount();

    @Override
    public final void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Pair<String, String> key = getItem(position);
        onBindViewHolder(holder, key.first, key.second, position);
    }

    protected abstract Filter getFilter();

    private void onBindViewHolder(@NonNull ViewHolder holder, String keyName, String keyValue, int position) {
        holder.keyName.setText(keyName);
        holder.keyValue.setText(keyValue);
        holder.itemView.setOnClickListener(v -> onClickItem(v, keyName, keyValue, position));
        holder.itemView.setOnTouchListener(new BouncyTouchListener());
        animateItemEntrance(holder.itemView, position);
    }

    /** Spring-in each row, staggered by position but capped so long lists stay snappy. */
    private void animateItemEntrance(@NonNull View view, int position) {
        if (view.getTag(R.id.tag_item_animated) != null) {
            // Already animated once; recycled rows must not replay the entrance.
            return;
        }
        view.setTag(R.id.tag_item_animated, Boolean.TRUE);
        Context ctx = view.getContext();
        view.setAlpha(0f);
        view.setTranslationY(UiUtils.dpToPx(ctx, 16));
        view.setScaleX(0.92f);
        view.setScaleY(0.92f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(Math.min(position, 8) * 35L)
                .setDuration(320L)
                .setInterpolator(AnimationUtils.loadInterpolator(ctx, R.interpolator.m3_spring_bouncy))
                .start();
    }

    /** Scales the row down on press and springs it back, giving tactile bounce feedback. */
    private static final class BouncyTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(110L)
                            .setInterpolator(new DecelerateInterpolator()).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(260L)
                            .setInterpolator(AnimationUtils.loadInterpolator(v.getContext(),
                                    R.interpolator.m3_spring_bouncy)).start();
                    break;
                default:
                    break;
            }
            return false;
        }
    }

    protected void onClickItem(View view, String keyName, String keyValue, int position) {
        View v = View.inflate(context, R.layout.dialog_edit, null);
        v.findViewById(R.id.button_help).setOnClickListener(v2 -> openHelp(keyName));
        ((TextView) v.findViewById(R.id.title)).setText(keyName);
        TextInputEditText editText = v.findViewById(R.id.txt);
        MaterialCheckBox performOnReboot = v.findViewById(R.id.checkbox);
        MaterialCheckBox performViaShortcut = v.findViewById(R.id.checkbox_2);
        boolean canEdit = canEdit();
        boolean canDelete = canDelete();
        editText.setText(keyValue);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setView(v)
                .setNegativeButton(R.string.close, null);
        if (canSetOnReboot() && (canEdit || canDelete)) {
            performOnReboot.setVisibility(View.VISIBLE);
        } else performOnReboot.setVisibility(View.GONE);
        if (canCreateShortcut() && (canEdit || canDelete)) {
            performViaShortcut.setVisibility(View.VISIBLE);
        } else performViaShortcut.setVisibility(View.GONE);
        if (canEdit) {
            builder.setPositiveButton(R.string.save, (dialog, which) -> {
                Editable editable = editText.getText();
                if (editable == null) return;
                update(keyName, editable.toString());
                if (canSetOnReboot() && performOnReboot.isChecked()) {
                    ActionItem actionItem = new ActionItem(ActionResult.TYPE_UPDATE, EditorUtils.toTableType(getListType()), keyName, editable.toString());
                    BootUtils.add(context, actionItem);
                }
                if (canCreateShortcut() && performViaShortcut.isChecked()) {
                    ActionItem actionItem = new ActionItem(ActionResult.TYPE_UPDATE, EditorUtils.toTableType(getListType()), keyName, editable.toString());
                    ShortcutUtils.displayShortcutTypeChooserDialog(context, actionItem);
                }
            });
        } else {
            editText.setKeyListener(null);
        }
        if (canDelete) {
            builder.setNeutralButton(R.string.delete, (dialog, which) -> {
                delete(keyName);
                if (canSetOnReboot() && performOnReboot.isChecked()) {
                    ActionItem actionItem = new ActionItem(ActionResult.TYPE_DELETE, EditorUtils.toTableType(getListType()), keyName, null);
                    BootUtils.add(context, actionItem);
                }
                if (canCreateShortcut() && performViaShortcut.isChecked()) {
                    ActionItem actionItem = new ActionItem(ActionResult.TYPE_DELETE, EditorUtils.toTableType(getListType()), keyName, null);
                    ShortcutUtils.displayShortcutTypeChooserDialog(context, actionItem);
                }
            });
        }
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            UiUtils.playBouncyDialogEntrance(dialog);
            if (canEdit()) {
                editText.requestFocus();
                editText.requestFocusFromTouch();
                if (keyValue != null) {
                    editText.setSelection(0, keyValue.length());
                }
                editText.postDelayed(() -> {
                    InputMethodManager imm = (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
                }, 200);
            }
        });
        dialog.show();
    }

    protected void setMessage(CharSequence charSequence) {
        new MaterialAlertDialogBuilder(context)
                .setMessage(charSequence)
                .setNegativeButton(R.string.close, null)
                .show();
    }

    private void openHelp(String keyName) {
        String str;
        StringBuilder sb = new StringBuilder("https://search.brave.com/search?q=android+");
        switch (getListType()) {
            case TableTypeInt.TABLE_SYSTEM:
                str = "settings put system \"";
                break;
            case TableTypeInt.TABLE_SECURE:
                str = "settings put secure \"";
                break;
            case TableTypeInt.TABLE_GLOBAL:
                str = "settings put global \"";
                break;
            case TableTypeInt.TABLE_PROPERTIES:
                str = "setprop \"";
                break;
            case TableTypeInt.TABLE_JAVA:
                str = "java properties \"";
                break;
            case TableTypeInt.TABLE_ENV:
                str = "environment \"";
                break;
            default:
                // Unsupported
                return;
        }
        sb.append(str);
        sb.append(Uri.encode(keyName));
        sb.append('\"');
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(sb.toString())));
        } catch (Exception ignore) {
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView keyName;
        public final TextView keyValue;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            keyName = itemView.findViewById(R.id.txtName);
            keyValue = itemView.findViewById(R.id.txtValue);
        }
    }
}
