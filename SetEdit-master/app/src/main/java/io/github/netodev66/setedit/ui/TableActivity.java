package io.github.netodev66.setedit.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import io.github.netodev66.setedit.R;
import io.github.netodev66.setedit.TableTypeInt;

/** Hosts a single table browser inside a plain activity, so it can be opened from anywhere. */
public class TableActivity extends AppCompatActivity {

    private static final String EXTRA_TABLE = "table";

    public static Intent intent(@NonNull Context context, @TableTypeInt int tableType) {
        Intent intent = new Intent(context, TableActivity.class);
        intent.putExtra(EXTRA_TABLE, tableType);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table);
        if (savedInstanceState == null) {
            TableFragment fragment = TableFragment.newInstance(
                    getIntent().getIntExtra(EXTRA_TABLE, TableTypeInt.TABLE_SYSTEM));
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.table_host, fragment)
                    .commit();
        }
    }
}
