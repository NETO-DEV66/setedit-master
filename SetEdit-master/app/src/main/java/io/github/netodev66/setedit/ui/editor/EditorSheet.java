package io.github.netodev66.setedit.ui.editor;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

import java.io.File;

import io.github.netodev66.setedit.R;
import io.github.netodev66.setedit.TableType;
import io.github.netodev66.setedit.TableTypeInt;
import io.github.netodev66.setedit.boot.ActionItem;
import io.github.netodev66.setedit.boot.BootUtils;
import io.github.netodev66.setedit.data.AppPrefs;
import io.github.netodev66.setedit.data.AutoBackup;
import io.github.netodev66.setedit.data.FavoritesStore;
import io.github.netodev66.setedit.data.HistoryStore;
import io.github.netodev66.setedit.data.RiskyKeys;
import io.github.netodev66.setedit.shortcut.ShortcutUtils;
import io.github.netodev66.setedit.ui.Tables;
import io.github.netodev66.setedit.ui.ValueType;
import io.github.netodev66.setedit.util.ClipboardUtils;
import io.github.netodev66.setedit.utils.ActionResult;

/**
 * The bottom sheet used to view, edit, favorite or delete a single key.
 *
 * <p>It presents the right control for the value — a switch for booleans, a stepper for numbers and
 * a text field otherwise — and, for settings tables, the same reboot/shortcut options the editor has
 * always offered. Risky keys are confirmed first, and the previous value is recorded so it can be
 * reverted.
 */
public class EditorSheet extends BottomSheetDialogFragment {

    public interface Listener {
        /** Writes {@code newValue} for {@code key}; {@code isNew} means it must be created. */
        void onSave(@NonNull String key, @NonNull String newValue, boolean isNew);

        /** Deletes {@code key}. */
        void onDelete(@NonNull String key);

        /** Called after a successful edit so the caller can refresh. */
        default void onEdited() {
        }
    }

    private static final String ARG_TABLE = "table";
    private static final String ARG_KEY = "key";
    private static final String ARG_VALUE = "value";
    private static final String ARG_CAN_EDIT = "can_edit";
    private static final String ARG_CAN_DELETE = "can_delete";
    private static final String ARG_CAN_CREATE = "can_create";
    private static final String ARG_CAN_REBOOT = "can_reboot";
    private static final String ARG_CAN_SHORTCUT = "can_shortcut";
    private static final String ARG_IS_NEW = "is_new";

    @Nullable
    private Listener listener;

