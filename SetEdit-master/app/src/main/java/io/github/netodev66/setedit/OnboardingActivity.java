package io.github.netodev66.setedit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textview.MaterialTextView;

import io.github.netodev66.setedit.data.AppPrefs;

/** A short three-page intro shown once, explaining what the app does and how access works. */
public class OnboardingActivity extends AppCompatActivity {

    private static final int[] ICONS = {
            R.drawable.ic_table_system, R.drawable.ic_shield, R.drawable.ic_nav_backup
    };
    private static final int[] TITLES = {
            R.string.onboard_title_1, R.string.onboard_title_2, R.string.onboard_title_3
    };
    private static final int[] BODIES = {
            R.string.onboard_body_1, R.string.onboard_body_2, R.string.onboard_body_3
    };

    private ViewPager2 pager;
    private MaterialButton nextButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        pager = findViewById(R.id.onboard_pager);
        nextButton = findViewById(R.id.onboard_next);
        MaterialButton skipButton = findViewById(R.id.onboard_skip);
        TabLayout dots = findViewById(R.id.onboard_dots);

        pager.setAdapter(new PageAdapter());
        new TabLayoutMediator(dots, pager, true, false, (tab, position) -> {
        }).attach();

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                nextButton.setText(position == TITLES.length - 1
                        ? R.string.onboard_start : R.string.onboard_next);
            }
        });

        nextButton.setOnClickListener(v -> {
            int current = pager.getCurrentItem();
            if (current < TITLES.length - 1) {
                pager.setCurrentItem(current + 1, true);
            } else {
                finishOnboarding();
            }
        });
        skipButton.setOnClickListener(v -> finishOnboarding());
    }

    private void finishOnboarding() {
        AppPrefs.setOnboarded(this, true);
        finish();
    }

    private static final class PageAdapter extends RecyclerView.Adapter<PageAdapter.Holder> {
        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_onboard_page, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            holder.icon.setImageResource(ICONS[position]);
            holder.title.setText(TITLES[position]);
            holder.body.setText(BODIES[position]);
        }

        @Override
        public int getItemCount() {
            return TITLES.length;
        }

        static final class Holder extends RecyclerView.ViewHolder {
            final ImageView icon;
            final MaterialTextView title;
            final MaterialTextView body;

            Holder(@NonNull View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.onboard_icon);
                title = itemView.findViewById(R.id.onboard_title);
                body = itemView.findViewById(R.id.onboard_body);
            }
        }
    }
}
