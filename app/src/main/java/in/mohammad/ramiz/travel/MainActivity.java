package in.mohammad.ramiz.travel;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import in.mohammad.ramiz.travel.R;
import in.mohammad.ramiz.travel.databinding.ActivityMainBinding;
import in.mohammad.ramiz.travel.ui.assistant.AssistantFragment;
import in.mohammad.ramiz.travel.ui.home.HomeFragment;
import in.mohammad.ramiz.travel.ui.packing.PackingFragment;
import in.mohammad.ramiz.travel.ui.route.RouteFragment;
import in.mohammad.ramiz.travel.ui.timeline.TimelineFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    (Map<String, Boolean> result) -> {
                        // Fragments react to location availability on their own.
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        requestRuntimePermissions();

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_explore) {
                show(new HomeFragment());
            } else if (id == R.id.nav_route) {
                show(new RouteFragment());
            } else if (id == R.id.nav_assistant) {
                show(new AssistantFragment());
            } else if (id == R.id.nav_packing) {
                show(new PackingFragment());
            } else if (id == R.id.nav_timeline) {
                show(new TimelineFragment());
            }
            return true;
        });

        if (savedInstanceState == null) {
            binding.bottomNav.setSelectedItemId(R.id.nav_explore);
        }
    }

    /** Used by HomeFragment to jump to route preview with a destination. */
    public void openRoute(double lat, double lng, String name) {
        binding.bottomNav.setSelectedItemId(R.id.nav_route);
        Fragment f = RouteFragment.newInstance(lat, lng, name);
        show(f);
    }

    public void openTab(int menuId) {
        binding.bottomNav.setSelectedItemId(menuId);
    }

    private void show(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void requestRuntimePermissions() {
        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
        perms.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (Build.VERSION.SDK_INT >= 33) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        permissionLauncher.launch(perms.toArray(new String[0]));
    }
}