    private static final java.util.concurrent.ExecutorService BACKUP_EXECUTOR =
            java.util.concurrent.Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "setedit-auto-backup");
                thread.setDaemon(true);
                return thread;
            });

    @TableTypeInt
    private int tableType;
    @NonNull
    private String key = "";
    @NonNull
    private String value = "";
    private boolean canEdit;
    private boolean canDelete;
    private boolean canCreate;
    private boolean canSetOnReboot;
    private boolean canCreateShortcut;
    private boolean isNew;

    private TextInputEditText valueInput;
    private TextInputEditText keyInput;
    private MaterialSwitch boolSwitch;
    private TextInputLayout valueLayout;

    public static EditorSheet edit(@TableTypeInt int tableType, @NonNull String key,
                                   @NonNull String value, boolean canEdit, boolean canDelete,
                                   boolean canSetOnReboot, boolean canCreateShortcut) {
        return build(tableType, key, value, canEdit, canDelete, false, canSetOnReboot,
                canCreateShortcut, false);
    }

    public static EditorSheet create(@TableTypeInt int tableType, boolean canSetOnReboot,
                                     boolean canCreateShortcut) {
        return build(tableType, "", "", true, false, true, canSetOnReboot, canCreateShortcut, true);
    }

    private static EditorSheet build(@TableTypeInt int tableType, String key, String value,
                                     boolean canEdit, boolean canDelete, boolean canCreate,
                                     boolean canSetOnReboot, boolean canCreateShortcut,
                                     boolean isNew) {
        EditorSheet sheet = new EditorSheet();
        Bundle args = new Bundle();
        args.putInt(ARG_TABLE, tableType);
        args.putString(ARG_KEY, key);
        args.putString(ARG_VALUE, value);
        args.putBoolean(ARG_CAN_EDIT, canEdit);
        args.putBoolean(ARG_CAN_DELETE, canDelete);
        args.putBoolean(ARG_CAN_CREATE, canCreate);
        args.putBoolean(ARG_CAN_REBOOT, canSetOnReboot);
        args.putBoolean(ARG_CAN_SHORTCUT, canCreateShortcut);
        args.putBoolean(ARG_IS_NEW, isNew);
        sheet.setArguments(args);
        return sheet;
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = requireArguments();
        tableType = args.getInt(ARG_TABLE, TableTypeInt.TABLE_SYSTEM);
        key = args.getString(ARG_KEY, "");
        value = args.getString(ARG_VALUE, "");
        canEdit = args.getBoolean(ARG_CAN_EDIT, false);
        canDelete = args.getBoolean(ARG_CAN_DELETE, false);
        canCreate = args.getBoolean(ARG_CAN_CREATE, false);
        canSetOnReboot = args.getBoolean(ARG_CAN_REBOOT, false);
        canCreateShortcut = args.getBoolean(ARG_CAN_SHORTCUT, false);
        isNew = args.getBoolean(ARG_IS_NEW, false);

        MaterialTextView title = view.findViewById(R.id.sheet_title);
        MaterialTextView keyText = view.findViewById(R.id.sheet_key);
        MaterialTextView typeText = view.findViewById(R.id.sheet_type);
        MaterialTextView riskyBadge = view.findViewById(R.id.sheet_risky_badge);
        MaterialTextView historyHeader = view.findViewById(R.id.sheet_history_header);
        MaterialTextView historyBody = view.findViewById(R.id.sheet_history_body);
        keyInput = view.findViewById(R.id.sheet_key_input);
        valueInput = view.findViewById(R.id.sheet_value);
        valueLayout = view.findViewById(R.id.sheet_value_layout);
        boolSwitch = view.findViewById(R.id.sheet_bool);
        View boolRow = view.findViewById(R.id.sheet_bool_row);
        View numberRow = view.findViewById(R.id.sheet_number_row);
        MaterialCheckBox rebootBox = view.findViewById(R.id.sheet_reboot);
        MaterialCheckBox shortcutBox = view.findViewById(R.id.sheet_shortcut);
        MaterialButton saveButton = view.findViewById(R.id.sheet_save);
        MaterialButton cancelButton = view.findViewById(R.id.sheet_cancel);
        MaterialButton deleteButton = view.findViewById(R.id.sheet_delete);
        MaterialButton revertButton = view.findViewById(R.id.sheet_revert);
        ImageButton starButton = view.findViewById(R.id.sheet_star);
        ImageButton helpButton = view.findViewById(R.id.sheet_help);
        ImageButton copyButton = view.findViewById(R.id.sheet_copy);
        TextInputLayout keyLayout = view.findViewById(R.id.sheet_key_layout);

        title.setText(isNew ? R.string.sheet_new_title : R.string.sheet_edit_title);
        keyText.setText(key);
        keyText.setVisibility(isNew ? View.GONE : View.VISIBLE);
        keyLayout.setVisibility(isNew ? View.VISIBLE : View.GONE);
        boolean canFavorite = !isNew && Tables.isSettingsTable(tableType);
        helpButton.setVisibility(isNew ? View.GONE : View.VISIBLE);
        copyButton.setVisibility(isNew ? View.GONE : View.VISIBLE);
        riskyBadge.setVisibility(!isNew && RiskyKeys.isRisky(key) ? View.VISIBLE : View.GONE);
        starButton.setVisibility(canFavorite ? View.VISIBLE : View.GONE);

        // Spring the sheet's content in for a lively open.
        io.github.netodev66.util.UiUtils.playBouncyEntrance(view, 0.94f, 320L);
        io.github.netodev66.util.UiUtils.attachPressBounce(starButton);

        ValueType.Type type = ValueType.of(value);
        applyType(view, type, typeText, boolRow, numberRow);
        valueInput.setText(value);

        // Boolean: drive the text value from the switch and hide the text field.
        boolSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                valueInput.setText(isChecked ? "1" : "0"));

        // Number: steppers adjust the text field.
        MaterialButton minus = view.findViewById(R.id.sheet_minus);
        MaterialButton plus = view.findViewById(R.id.sheet_plus);
        MaterialButton minus10 = view.findViewById(R.id.sheet_minus_10);
        MaterialButton plus10 = view.findViewById(R.id.sheet_plus_10);
        minus.setOnClickListener(v -> stepValue(-1));
        plus.setOnClickListener(v -> stepValue(1));
        minus10.setOnClickListener(v -> stepValue(-10));
        plus10.setOnClickListener(v -> stepValue(10));

        if (canFavorite) {
            boolean favorite = FavoritesStore.isFavorite(requireContext(), tableName(), key);
            updateStar(starButton, favorite);
            starButton.setOnClickListener(v -> {
                boolean now = FavoritesStore.toggle(requireContext(), tableName(), key);
                updateStar(starButton, now);
            });
        }

        if (helpButton.getVisibility() == View.VISIBLE) {
            helpButton.setOnClickListener(v -> openHelp());
        }

        copyButton.setOnClickListener(v -> showCopyDialog());

        if (canSetOnReboot && (canEdit || canDelete)) {
            rebootBox.setVisibility(View.VISIBLE);
        }
        if (canCreateShortcut && (canEdit || canDelete)) {
            shortcutBox.setVisibility(View.VISIBLE);
        }

        if (!canEdit && !isNew) {
            valueInput.setKeyListener(null);
            valueInput.setFocusable(false);
        }
        deleteButton.setVisibility(canDelete ? View.VISIBLE : View.GONE);
        saveButton.setVisibility((canEdit || canCreate || isNew) ? View.VISIBLE : View.GONE);

        // History / revert for existing keys.
        if (!isNew) {
            HistoryStore.Entry latest = HistoryStore.latestFor(requireContext(), tableName(), key);
            if (latest != null && latest.oldValue != null) {
                historyHeader.setVisibility(View.VISIBLE);
                historyBody.setVisibility(View.VISIBLE);
                historyBody.setText(latest.oldValue + " \u2192 " + latest.newValue);
                revertButton.setVisibility(View.VISIBLE);
                revertButton.setOnClickListener(v -> confirmRevert(latest));
            }
        }

        saveButton.setOnClickListener(v -> {
            String newKey = isNew ? text(keyInput) : key;
            String newValue = text(valueInput);
            if (TextUtils.isEmpty(newKey)) {
                if (keyInput != null) keyInput.setError(getString(R.string.name));
                return;
            }
            if (AppPrefs.isConfirmRisky(requireContext()) && RiskyKeys.isRisky(newKey) && !isNew) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.risky_key_title)
                        .setMessage(getString(R.string.risky_key_message, newKey))
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.continue_anyway, (d, w) -> commit(newKey, newValue, rebootBox, shortcutBox))
                        .show();
            } else {
                commit(newKey, newValue, rebootBox, shortcutBox);
            }
        });

        cancelButton.setOnClickListener(v -> dismiss());
        deleteButton.setOnClickListener(v -> confirmDelete(rebootBox, shortcutBox));
    }

    private void applyType(@NonNull View view, @NonNull ValueType.Type type,
                           @NonNull MaterialTextView typeText, @NonNull View boolRow,
                           @NonNull View numberRow) {
        switch (type) {
            case BOOLEAN:
                typeText.setText(R.string.sheet_type_boolean);
                boolRow.setVisibility(View.VISIBLE);
                valueLayout.setVisibility(View.GONE);
                boolSwitch.setChecked("1".equals(value.trim()) || "true".equalsIgnoreCase(value.trim()));
                break;
            case NUMBER:
                typeText.setText(R.string.sheet_type_number);
                numberRow.setVisibility(View.VISIBLE);
                break;
            default:
                typeText.setText(R.string.sheet_type_text);
                break;
        }
        typeText.setVisibility(View.VISIBLE);
    }

    private void stepValue(long delta) {
        valueInput.setText(ValueType.adjust(text(valueInput), delta));
    }

    private void commit(@NonNull String newKey, @NonNull String newValue,
                        @NonNull MaterialCheckBox rebootBox, @NonNull MaterialCheckBox shortcutBox) {
        boolean risky = !isNew && RiskyKeys.isRisky(newKey);
        if (risky && AppPrefs.isAutoBackup(requireContext())) {
            // Snapshot on a background thread. It is a safety net, not a prerequisite, so the write
            // does not wait for it — and nothing here touches the fragment once it is dismissed.
            Context appContext = requireContext().getApplicationContext();
            String reason = newKey;
            BACKUP_EXECUTOR.submit(() -> {
                File snapshot = AutoBackup.snapshot(appContext, reason);
                if (snapshot != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            Toast.makeText(appContext,
                                    appContext.getString(R.string.auto_backup_saved, reason),
                                    Toast.LENGTH_SHORT).show());
                }
            });
        }
        finishCommit(newKey, newValue, canSetOnReboot && rebootBox.isChecked(),
                canCreateShortcut && shortcutBox.isChecked());
    }

    private void finishCommit(@NonNull String newKey, @NonNull String newValue,
                              boolean reboot, boolean shortcut) {
        String oldValue = value;
        if (listener != null) listener.onSave(newKey, newValue, isNew);
        HistoryStore.record(requireContext(), tableName(), newKey, oldValue, newValue);
        if (reboot) {
            BootUtils.add(requireContext(), new ActionItem(
                    isNew ? ActionResult.TYPE_CREATE : ActionResult.TYPE_UPDATE,
                    tableName(), newKey, newValue));
        }
        if (shortcut) {
            ShortcutUtils.displayShortcutTypeChooserDialog(
                    (androidx.fragment.app.FragmentActivity) requireActivity(),
                    new ActionItem(isNew ? ActionResult.TYPE_CREATE : ActionResult.TYPE_UPDATE,
                            tableName(), newKey, newValue));
        }
        if (listener != null) listener.onEdited();
        dismiss();
    }

    /** Offers to copy the key, the current value or the shell command that sets it. */
    private void showCopyDialog() {
        CharSequence[] options = {
                getString(R.string.copy_key),
                getString(R.string.copy_value),
                getString(R.string.copy_command)
        };
        String currentValue = text(valueInput);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.copy_title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        ClipboardUtils.copy(requireContext(), key, key);
                    } else if (which == 1) {
                        ClipboardUtils.copy(requireContext(), key, currentValue);
                    } else {
                        ClipboardUtils.copy(requireContext(), key,
                                Tables.shellCommand(tableType, key, currentValue));
                    }
                })
                .show();
    }

    private void confirmDelete(@NonNull MaterialCheckBox rebootBox, @NonNull MaterialCheckBox shortcutBox) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete)
                .setMessage(key)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    if (listener != null) listener.onDelete(key);
                    HistoryStore.record(requireContext(), tableName(), key, value, null);
                    if (canSetOnReboot && rebootBox.isChecked()) {
                        BootUtils.add(requireContext(), new ActionItem(
                                ActionResult.TYPE_DELETE, tableName(), key, null));
                    }
                    if (canCreateShortcut && shortcutBox.isChecked()) {
                        ShortcutUtils.displayShortcutTypeChooserDialog(
                                (androidx.fragment.app.FragmentActivity) requireActivity(),
                                new ActionItem(ActionResult.TYPE_DELETE, tableName(), key, null));
                    }
                    if (listener != null) listener.onEdited();
                    dismiss();
                })
                .show();
    }

    private void confirmRevert(@NonNull HistoryStore.Entry entry) {
        if (entry.oldValue == null) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.revert_title)
                .setMessage(getString(R.string.revert_message, entry.key, entry.oldValue))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.revert, (d, w) -> {
                    if (listener != null) listener.onSave(entry.key, entry.oldValue, false);
                    HistoryStore.record(requireContext(), tableName(), entry.key,
                            value, entry.oldValue);
                    Toast.makeText(requireContext(), R.string.reverted, Toast.LENGTH_SHORT).show();
                    if (listener != null) listener.onEdited();
                    dismiss();
                })
                .show();
    }

    private void updateStar(@NonNull ImageButton button, boolean favorite) {
        button.setImageResource(favorite ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
        button.setContentDescription(getString(
                favorite ? R.string.remove_favorite : R.string.add_favorite));
    }

    private void openHelp() {
        String prefix;
        switch (tableType) {
            case TableTypeInt.TABLE_SECURE:
                prefix = "settings put secure \"";
                break;
            case TableTypeInt.TABLE_GLOBAL:
                prefix = "settings put global \"";
                break;
            case TableTypeInt.TABLE_PROPERTIES:
                prefix = "setprop \"";
                break;
            case TableTypeInt.TABLE_JAVA:
                prefix = "java properties \"";
                break;
            case TableTypeInt.TABLE_ENV:
                prefix = "environment \"";
                break;
            default:
                prefix = "settings put system \"";
                break;
        }
        String url = "https://search.brave.com/search?q=android+" + prefix + Uri.encode(key) + "\"";
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ignored) {
        }
    }

    @NonNull
    private String tableName() {
        return settingsTableName();
    }

    @NonNull
    private String settingsTableName() {
        switch (tableType) {
            case TableTypeInt.TABLE_SECURE:
                return TableType.TABLE_SECURE;
            case TableTypeInt.TABLE_GLOBAL:
                return TableType.TABLE_GLOBAL;
            default:
                return TableType.TABLE_SYSTEM;
        }
    }

    @NonNull
    private static String text(@Nullable TextInputEditText editText) {
        if (editText == null) return "";
        Editable editable = editText.getText();
        return editable == null ? "" : editable.toString();
    }
}
