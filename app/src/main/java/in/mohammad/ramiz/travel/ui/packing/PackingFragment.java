package in.mohammad.ramiz.travel.ui.packing;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import in.mohammad.ramiz.travel.databinding.FragmentPackingBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PackingFragment extends Fragment {

    private FragmentPackingBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPackingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        PackingViewModel viewModel = new ViewModelProvider(this).get(PackingViewModel.class);

        PackingAdapter adapter = new PackingAdapter(
                (id, packed) -> viewModel.setPacked(id, packed),
                viewModel::dismiss);
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recycler.setAdapter(adapter);

        viewModel.getItems().observe(getViewLifecycleOwner(), items -> {
            adapter.submit(items);
            boolean empty = items == null || items.isEmpty();
            binding.textEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

