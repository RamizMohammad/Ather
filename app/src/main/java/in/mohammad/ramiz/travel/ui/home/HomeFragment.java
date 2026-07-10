package in.mohammad.ramiz.travel.ui.home;

import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import in.mohammad.ramiz.travel.core.Result;
import in.mohammad.ramiz.travel.databinding.FragmentHomeBinding;
import in.mohammad.ramiz.travel.domain.model.PlaceResult;
import in.mohammad.ramiz.travel.domain.model.Recommendation;
import in.mohammad.ramiz.travel.MainActivity;
import in.mohammad.ramiz.travel.util.FormatUtil;

import java.util.Calendar;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HomeFragment extends Fragment {

    private static final long SEARCH_DEBOUNCE_MS = 350;
    private static final int MIN_QUERY_LENGTH = 3;

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private boolean weatherRequested;
    private PlaceSuggestionAdapter suggestionAdapter;
    /** True while a GO-button search is in flight (navigate to top result). */
    private boolean navigateOnResult;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    @Nullable
    private Runnable pendingSearch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding.textGreeting.setText(greeting());
        viewModel.startLocation();

        viewModel.getLocation().observe(getViewLifecycleOwner(), this::onLocation);
        viewModel.getWeather().observe(getViewLifecycleOwner(), w -> {
            if (w == null) return;
            String chip = (w.conditionText != null ? w.conditionText.toUpperCase() : "")
                    + ", " + FormatUtil.temp(w.tempC) + "C";
            binding.chipWeather.setText(chip.trim());
            if (w.stale) binding.chipWeather.append(" (offline)");
        });
        viewModel.getInsights().observe(getViewLifecycleOwner(), this::renderInsight);
        viewModel.getSearchResults().observe(getViewLifecycleOwner(), this::onSearchResults);

        suggestionAdapter = new PlaceSuggestionAdapter(this::openPlace);
        binding.recyclerSuggestions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerSuggestions.setAdapter(suggestionAdapter);

        binding.inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                scheduleLiveSearch(s != null ? s.toString().trim() : "");
            }
        });

        binding.inputSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_GO) {
                submitSearch();
                return true;
            }
            return false;
        });
        binding.buttonExplore.setOnClickListener(v -> submitSearch());
    }

    /** Debounced as-you-type search; results appear as a closest-first list. */
    private void scheduleLiveSearch(String query) {
        if (pendingSearch != null) searchHandler.removeCallbacks(pendingSearch);
        if (query.length() < MIN_QUERY_LENGTH) {
            hideSuggestions();
            return;
        }
        pendingSearch = () -> {
            navigateOnResult = false;
            viewModel.search(query);
        };
        searchHandler.postDelayed(pendingSearch, SEARCH_DEBOUNCE_MS);
    }

    private void submitSearch() {
        String query = binding.inputSearch.getText() != null
                ? binding.inputSearch.getText().toString().trim() : "";
        if (query.isEmpty()) {
            Toast.makeText(requireContext(), "Where are you going?", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pendingSearch != null) searchHandler.removeCallbacks(pendingSearch);
        binding.buttonExplore.setEnabled(false);
        navigateOnResult = true;
        viewModel.search(query);
    }

    private void onSearchResults(Result<List<PlaceResult>> result) {
        binding.buttonExplore.setEnabled(true);
        List<PlaceResult> places = result.getData();
        if (result.getStatus() == Result.Status.ERROR || places == null || places.isEmpty()) {
            hideSuggestions();
            if (navigateOnResult) {
                navigateOnResult = false;
                Toast.makeText(requireContext(),
                        "Couldn't find that place. Check your connection and try again.",
                        Toast.LENGTH_LONG).show();
            }
            return;
        }
        if (navigateOnResult) {
            navigateOnResult = false;
            openPlace(places.get(0)); // closest match
            return;
        }
        suggestionAdapter.submit(places);
        binding.recyclerSuggestions.setVisibility(View.VISIBLE);
    }

    private void openPlace(PlaceResult place) {
        hideSuggestions();
        hideKeyboard();
        ((MainActivity) requireActivity()).openRoute(place.lat, place.lng, place.name);
    }

    private void hideSuggestions() {
        if (binding == null) return;
        suggestionAdapter.clear();
        binding.recyclerSuggestions.setVisibility(View.GONE);
    }

    private void hideKeyboard() {
        InputMethodManager imm = ContextCompat.getSystemService(
                requireContext(), InputMethodManager.class);
        if (imm != null && binding != null) {
            imm.hideSoftInputFromWindow(binding.inputSearch.getWindowToken(), 0);
        }
    }

    private void onLocation(Location location) {
        if (location == null) return;
        viewModel.updateSearchBias(location);
        if (weatherRequested) return;
        weatherRequested = true;
        viewModel.refreshWeather(location.getLatitude(), location.getLongitude());
    }

    private void renderInsight(List<Recommendation> recs) {
        if (recs == null || recs.isEmpty()) {
            binding.textInsightTitle.setText("All clear for today.");
            binding.textInsightBody.setText(
                    "No weather or travel conditions need your attention right now.");
            return;
        }
        Recommendation top = recs.get(0);
        binding.textInsightTitle.setText(top.title);
        binding.textInsightBody.setText(top.message);
    }

    private String greeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) return "Good Morning.";
        if (hour < 18) return "Good Afternoon.";
        return "Good Evening.";
    }

    @Override
    public void onDestroyView() {
        if (pendingSearch != null) searchHandler.removeCallbacks(pendingSearch);
        super.onDestroyView();
        binding = null;
    }
}

